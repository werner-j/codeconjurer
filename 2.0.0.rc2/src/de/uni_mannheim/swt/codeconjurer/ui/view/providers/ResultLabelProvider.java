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
		String source = element.toString();
		if (source != null && !source.equals("")) {
			color = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
		} else {
			color = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
		}
		return color;
	}

	@Override
	public Color getBackground(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
	}

}
