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
package de.uni_mannheim.swt.codeconjurer.ui.controller;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeSelection;

import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;

/**
 * @author Werner Janjic
 * 
 */
public class ResultDoubleClickListener implements IDoubleClickListener {

	private Logger logger = Logger.getLogger(ResultDoubleClickListener.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse
	 * .jface.viewers.DoubleClickEvent)
	 */
	@Override
	public void doubleClick(DoubleClickEvent event) {
		TreeSelection selection = (TreeSelection) event.getSelection();
		BodyDeclaration selectedElement = (BodyDeclaration) selection
				.getFirstElement();
		logger.debug("Selected element's source: \r\n"
				+ selectedElement.toString());
		logger.debug("URI: "
				+ selectedElement.getProperty(ResultProperty.URI.name()));
	}
}
