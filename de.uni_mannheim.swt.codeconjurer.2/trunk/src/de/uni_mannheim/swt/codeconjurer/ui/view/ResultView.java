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
package de.uni_mannheim.swt.codeconjurer.ui.view;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEvent;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEventListener;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.Result;
import de.uni_mannheim.swt.codeconjurer.domain.search.Search;
import de.uni_mannheim.swt.codeconjurer.ui.dnd.BodyDeclarationTransfer;
import de.uni_mannheim.swt.codeconjurer.ui.dnd.JavaEditorDropListener;
import de.uni_mannheim.swt.codeconjurer.ui.listener.UIEvent;
import de.uni_mannheim.swt.codeconjurer.ui.listener.UIListener;
import de.uni_mannheim.swt.codeconjurer.ui.view.elements.CodePreview;
import de.uni_mannheim.swt.codeconjurer.ui.view.elements.ResultTree;

public class ResultView extends ViewPart implements SearchEventListener,
		UIListener, IPartListener2 {

	/* Variables */
	private Label statusLabel;
	private CodePreview preview;
	private ResultTree resultTree;

	private final String TITLE = "Reuse View";

	private Logger logger = Logger.getLogger(ResultView.class);

	public ResultView() {
		super();
	}

	public void setFocus() {
		statusLabel.setFocus();
	}

	/**
	 * Create View for results.
	 */
	public void createPartControl(Composite parent) {
		this.showBusy(true);
		PluginUI.addUIListener(this);
		createToolbarActions();

		/* A small status bar on top */
		Composite top = new Composite(parent, SWT.FILL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginBottom = 0;
		layout.numColumns = 1;
		top.setLayout(layout);
		Composite banner = new Composite(top, SWT.NONE);
		banner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL,
				GridData.VERTICAL_ALIGN_BEGINNING, true, false));
		banner.setLayout(layout);
		statusLabel = new Label(banner, SWT.NONE);
		statusLabel.setText("Code Conjurer");

		/* SashForm for content */
		SashForm sashForm = new SashForm(top, SWT.MULTI);
		sashForm.setBackground(parent.getDisplay().getSystemColor(
				SWT.COLOR_WHITE));
		sashForm.setOrientation(SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		ISelectionChangedListener listener = new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				BodyDeclaration selected = (BodyDeclaration) selection
						.getFirstElement();

				// Show the source code of the selection
				if (preview != null) {
					if (selected != null) {
						preview.setCode(selected.toString());
					} else {
						preview.setCode("");
					}
				}

			}
		};

		resultTree = new ResultTree(sashForm, SWT.BORDER, listener);

		preview = new CodePreview(sashForm);

		if (preview != null) {
			sashForm.setWeights(new int[] { 2, 3 });
		}

		CodeConjurer.getInstance().addSearchEventListener(this);
		getSite().getPage().addPartListener(this);

		onEvent(UIEvent.CREATED);
		this.showBusy(false);
	}

	/**
	 * Create small icons in the view
	 */
	private void createToolbarActions() {
		IActionBars bars = getViewSite().getActionBars();
		Action collapseAction = new Action() {
			@Override
			public void run() {
				logger.debug("Collapse Results");
				resultTree.collapseAll();
			}
		};
		collapseAction.setText("Collapse Results");
		collapseAction.setImageDescriptor(Activator
				.getImageDescriptor("icons/collapse.png"));
		bars.getToolBarManager().add(collapseAction);
		Action expandAction = new Action() {
			@Override
			public void run() {
				logger.debug("Expand Results");
				resultTree.expandAll();
			}
		};
		expandAction.setText("Expand Results");
		expandAction.setImageDescriptor(Activator
				.getImageDescriptor("icons/expand.png"));
		bars.getToolBarManager().add(expandAction);
		Action performSearchAction = new Action() {
			@Override
			public void run() {
				logger.debug("Perform a search");
				CodeConjurer.getInstance().search(false);
			}
		};
		performSearchAction.setText("Search Reusable Code");
		performSearchAction.setImageDescriptor(Activator
				.getImageDescriptor("icons/code_conjurer_m.png"));
		bars.getToolBarManager().add(performSearchAction);
		Action refreshAction = new Action() {
			@Override
			public void run() {
				logger.debug("Refresh View");
				onEvent(UIEvent.REFRESH);
			}
		};
		refreshAction.setText("Refresh Result View");
		refreshAction.setImageDescriptor(Activator
				.getImageDescriptor("icons/refresh.png"));
		bars.getToolBarManager().add(refreshAction);
	}

	@Override
	public void onEvent(SearchEvent event) {
		logger.debug("Event: " + event);
		final ResultView view = this;
		// An update of the tree should happen at all changes except for a
		// successfully downloaded source (to prevent flickering)
		if (event != SearchEvent.SOURCE_ADDED) {
			logger.debug("Refresh ResultTres");
			PluginUI.getWindow().getWorkbench().getDisplay()
					.asyncExec(new Runnable() {
						@Override
						public void run() {
							resultTree.refresh();
							TreeItem selection = resultTree
									.getSelectedElement();
							if (selection != null)
								preview.setCode(selection.getData().toString());
							// Indicate that something has happened and add a
							// star to the view's title
							String name = view.getPartName();
							if (!name.contains("*")
									&& !(getSite().getPage()
											.isPartVisible(view))) {
								view.setPartName("* " + view.getPartName());
							}
						}
					});
			logger.debug("Update Statusline");
			if (event == SearchEvent.SERVERERROR) {
				updateStatus("A server error occured during the search. Check your settings and contact the administrator if this problem persists.");
			} else if (event == SearchEvent.INVALID_USER) {
				updateStatus("Invalid username / password. Please check your preference settings.");
			} else {
				updateStatus();
			}
		}
	}

	@Override
	public void onEvent(UIEvent event) {
		this.showBusy(true);
		final ResultView view = this;
		if (event == UIEvent.REFRESH) {
			logger.debug("Refresh ResultTres");
			PluginUI.getWindow().getWorkbench().getDisplay()
					.asyncExec(new Runnable() {
						@Override
						public void run() {
							resultTree.refresh();
							TreeItem selection = resultTree
									.getSelectedElement();
							if (selection != null)
								preview.setCode(selection.getData().toString());
							// Indicate that something has happened and add a
							// star to the view's title
							String name = view.getPartName();
							if (!name.contains("*")
									&& !(getSite().getPage()
											.isPartVisible(view))) {
								view.setPartName("* " + view.getPartName());
							}
						}
					});
			logger.debug("Update Statusline");
			updateStatus();
		}
		this.showBusy(false);
	}

	@Override
	public void partActivated(final IWorkbenchPartReference partRef) {
		String id = partRef.getId();
		if (id.equals("org.eclipse.jdt.ui.CompilationUnitEditor")) {
			IEditorPart editor = PluginUI.getActiveEditor();
			Control ctrl = (Control) editor.getAdapter(Control.class);
			DropTarget dropTarget = (DropTarget) ctrl
					.getData(DND.DROP_TARGET_KEY);
			if (dropTarget != null) {
				try {
					// Add drop listener to editor
					ArrayList<DropTargetListener> dropListeners = new ArrayList<DropTargetListener>(
							Arrays.asList(dropTarget.getDropListeners()));
					if (!dropListeners.contains(JavaEditorDropListener
							.getInstance())) {
						logger.debug("Add drop listener to "
								+ dropTarget.toString());
						dropTarget.addDropListener(JavaEditorDropListener
								.getInstance());
					}

					// Add new transfer type to editor
					ArrayList<Transfer> transfers = new ArrayList<Transfer>(
							Arrays.asList(dropTarget.getTransfer()));
					if (!transfers.contains(BodyDeclarationTransfer
							.getInstance())) {
						transfers.add(BodyDeclarationTransfer.getInstance());
						dropTarget.setTransfer(transfers
								.toArray(new Transfer[transfers.size()]));
					}
				} catch (Exception t) {
					logger.debug("Could not register drop service: "
							+ t.getMessage());
					t.printStackTrace();
				}
			}
			updateStatus();
			resultTree.refresh();
			TreeItem selection = resultTree.getSelectedElement();
			if (selection != null)
				preview.setCode(selection.getData().toString());
		}
		if (partRef.getId().equals(
				"de.uni_mannheim.swt.codeconjurer.views.ResultView")) {
			if (partRef.getTitle().contains("*")) {
				partRef.getPage().getWorkbenchWindow().getShell().getDisplay()
						.asyncExec(new Runnable() {
							@Override
							public void run() {
								((ResultView) partRef.getPart(true))
										.setPartName(TITLE);
							}
						});
			}
		}
	}

	@Override
	public void partBroughtToTop(final IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(
				"de.uni_mannheim.swt.codeconjurer.views.ResultView")) {
			if (partRef.getTitle().contains("*")) {
				partRef.getPage().getWorkbenchWindow().getShell().getDisplay()
						.asyncExec(new Runnable() {
							@Override
							public void run() {
								((ResultView) partRef.getPart(true))
										.setPartName(TITLE);
							}
						});
			}
		}
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub

	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub

	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub

	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub

	}

	@Override
	public void partVisible(final IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(
				"de.uni_mannheim.swt.codeconjurer.views.ResultView")) {
			if (partRef.getTitle().contains("*")) {
				partRef.getPage().getWorkbenchWindow().getShell().getDisplay()
						.asyncExec(new Runnable() {
							@Override
							public void run() {
								((ResultView) partRef.getPart(true))
										.setPartName(TITLE);
							}
						});
			}
		}
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub

	}

	public void updateStatus() {
		updateStatus("");
	}

	/**
	 * Set the statusline of the result view
	 * 
	 * @param message
	 */
	private void updateStatus(final String msg) {
		PluginUI.getWindow().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				// Request from user to set preferences
				boolean noServer = Activator.getDefault().getPreferenceStore()
						.getString(PreferenceConstants.P_SERVER).equals("");
				boolean noUsername = Activator.getDefault()
						.getPreferenceStore()
						.getString(PreferenceConstants.P_USERNAME).equals("");
				boolean noPassword = Activator.getDefault()
						.getPreferenceStore()
						.getString(PreferenceConstants.P_PASSWORD).equals("");
				String message = "";
				if (msg.equals("")) {
					if (noServer || noUsername || noPassword) {
						message = "Please set up preferences first. Go to Eclipse -> Preferences -> Code Conjurer.";
					} else {
						Search search = CodeConjurer.getInstance()
								.getActiveEditorSearch();

						if (search != null) {
							Result result = null;
							int results = 0;
							switch (search.getType()) {
							case Search.INTERFACE:
								result = search.getSearchResult();
								results = result.getResultItems().length;
								if (results > 0) {
									message = (results
											+ " results found. "
											+ result.getNumberOfSuccessfullyFetchedSources()
											+ " items successfully retrieved. :: Result created " + result
											.getCreationDate());
								}
								break;

							case Search.TEST:
								result = search.getSearchResult();
								results = result.getResultItems().length;
								if (results > 0) {
									message = (results
											+ " candidates passed test. "
											+ result.getNumberOfSuccessfullyFetchedSources()
											+ " items successfully retrieved. :: Result created " + result
											.getCreationDate());
								}
								break;

							default:
								message = "No results.";
								break;
							}
						} else {
							message = ("No search results available.");
						}
					}
				} else {
					message = msg;
				}
				if (statusLabel != null && !statusLabel.isDisposed()) {
					statusLabel.setText("[Code Conjurer] " + message);
					statusLabel.pack();
					statusLabel.getParent().pack();
				}
			}
		});
		logger.debug("Status refreshed!");
	}
}