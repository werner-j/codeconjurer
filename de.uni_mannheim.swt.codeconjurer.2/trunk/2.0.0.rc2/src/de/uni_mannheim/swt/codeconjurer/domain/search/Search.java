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

	protected ArrayList<SearchEventListener> listeners = new ArrayList<SearchEventListener>();

	protected Query query;
	protected Result result;

	private IEditorPart editor;

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

	protected void notifySearchEventListeners(SearchEvent event) {
		logger.debug("Notify SearchEventListeners: " + event.toString());
		for (SearchEventListener listener : listeners) {
			listener.onEvent(event);
		}
	}

}