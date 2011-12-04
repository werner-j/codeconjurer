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
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorPart;

import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEvent;
import de.uni_mannheim.swt.codeconjurer.domain.listener.SearchEventListener;
import de.uni_mannheim.swt.codeconjurer.domain.result.Result;

/**
 * Interface for classes that perform searches on MB
 * 
 * @author Werner Janjic
 */
public abstract class Search extends Job {

	protected Logger logger = Logger.getLogger(Search.class);

	public final static int STANDARD_SEARCH = 1;
	public final static int TEST_DRIVEN_SEARCH = 2;

	protected final static int TIMEOUT = 30000;

	protected int kind;

	protected long startTime, endTime;

	protected ArrayList<SearchEventListener> listeners = new ArrayList<SearchEventListener>();

	protected HashMap<String, String> properties = new HashMap<String, String>();

	protected Query query;
	protected Result result;

	protected boolean finished = false;

	private IEditorPart editor;

	/**
	 * Creates a Search
	 * 
	 * @param name
	 */
	public Search(IEditorPart editor, String name, Query query, int kind) {
		super(name);
		this.query = query;
		this.editor = editor;
		this.result = new Result(kind);
		this.kind = kind;
		startTime = System.nanoTime();
	}

	/**
	 * Returns the results of this search
	 * 
	 * @return
	 */
	public Result getSearchResult() {
		return result;
	}

	/**
	 * The editor this search is associated with
	 * 
	 * @return
	 */
	public IEditorPart getEditor() {
		return editor;
	}

	/**
	 * Add a listener to a search
	 * 
	 * @param listener
	 */
	public void addSearchEventListener(SearchEventListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Returns the kind of this search
	 */
	public int getKind() {
		return kind;
	}

	/**
	 * Notify listeners about a SearchEvent
	 * 
	 * @param event
	 */
	protected void notifySearchEventListeners(SearchEvent event) {
		logger.debug("Notify SearchEventListeners: " + event.toString());
		for (SearchEventListener listener : listeners) {
			listener.onEvent(event);
		}
	}

	/**
	 * Call this before returning the status and finishing the job
	 */
	protected void done() {
		logger.debug("Job done...");
		endTime = System.nanoTime();
		finished = true;
	}

	/**
	 * Returns true if this Job is finished and false if the Search is still in
	 * progress.
	 * 
	 * @return
	 */
	public boolean isFinished() {
		return finished;
	}

	/**
	 * Set a property of this search
	 * 
	 * @param name
	 * @param value
	 */
	public void setProperty(String name, String value) {
		properties.put(name, value);
	}

	/**
	 * Returns a property of the search as string object
	 * 
	 * @param name
	 * @return
	 */
	public String getPropertyAsString(String name) {
		return properties.get(name);
	}

	/**
	 * Returns the value of the given property as an integer value
	 * 
	 * @param name
	 * @return
	 */
	public int getPropertyAsInteger(String name) {
		return Integer.parseInt(properties.get(name));
	}

	/**
	 * Returns the duration of this search
	 * 
	 * @return
	 */
	public long getDuration() {
		return (endTime > 0 ? endTime : System.nanoTime()) - startTime;
	}

	@Override
	protected void canceling() {
		logger.debug("Cancelling Search Job");
		Thread.currentThread().interrupt();
	}

}