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
package de.uni_mannheim.swt.codeconjurer.application.util;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * This class is taken from
 * 
 * @author http://wiki.eclipse.org/
 *         FAQ_How_do_I_prevent_two_jobs_from_running_at_the_same_time
 * 
 */
public class ParallelSearchPrevention implements ISchedulingRule {

	public boolean isConflicting(ISchedulingRule rule) {
		return rule == this;
	}

	public boolean contains(ISchedulingRule rule) {
		return rule == this;
	}

}