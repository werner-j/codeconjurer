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
package de.uni_mannheim.swt.codeconjurer.ui.threads.helpers;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Display;

/**
 * @author Werner Janjic
 * 
 */
public class AskForClassNameThread implements Runnable {

	private Logger logger = Logger.getLogger(this.getClass());

	private String className = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		InputDialog dlg = new InputDialog(
				Display.getCurrent().getActiveShell(), "Create new Java class",
				"Enter a name for the enclosing Java class:", "", null);
		if (dlg.open() == InputDialog.OK) {
			className = dlg.getValue();
			logger.debug("Classname: " + dlg.getValue());
		} else {
			logger.debug("Do not create a class.");
		}
	}

	/**
	 * Returns the value the user entered in the dialog. If the user decided not
	 * to create a class, this will be <tt>null</tt>.
	 * 
	 * @return
	 */
	public String getClassName() {
		return className;
	}

}
