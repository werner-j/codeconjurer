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
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.merotronics.merobase.ws.action.Executability;

import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;

/**
 * @author Werner Janjic
 * 
 */
public class ResultLabelProvider extends LabelProvider implements
		ITableLabelProvider, ITableColorProvider, ITableFontProvider {

	// private Logger logger = Logger.getLogger(ResultLabelProvider.class);

	@Override
	public Image getColumnImage(Object item, int columnIndex) {
		Image img = null;
		BodyDeclaration element = (BodyDeclaration) item;
		if (columnIndex == 0) {
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
		}
		return img;
	}

	@Override
	public String getColumnText(Object item, int columnIndex) {
		BodyDeclaration element = (BodyDeclaration) item;
		if (element.getNodeType() == ASTNode.TYPE_DECLARATION) {
			if (columnIndex == 0) {
				return ((TypeDeclaration) element).getName()
						.getFullyQualifiedName();
			}
			if (columnIndex == 1) {
				String license = ""
						+ element.getProperty(ResultProperty.LICENSE.name());
				if (!license.contains("null")) {
					return license;
				}
			}
			if (columnIndex == 2) {
				if (((String) element.getProperty(ResultProperty.EXECUTABILITY
						.name())).equals(Executability.TESTED.value())) {
					return "TESTED";
				}
			}
		}
		if (element.getNodeType() == ASTNode.METHOD_DECLARATION
				&& columnIndex == 0) {
			return ((MethodDeclaration) element).getName()
					.getFullyQualifiedName();
		}
		return null;
	}

	@Override
	public Color getForeground(Object item, int columnIndex) {
		BodyDeclaration element = (BodyDeclaration) item;
		Color color = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
		if (columnIndex == 2) {
			if (((String) element.getProperty(ResultProperty.EXECUTABILITY
					.name())).equals(Executability.TESTED.value())) {
				color = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
			}
		}
		return color;
	}

	@Override
	public Color getBackground(Object item, int columnIndex) {
		BodyDeclaration element = (BodyDeclaration) item;
		Color color = Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
		if (element.getNodeType() == ASTNode.TYPE_DECLARATION) {
			color = Display.getCurrent().getSystemColor(
					SWT.COLOR_WIDGET_LIGHT_SHADOW);
		}
		if (columnIndex == 2
				&& element.getNodeType() == ASTNode.TYPE_DECLARATION) {
			if (((String) element.getProperty(ResultProperty.EXECUTABILITY
					.name())).equals(Executability.TESTED.value())) {
				color = Display.getCurrent().getSystemColor(
						SWT.COLOR_DARK_GREEN);
			}
		}
		return color;
	}

	@Override
	public Font getFont(Object item, int columnIndex) {
		BodyDeclaration element = (BodyDeclaration) item;
		if (element.getNodeType() == ASTNode.TYPE_DECLARATION
				&& columnIndex == 0) {
			return JFaceResources.getFontRegistry().getBold(
					JFaceResources.DEFAULT_FONT);
		}
		return null;
	}

}
