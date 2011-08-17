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
package de.uni_mannheim.swt.codeconjurer.ui.dnd;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;

/**
 * @author Werner Janjic
 * 
 */
public class JavaEditorDropListener extends ViewerDropAdapter {

	protected JavaEditorDropListener(Viewer viewer) {
		super(viewer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	@Override
	public boolean performDrop(Object data) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object
	 * , int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {
		// TODO Auto-generated method stub
		return false;
	}

}
