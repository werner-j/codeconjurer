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
package de.uni_mannheim.swt.codeconjurer.ui.preferences;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Werner Janjic
 * 
 */
public class PasswordFieldEditor extends StringFieldEditor {

	public PasswordFieldEditor(String name, String label, Composite parent) {
		super(name, label, parent);
	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numCols) {
		super.doFillIntoGrid(parent, numCols);
		getTextControl().setEchoChar('\u25CF');
	}

}
