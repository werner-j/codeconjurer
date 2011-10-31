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
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.part.IDropActionDelegate;

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
		IPackageFragment pkg = (IPackageFragment) target;
		try {
			pkg.createCompilationUnit("Stack.java", new String((byte[]) source),
					true, null);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("Received " + source.toString() + " for "
				+ target.toString());
		return false;
	}

}
