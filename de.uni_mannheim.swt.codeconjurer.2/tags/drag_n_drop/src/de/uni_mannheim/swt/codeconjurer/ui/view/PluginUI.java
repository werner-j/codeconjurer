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
package de.uni_mannheim.swt.codeconjurer.ui.view;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import de.uni_mannheim.swt.codeconjurer.techsrv.CrashReporter;
import de.uni_mannheim.swt.codeconjurer.ui.listener.UIEvent;
import de.uni_mannheim.swt.codeconjurer.ui.listener.UIListener;

/**
 * @author Werner Janjic
 * 
 */
public class PluginUI {

	private static Logger logger = Logger.getLogger(PluginUI.class);

	private static IWorkbenchWindow window;
	private static ResultView resultView;

	private static ArrayList<UIListener> listeners = new ArrayList<UIListener>();

	public static IWorkbenchWindow getWindow() {
		return window;
	}

	public static void setWindow(IWorkbenchWindow window) {
		PluginUI.window = window;
	}

	/**
	 * Returns the active editor or null if none active
	 * 
	 * @return
	 */
	public static IEditorPart getActiveEditor() {
		if (window == null)
			return null;
		IWorkbenchPage page = window.getActivePage();
		if (page == null)
			return null;
		IEditorPart editor = page.getActiveEditor();
		if (editor == null)
			return null;
		return editor;
	}

	/**
	 * Show recommendations view in workbench
	 */
	public static void showRecommendationsView(final boolean inBackground) {
		PluginUI.getWindow().getWorkbench().getDisplay()
				.asyncExec(new Runnable() {

					@Override
					public void run() {
						try {
							if (inBackground) {
								resultView = (ResultView) PluginUI
										.getWindow()
										.getActivePage()
										.showView(
												"de.uni_mannheim.swt.codeconjurer.views.ResultView",
												null,
												IWorkbenchPage.VIEW_VISIBLE);
							} else {
								resultView = (ResultView) PluginUI
										.getWindow()
										.getActivePage()
										.showView(
												"de.uni_mannheim.swt.codeconjurer.views.ResultView");
							}
						} catch (PartInitException e) {
							CrashReporter.reportException(e);
							logger.debug("Could not open Reuse View!\r\n"
									+ e.getLocalizedMessage());
						}
					}
				});
	}

	/**
	 * Returns the instance of the result view
	 * 
	 * @return
	 */
	public static ResultView getResultView() {
		if (resultView == null) {
			resultView = (ResultView) PluginUI.getWindow().getActivePage()
					.findViewReference("").getView(false);
		}
		return resultView;
	}

	/**
	 * Add listener to UI Events
	 * 
	 * @param listener
	 */
	public static void addUIListener(UIListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove listener from UI Events
	 * 
	 * @param listener
	 * @return
	 */
	public static boolean removeUIListener(UIListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * Fire UI Event and notify listeners
	 * 
	 * @param event
	 */
	public static void fireEvent(UIEvent event) {
		for (UIListener listener : listeners) {
			listener.onEvent(event);
		}
	}

}
