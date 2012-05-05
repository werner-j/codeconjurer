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

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorPart;

import com.merotronics.merobase.ws.action.IOException_Exception;
import com.merotronics.merobase.ws.action.ResultBean;
import com.merotronics.merobase.ws.client.util.WSConnection;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEvent;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultItem;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;
import de.uni_mannheim.swt.codeconjurer.ui.listener.UIEvent;
import de.uni_mannheim.swt.codeconjurer.ui.view.PluginUI;

public class StandardSearch extends Search {

	private String serverLocation = Activator.getDefault().getPreferenceStore()
			.getString(PreferenceConstants.P_SERVER);
	private String username = Activator.getDefault().getPreferenceStore()
			.getString(PreferenceConstants.P_USERNAME);
	private String password = Activator.getDefault().getPreferenceStore()
			.getString(PreferenceConstants.P_PASSWORD);
	private int numResults = Integer.parseInt(Activator.getDefault()
			.getPreferenceStore().getString(PreferenceConstants.P_RESULTS));

	private WSConnection ws;
	private String session;

	private HashMap<String, String> supplInf = new HashMap<String, String>();

	public StandardSearch(IEditorPart editor, String name, Query query, int kind) {
		super(editor, name, query, kind);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		supplInf.put("Query", query.getQuery());
		supplInf.put("MaxNumResults", "#" + numResults);

		try {
			ws = new WSConnection(serverLocation);
			session = null;
			final String queryString = query.getQuery();
			if (query == null || queryString == null) {
				notifySearchEventListeners(SearchEvent.ERROR);
				done();
				return Status.CANCEL_STATUS;
			}
			result.setQuery(query);
			logger.debug("Initialize search for " + numResults
					+ " components at " + serverLocation + ".");
			if (queryString.equals("")) {
				notifySearchEventListeners(SearchEvent.ERROR);
				done();
				return Status.CANCEL_STATUS;
			}

			// Get session or timeout
			Thread connect = new Thread() {
				@Override
				public void run() {
					try {
						session = ws.initComponentSearch(queryString, username,
								password, numResults);
					} catch (IOException_Exception e) {
						CrashReporter.reportException(e,
								"Problem initializing component search",
								supplInf);
					}
				}
			};
			connect.start();

			long startTime = System.nanoTime();
			long currentTime = 0;
			long timediff = 0;
			// Wait for session or timeout
			while (connect.isAlive() && session == null) {
				currentTime = System.nanoTime();
				timediff = currentTime - startTime;
				if (timediff > TIMEOUT) {
					session = "error: Connection timed out: " + timediff + ">"
							+ TIMEOUT;
				}
				if (monitor.isCanceled()) {
					connect.interrupt();
					session = "error: Connection cancelled by user request.";
				}
				Thread.sleep(1000);
				PluginUI.fireEvent(UIEvent.SETSTATUS);
			}
			logger.trace("Start time: " + startTime);
			logger.trace("Current time: " + currentTime);
			logger.trace("========================================");
			logger.trace("Difference: " + timediff);

			// Show error message if session is null
			if (session == null) {
				notifySearchEventListeners(SearchEvent.ERROR);
				done();
				return Status.CANCEL_STATUS;
			}

			if (session.equals("error: Connection timed out.")) {
				throw new TimeoutException("Connection to server timed out: "
						+ timediff + ">" + TIMEOUT);
			}

			// Check for error message from server
			if (session.toLowerCase().startsWith("error:")) {
				logger.debug("Server reported " + session);
				notifySearchEventListeners(SearchEvent.ERROR);
				done();
				throw new ConnectException("Server reported " + session);
			}

			result.setSessionId(session);
			logger.debug("Received session id: " + session);

			if (session.contains("no or not enough results")) {
				notifySearchEventListeners(SearchEvent.NO_RESULTS);
				done();
				return Status.CANCEL_STATUS;
			}

			if (ws.isLoginFailed()) {
				notifySearchEventListeners(SearchEvent.INVALID_USER);
				done();
				return Status.CANCEL_STATUS;
			}

			switch (kind) {
			case Search.STANDARD_SEARCH:
				notifySearchEventListeners(SearchEvent.STARTED);
				break;

			case Search.TEST_DRIVEN_SEARCH:
				notifySearchEventListeners(SearchEvent.TEST_STARTED);
				break;

			default:
				break;
			}

			// Wait for results
			int loop = 0;
			int tested = 0;

			ArrayList<ResultBean> results = ws.getResults(session);

			while (!ws.isFinished()) {
				if (monitor.isCanceled()) {
					logger.debug("Enable searching again.");
					notifySearchEventListeners(SearchEvent.CANCELLED);
					done();
					return Status.CANCEL_STATUS;
				}
				setProperty("eta", Long.toString(ws.getTimeLeft()));

				if (kind == TEST_DRIVEN_SEARCH) {
					int inc = 0;
					int successfullyTested = ws.getSuccessfullyTested();
					int completelyTested = ws.getCompletelyTested();
					int candidates = ws.getResultSize();
					if (ws.getCompletelyTested() > tested) {
						inc = completelyTested - tested;
						tested = completelyTested;

						results = ws.getResults(session);
						logger.debug("Webservice returned " + results.size()
								+ " results from Merobase.");
						if (result.updateResultList(results)) {
							notifySearchEventListeners(SearchEvent.RESULT_ADDED);
						}
					}
					logger.debug("Search still in progress... "
							+ completelyTested + " tested of " + candidates
							+ " candidates. Successful: " + successfullyTested);
					logger.debug("Tested: " + tested + " / Increment: " + inc);

					monitor.worked(inc);
				}

				// Add sources for items which don't have one attached yet
				for (ResultItem r : result.getResultItems()) {
					boolean fetched = getSource(r);
					if (fetched) {
						notifySearchEventListeners(SearchEvent.SOURCE_ADDED);
					}
					if (monitor.isCanceled()) {
						logger.debug("Enable searching again.");
						notifySearchEventListeners(SearchEvent.CANCELLED);
						done();
						return Status.CANCEL_STATUS;
					}
				}

				// Sleep timer increases with every iteration to a 3s maximum.
				int sleep = Math.min(loop++, 3);
				logger.debug("Sleep for " + sleep + " seconds.");
				try {
					TimeUnit.SECONDS.sleep(sleep);
				} catch (InterruptedException e) {
					CrashReporter.reportException(e, supplInf);
					notifySearchEventListeners(SearchEvent.CANCELLED);
					done();
					return Status.CANCEL_STATUS;
				}
			}
			// Store results
			results = ws.getResults(session);
			logger.debug("Webservice returned " + results.size()
					+ " results from Merobase.");
			if (result.updateResultList(results)) {
				notifySearchEventListeners(SearchEvent.RESULT_ADDED);
			}

			monitor.beginTask("Fetch Sourcecode", result.size());
			logger.debug("Beginn fetching sourcecode for " + result.size()
					+ " results.");

			for (ResultItem r : result.getResultItems()) {
				getSource(r);
				notifySearchEventListeners(SearchEvent.SOURCE_ADDED);
				monitor.worked(1);
				if (monitor.isCanceled()) {
					logger.debug("Enable searching again.");
					notifySearchEventListeners(SearchEvent.CANCELLED);
					done();
					return Status.CANCEL_STATUS;
				}
			}
			notifySearchEventListeners(SearchEvent.RESULT_ADDED);
		} catch (Throwable e) {
			CrashReporter.reportException(e, "Session " + session, supplInf);
			logger.debug("Problem during search: " + e.toString());
			notifySearchEventListeners(SearchEvent.SERVERERROR);
			done();
			return Status.CANCEL_STATUS;
		}
		notifySearchEventListeners(SearchEvent.FINISHED);
		done();
		return Status.OK_STATUS;
	}

	private boolean getSource(ResultItem r) {
		if (r != null) {
			String source = r.getSource(true);
			if (source == null) {
				String urlString = r.getProperty(ResultProperty.SHORT_URL);
				try {
					source = ws.componentSource(urlString, username, password,
							10000);
					// Add branding to sourcecode
					source = brandSource(source, urlString);
				} catch (Exception e) {
					CrashReporter.reportException(
							e,
							"Could not fetch URL "
									+ r.getProperty(ResultProperty.SHORT_URL),
							supplInf);
					logger.debug(e.getLocalizedMessage());
					source = "/** Source could not be fetched */";
					return false;
				}

				// Add source
				result.addSource(urlString, source);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Add a branding to the source
	 * 
	 * @param source
	 * @param urlString
	 * @return
	 */
	private String brandSource(String source, String urlString) {
		if (source.startsWith("/*")) {
			source = "/** \r\n * Sourcecode fetched by merobase.com \r\n * Origin: "
					+ urlString
					+ "\r\n"
					+ " * "
					+ source.substring(3, source.length());
		} else {
			source = "/** \r\n * Sourcecode fetched by merobase.com \r\n * Origin: "
					+ urlString + "\r\n */\r\n" + source;
		}
		return source;
	}
}