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