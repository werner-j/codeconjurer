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

	public final static int INTERFACE = 1;
	public final static int TEST = 2;

	protected ArrayList<SearchEventListener> listeners = new ArrayList<SearchEventListener>();

	protected Query query;
	protected Result result;

	protected boolean finished = false;

	private IEditorPart editor;

	protected int type = 0;

	/**
	 * Creates a Search
	 * 
	 * @param name
	 */
	public Search(IEditorPart editor, String name, Query query) {
		super(name);
		this.query = query;
		this.editor = editor;
		this.result = new Result();
	}

	/**
	 * Returns the results of this search
	 * 
	 * @return
	 */
	public Result getSearchResult() {
		return result;
	}

	public IEditorPart getEditor() {
		return editor;
	}

	public void addSearchEventListener(SearchEventListener listener) {
		this.listeners.add(listener);
	}

	public int getType() {
		return type;
	}

	protected void notifySearchEventListeners(SearchEvent event) {
		logger.debug("Notify SearchEventListeners: " + event.toString());
		for (SearchEventListener listener : listeners) {
			listener.onEvent(event);
		}
	}

	protected void done() {
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

}