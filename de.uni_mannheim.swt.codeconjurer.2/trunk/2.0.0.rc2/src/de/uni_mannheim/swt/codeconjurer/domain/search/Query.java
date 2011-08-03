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

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

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
	 * Returns an MQL representation of the query
	 * 
	 * @return
	 */
	public String getMqlQuery() {
		StringBuilder query = new StringBuilder();

		IType primaryType = typeRoot.findPrimaryType();
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
			query.append(") lang:java type:class form:source original:yes");
		} catch (Exception ex) {
			logger.warn(ex.getMessage());
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
}
