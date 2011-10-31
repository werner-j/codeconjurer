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
package de.uni_mannheim.swt.codeconjurer.domain.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.uni_mannheim.swt.codeconjurer.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
	 * initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.P_LAUNCH, 0);
		store.setDefault(PreferenceConstants.P_FIRST_LAUNCH, true);
		store.setDefault(PreferenceConstants.P_SERVER,
				"http://www.merobase.com");
		store.setDefault(PreferenceConstants.P_USERNAME, "");
		store.setDefault(PreferenceConstants.P_PASSWORD, "");
		store.setDefault(PreferenceConstants.P_RESULTS, 20);
		store.setDefault(PreferenceConstants.P_OVERWRITE_ON_INSERT, true);
		store.setDefault(PreferenceConstants.P_ORGANIZE_IMPORTS, true);
		store.setDefault(PreferenceConstants.P_FORMAT_ON_INSERT, true);
		store.setDefault(PreferenceConstants.P_UDC, true);
		store.setDefault(PreferenceConstants.P_CRASH_REPORTING, true);
	}

}
