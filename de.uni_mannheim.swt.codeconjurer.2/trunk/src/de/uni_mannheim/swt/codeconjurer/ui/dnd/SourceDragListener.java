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
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.widgets.TreeItem;

import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;

/**
 * @author Werner Janjic
 * 
 */
public class SourceDragListener implements DragSourceListener {

	private TreeViewer viewer;
	private Logger logger = Logger.getLogger(SourceDragListener.class);

	private TreeItem selection;
	private BodyDeclaration selectedElement;

	public SourceDragListener(TreeViewer viewer) {
		logger.debug("SourceDragListener registered!");
		this.viewer = viewer;
	}

	/**
	 * If something is selected a drag can start
	 */
	@Override
	public void dragStart(DragSourceEvent event) {
		if (CodeConjurer.getInstance().isBackgroundAgentEnabled()) {
			CodeConjurer.getInstance().getBackgroundAgentListener()
					.ignoreNextEvent(true);
		}
		selection = viewer.getTree().getSelection()[0];
		if (selection != null) {
			selectedElement = (BodyDeclaration) selection.getData();
			event.doit = (selectedElement != null);
		}
	}

	/**
	 * The source code of the selection
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		/*
		 * String licText = selectedElement.getProperty(ResultProperty.LICENSE
		 * .name()) + ""; String license = ""; if
		 * (!licText.equals("no license")) { license =
		 * "// Code released under the terms of the " + licText + "\r\n"; }
		 * transferString = license + "// " +
		 * selectedElement.getProperty(ResultProperty.SHORT_URL.name()) + "\r\n"
		 * + selectedElement.toString(); if
		 * (TextTransfer.getInstance().isSupportedType(event.dataType)) {
		 * event.data = transferString; } logger.debug("Transfer data:\r\n" +
		 * event.data);
		 */
		event.data = new BodyDeclaration[] { selectedElement };
	}

	@Override
	public void dragFinished(DragSourceEvent event) {
		if (event.doit) {
			logger.debug("Drag of "
					+ viewer.getTree().getSelection()[0].getText()
					+ " finished. Format the Sourcecode properly...");
			// setContents();
		}
	}

}
