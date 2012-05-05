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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Observable;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.BodyDeclaration;

import com.merotronics.merobase.ws.action.ResultBean;

import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.search.Query;

/**
 * @author Werner Janjic
 * 
 */
public class Result extends Observable {

	private Logger logger = Logger.getLogger(Result.class);

	private Query query;
	private String sessionId;

	// SHORT_URL, ResultItem
	private LinkedHashMap<String, ResultItem> resultItems = new LinkedHashMap<String, ResultItem>();

	private LinkedHashMap<String, String> successfulSources = new LinkedHashMap<String, String>();

	private Calendar creation;

	private int kind;

	/**
	 * Default Constructor
	 */
	public Result(int kind) {
		super();
		this.kind = kind;
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
			ResultItem resultItem = new ResultItem(result, kind);
			resultItem.setQuery(query);
			resultItem.setSearchId(sessionId);
			resultItems.put(result.getShortUrl(), resultItem);
		}
		setChanged();
		notifyObservers();
	}

	/**
	 * This method may be used to add new results without touching existing
	 * ones. This may be used to avoid fetching source code again for previously
	 * found members.
	 * 
	 * @param results
	 * @return true if new results were added, false else
	 */
	public boolean updateResultList(ArrayList<ResultBean> results) {
		boolean newResult = false;
		for (ResultBean result : results) {
			if (!resultItems.containsKey(result.getShortUrl())) {
				ResultItem resultItem = new ResultItem(result, kind);
				resultItem.setQuery(query);
				resultItem.setSearchId(sessionId);
				resultItems.put(result.getShortUrl(), resultItem);
				newResult = true;
			}
		}
		if (newResult) {
			setChanged();
			notifyObservers();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * This method may be used to <b>override</b> the list of result items
	 * 
	 * @param results
	 */
	public void setResultList(ArrayList<ResultBean> results) {
		resultItems = new LinkedHashMap<String, ResultItem>();
		for (ResultBean result : results) {
			ResultItem resultItem = new ResultItem(result, kind);
			resultItem.setQuery(query);
			resultItem.setSearchId(sessionId);
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
	 * Returns the number of candidates from this set that passed the test case
	 * of a test-driven search
	 * 
	 * @return
	 */
	public int getNumberOfSuccessfulTests() {
		int passes = 0;
		for (ResultItem result : resultItems.values()) {
			if (result.getProperty(ResultProperty.EXECUTABILITY).equals(
					"TESTED")) {
				passes++;
			}
		}
		return passes;
	}

	/**
	 * Returns the <code>BodyDeclaration</code> associated with the given uri or
	 * null if no match.
	 * 
	 * @param uri
	 * @return
	 */
	public BodyDeclaration find(String uri) {
		String shortUrl = "";
		if (uri.contains(CodeConjurer.URI_DELIMITER)) {
			int pos = uri.indexOf(CodeConjurer.URI_DELIMITER);
			shortUrl = uri.substring(0, pos);
		} else {
			shortUrl = uri;
		}
		ResultItem result = getResultItem(shortUrl);
		if (result != null) {
			BodyDeclaration declaration = result.find(uri);
			return declaration;
		} else {
			return null;
		}
	}

	/**
	 * Returns the <code>ResultItem</code> for the given URL
	 * 
	 * @param shortUrl
	 * @return
	 */
	public ResultItem getResultItem(String shortUrl) {
		if (shortUrl.contains(CodeConjurer.URI_DELIMITER)) {
			shortUrl = shortUrl.substring(0,
					shortUrl.indexOf(CodeConjurer.URI_DELIMITER));
		}
		return resultItems.get(shortUrl);
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
			return resultItems.get(shortUrl).getSource(true);
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
			resultItems.get(shortUrl).setSource(source);
			successfulSources.put(shortUrl, source);
		} else {
			logger.debug("Source for " + shortUrl + " not available");
			source = "// Sourcecode for " + shortUrl + "\r\n// not available.";
			resultItems.get(shortUrl).setSource(source);
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

	/**
	 * @return the query that returned this result
	 */
	public Query getQuery() {
		return query;
	}

	/**
	 * @param query
	 *            the query object of this result
	 */
	public void setQuery(Query query) {
		this.query = query;
	}

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * @param sessionId
	 *            the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

}
