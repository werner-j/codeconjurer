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
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
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
 * @author Werner Janjic
 * 
 */
public class TypeDropJob extends Job {

	private Logger logger = Logger.getLogger(TypeDropJob.class);

	private IPackageFragment target;
	private byte[] source;

	public TypeDropJob(byte[] source, IPackageFragment target) {
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
			IPackageFragment pkg = (IPackageFragment) target;
			String uri = new String(source);
			Search search = CodeConjurer.getInstance().getActiveEditorSearch();
			BodyDeclaration selectedElement = search.getSearchResult()
					.find(uri);
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
			}
		} catch (Exception e) {
			logger.debug(e.toString());
			CrashReporter.reportException(e);
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

}
