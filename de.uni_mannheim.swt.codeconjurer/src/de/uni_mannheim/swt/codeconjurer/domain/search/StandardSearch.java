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
package de.uni_mannheim.swt.codeconjurer.domain.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorPart;

import com.merotronics.merobase.ws.client.util.WSConnection;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEvent;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultItem;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;

public class StandardSearch extends Search {

	public StandardSearch(IEditorPart editor, String name, Query query) {
		super(editor, name + query.getMqlQuery(), query);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		// String serverLocation = "http://10.1.83.19:8080";
		// String serverLocation = "http://www.merobase.com";
		// String username = "statistics";
		// String password = "statistics";
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
			String session = ws.initComponentSearch(query.getMqlQuery(),
					username, password, numResults);

			notifySearchEventListeners(SearchEvent.STARTED);

			// Wait for results
			int loop = 0;
			while (!ws.isFinished()) {
				// Poll for new results
				ws.getResults(session, username, password);
				if (monitor.isCanceled()) {
					logger.debug("Enable searching again.");
					notifySearchEventListeners(SearchEvent.CANCELLED);
					return Status.CANCEL_STATUS;
				}
				logger.debug("Search still in progress... #" + result.size());
				// Sleep timer increases with every iteration to a 10s maximum.
				int sleep = Math.min(500 * loop++, 10000);
				logger.debug("Sleep for " + ((double) sleep / 1000)
						+ " seconds.");
				Thread.sleep(sleep);
			}
			// Store results
			result.addResultList(ws.getResults(session, username, password));
			notifySearchEventListeners(SearchEvent.RESULT_ADDED);

			monitor.beginTask("Fetch Sourcecode", result.size());
			logger.debug("Beginn fetching sourcecode for " + result.size()
					+ " results.");

			for (ResultItem r : result.getResultItems()) {
				String source = null;
				try {
					source = ws.componentSource(
							r.getProperty(ResultProperty.SHORT_URL), username,
							password);
				} catch (Exception e) {
					logger.debug(e.getLocalizedMessage());
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
		} catch (Exception e) {
			logger.debug(e.getLocalizedMessage());
			notifySearchEventListeners(SearchEvent.ERROR);
			return Status.CANCEL_STATUS;
		}
		notifySearchEventListeners(SearchEvent.FINISHED);
		return Status.OK_STATUS;
	}
}