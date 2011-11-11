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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.PluginTransfer;

import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.search.Search;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;
import de.uni_mannheim.swt.codeconjurer.ui.controller.ResultDoubleClickListener;
import de.uni_mannheim.swt.codeconjurer.ui.dnd.ResultTreeDragListener;
import de.uni_mannheim.swt.codeconjurer.ui.view.PluginUI;
import de.uni_mannheim.swt.codeconjurer.ui.view.providers.ResultContentProvider;
import de.uni_mannheim.swt.codeconjurer.ui.view.providers.ResultLabelProvider;

/**
 * @author Werner Janjic
 * 
 */
public class ResultTree {

	private Logger logger = Logger.getLogger(ResultTree.class);

	private TreeViewer treeViewer;
	private Tree tree;
	private ResultContentProvider resultContentProvider;
	private ResultLabelProvider resultLabelProvider;

	/**
	 * Creates a new ResultTree object
	 * 
	 * @param parent
	 * @param style
	 */
	public ResultTree(Composite parent, int style,
			ISelectionChangedListener listener) {
		tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tree.setHeaderVisible(true);

		TreeColumn colResource = new TreeColumn(tree, SWT.LEFT);
		colResource.setText("Resource");
		colResource.setWidth(200);
		colResource.setMoveable(true);
		colResource.setToolTipText("Shows the retrieved components in the "
				+ "order provided by the merobase server.");

		TreeColumn colLicense = new TreeColumn(tree, SWT.LEFT);
		colLicense.setText("License");
		colLicense.setWidth(100);
		colLicense.setMoveable(true);
		colLicense
				.setToolTipText("The resource's license or 'no license' if not parseable / unknown license.");

		TreeColumn colTested = new TreeColumn(tree, SWT.LEFT);
		colTested.setText("JUnit");
		colTested.setWidth(50);
		colTested.setMoveable(true);
		colTested
				.setToolTipText("This column will be coloured green, if the artifact passed a server-side "
						+ "JUnit test during a test-driven search.");

		treeViewer = new TreeViewer(tree);
		treeViewer.setUseHashlookup(true);
		resultContentProvider = new ResultContentProvider();
		resultLabelProvider = new ResultLabelProvider();
		treeViewer.setContentProvider(resultContentProvider);
		treeViewer.setLabelProvider(resultLabelProvider);
		treeViewer.setInput(CodeConjurer.getInstance().getActiveEditorSearch());

		treeViewer.addSelectionChangedListener(listener);

		// Enable Drag & Drop Support
		int ops = DND.DROP_DEFAULT;
		Transfer[] transfers = new Transfer[] { PluginTransfer.getInstance(),
				TextTransfer.getInstance() };
		treeViewer.addDragSupport(ops, transfers, new ResultTreeDragListener(
				treeViewer));

		// Handle Double Clicks on items
		treeViewer.addDoubleClickListener(new ResultDoubleClickListener());
	}

	/**
	 * Refresh the results
	 */
	public void refresh() {
		IWorkbenchWindow window = PluginUI.getWindow();
		if (window == null)
			return;
		Shell shell = window.getShell();
		if (shell == null)
			return;
		Display display = shell.getDisplay();
		if (display == null)
			return;
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (treeViewer != null) {
					logger.debug("Update TreeViewer");
					Search oldInput = (Search) treeViewer.getInput();
					Search newInput = CodeConjurer.getInstance()
							.getActiveEditorSearch();
					Tree tree = treeViewer.getTree();
					if (tree != null && !tree.isDisposed()) {
						TreeItem[] selection = tree.getSelection();
						Object[] expandedElements = treeViewer
								.getExpandedElements();
						if (newInput != oldInput) {
							treeViewer.setInput(newInput);
						} else {
							newInput = null;
						}
						treeViewer.refresh();
						// If there is no input we are finished.
						if (newInput == null) {
							return;
						}
						try {
							if (selection != null && selection.length > 0) {
								// Set selection only if no item is disposed
								// (resource change, e.g.)
								boolean disposed = false;
								for (TreeItem item : selection) {
									if (item.isDisposed())
										disposed = true;
								}
								if (!disposed) {
									treeViewer.getTree()
											.setSelection(selection);

								}
							}

							if (expandedElements != null
									&& expandedElements.length > 0) {
								logger.debug("Expand elements again");
								treeViewer
										.setExpandedElements(expandedElements);
							}
						} catch (Exception e) {
							CrashReporter.reportException(e);
							logger.debug("Exception setting tree status: "
									+ e.getLocalizedMessage());
							logger.debug("Selection is " + selection);
							logger.debug("Expanded Elements are "
									+ expandedElements);
						}
					}
				}
			}
		});
	}

	/**
	 * Returns the first selected item from the tree if any
	 * 
	 * @return
	 */
	public TreeItem getSelectedElement() {
		Tree tree = treeViewer.getTree();
		if (!tree.isDisposed()) {
			TreeItem[] selection = tree.getSelection();
			if (selection.length > 0)
				return selection[0];
			else
				return null;
		} else {
			return null;
		}
	}

	/**
	 * Collapses all nodes of the tree starting at root.
	 */
	public void collapseAll() {
		treeViewer.collapseAll();
	}

	/**
	 * Expands all nodes of the tree starting at root.
	 */
	public void expandAll() {
		treeViewer.expandAll();
	}

}
