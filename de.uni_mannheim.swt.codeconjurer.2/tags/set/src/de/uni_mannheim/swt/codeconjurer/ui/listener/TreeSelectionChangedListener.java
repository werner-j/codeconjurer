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
package de.uni_mannheim.swt.codeconjurer.ui.listener;

import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import de.uni_mannheim.swt.codeconjurer.Activator;
import de.uni_mannheim.swt.codeconjurer.domain.preferences.PreferenceConstants;
import de.uni_mannheim.swt.codeconjurer.domain.result.ResultProperty;
import de.uni_mannheim.swt.codeconjurer.domain.search.Search;
import de.uni_mannheim.swt.codeconjurer.ui.view.elements.CodePreview;

/**
 * @author Werner Janjic
 * 
 */
public class TreeSelectionChangedListener implements ISelectionChangedListener {

	private CodePreview preview;

	public TreeSelectionChangedListener(CodePreview preview) {
		this.preview = preview;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(
	 * org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		BodyDeclaration selected = (BodyDeclaration) selection
				.getFirstElement();

		// Show the source code of the selection
		if (preview != null) {
			if (selected != null) {
				String previewCode = "";
				String adapterCode = "";
				if (selected.getNodeType() == BodyDeclaration.TYPE_DECLARATION) {
					previewCode = (String) selected
							.getProperty(ResultProperty.RAW_SOURCE.name());
					adapterCode = (String) selected
							.getProperty(ResultProperty.TEST_RESULT.name());
				} else {
					previewCode = selected.toString();
					adapterCode = "// No adapter code available";
				}
				boolean showAdapter = Activator.getDefault()
						.getPreferenceStore()
						.getBoolean(PreferenceConstants.P_SHOW_ADAPTER);
				String searchKind = selected.getProperty(
						ResultProperty.SEARCH_KIND.name()).toString();
				if (showAdapter
						&& searchKind.equals(String
								.valueOf(Search.TEST_DRIVEN_SEARCH))) {
					preview.setCode(adapterCode);
				} else {
					preview.setCode(previewCode);
				}
			} else {
				preview.setCode("");
			}
		}
	}

}
