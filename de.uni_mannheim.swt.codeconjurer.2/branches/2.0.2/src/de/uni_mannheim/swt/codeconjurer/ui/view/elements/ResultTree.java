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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
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

import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.search.Search;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;
import de.uni_mannheim.swt.codeconjurer.ui.dnd.SourceDragListener;
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
		Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tree.setHeaderVisible(true);
		TreeColumn colResource = new TreeColumn(tree, SWT.LEFT);
		colResource.setText("Resource");
		colResource.setWidth(250);
		colResource.setMoveable(true);
		colResource.setToolTipText("Shows the retrieved components in the "
				+ "order provided by the merobase server.");

		treeViewer = new TreeViewer(tree);
		treeViewer.setUseHashlookup(true);
		resultContentProvider = new ResultContentProvider();
		resultLabelProvider = new ResultLabelProvider();
		treeViewer.setContentProvider(resultContentProvider);
		treeViewer.setLabelProvider(resultLabelProvider);
		treeViewer.setInput(CodeConjurer.getInstance().getActiveSearch());

		treeViewer.addSelectionChangedListener(listener);

		// Enable Drag & Drop Support
		int ops = DND.DROP_COPY;
		Transfer[] transfers = new Transfer[] { TextTransfer.getInstance() };
		treeViewer.addDragSupport(ops, transfers, new SourceDragListener(
				treeViewer));

		// Handle Double Clicks on items
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				// TODO Auto-generated method stub

			}
		});
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
				logger.debug("Refresh " + treeViewer);
				if (treeViewer != null) {
					Search oldInput = (Search) treeViewer.getInput();
					Search newInput = CodeConjurer.getInstance()
							.getActiveSearch();
					Tree tree = treeViewer.getTree();
					if (tree != null && !tree.isDisposed()) {
						TreeItem[] selection = tree.getSelection();
						Object[] expandedElements = treeViewer
								.getExpandedElements();
						if (newInput != oldInput) {
							treeViewer.setInput(newInput);
						}
						treeViewer.refresh();
						// If there is no input we are finished.
						if (newInput == null) {
							return;
						}
						try {
							if (selection != null && selection.length > 0)
								treeViewer.getTree().setSelection(selection);
							if (expandedElements != null
									&& expandedElements.length > 0)
								treeViewer
										.setExpandedElements(expandedElements);
						} catch (Exception e) {
							CrashReporter.reportException(e);
							logger.info("Exception setting tree status: "
									+ e.getMessage());
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
}
