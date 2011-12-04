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

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;

/**
 * @author Werner Janjic
 * 
 */
public class Query {

	private ITypeRoot typeRoot;
	private Logger logger = Logger.getLogger(Query.class);

	/**
	 * Constructor
	 * 
	 * @param javaElement
	 */
	public Query(ITypeRoot typeRoot) {
		this.typeRoot = typeRoot;
	}

	/**
	 * Set the Java element from which to derive a query
	 * 
	 * @param javaElement
	 */
	public void setJavaSource(ITypeRoot typeRoot) {
		this.typeRoot = typeRoot;
	}

	/**
	 * Returns the primary type of this query as String
	 * 
	 * @return
	 */
	public String getPrimaryType() {
		return typeRoot.findPrimaryType().getElementName();
	}

	/**
	 * Returns an MQL query for interface-based searches or the sourcecode if a
	 * testcase is being edited
	 * 
	 * @return
	 */
	public String getQuery() {
		String query = "";
		IType primaryType = typeRoot.findPrimaryType();
		String superclass = "";
		try {
			superclass = primaryType.getSuperclassName();
			if (superclass != null && superclass.equals("TestCase")) {
				query = /*primaryType.*/getSource()
						+ " // <con>(protocol:cvs OR protocol:svn) original:yes type:class form:source lang:java</con>";
			} else {
				query = getMqlQuery(primaryType);
			}
		} catch (Exception e) {
			CrashReporter.reportException(e);
			logger.debug(e.getLocalizedMessage());
		}
		return query;
	}

	/**
	 * Returns an MQL representation of the query
	 * 
	 * @return
	 */
	public String getMqlQuery(IType primaryType) {
		StringBuilder query = new StringBuilder();

		query.append(primaryType.getElementName());
		try {
			query.append(" ( ");
			IMethod[] methods = primaryType.getMethods();
			if (methods.length > 0) {
				for (IMethod method : methods) {
					query.append(method.getElementName() + "(");
					ILocalVariable[] parameters = method.getParameters();
					if (parameters.length > 0) {
						for (int p = 0; p < parameters.length - 1; p++) {
							query.append(getSimpleName(parameters[p]
									.getTypeSignature()) + ",");
						}
						query.append(getSimpleName(parameters[parameters.length - 1]
								.getTypeSignature()));
					}
					query.append("):" + getSimpleName(method.getReturnType())
							+ "; ");
				}
			}
			query.append(") lang:java type:class form:source original:yes (protocol:cvs OR protocol:svn)");
		} catch (Exception e) {
			CrashReporter.reportException(e);
			logger.warn(e.getMessage());
		}

		return query.toString();
	}

	/**
	 * Returns the sourcecode of the query or null on JavaModelException
	 * 
	 * @return
	 */
	public String getSource() {
		try {
			return typeRoot.getSource();
		} catch (JavaModelException e) {
			CrashReporter.reportException(e);
			logger.warn(e.getMessage());
			return null;
		}
	}

	/**
	 * Returns the simple name of the signature
	 * 
	 * @param signature
	 * @return
	 */
	private String getSimpleName(String signature) {
		return Signature.getSignatureSimpleName(signature).toString();
	}

	/**
	 * Returns the type of the search
	 * 
	 * @return a test-driven search returns "tds", an other kind "standard"
	 */
	public String getType() {
		IType type = typeRoot.findPrimaryType();
		if (type != null) {
			try {
				String superclass = type.getSuperclassName();
				if (superclass != null) {
					logger.debug("Query has superclass " + superclass);
					return superclass.equals("TestCase") ? "tds" : "standard";
				}
			} catch (Exception e) {
				CrashReporter.reportException(e,
						"No primary type found when trying to identify type.",
						null);
				logger.debug("No primary type found when trying to identify type.\r\n"
						+ e.getMessage());
				return "";
			}
		}
		return "";
	}
}
