/*
 * Copyright (c) 2007-2011
 * University of Mannheim, Chair for Software-Engineering
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 */
package de.uni_mannheim.swt.codeconjurer.ui.view;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEvent;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEventListener;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.Result;
import de.uni_mannheim.swt.codeconjurer.ui.view.elements.CodePreview;
import de.uni_mannheim.swt.codeconjurer.ui.view.elements.ResultTree;

public class ResultView extends ViewPart implements SearchEventListener,
		IPartListener2 {

	private Label statusLabel;
	private CodePreview preview;
	private ResultTree resultTree;

	private Logger logger = Logger.getLogger(ResultView.class);

	/* Variables */
	// private Label status;

	public ResultView() {
		super();
	}

	public void setFocus() {
	}

	/**
	 * Create View for results.
	 */
	public void createPartControl(Composite parent) {

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
					if (selected != null)
						preview.setCode(selected.toString());
					else
						preview.setCode("");
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

		onEvent(null);
	}

	@Override
	public void onEvent(SearchEvent event) {
		logger.debug("Event: " + event);
		final ResultView view = this;
		if (event != null) {
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
			Result result = CodeConjurer.getInstance().getActiveSearch()
					.getSearchResult();
			setStatus(result.getResultItems().length + " Results Found. "
					+ result.getNumberOfSuccessfullyFetchedSources()
					+ " items successfully fetched. :: Result created "
					+ result.getCreationDate());
		}

		// Request from user to set preferences
		boolean noServer = Activator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.P_SERVER).equals("");
		boolean noUsername = Activator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.P_USERNAME).equals("");
		boolean noPassword = Activator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.P_PASSWORD).equals("");

		if (noServer || noUsername || noPassword) {
			preview.setCode("Please set Code Conjurer " + "Preferences first.");
		}
	}

	@Override
	public void partActivated(final IWorkbenchPartReference partRef) {
		if (partRef.getId().equals("org.eclipse.jdt.ui.CompilationUnitEditor")) {
			resultTree.refresh();
			TreeItem selection = resultTree.getSelectedElement();
			if (selection != null)
				preview.setCode(selection.getData().toString());
		}
		if (partRef.getId().equals(
				"com.merobase.app.codeconjurer.views.ResultView")) {
			if (partRef.getTitle().contains("*")) {
				partRef.getPage().getWorkbenchWindow().getShell().getDisplay()
						.asyncExec(new Runnable() {
							@Override
							public void run() {
								String title = partRef.getTitle().substring(
										partRef.getTitle().indexOf("*") + 1,
										partRef.getTitle().length());
								((ResultView) partRef.getPart(true))
										.setPartName(title);
							}
						});
			}
		}
	}

	@Override
	public void partBroughtToTop(final IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(
				"com.merobase.app.codeconjurer.views.ResultView")) {
			if (partRef.getTitle().contains("*")) {
				partRef.getPage().getWorkbenchWindow().getShell().getDisplay()
						.asyncExec(new Runnable() {
							@Override
							public void run() {
								String title = partRef.getTitle().substring(
										partRef.getTitle().indexOf("*") + 1,
										partRef.getTitle().length());
								((ResultView) partRef.getPart(true))
										.setPartName(title);
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
				"com.merobase.app.codeconjurer.views.ResultView")) {
			if (partRef.getTitle().contains("*")) {
				partRef.getPage().getWorkbenchWindow().getShell().getDisplay()
						.asyncExec(new Runnable() {
							@Override
							public void run() {
								String title = partRef.getTitle().substring(
										partRef.getTitle().indexOf("*") + 1,
										partRef.getTitle().length());
								((ResultView) partRef.getPart(true))
										.setPartName(title);
							}
						});
			}
		}
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		// TODO Auto-generated method stub

	}

	/**
	 * Set the statusline of the result view
	 * 
	 * @param message
	 */
	public void setStatus(final String message) {
		PluginUI.getWindow().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				statusLabel.setText("Code Conjurer :: " + message);
				statusLabel.getParent().pack();
			}
		});
	}

}