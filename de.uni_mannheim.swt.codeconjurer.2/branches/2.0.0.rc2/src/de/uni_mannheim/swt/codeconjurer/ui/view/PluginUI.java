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
package de.uni_mannheim.swt.codeconjurer.ui.view;

import org.apache.log4j.Logger;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

/**
 * @author Werner Janjic
 * 
 */
public class PluginUI {

	private static Logger logger = Logger.getLogger(PluginUI.class);

	private static IWorkbenchWindow window;

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

	public static void showRecommendationsView() {
		PluginUI.getWindow().getWorkbench().getDisplay()
				.asyncExec(new Runnable() {

					@Override
					public void run() {
						try {
							PluginUI.getWindow()
									.getActivePage()
									.showView(
											"com.merobase.app.codeconjurer.views.ResultView",
											null, IWorkbenchPage.VIEW_ACTIVATE);
						} catch (PartInitException e1) {
							logger.debug("Could not open Reuse View!\r\n"
									+ e1.getLocalizedMessage());
						}
					}
				});
	}

}
