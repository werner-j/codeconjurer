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
package de.uni_mannheim.swt.codeconjurer.application;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IEditorPart;

import de.uni_mannheim.swt.codeconjurer.application.listener.CodeChangedListener;
import de.uni_mannheim.swt.codeconjurer.application.util.ParallelSearchPrevention;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEventListener;
import de.uni_mannheim.swt.codeconjurer.domain.search.Query;
import de.uni_mannheim.swt.codeconjurer.domain.search.Search;
import de.uni_mannheim.swt.codeconjurer.domain.search.StandardSearch;
import de.uni_mannheim.swt.codeconjurer.ui.view.PluginUI;

/**
 * @author Werner Janjic
 * 
 */
public class CodeConjurer {

	private static CodeConjurer instance;
	private HashMap<IEditorPart, ParallelSearchPrevention> searchJobs = new HashMap<IEditorPart, ParallelSearchPrevention>();
	private Logger logger = Logger.getLogger(CodeChangedListener.class);

	// The plug-in's main listener
	private CodeChangedListener codeListener;
	private ArrayList<SearchEventListener> listeners;
	private boolean backgroundAgentEnabled = false;

	private HashMap<IEditorPart, ArrayList<Search>> searches = new HashMap<IEditorPart, ArrayList<Search>>();

	/**
	 * Default Constructor of the Code Conjurer app -- singleton
	 */
	private CodeConjurer() {
		codeListener = new CodeChangedListener();
		listeners = new ArrayList<SearchEventListener>();
	}

	/**
	 * Returns an instance of Code Conjurer on which system operations can be
	 * performed
	 * 
	 * @return
	 */
	public static CodeConjurer getInstance() {
		if (instance == null) {
			instance = new CodeConjurer();
		}
		return instance;
	}

	/**
	 * Activate the background agent for pro-active searches
	 */
	public void setBackgroundAgentEnabled(boolean enabled) {
		if (enabled) {
			JavaCore.addElementChangedListener(codeListener);
			backgroundAgentEnabled = true;
			logger.debug("Background agent enabled");
		} else {
			JavaCore.removeElementChangedListener(codeListener);
			backgroundAgentEnabled = false;
			logger.debug("Background agent disabled. Send cancel to all searches...");
			for (ArrayList<Search> searchList : searches.values()) {
				for (Search search : searchList) {
					search.cancel();
					try {
						search.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			logger.debug("All searches stopped!");
		}
	}

	/**
	 * Tells if the background agent is turned on
	 * 
	 * @return
	 */
	public boolean isBackgroundAgentEnabled() {
		return backgroundAgentEnabled;
	}

	/**
	 * Perform a search using the code from the active eclipse editor as input
	 */
	public void search() {
		IEditorPart editor = PluginUI.getActiveEditor();
		// If there is no active editor, a search cannot be performed
		if (editor == null
				|| !editor.getEditorSite().getId()
						.equals("org.eclipse.jdt.ui.CompilationUnitEditor")) {
			logger.warn("No active editor found on workbench or active editor not an CompilationUnitEditor.");
			return;
		}
		logger.debug("Make sure the recommendations view is present...");
		PluginUI.showRecommendationsView();
		logger.debug("Fetch source from active editor");
		ITypeRoot typeRoot = JavaUI.getEditorInputTypeRoot(editor
				.getEditorInput());
		Query query = new Query(typeRoot);
		logger.debug("Sourcecode\r\n" + query.getSource() + "\r\n");
		logger.debug("Query: " + query.getQuery());
		final StandardSearch search = new StandardSearch(editor,
				"Search for reusable assets for " + editor.getTitle(), query);
		// Delegate the search event listeners to the search
		for (SearchEventListener listener : listeners) {
			search.addSearchEventListener(listener);
		}
		if (searches.get(editor) != null) {
			logger.debug("Search in progress... Cancel old search.");
			for (Search s : searches.get(editor)) {
				s.cancel();
				try {
					s.join();
				} catch (InterruptedException e) {
					logger.debug("Could not cancel Job: " + e.getMessage());
				}
			}
		} else {
			searches.put(editor, new ArrayList<Search>());
		}
		searches.get(editor).add(search);
		// This listener ensures that only one search can be
		// performed at the same time for an editor
		search.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					logger.debug("Search completed successfully.");
					search.getSearchResult().notifyObservers(
							search.getSearchResult());
				} else {
					logger.debug("Search aborted.");
				}
				CodeConjurer.getInstance().setSearchFinished(search);
			}
		});
		// The rule for one search per editor
		ParallelSearchPrevention concurrencyRule = null;
		if (searchJobs.containsKey(editor)) {
			concurrencyRule = searchJobs.get(editor);
		} else {
			concurrencyRule = new ParallelSearchPrevention();
			searchJobs.put(editor, concurrencyRule);
		}
		search.setRule(concurrencyRule);
		search.setPriority(Job.LONG);
		search.schedule(1000);
	}

	/**
	 * Returns the search corresponding to the active editor
	 * 
	 * @return
	 */
	public Search getActiveSearch() {
		ArrayList<Search> searchList = searches.get(PluginUI.getActiveEditor());
		if (searchList != null && searchList.size() > 0) {
			return searchList.get(searchList.size() - 1);
		} else {
			return null;
		}
	}

	/**
	 * This should be called when a search is scheduled and when it finishes
	 * 
	 * @param searchRunning
	 */
	public void setSearchFinished(Search search) {
		IEditorPart editor = search.getEditor();
		searchJobs.remove(editor);
	}

	/**
	 * Register a new listener that is interested in notifications from searches
	 * 
	 * @param listener
	 */
	public void addSearchEventListener(SearchEventListener listener) {
		listeners.add(listener);
	}

	/**
	 * Returns the background agent listener class
	 */
	public CodeChangedListener getBackgroundAgentListener() {
		return codeListener;
	}

}
