/*
 * Copyright (c) 2007-2011
 * University of Mannheim, Chair for Software-Engineering
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Werner Janjic -- initial development and documentation
 */
package de.uni_mannheim.swt.codeconjurer.ui.threads;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.actions.FormatAllAction;
import org.eclipse.jdt.ui.actions.OrganizeImportsAction;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;
import de.uni_mannheim.swt.codeconjurer.domain.search.Search;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;
import de.uni_mannheim.swt.codeconjurer.ui.threads.helpers.AskForClassNameThread;
import de.uni_mannheim.swt.codeconjurer.ui.view.PluginUI;

/**
 * @author Werner Janjic
 * 
 */
public class PackageFragmentDropJob extends Job {

	private Logger logger = Logger.getLogger(PackageFragmentDropJob.class);

	private IPackageFragment target;
	private byte[] source;

	public PackageFragmentDropJob(byte[] source, IPackageFragment target) {
		super("Insert dropped asset");
		this.source = source;
		this.target = target;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
	 * IProgressMonitor)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			final IPackageFragment pkg = (IPackageFragment) target;
			String uri = new String(source);
			Search search = CodeConjurer.getInstance().getActiveEditorSearch();
			final BodyDeclaration selectedElement = search.getSearchResult()
					.find(uri);

			// Insert class
			if (selectedElement.getNodeType() == BodyDeclaration.TYPE_DECLARATION) {
				monitor.beginTask("Insert class into package", 2);
				TypeDeclaration typeDec = (TypeDeclaration) selectedElement;

				String adapterCode = null;

				// If this is a result from test-driven search and an adapter is
				// present, use it.
				if (Integer.parseInt((String) typeDec
						.getProperty(ResultProperty.SEARCH_KIND.name())) == Search.TEST_DRIVEN_SEARCH) {
					adapterCode = (String) typeDec
							.getProperty(ResultProperty.TEST_RESULT.name());
					if (adapterCode.contains("<adapter>false</adapter>")) {
						adapterCode = null; // Only if an adapter is necessary,
											// we leave this value set
					}
				}

				String name = typeDec.getName().toString();
				String sourceCode = (String) selectedElement
						.getProperty(ResultProperty.RAW_SOURCE.name());

				/** Indicates if an adapter was created */
				boolean adapted = false;

				/* Insert the adapter if the user wishes */
				if (Activator.getDefault().getPreferenceStore()
						.getBoolean(PreferenceConstants.P_SHOW_ADAPTER)) {
					if (adapterCode != null) {
						adapterCode = adapterCode
								.replace(
										"merobase_auto_generated_package_for_adaptation",
										pkg.getElementName());

						monitor.subTask("Insert adapter class");
						// Find out the name of the adapter
						ASTParser parser = ASTParser.newParser(AST.JLS3);
						parser.setSource(adapterCode.toCharArray());
						CompilationUnit astRoot = (CompilationUnit) parser
								.createAST(null);
						List<TypeDeclaration> typeList = astRoot.types();
						if (typeList != null && typeList.size() > 0) {
							TypeDeclaration adapterType = (TypeDeclaration) typeList
									.get(0);
							insertType(pkg, adapterType.getName().toString(),
									adapterCode, monitor);
						}
						monitor.worked(1);

						monitor.subTask("Check for adaptee package");
						IPackageFragmentRoot pkgRoot = (IPackageFragmentRoot) pkg
								.getParent();
						String pkgname = pkg.getElementName();
						// pkgRoot.open(monitor);
						IPackageFragment adapteePkg = pkgRoot
								.createPackageFragment(pkgname + "."
										+ "adaptee", true, monitor);

						monitor.subTask("Insert adaptee with functionality");
						/* Insert the adaptee class */
						insertType(adapteePkg, name, sourceCode, monitor);
						monitor.worked(1);
						adapted = true;
					}
				}

				// Insert the result without adapter
				if (!adapted) {
					insertType(pkg, name, sourceCode, monitor);
					monitor.worked(1);
				}

			}
			if (selectedElement.getNodeType() == BodyDeclaration.METHOD_DECLARATION) {
				MethodDeclaration methodDec = (MethodDeclaration) selectedElement;
				IWorkbenchWindow window = PluginUI.getWindow();
				if (window == null)
					return Status.CANCEL_STATUS;
				Shell shell = window.getShell();
				if (shell == null)
					return Status.CANCEL_STATUS;
				Display display = shell.getDisplay();
				if (display == null)
					return Status.CANCEL_STATUS;

				AskForClassNameThread askForClass = new AskForClassNameThread();
				display.syncExec(askForClass);
				String className = askForClass.getClassName();

				if (className != null) {
					if (className.contains(".")) {
						className = className.substring(0,
								className.indexOf("."));
					}

					// Create new CompilationUnit
					try {
						AST ast = AST.newAST(AST.JLS3);
						CompilationUnit unit = ast.newCompilationUnit();
						PackageDeclaration packageDeclaration = ast
								.newPackageDeclaration();
						packageDeclaration.setName(ast.newSimpleName(pkg
								.getElementName()));
						unit.setPackage(packageDeclaration);
						TypeDeclaration type = ast.newTypeDeclaration();
						Javadoc comment = ast.newJavadoc();
						TagElement tag = ast.newTagElement();
						TextElement text = ast.newTextElement();
						text.setText("Generated class for method "
								+ methodDec.getName());
						tag.fragments().add(text);
						comment.tags().add(tag);
						type.setJavadoc(comment);
						type.setInterface(false);
						type.modifiers()
								.add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
						type.setName(ast.newSimpleName(className));
						type.bodyDeclarations().add(
								BodyDeclaration.copySubtree(type.getAST(),
										methodDec));
						unit.types().add(type);

						String source = unit.toString();
						pkg.createCompilationUnit(className + ".java", source,
								true, null);
					} catch (Exception e) {
						logger.error("Problem when creating new file for method: "
								+ e.toString());
						CrashReporter.reportException(e,
								"Problem when creating new file for method.",
								null);
					}
				}

			}

		} catch (Exception e) {
			logger.debug(e.toString());
			CrashReporter.reportException(e);
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	private void insertType(IPackageFragment pkg, String name,
			String sourceCode, final IProgressMonitor monitor) throws Exception {
		// If a CompilatonUnit with the same name exists and is
		// opened, we must close it before overwrite.
		ICompilationUnit icu = pkg.getCompilationUnit(name + ".java");
		if (icu != null && icu.isWorkingCopy()) {
			icu.close();
		}

		// Create the compilationUnit
		final ICompilationUnit icu2 = pkg.createCompilationUnit(name + ".java",
				sourceCode, Activator.getDefault().getPreferenceStore()
						.getBoolean(PreferenceConstants.P_OVERWRITE_ON_INSERT),
				monitor);

		icu2.createPackageDeclaration(pkg.getElementName(), monitor);

		IFile input = (IFile) icu2.getResource();
		final IEditorInput editorInput = new FileEditorInput(input);
		final IEditorDescriptor desc = PlatformUI.getWorkbench()
				.getEditorRegistry().getDefaultEditor(input.getName());

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					IEditorPart editor = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage()
							.openEditor(editorInput, desc.getId());

					// Organize imports if necessary
					if (Activator.getDefault().getPreferenceStore()
							.getBoolean(PreferenceConstants.P_ORGANIZE_IMPORTS)) {
						OrganizeImportsAction organize = new OrganizeImportsAction(
								editor.getEditorSite());
						organize.run(icu2);
					}

					// Format code properly
					if (Activator.getDefault().getPreferenceStore()
							.getBoolean(PreferenceConstants.P_FORMAT_ON_INSERT)) {
						FormatAllAction format = new FormatAllAction(editor
								.getEditorSite());
						format.runOnMultiple(new ICompilationUnit[] { icu2 });
					}
					icu2.commitWorkingCopy(true, monitor);
				} catch (Exception e) {
					logger.debug(e.toString());
					CrashReporter.reportException(e);
				}
			}
		});
		icu2.close();
	}

}
