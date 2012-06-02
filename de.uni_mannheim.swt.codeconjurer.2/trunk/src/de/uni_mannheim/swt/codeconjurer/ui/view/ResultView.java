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

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEvent;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEventListener;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.Result;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;
import de.uni_mannheim.swt.codeconjurer.domain.search.Search;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;
import de.uni_mannheim.swt.codeconjurer.ui.dnd.JavaEditorDropListener;
import de.uni_mannheim.swt.codeconjurer.ui.listener.TreeSelectionChangedListener;
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
		askUserForUDC();

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
		Composite statusBar = new Composite(top, SWT.NONE);
		statusBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL,
				GridData.VERTICAL_ALIGN_BEGINNING, true, false));
		statusBar.setLayout(layout);
		statusLabel = new Label(statusBar, SWT.NONE);
		statusLabel.setText("Code Conjurer");

		/* SashForm for content */
		SashForm sashForm = new SashForm(top, SWT.MULTI);
		sashForm.setBackground(parent.getDisplay().getSystemColor(
				SWT.COLOR_WHITE));
		sashForm.setOrientation(SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		resultTree = new ResultTree(sashForm);

		if (preview != null) {
			sashForm.setWeights(new int[] { 2, 3 });
		}

		preview = new CodePreview(sashForm);

		ISelectionChangedListener listener = new TreeSelectionChangedListener(
				preview);

		resultTree.setListener(listener);

		CodeConjurer.getInstance().addSearchEventListener(this);
		getSite().getPage().addPartListener(this);

		onEvent(UIEvent.CREATED);
		this.showBusy(false);
	}

	/**
	 * This is used to greet the user at first launch
	 */
	private void askUserForUDC() {
		boolean firstlaunch = Activator.getDefault().getPreferenceStore()
				.getBoolean(PreferenceConstants.P_FIRST_LAUNCH);
		// At first launch ask for UDC and crash reporting
		if (firstlaunch) {
			Activator.getDefault().getPreferenceStore()
					.setValue(PreferenceConstants.P_FIRST_LAUNCH, false);
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					MessageDialog
							.openInformation(
									PlatformUI.getWorkbench()
											.getActiveWorkbenchWindow()
											.getShell(),
									"Code Conjurer First Start",
									"Please set a username and password in Eclipse->Preferences->Code Conjurer. \r\n\r\n"
											+ "Code Conjurer collects anonymous crash and usage data for "
											+ "the developers to improve its "
											+ "reliability and results. You can turn off this debug "
											+ "feature in the preferences dialog.");
				}
			});
		}
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

	}

	@Override
	public void onEvent(SearchEvent event) {
		logger.debug("Event: " + event);
		final ResultView view = this;
		// An update of the tree should happen at all changes except for a
		// successfully downloaded source (to prevent flickering)
		if (event != SearchEvent.SOURCE_ADDED
				&& !PluginUI.getWindow().getWorkbench().getDisplay()
						.isDisposed()) {
			logger.debug("Refresh ResultTree");
			PluginUI.getWindow().getWorkbench().getDisplay()
					.asyncExec(new Runnable() {
						@Override
						public void run() {
							setPreview();
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
		}
		if (event == SearchEvent.SOURCE_ADDED) {
			logger.debug("Refresh ResultTree");
			PluginUI.getWindow().getWorkbench().getDisplay()
					.asyncExec(new Runnable() {
						@Override
						public void run() {
							setPreview();
						}
					});
		}

		logger.debug("Update Statusline");
		if (event == SearchEvent.SERVERERROR) {
			updateStatus("A server error occured during the search. Check your settings and contact the administrator if this problem persists.");
		} else if (event == SearchEvent.INVALID_USER) {
			updateStatus("Invalid username / password. Please check your preference settings.");
		} else if (event == SearchEvent.NO_RESULTS) {
			updateStatus("No results available. Perhaps you could try another search method (see preferences).");
		} else {
			updateStatus();
		}

	}

	/**
	 * Set the preview window to the current selection. If adapter modus is on,
	 * the adapter is shown.
	 */
	protected void setPreview() {
		resultTree.refresh();
		TreeItem selection = resultTree.getSelectedElement();
		if (selection != null) {
			BodyDeclaration selected = (BodyDeclaration) selection.getData();
			// Show the source code of the selection
			if (preview != null) {
				if (selected != null) {
					String previewCode = "";
					String adapterCode = "";
					if (selected.getNodeType() == BodyDeclaration.TYPE_DECLARATION) {
						previewCode = (String) selected
								.getProperty(ResultProperty.RAW_SOURCE.name());
						adapterCode = (String) selected
								.getProperty(ResultProperty.TEST_RESULT.name());
					} else {
						previewCode = selected.toString();
						adapterCode = "// Adapter not available";
					}
					boolean showAdapter = Activator.getDefault()
							.getPreferenceStore()
							.getBoolean(PreferenceConstants.P_SHOW_ADAPTER);
					String searchKind = selected.getProperty(
							ResultProperty.SEARCH_KIND.name()).toString();
					if (showAdapter
							&& searchKind.equals(String
									.valueOf(Search.TEST_DRIVEN_SEARCH))) {
						preview.setCode(adapterCode);
					} else {
						preview.setCode(previewCode);
					}
				} else {
					preview.setCode("");
				}
			}
		}
	}

	/**
	 * This method is used to update the view and the status line
	 * 
	 * @param event
	 */
	@Override
	public void onEvent(UIEvent event) {
		this.showBusy(true);
		final ResultView view = this;
		if (event == UIEvent.REFRESH) {
			logger.debug("Refresh ResultTree");
			IWorkbenchWindow window = PluginUI.getWindow();
			if (window != null) {
				IWorkbench wb = window.getWorkbench();
				if (wb != null) {
					wb.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							setPreview();
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
				}
			}
			logger.debug("Update Statusline");
			updateStatus();
		}
		this.showBusy(false);
	}

	@Override
	public void partActivated(final IWorkbenchPartReference partRef) {
		if (partRef != null) {
			String id = partRef.getId();
			if (id.equals("org.eclipse.jdt.ui.CompilationUnitEditor")) {
				try {
					IEditorPart editor = PluginUI.getActiveEditor();
					if (editor != null) {
						Control ctrl = (Control) editor
								.getAdapter(Control.class);
						DropTarget dropTarget = (DropTarget) ctrl
								.getData(DND.DROP_TARGET_KEY);
						if (dropTarget != null) {
							try {
								// Add drop listener to editor
								logger.debug("Add drop listener to "
										+ dropTarget.toString());
								dropTarget
										.addDropListener(JavaEditorDropListener.getInstance());

							} catch (Exception e) {
								CrashReporter.reportException(e);
								logger.debug("Could not register drop service: "
										+ e.getMessage());
								e.printStackTrace();
							}
						}
					}
				} catch (Exception e) {
					CrashReporter.reportException(e);
				}
				updateStatus();
				setPreview();
			}
		}
	}

	@Override
	public void partBroughtToTop(final IWorkbenchPartReference partRef) {
		partActivated(partRef);
		partVisible(partRef);
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		PluginUI.fireEvent(UIEvent.REFRESH);
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		PluginUI.fireEvent(UIEvent.REFRESH);
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		partActivated(partRef);
		partVisible(partRef);
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		PluginUI.fireEvent(UIEvent.REFRESH);
	}

	@Override
	public void partVisible(final IWorkbenchPartReference partRef) {
		if (partRef != null) {
			partActivated(partRef);
			if (partRef.getId().equals(
					"de.uni_mannheim.swt.codeconjurer.views.ResultView")) {
				if (partRef.getTitle().contains("*")) {
					partRef.getPage().getWorkbenchWindow().getShell()
							.getDisplay().asyncExec(new Runnable() {
								@Override
								public void run() {
									((ResultView) partRef.getPart(true))
											.setPartName(TITLE);
								}
							});
				}
			}
		}
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		PluginUI.fireEvent(UIEvent.REFRESH);
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
		IWorkbenchWindow window = PluginUI.getWindow();
		if (window != null) {
			IWorkbench wb = window.getWorkbench();
			if (wb != null) {
				wb.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						// Request from user to set preferences
						boolean noServer = Activator.getDefault()
								.getPreferenceStore()
								.getString(PreferenceConstants.P_SERVER)
								.equals("");
						boolean noUsername = Activator.getDefault()
								.getPreferenceStore()
								.getString(PreferenceConstants.P_USERNAME)
								.equals("");
						boolean noPassword = Activator.getDefault()
								.getPreferenceStore()
								.getString(PreferenceConstants.P_PASSWORD)
								.equals("");
						StringBuilder message = new StringBuilder();
						if (msg.equals("")) {
							if (noServer || noUsername || noPassword) {
								message.append("Please set up preferences first. Go to Eclipse -> Preferences -> Code Conjurer.");
							} else {
								Search search = CodeConjurer.getInstance()
										.getActiveEditorSearch();
								ActionContributionItem showAdapterAction = (ActionContributionItem) getViewSite()
										.getActionBars()
										.getToolBarManager()
										.find("de.uni_mannheim.swt.codeconjurer.showAdapterAction");

								if (search != null) {
									Result result = null;
									int results = 0;
									switch (search.getKind()) {
									case Search.STANDARD_SEARCH:
										showAdapterAction.getAction()
												.setEnabled(false);
										result = search.getSearchResult();
										results = result.getResultItems().length;
										if (results > 0) {
											message.append(results
													+ " results found. "
													+ result.getNumberOfSuccessfullyFetchedSources()
													+ " items successfully downloaded. :: Result created "
													+ result.getCreationDate());
										}
										break;

									case Search.TEST_DRIVEN_SEARCH:
										showAdapterAction.getAction()
												.setEnabled(true);
										result = search.getSearchResult();
										int passes = result
												.getNumberOfSuccessfulTests();
										int retrieved = result
												.getNumberOfSuccessfullyFetchedSources();
										long time = search.getDuration();
										String duration;
										if (time / Math.pow(10, 9) < 120) {
											duration = String.valueOf(time
													/ Math.pow(10, 9));
											duration = duration.substring(0,
													duration.indexOf("."));
											duration += " s";
										} else {
											duration = String
													.valueOf((time / Math.pow(
															10, 9)) / 60);
											duration = duration.substring(0,
													duration.indexOf(".") + 2);
											duration += " min";
										}
										results = result.getResultItems().length;
										if (search.getState() == Job.RUNNING) {
											message.append("Testing in progress");
											message.append(" :: Time elapsed "
													+ duration);
										}
										if (retrieved > 0) {
											message.append(" :: "
													+ passes
													+ " of "
													+ results
													+ " candidates passed test. "
													+ retrieved
													+ " items successfully downloaded.");
											if (search.isFinished()) {
												message.append(" :: Result created "
														+ result.getCreationDate());
												message.append(" :: Result created in "
														+ duration);
											}
										}
										break;

									default:
										message.append("No results.");
										break;
									}
								} else {
									if (showAdapterAction != null) {
										IAction action = showAdapterAction
												.getAction();
										if (action != null) {
											action.setEnabled(false);
											message.append("No search results available.");
										}
									}
								}
							}
						} else {
							message.append(msg);
						}
						if (statusLabel != null && !statusLabel.isDisposed()) {
							statusLabel.setText("[Code Conjurer] "
									+ message.toString());
							statusLabel.pack();
							statusLabel.getParent().pack();
						}
					}
				});
			}
		}
		logger.debug("Status refreshed!");
	}

}