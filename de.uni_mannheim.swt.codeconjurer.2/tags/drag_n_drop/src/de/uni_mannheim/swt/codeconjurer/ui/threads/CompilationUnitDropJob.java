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

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.actions.FormatAllAction;
import org.eclipse.jdt.ui.actions.OrganizeImportsAction;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;
import de.uni_mannheim.swt.codeconjurer.domain.search.Search;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;

/**
 * This job handles elements dropped on a CompilationUnit object
 * 
 * @author Werner Janjic
 * 
 */
public class CompilationUnitDropJob extends Job {

	private Logger logger = Logger.getLogger(CompilationUnitDropJob.class);

	private ICompilationUnit target;
	private byte[] source;

	public CompilationUnitDropJob(byte[] source, ICompilationUnit target) {
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
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			IPackageFragment pkg = (IPackageFragment) target.getParent();
			String uri = new String(source);
			Search search = CodeConjurer.getInstance().getActiveEditorSearch();
			BodyDeclaration selectedElement = search.getSearchResult()
					.find(uri);

			// Insert method
			if (selectedElement.getNodeType() == BodyDeclaration.METHOD_DECLARATION) {
				boolean overwrite = Activator.getDefault().getPreferenceStore()
						.getBoolean(PreferenceConstants.P_OVERWRITE_ON_INSERT);
				ICompilationUnit cpu = target.getWorkingCopy(null);
				// creation of DOM/AST from an ICompilationUnit
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setSource(cpu);
				CompilationUnit astRoot = (CompilationUnit) parser
						.createAST(null);

				// creation of ASTRewrite
				astRoot.recordModifications();
				IMethod method = null;
				String content = createPreambule(selectedElement) + "\r\n"
						+ selectedElement.toString();
				try {
					method = astRoot.getTypeRoot().findPrimaryType()
							.createMethod(content, null, overwrite, null);
				} catch (JavaModelException e) {
					logger.debug("Method could not be created: "
							+ e.getLocalizedMessage());
				}

				if (method != null) {
					if (overwrite) {
						IMethod[] methods = astRoot.getTypeRoot()
								.findPrimaryType().findMethods(method);
						for (int i = 0; i < methods.length - 1; i++) {
							methods[i].delete(false, null);
						}
					}
				}

				// update of the compilation unit
				cpu.getBuffer().setContents(
						astRoot.getTypeRoot().findPrimaryType()
								.getCompilationUnit().getSource());
				cpu.reconcile(ICompilationUnit.NO_AST, false, null, null);
				cpu.commitWorkingCopy(false, null);
				return Status.OK_STATUS;
			}

			// Insert class
			if (selectedElement.getNodeType() == BodyDeclaration.TYPE_DECLARATION) {
				TypeDeclaration typeDec = (TypeDeclaration) selectedElement;
				String name = typeDec.getName().toString();
				String sourceCode = (String) selectedElement
						.getProperty(ResultProperty.RAW_SOURCE.name());

				// If a CompilatonUnit with the same name exists and is
				// opened, we must close it before overwrite.
				ICompilationUnit icu = pkg.getCompilationUnit(name + ".java");
				if (icu != null && icu.isWorkingCopy()) {
					icu.close();
				}

				// Create the compilationUnit
				final ICompilationUnit icu2 = pkg
						.createCompilationUnit(
								name + ".java",
								sourceCode,
								Activator
										.getDefault()
										.getPreferenceStore()
										.getBoolean(
												PreferenceConstants.P_OVERWRITE_ON_INSERT),
								null);

				icu2.createPackageDeclaration(pkg.getElementName(), null);

				IFile input = (IFile) icu2.getResource();
				final IEditorInput editorInput = new FileEditorInput(input);
				final IEditorDescriptor desc = PlatformUI.getWorkbench()
						.getEditorRegistry().getDefaultEditor(input.getName());

				PlatformUI.getWorkbench().getDisplay()
						.asyncExec(new Runnable() {
							@Override
							public void run() {
								try {
									IEditorPart editor = PlatformUI
											.getWorkbench()
											.getActiveWorkbenchWindow()
											.getActivePage()
											.openEditor(editorInput,
													desc.getId());

									// Organize imports if necessary
									if (Activator
											.getDefault()
											.getPreferenceStore()
											.getBoolean(
													PreferenceConstants.P_ORGANIZE_IMPORTS)) {
										OrganizeImportsAction organize = new OrganizeImportsAction(
												editor.getEditorSite());
										organize.run(icu2);
									}

									// Format code properly
									if (Activator
											.getDefault()
											.getPreferenceStore()
											.getBoolean(
													PreferenceConstants.P_FORMAT_ON_INSERT)) {
										FormatAllAction format = new FormatAllAction(
												editor.getEditorSite());
										format.runOnMultiple(new ICompilationUnit[] { icu2 });
									}
								} catch (Exception e) {
									logger.debug(e.toString());
									CrashReporter.reportException(e);
								}
							}
						});
				return Status.OK_STATUS;
			}
		} catch (Exception e) {
			e.printStackTrace();
			CrashReporter.reportException(e);
			return Status.CANCEL_STATUS;
		}
		return Status.CANCEL_STATUS;
	}

	/**
	 * Creates a preambule string for the insertion with source and license
	 * information
	 * 
	 * @param bodyDeclaration
	 */
	private String createPreambule(BodyDeclaration bodyDeclaration) {
		String content = "";
		String licenseType = (String) bodyDeclaration
				.getProperty(ResultProperty.LICENSE.name());
		if (licenseType != null && !licenseType.equals("no license")) {
			content += "// Released under the terms of the " + licenseType
					+ "\r\n";
		}
		content += "// Origin: "
				+ bodyDeclaration.getProperty(ResultProperty.SHORT_URL.name());
		return content;
	}

}