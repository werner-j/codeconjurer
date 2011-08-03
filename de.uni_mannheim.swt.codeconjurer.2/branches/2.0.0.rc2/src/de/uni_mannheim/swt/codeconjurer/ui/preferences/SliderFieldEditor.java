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
package de.uni_mannheim.swt.codeconjurer.ui.preferences;

import org.apache.log4j.Logger;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;

/**
 * @author Werner Janjic
 * 
 */
public class SliderFieldEditor extends FieldEditor {

	private Composite parent;
	private Logger logger = Logger.getLogger(SliderFieldEditor.class);

	private int counter = 0;
	private Composite sliderComp;
	private Slider resultSlider;
	private Label maxResults;

	public SliderFieldEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
	}

	public SliderFieldEditor(String name, String labelText, int minValue,
			int maxValue, int steps, Composite parent) {
		super(name, labelText, parent);
		setSliderValues(minValue, maxValue, steps);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#adjustForNumColumns(int)
	 */
	@Override
	protected void adjustForNumColumns(int numColumns) {
		GridData gd = (GridData) resultSlider.getParent().getLayoutData();
		gd.horizontalSpan = 1;
		// We only grab excess space if we have to
		// If another field editor has more columns then
		// we assume it is setting the width.
		gd.grabExcessHorizontalSpace = gd.horizontalSpan == 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.preference.FieldEditor#doFillIntoGrid(org.eclipse.swt
	 * .widgets.Composite, int)
	 */
	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		this.parent = parent;
		Label label = getLabelControl(this.parent);
		GridData labelData = new GridData();
		labelData.horizontalSpan = 1;
		label.setLayoutData(labelData);

		sliderComp = new Composite(this.parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginRight = 50;
		gl.numColumns = 2;
		sliderComp.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = gd.horizontalSpan == 1;
		sliderComp.setLayoutData(gd);

		resultSlider = new Slider(sliderComp, SWT.NONE);
		maxResults = new Label(sliderComp, SWT.NONE);
		maxResults.setLayoutData(gd);

		resultSlider.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				setCounter(resultSlider.getSelection());
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doLoad()
	 */
	@Override
	protected void doLoad() {
		int counter = getPreferenceStore().getInt(getPreferenceName());
		setCounter(counter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
	 */
	@Override
	protected void doLoadDefault() {
		int defaultValue = getPreferenceStore().getDefaultInt(
				getPreferenceName());
		setCounter(defaultValue);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#doStore()
	 */
	@Override
	protected void doStore() {
		int value = getCounter();
		getPreferenceStore().setValue(getPreferenceName(), value);
		logger.debug("Stored " + value + " for " + getPreferenceName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditor#getNumberOfControls()
	 */
	@Override
	public int getNumberOfControls() {
		return 2;
	}

	/**
	 * Set current value of the slider
	 * 
	 * @return
	 */
	protected int getCounter() {
		return counter;
	}

	/**
	 * Set counter value of the slider
	 * 
	 * @param counter
	 */
	protected void setCounter(int counter) {
		this.counter = counter;
		resultSlider.setSelection(counter);
		StringBuilder counterString = new StringBuilder();
		counterString.append(counter);
		maxResults.setText(counterString.toString());
	}

	/**
	 * Set the slider's values
	 * 
	 * @param minValue
	 *            new minimum value
	 * @param maxValue
	 *            new maximum value
	 * @params steps value of one de/incremental step
	 */
	protected void setSliderValues(int minValue, int maxValue, int steps) {
		resultSlider
				.setValues(counter, minValue, maxValue, steps, steps, steps);
	}

}
