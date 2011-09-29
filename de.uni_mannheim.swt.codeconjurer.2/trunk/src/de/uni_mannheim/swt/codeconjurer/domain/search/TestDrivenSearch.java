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
package de.uni_mannheim.swt.codeconjurer.domain.search;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorPart;

import com.merotronics.merobase.ws.action.ResultBean;
import com.merotronics.merobase.ws.client.util.WSConnection;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEvent;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultItem;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;
import de.uni_mannheim.swt.codeconjurer.ui.view.PluginUI;

public class TestDrivenSearch extends Search {

	long eta = -1;
	private int successfullyTested = -1;
	private int completelyTested = -1;
	private int candidates = -1;

	public TestDrivenSearch(IEditorPart editor, String name, Query query) {
		super(editor, name, query);
		super.type = Search.TEST;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		String serverLocation = Activator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.P_SERVER);
		String username = Activator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.P_USERNAME);
		String password = Activator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.P_PASSWORD);
		int numResults = Integer.parseInt(Activator.getDefault()
				.getPreferenceStore().getString(PreferenceConstants.P_RESULTS));

		try {
			WSConnection ws = new WSConnection(serverLocation);
			String session = "";
			logger.debug("Initialize search for " + numResults
					+ " components at " + serverLocation + ".");
			String queryString = query.getQuery();
			if (queryString.equals("")) {
				notifySearchEventListeners(SearchEvent.ERROR);
				return Status.CANCEL_STATUS;
			}
			session = ws.initComponentSearch(queryString, username, password,
					numResults);
			logger.debug("Received session id: " + session);

			// Check for invalid login
			if (ws.isLoginFailed()) {
				notifySearchEventListeners(SearchEvent.INVALID_USER);
				return Status.CANCEL_STATUS;
			}

			// Show error message from server
			if (session == null) {
				notifySearchEventListeners(SearchEvent.ERROR);
				return Status.CANCEL_STATUS;
			}

			notifySearchEventListeners(SearchEvent.TEST_STARTED);

			int size = ws.getResultSize();
			logger.debug("Test " + size + " candidates...");
			monitor.beginTask("Testing...", numResults);

			// Wait for results
			int loop = 0;
			int tested = 0;

			while (!ws.isFinished()) {
				eta = ws.getTimeLeft();
				successfullyTested = ws.getSuccessfullyTested();
				completelyTested = ws.getCompletelyTested();
				candidates = ws.getResultSize();
				if (monitor.isCanceled()) {
					logger.debug("Enable searching again.");
					notifySearchEventListeners(SearchEvent.CANCELLED);
					return Status.CANCEL_STATUS;
				}
				logger.debug("Search still in progress... " + completelyTested
						+ " tested of " + candidates + " candidates. Successful: " + successfullyTested);
				// Sleep timer increases with every iteration to a 10s maximum.
				int sleep = Math.min(loop++, 3);
				logger.debug("Sleep for " + sleep + " seconds.");
				try {
					TimeUnit.SECONDS.sleep(sleep);
				} catch (InterruptedException e) {
					notifySearchEventListeners(SearchEvent.CANCELLED);
					return Status.CANCEL_STATUS;
				}
				int inc = 0;
				if (ws.getCompletelyTested() > tested) {
					inc = completelyTested - tested;
					tested = completelyTested;
				}
				logger.debug("Tested: " + tested + " / Increment: " + inc);
				monitor.worked(inc);
			}
			// Store results
			ArrayList<ResultBean> results = ws.getResults(session);
			logger.debug("Webservice returned " + results.size()
					+ " results from Merobase.");
			result.setResultList(results);
			notifySearchEventListeners(SearchEvent.RESULT_ADDED);

			monitor.beginTask("Fetch Sourcecode", result.size());
			logger.debug("Beginn fetching sourcecode for " + result.size()
					+ " results.");

			for (ResultItem r : result.getResultItems()) {
				String source = null;
				try {
					source = ws.componentSource(
							r.getProperty(ResultProperty.SHORT_URL), username,
							password, 10);
				} catch (Exception e) {
					source = "/** Source could not be fetched from "
							+ r.getProperty(ResultProperty.SHORT_URL) + " */";
					logger.debug("Could not retrieve sourcecode for "
							+ r.getProperty(ResultProperty.SHORT_URL));
					e.printStackTrace();
					notifySearchEventListeners(SearchEvent.SERVERERROR);
					// return Status.CANCEL_STATUS;
				}
				result.addSource(r.getProperty(ResultProperty.SHORT_URL),
						source);
				notifySearchEventListeners(SearchEvent.SOURCE_ADDED);
				monitor.worked(1);
				if (monitor.isCanceled()) {
					logger.debug("Enable searching again.");
					notifySearchEventListeners(SearchEvent.CANCELLED);
					return Status.CANCEL_STATUS;
				}
			}
			// Show Test-Driven Results in foreground
			PluginUI.showRecommendationsView(false);
			notifySearchEventListeners(SearchEvent.FINISHED);
			return Status.OK_STATUS;
		} catch (Throwable e) {
			logger.debug("Problem during search: " + e.getMessage());
			notifySearchEventListeners(SearchEvent.SERVERERROR);
			e.printStackTrace();
			return Status.CANCEL_STATUS;
		}
	}

	@Override
	protected void canceling() {
		super.canceling();
		logger.debug("Cancelling Search Job");
		Thread.currentThread().interrupt();
	}

	/**
	 * @return the estimated time left
	 */
	public long getEta() {
		return eta;
	}

	/**
	 * @return the successfully tested
	 */
	public int getSuccessfullyTested() {
		return successfullyTested;
	}

	/**
	 * @return the number of already tested
	 */
	public int getCompletelyTested() {
		return completelyTested;
	}

	/**
	 * @return the number of candidates
	 */
	public int getCandidates() {
		return candidates;
	}

}