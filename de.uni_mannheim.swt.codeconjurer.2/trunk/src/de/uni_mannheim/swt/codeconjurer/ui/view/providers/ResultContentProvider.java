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
package de.uni_mannheim.swt.codeconjurer.ui.view.providers;

import java.util.ArrayList;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultItem;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;
import de.uni_mannheim.swt.codeconjurer.domain.search.Search;

/**
 * @author Werner Janjic
 * 
 */
public class ResultContentProvider implements ITreeContentProvider {

	private Logger logger = Logger.getLogger(ResultContentProvider.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface
	 * .viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		logger.debug("New input on " + viewer.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.
	 * Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		Search search = (Search) inputElement;
		ResultItem[] results = search.getSearchResult().getResultItems();
		boolean noShowNegatives = false;
		ArrayList<BodyDeclaration> elements = new ArrayList<BodyDeclaration>();
		if (search.getKind() == Search.TEST_DRIVEN_SEARCH) {
			// Remove failed candidates from result view if not requested.
			if (!Activator.getDefault().getPreferenceStore()
					.getBoolean(PreferenceConstants.P_SHOW_NEGATIVES)) {
				noShowNegatives = true;
			}
			for (ResultItem result : results) {
				BodyDeclaration typeRoot = result.getTypeRoot();
				if (noShowNegatives
						&& result.getProperty(ResultProperty.TEST_RESULT)
								.startsWith("// No adapter created")) {
					logger.trace("Skip negative result.");
				} else {
					logger.trace("Add result.");
					if (typeRoot != null) {
						elements.add(typeRoot);
					}
				}
			}
		} else {
			for (ResultItem result : results) {
				BodyDeclaration typeRoot = result.getTypeRoot();
				if (typeRoot != null) {
					elements.add(typeRoot);
				}
			}
		}
		return elements.toArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.
	 * Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		BodyDeclaration element = (BodyDeclaration) parentElement;
		ArrayList<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
		if (element.getNodeType() == ASTNode.TYPE_DECLARATION) {
			for (MethodDeclaration method : ((TypeDeclaration) element)
					.getMethods()) {
				// Copy properties from class to methods
				Set<?> properties = element.properties().keySet();
				for (String property : (String[]) properties
						.toArray(new String[element.properties().size()])) {
					method.setProperty(property, element.getProperty(property));
				}
				// Set a unique identifier (required for DND)
				String sign = "" + method.getReturnType2() + method.getName();
				for (Object p : method.parameters()) {
					sign += p.toString();
				}
				// Replace the copied parent's URI from above with a new URI
				// extended with the signature of the child and a mark
				method.setProperty(ResultProperty.URI.name(),
						method.getProperty(ResultProperty.URI.name())
								+ CodeConjurer.URI_DELIMITER + sign);
				methods.add(method);
			}
			return methods.toArray();
		}
		if (element.getNodeType() == ASTNode.ENUM_DECLARATION) {
			for (Object declaration : ((EnumDeclaration) element)
					.bodyDeclarations()) {
				if (declaration instanceof MethodDeclaration) {
					MethodDeclaration method = (MethodDeclaration) declaration;
					// Copy properties from class to methods
					Set<?> properties = element.properties().keySet();
					for (String property : (String[]) properties
							.toArray(new String[element.properties().size()])) {
						method.setProperty(property,
								element.getProperty(property));
					}
					// Set a unique identifier (required for DND)
					String sign = "" + method.getReturnType2()
							+ method.getName();
					for (Object p : method.parameters()) {
						sign += p.toString();
					}
					// Replace the copied parent's URI from above with a new URI
					// extended with the signature of the child and a mark
					method.setProperty(ResultProperty.URI.name(),
							method.getProperty(ResultProperty.URI.name())
									+ CodeConjurer.URI_DELIMITER + sign);
					methods.add(method);
				}
			}
			return methods.toArray();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object
	 * )
	 */
	@Override
	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.
	 * Object)
	 */
	@Override
	public boolean hasChildren(Object parentElement) {
		BodyDeclaration element = (BodyDeclaration) parentElement;

		// Only AbstractTypeDeclarations may have children
		if (element.getNodeType() == ASTNode.TYPE_DECLARATION) {
			if (((TypeDeclaration) element).getMethods().length > 0) {
				return true;
			}
		}
		if (element.getNodeType() == ASTNode.ENUM_DECLARATION) {
			if (((EnumDeclaration) element).bodyDeclarations().size() > 0) {
				return true;
			}
		}

		return false;
	}

}
