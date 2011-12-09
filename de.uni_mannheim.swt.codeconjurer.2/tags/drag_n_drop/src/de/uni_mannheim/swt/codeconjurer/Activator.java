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
package de.uni_mannheim.swt.codeconjurer;

import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.techsrv.UsageDataSender;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "de.uni_mannheim.swt.codeconjurer"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 * 
	 * @throws IOException
	 */
	public Activator() throws IOException {
		PatternLayout layout = new PatternLayout("%-5p %c{1}: %m%n");
		ConsoleAppender appender = new ConsoleAppender(layout);
		Logger.getRootLogger().addAppender(appender);
		Logger.getRootLogger().setLevel(Level.ALL);
		Logger.getLogger(Activator.class).debug(
				"Code Conjurer plug-in loaded...");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// Count the number of CC launches
		int launches = getDefault().getPreferenceStore().getInt(
				PreferenceConstants.P_LAUNCH);
		getDefault().getPreferenceStore().setValue(
				PreferenceConstants.P_LAUNCH, ++launches);

		// Store the Eclipse Version
		String v = Display.getAppVersion();
		getDefault().getPreferenceStore().setValue(
				PreferenceConstants.P_PLATFORM_VERSION, v);

		Logger.getLogger(Activator.class).debug(
				"Code Conjurer plug-in start #" + launches);

		// Report number of launches from time to time
		if (launches < 2 || launches % 10 == 0) {
			UsageDataSender.sendInformation(new String[] { "Launches_"
					+ launches });
		}

		// Set default values
		getDefault().getPreferenceStore().setToDefault(
				PreferenceConstants.P_SHOW_ADAPTER);
		getDefault().getPreferenceStore().setToDefault(
				PreferenceConstants.P_SHOW_NEGATIVES);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		CodeConjurer.getInstance().setBackgroundAgentEnabled(false);
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

}
