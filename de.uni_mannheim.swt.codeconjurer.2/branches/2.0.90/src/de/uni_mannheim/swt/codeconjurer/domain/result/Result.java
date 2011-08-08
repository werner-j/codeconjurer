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
package de.uni_mannheim.swt.codeconjurer.domain.result;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Observable;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.BodyDeclaration;

import com.merotronics.merobase.ws.action.ResultBean;

/**
 * @author Werner Janjic
 * 
 */
public class Result extends Observable {

	private Logger logger = Logger.getLogger(Result.class);

	private HashMap<String, ResultItem> resultItems = new HashMap<String, ResultItem>();
	private HashMap<String, String> successfulSources = new HashMap<String, String>();

	private Calendar creation;

	/**
	 * Default Constructor
	 */
	public Result() {
		super();
		creation = Calendar.getInstance(Locale.getDefault());
		creation.setTimeInMillis(System.currentTimeMillis());
	}

	/**
	 * Returns the time and date the result was created
	 * 
	 * @return
	 */
	public String getCreationDate() {
		SimpleDateFormat formatter = new SimpleDateFormat(
				"E yyyy-MM-dd 'at' HH:mm zzz");
		return formatter.format(creation.getTime());
	}

	/**
	 * This method may be used to add a list of results items to this result
	 * 
	 * @param results
	 */
	public void addResultList(ArrayList<ResultBean> results) {
		for (ResultBean result : results) {
			ResultItem resultItem = new ResultItem(result);
			resultItems.put(result.getShortUrl(), resultItem);
		}
		setChanged();
		notifyObservers();
	}

	/**
	 * This method may be used to <b>override</b> the list of result items
	 * 
	 * @param results
	 */
	public void setResultList(ArrayList<ResultBean> results) {
		resultItems = new HashMap<String, ResultItem>();
		for (ResultBean result : results) {
			ResultItem resultItem = new ResultItem(result);
			resultItems.put(result.getShortUrl(), resultItem);
		}
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the result items
	 * 
	 * @return
	 */
	public ResultItem[] getResultItems() {
		ResultItem[] results = new ResultItem[resultItems.values().size()];
		return resultItems.values().toArray(results);
	}

	/**
	 * Returns the sourcecode for the provided URL
	 * 
	 * @param shortUrl
	 * @return
	 */
	public String getSource(String shortUrl) {
		if (!successfulSources.containsKey(shortUrl)) {
			return "// Sourcecode for " + shortUrl
					+ "\r\n// not available from cache.";
		} else {
			return resultItems.get(shortUrl).getSource();
		}
	}

	/**
	 * Set the source code for the given URL
	 * 
	 * @param shortUrl
	 * @param source
	 */
	public void addSource(String shortUrl, String source) {
		if (source != null && !source.equals("null")
				&& !source.contains("/** Source could not be fetched */")) {
			logger.debug("Add source for " + shortUrl);
			resultItems.get(shortUrl).setSource(
					"// Brought to you by merobase.com\r\n// Origin: \r\n// "
							+ shortUrl + "\r\n" + source);
			successfulSources.put(shortUrl, source);
		} else {
			logger.debug("Source for " + shortUrl + " not available");
			source = "// Sourcecode for " + shortUrl + "\r\n// not available.";
			resultItems.get(shortUrl).setSource(source);
			// successfulSources.put(shortUrl, source);
			successfulSources.remove(shortUrl);
		}
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the number of successfully fetched sources
	 * 
	 * @return
	 */
	public int getNumberOfSuccessfullyFetchedSources() {
		return successfulSources.size();
	}

	/**
	 * Returns the corresponding CompilationUnit object to the given shortUrl
	 * 
	 * @param shortUrl
	 * @return
	 */
	public BodyDeclaration getTypeRoot(String shortUrl) {
		return resultItems.get(shortUrl).getTypeRoot();
	}

	/**
	 * Returns the number of stored results
	 * 
	 * @return
	 */
	public int size() {
		return resultItems.size();
	}

}
