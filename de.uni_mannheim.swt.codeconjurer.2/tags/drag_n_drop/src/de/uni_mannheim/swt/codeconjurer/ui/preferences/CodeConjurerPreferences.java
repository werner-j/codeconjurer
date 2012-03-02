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

import org.eclipse.core.runtime.Platform;
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

	private String platformVersion = Activator.getDefault()
			.getPreferenceStore()
			.getString(PreferenceConstants.P_PLATFORM_VERSION);

	public CodeConjurerPreferences() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());

		String ccVersion = Platform
				.getBundle("de.uni_mannheim.swt.codeconjurer").getHeaders()
				.get("Bundle-Version");

		setDescription("Code Conjurer " + ccVersion);
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	public void createFieldEditors() {
		addField(new LabelFieldEditor("", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_SERVER,
				"&Servername:", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_USERNAME,
				"&Username:", getFieldEditorParent()));
		PasswordFieldEditor pwEditor = new PasswordFieldEditor(
				PreferenceConstants.P_PASSWORD, "&Password:",
				getFieldEditorParent());
		addField(pwEditor);
		addField(new LabelFieldEditor("", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.P_KEYWORD_SEARCH,
				"&Keyword-based search", getFieldEditorParent()));
		// Choose between 5 and 100 results
		addField(new SliderFieldEditor(PreferenceConstants.P_RESULTS,
				"&Results per search:", 5, 105, 5, getFieldEditorParent()));

		// Issue with JLS3 parser in Eclipse 3.7.1
		if (platformVersion.contains("3.7.1")) {
			// showJLSBugNote();
		}
	}

	/**
	 * Show an alert when Eclipse 3.7.1 is used.
	 * 
	 * @see eclipse bug 361938:
	 *      https://bugs.eclipse.org/bugs/show_bug.cgi?id=361938
	 */
	private void showJLSBugNote() {
		addField(new LabelFieldEditor("", getFieldEditorParent()));
		addField(new LabelFieldEditor(
				"You are using Eclipse "
						+ platformVersion
						+ " "
						+ "which has an issue with\r\n"
						+ "the Java parser.\r\n"
						+ "Due to a bug in org.eclipse.jdt.core.dom.TryStatement some results\r\n"
						+ "may not show source code. This issue is fixed in 3.7.2 and does not\r\n"
						+ "appear in 3.7.0.\r\n\r\n"
						+ "See https://bugs.eclipse.org/bugs/show_bug.cgi?id=361938 for details.",
				getFieldEditorParent()));
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