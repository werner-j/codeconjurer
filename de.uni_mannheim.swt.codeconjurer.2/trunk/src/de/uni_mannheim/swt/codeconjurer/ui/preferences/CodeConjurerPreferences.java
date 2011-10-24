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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class CodeConjurerPreferences extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	public CodeConjurerPreferences() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Code Conjurer Preferences");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	public void createFieldEditors() {
		addField(new StringFieldEditor(PreferenceConstants.P_SERVER,
				"&Servername:", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_USERNAME,
				"&Username:", getFieldEditorParent()));
		PasswordFieldEditor pwEditor = new PasswordFieldEditor(
				PreferenceConstants.P_PASSWORD, "&Password:",
				getFieldEditorParent());
		addField(pwEditor);
		// Choose between 5 and 100 results
		addField(new SliderFieldEditor(PreferenceConstants.P_RESULTS,
				"&Results per Search:", 5, 105, 5, getFieldEditorParent()));
		addField(new BooleanFieldEditor(
				PreferenceConstants.P_OVERWRITE_ON_INSERT,
				"Overwrite on Insert", BooleanFieldEditor.SEPARATE_LABEL,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.P_UDC,
				"Report Crash to Developers",
				BooleanFieldEditor.SEPARATE_LABEL, getFieldEditorParent()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}