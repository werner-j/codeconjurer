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
package de.uni_mannheim.swt.codeconjurer.domain.result;

import java.util.HashMap;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * @author Werner Janjic
 * 
 */
public class ResultItem {

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
	public ResultItem(String shortUrl, String name) {
		properties.put(ResultProperty.SHORT_URL, shortUrl);
		properties.put(ResultProperty.NAME, name);
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
		parser.setSource(source.toCharArray());
		resultCompilationUnit = (CompilationUnit) parser.createAST(null);
	}

	/**
	 * Returns the source code of this item
	 * 
	 * @return
	 */
	public String getSource() {
		return ("" + resultCompilationUnit.getJavaElement());
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
				TypeDeclaration type = (TypeDeclaration) typeObject;
				if (type.getName().getFullyQualifiedName()
						.equals(getProperty(ResultProperty.NAME)))
					typeDec = type;
			}
			return typeDec;
		} else {
			return null;
		}
	}
}
