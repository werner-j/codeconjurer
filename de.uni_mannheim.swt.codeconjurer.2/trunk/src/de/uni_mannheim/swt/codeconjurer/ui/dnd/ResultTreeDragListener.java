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
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.PluginTransferData;

import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;
import de.uni_mannheim.swt.codeconjurer.ui.listener.UIEvent;
import de.uni_mannheim.swt.codeconjurer.ui.view.PluginUI;

/**
 * @author Werner Janjic
 * 
 */
public class ResultTreeDragListener implements DragSourceListener {

	private Logger logger = Logger.getLogger(ResultTreeDragListener.class);

	private TreeViewer viewer;
	private TreeItem[] selection;

	/**
	 * Standard constructor for this Listener
	 * 
	 * @param viewer
	 */
	public ResultTreeDragListener(TreeViewer viewer) {
		super();
		this.viewer = viewer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DragSourceListener#dragStart(org.eclipse.swt.dnd.
	 * DragSourceEvent)
	 */
	@Override
	public void dragStart(DragSourceEvent event) {
		selection = viewer.getTree().getSelection();
		if (selection == null || selection.length == 0) {
			event.doit = false;
		}
		BodyDeclaration selectedElement = (BodyDeclaration) selection[0]
				.getData();
		logger.debug("URI: "
				+ selectedElement.getProperty(ResultProperty.URI.name()));
	}

	/**
	 * Set the transfer data. A method should be dropped to the text editor,
	 * thus we need a TextTransfer. A type should go into the package explorer,
	 * where we need a PluginTransfer.
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		logger.debug(event.dataType + " requested by drop target.");

		try {
			BodyDeclaration selectedElement = (BodyDeclaration) selection[0]
					.getData();
			if (PluginTransfer.getInstance().isSupportedType(event.dataType)) {
				// For a PluginTransfer, we transmit the URI of the element.
				// The DropListener is responsible for handling this.
				Object uri = selectedElement.getProperty(ResultProperty.URI
						.name());
				if (uri != null)
					event.data = new PluginTransferData(
							"de.uni_mannheim.swt.codeconjurer.ui.dnd.pluginDropAction",
							(((String) uri).getBytes()));
				logger.debug("PluginTransfer triggered for " + uri);
			} else if (TextTransfer.getInstance().isSupportedType(
					event.dataType)) {
				// TextTransfer simply transfers the selected code element.
				String sourceLink = "@origin \r\n * "
						+ selectedElement.getProperty(ResultProperty.SHORT_URL
								.name()) + "";
				String data = selectedElement.toString();
				if (data.startsWith("/**") && data.contains("*/")) {
					event.data = data.substring(0, data.indexOf("*/") - 2)
							+ "\r\n * \r\n * " + sourceLink + "\r\n *"
							+ data.substring(data.indexOf("*/"), data.length());
				} else {
					event.data = "/** " + sourceLink + "\r\n */"
							+ selectedElement.toString();
				}
				logger.debug("TextTransfer triggered");
			}
		} catch (Exception e) {
			CrashReporter.reportException(e);
			logger.debug("Could not set drag data: " + e.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.dnd.DragSourceListener#dragFinished(org.eclipse.swt.dnd
	 * .DragSourceEvent)
	 */
	@Override
	public void dragFinished(DragSourceEvent event) {
		if (event.doit) {
			logger.debug("Drag of "
					+ viewer.getTree().getSelection()[0].getText()
					+ " finished.");
		}
		// If we don't do this, the next drag may take the URL of the parent
		// instead of the selection.
		PluginUI.fireEvent(UIEvent.REFRESH);
	}

}
