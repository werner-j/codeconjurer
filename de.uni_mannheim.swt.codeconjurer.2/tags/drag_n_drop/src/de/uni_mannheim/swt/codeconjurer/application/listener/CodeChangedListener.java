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
package de.uni_mannheim.swt.codeconjurer.application.listener;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;

import de.uni_mannheim.swt.codeconjurer.application.CodeConjurer;

/**
 * Listener which should be notified on a change in Java Code. Should be added
 * to JavaCore.addElementChangedListener.
 * 
 * @author Werner Janjic
 * 
 */
public class CodeChangedListener implements IElementChangedListener {

	Logger logger = Logger.getLogger(CodeChangedListener.class.getName());

	private boolean ignoreNextEvent = false;

	/**
	 * This method is notified on a changed element and initiates a search if
	 * the appropriate change has occured.
	 */
	@Override
	public void elementChanged(ElementChangedEvent event) {
		IJavaElementDelta delta = event.getDelta();

		// We are interested only in AST changes
		if ((delta.getFlags() & IJavaElementDelta.F_AST_AFFECTED) != 0) {
			// Stop execution if we should ignore the event
			if (ignoreNextEvent) {
				ignoreNextEvent = false;
				return;
			}

			// Check if the affected type is a compilation unit
			if (checkAffectedType(delta, IJavaElement.COMPILATION_UNIT)) {
				logger.debug("Change to a compilation unit detected. Initiate search.");
				CodeConjurer.getInstance().search(true);
			}
			// Check if the affected type is a class
			if (checkAffectedType(delta, IJavaElement.TYPE)) {
				logger.debug("Change to a type detected. Initiate search.");
				CodeConjurer.getInstance().search(true);
			}
			// Check if the affected type is a method
			if (checkAffectedType(delta, IJavaElement.METHOD)) {
				logger.debug("Change to a method detected. Initiate search.");
				CodeConjurer.getInstance().search(true);
			}
		}

	}

	/**
	 * This method returns true if the last affected element of the delta is of
	 * an IJavaElement.Type
	 * 
	 * @param delta
	 *            the delta to check
	 * @param type
	 *            the type for which to check
	 * @return
	 */
	private boolean checkAffectedType(IJavaElementDelta delta, int type) {
		if (delta.getAffectedChildren().length == 0) {
			if (delta.getElement().getElementType() == type) {
				return true;
			}
		} else {
			for (IJavaElementDelta newDelta : delta.getAffectedChildren()) {
				return checkAffectedType(newDelta, type);
			}
		}
		return false;
	}

	/**
	 * Set this to true if the next code change event should be ignored
	 * 
	 * @param ignoreNextEvent
	 */
	public void ignoreNextEvent(boolean ignoreNextEvent) {
		this.ignoreNextEvent = ignoreNextEvent;
	}
}
