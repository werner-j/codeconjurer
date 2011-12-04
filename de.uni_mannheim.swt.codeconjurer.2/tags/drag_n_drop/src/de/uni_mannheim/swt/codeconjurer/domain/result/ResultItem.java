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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.merotronics.merobase.ws.action.ResultBean;

import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.search.Query;
import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;

/**
 * @author Werner Janjic
 * 
 */
public class ResultItem {

	private Logger logger = Logger.getLogger(ResultItem.class);

	// The properties of a result
	private HashMap<ResultProperty, String> properties = new HashMap<ResultProperty, String>();

	// The result as a CompilationUnit
	private CompilationUnit resultCompilationUnit;

	/**
	 * Standard constructor invoked with short URL of the result and it's source
	 * 
	 * @param shortUrl
	 * @param source
	 */
	public ResultItem(ResultBean rb, int searchKind) {
		properties.put(ResultProperty.SHORT_URL, rb.getShortUrl());
		properties.put(ResultProperty.NAME, rb.getName());
		properties.put(ResultProperty.EXECUTABILITY, rb.getExecutability()
				.value());
		properties.put(ResultProperty.LICENSE, rb.getLicense());
		properties.put(ResultProperty.LICENSE_DESCRIPTION,
				rb.getLicenseDescription());
		properties.put(ResultProperty.TEST_RESULT, rb.getTestResult());
		setSearchKind(searchKind);
		logger.debug("ResultItem created");
	}

	/**
	 * Returns the value for the given property
	 * 
	 * @param property
	 * @return
	 */
	public String getProperty(ResultProperty property) {
		return properties.get(property);
	}

	/**
	 * Set a property value for this result item
	 * 
	 * @param property
	 * @param value
	 */
	public void setSearchKind(int kind) {
		properties.put(ResultProperty.SEARCH_KIND, String.valueOf(kind));
	}

	/**
	 * Store the source code of this item
	 * 
	 * @param source
	 */
	public void setSource(String source) {
		properties.put(ResultProperty.RAW_SOURCE, source);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(source.toCharArray());
		Map<?, ?> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
		parser.setCompilerOptions(options);
		try {
			resultCompilationUnit = (CompilationUnit) parser.createAST(null);
		} catch (Throwable e) {
			CrashReporter.reportException(e);
			logger.debug("Could not parse source");
		}
		String sourceInCpu = "";
		List<?> types = resultCompilationUnit.types();
		sourceInCpu += types.get(0);
		logger.debug("Source of type root for "
				+ properties.get(ResultProperty.SHORT_URL) + ": " + sourceInCpu);
		logger.debug("Source set");
	}

	/**
	 * Returns the CompilationUnit associated with this result
	 * 
	 * @return
	 */
	public CompilationUnit getCompilationUnit() {
		return resultCompilationUnit;
	}

	/**
	 * Returns the source code of this item
	 * 
	 * @param raw
	 *            raw result or with <i>found by</i> header
	 * @return
	 */
	public String getSource(boolean raw) {
		if (resultCompilationUnit != null) {
			if (raw) {
				return properties.get(ResultProperty.RAW_SOURCE);
			} else {
				return ("// Sourcecode found by merobase.com\r\n" + "// "
						+ properties.get(ResultProperty.SHORT_URL) + "\r\n" + resultCompilationUnit
							.getJavaElement());
			}
		} else {
			return null;
		}
	}

	/**
	 * Returns the Java type root of this item
	 * 
	 * @return The Java type root (a compilation unit or a class file) this
	 *         compilation unit was created from, or null if it was not created
	 *         from a Java type root.
	 */
	public BodyDeclaration getTypeRoot() {
		if (resultCompilationUnit != null) {
			BodyDeclaration declaration = null;
			for (Object typeObject : resultCompilationUnit.types()) {
				BodyDeclaration typeBodyDec = (BodyDeclaration) typeObject;
				if (typeBodyDec.getNodeType() == BodyDeclaration.TYPE_DECLARATION) {
					TypeDeclaration type = (TypeDeclaration) typeBodyDec;
					if (type.getName().getFullyQualifiedName()
							.equals(getProperty(ResultProperty.NAME)))
						declaration = type;
				}
				if (typeBodyDec.getNodeType() == BodyDeclaration.ENUM_DECLARATION) {
					EnumDeclaration type = (EnumDeclaration) typeBodyDec;
					if (type.getName().getFullyQualifiedName()
							.equals(getProperty(ResultProperty.NAME)))
						declaration = type;
				}
			}
			return copyProperties(declaration);
		}
		return null;
	}

	/**
	 * Returns the <code>BodyDeclaration</code> associated with the given uri or
	 * null if no match.
	 * 
	 * @param uri
	 * @return
	 */
	public BodyDeclaration find(String uri) {
		if (getTypeRoot().getNodeType() != BodyDeclaration.TYPE_DECLARATION)
			return null;
		if (uri.equals(getTypeRoot().getProperty(ResultProperty.URI.name())))
			return getTypeRoot();
		for (MethodDeclaration method : ((TypeDeclaration) getTypeRoot())
				.getMethods()) {
			// Set a unique identifier (required for DND)
			String sign = "" + method.getReturnType2() + method.getName();
			for (Object p : method.parameters()) {
				sign += p.toString();
			}
			String mUri = getTypeRoot().getProperty(ResultProperty.URI.name())
					+ CodeConjurer.URI_DELIMITER + sign;
			if (mUri.equals(uri))
				return copyProperties(method);
		}
		return null;
	}

	/**
	 * @return the query that returned this result
	 */
	public String getQuery() {
		return (String) properties.get(ResultProperty.QUERY);
	}

	/**
	 * @param query
	 *            the query that returned this result
	 */
	public void setQuery(Query query) {
		if (query != null)
			properties.put(ResultProperty.QUERY, query.getQuery());
	}

	/**
	 * Returns the Search Session ID of this result
	 * 
	 * @return
	 */
	public String getSearchId() {
		return (String) (String) properties.get(ResultProperty.SEARCH_ID);
	}

	/**
	 * @param id
	 *            the session id of the search
	 */
	public void setSearchId(String id) {
		properties.put(ResultProperty.SEARCH_ID, id);
	}

	/**
	 * Copy the result item's properties to the declaration
	 * 
	 * @param declaration
	 * @return
	 */
	private BodyDeclaration copyProperties(BodyDeclaration declaration) {
		if (declaration != null) {
			for (ResultProperty key : properties.keySet()) {
				declaration.setProperty(key.name(), properties.get(key));
			}
			declaration.setProperty(ResultProperty.URI.name(),
					properties.get(ResultProperty.SHORT_URL));
		}
		return declaration;
	}

}
