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
package de.uni_mannheim.swt.codeconjurer.ui.dnd;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.part.IDropActionDelegate;

import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;
import de.uni_mannheim.swt.codeconjurer.ui.threads.CompilationUnitDropJob;
import de.uni_mannheim.swt.codeconjurer.ui.threads.PackageFragmentDropJob;

/**
 * @author Werner Janjic
 * 
 */
public class PluginDropActionDelegate implements IDropActionDelegate {

	private Logger logger = Logger.getLogger(PluginDropActionDelegate.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.IDropActionDelegate#run(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public boolean run(Object source, Object target) {
		try {
			if (target instanceof IPackageFragment) {
				return insertTypeDeclaration(source, (IPackageFragment) target);
			}
			if (target instanceof ICompilationUnit) {
				return insertTypeDeclaration(source, (ICompilationUnit) target);
			}
		} catch (JavaModelException e) {
			logger.debug("Do not overwrite.");
			return false;
		} catch (Exception e) {
			logger.debug("Problem during dropping to target.");
			CrashReporter.reportException(e);
			e.printStackTrace();
			return false;
		}
		return false;
	}

	private boolean insertTypeDeclaration(Object source, ICompilationUnit target)
			throws JavaModelException {
		CompilationUnitDropJob dropJob = new CompilationUnitDropJob(
				(byte[]) source, target);
		dropJob.schedule();
		return true;
	}

	/**
	 * Checks if the provided source is a type declaration and the target is an
	 * PackageFragment and inserts the type into the package.
	 * 
	 * @param source
	 * @param target
	 * @return
	 * @throws JavaModelException
	 * @throws Exception
	 */
	private boolean insertTypeDeclaration(Object source, IPackageFragment target)
			throws JavaModelException, Exception {
		// To make the UI stay responsive, we do this in a job
		PackageFragmentDropJob dropJob = new PackageFragmentDropJob((byte[]) source,
				(IPackageFragment) target);
		dropJob.schedule();
		return true;
	}

}
