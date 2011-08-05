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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * @author Werner Janjic
 * 
 */
public class ResultLabelProvider extends LabelProvider implements
		ITableLabelProvider, ITableColorProvider {

	@Override
	public Image getColumnImage(Object item, int columnIndex) {
		Image img = null;
		BodyDeclaration element = (BodyDeclaration) item;
		if (element.getNodeType() == ASTNode.TYPE_DECLARATION) {
			img = JavaUI.getSharedImages().getImage(
					ISharedImages.IMG_OBJS_CLASS);
		}
		if (element.getNodeType() == ASTNode.METHOD_DECLARATION) {
			MethodDeclaration method = (MethodDeclaration) element;
			if (method.isConstructor()) {
				img = JavaUI.getSharedImages().getImage(
						ISharedImages.IMG_OBJS_INNER_CLASS_DEFAULT);
			} else {
				img = JavaUI.getSharedImages().getImage(
						ISharedImages.IMG_OBJS_PUBLIC);
			}
		}
		return img;
	}

	@Override
	public String getColumnText(Object item, int columnIndex) {
		BodyDeclaration element = (BodyDeclaration) item;
		if (element.getNodeType() == ASTNode.TYPE_DECLARATION) {
			return ((TypeDeclaration) element).getName()
					.getFullyQualifiedName();
		}
		if (element.getNodeType() == ASTNode.METHOD_DECLARATION) {
			return ((MethodDeclaration) element).getName()
					.getFullyQualifiedName();
		}
		return null;
	}

	@Override
	public Color getForeground(Object item, int columnIndex) {
		BodyDeclaration element = (BodyDeclaration) item;
		Color color = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
		if (element.getProperty("executability").equals("TESTED")) {
			color = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
		}
		return color;
	}

	@Override
	public Color getBackground(Object item, int columnIndex) {
		BodyDeclaration element = (BodyDeclaration) item;
		Color color = Display.getCurrent().getSystemColor(
				SWT.COLOR_WHITE);
		if (element.getProperty("executability").equals("TESTED")) {
			color = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
		}
		return color;
	}

}
