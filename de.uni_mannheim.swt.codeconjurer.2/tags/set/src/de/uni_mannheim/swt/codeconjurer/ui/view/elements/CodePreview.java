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
package de.uni_mannheim.swt.codeconjurer.ui.view.elements;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;

import de.java2html.Java2Html;

/**
 * @author Werner Janjic
 * 
 */
public class CodePreview {

	private Browser codeBrowser;
	private Logger logger = Logger.getLogger(CodePreview.class);

	public CodePreview(Composite parent, int style) {
		codeBrowser = new Browser(parent, style);
	}

	public CodePreview(Composite parent) {
		codeBrowser = new Browser(parent, SWT.None);
	}

	/**
	 * Display highlighted code
	 * 
	 * @param code
	 *            A String containing the Java Code (formatted)
	 */
	public void setCode(final String code) {
		codeBrowser.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				String actualContent = codeBrowser.getText();
				if (code != null
						&& !Java2Html.convertToHtml(code).equals(actualContent)) {
					codeBrowser.setText(Java2Html.convertToHtml(code), false);
					logger.debug("Preview of: " + code);
				}
				codeBrowser.update();
			}
		});
	}

}
