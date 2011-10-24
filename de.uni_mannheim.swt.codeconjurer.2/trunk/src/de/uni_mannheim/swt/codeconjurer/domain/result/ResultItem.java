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

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.merotronics.merobase.ws.action.ResultBean;

import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;

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
	public ResultItem(ResultBean rb) {
		properties.put(ResultProperty.SHORT_URL, rb.getShortUrl());
		properties.put(ResultProperty.NAME, rb.getName());
		properties.put(ResultProperty.EXECUTABILITY, rb.getExecutability()
				.value());
		properties.put(ResultProperty.LICENSE, rb.getLicense());
		properties.put(ResultProperty.LICENSE_DESCRIPTION,
				rb.getLicenseDescription());
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
	 * Store the source code of this item
	 * 
	 * @param source
	 */
	public void setSource(String source) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(source.toCharArray());
		resultCompilationUnit = (CompilationUnit) parser.createAST(null);
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
	 * @return
	 */
	public String getSource() {
		return ("// Sourcecode found by merobase.com\r\n" + "// "
				+ properties.get(ResultProperty.SHORT_URL) + "\r\n" + resultCompilationUnit
					.getJavaElement());
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
			BodyDeclaration typeDec = null;
			for (Object typeObject : resultCompilationUnit.types()) {
				BodyDeclaration typeBodyDec = (BodyDeclaration) typeObject;
				if (typeBodyDec.getNodeType() == BodyDeclaration.TYPE_DECLARATION) {
					TypeDeclaration type = (TypeDeclaration) typeBodyDec;
					if (type.getName().getFullyQualifiedName()
							.equals(getProperty(ResultProperty.NAME)))
						typeDec = type;
				}
				if (typeBodyDec.getNodeType() == BodyDeclaration.ENUM_DECLARATION) {
					EnumDeclaration type = (EnumDeclaration) typeBodyDec;
					if (type.getName().getFullyQualifiedName()
							.equals(getProperty(ResultProperty.NAME)))
						typeDec = type;
				}
			}
			if (typeDec != null) {
				typeDec.setProperty(ResultProperty.SHORT_URL.name(),
						properties.get(ResultProperty.SHORT_URL));
				typeDec.setProperty(ResultProperty.URI.name(),
						properties.get(ResultProperty.SHORT_URL));
				typeDec.setProperty(ResultProperty.EXECUTABILITY.name(),
						properties.get(ResultProperty.EXECUTABILITY));
				typeDec.setProperty(ResultProperty.LICENSE.name(),
						properties.get(ResultProperty.LICENSE));
			} else {
				logger.debug("No primary type for "
						+ properties.get(ResultProperty.SHORT_URL));
			}
			return typeDec;
		} else {
			return null;
		}
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
			String lUri = getTypeRoot().getProperty(ResultProperty.URI.name())
					+ CodeConjurer.URI_DELIMITER + sign;
			if (lUri.equals(uri))
				return method;
		}
		return null;
	}
}
