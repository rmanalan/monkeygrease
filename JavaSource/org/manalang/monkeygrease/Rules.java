/**
 * Monkeygrease
 * Copyright (C) 2005 Rich Manalang
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.manalang.monkeygrease;

import java.util.ArrayList;
import java.util.Iterator;

public class Rules extends ArrayList {

	private static final long serialVersionUID = 1L;

	public Rules() {
		super();
	}

	public String toString() {
		String sRule = "";
		Iterator iter = this.iterator();

		while (iter.hasNext()) {
			Rule rule = (Rule) iter.next();
			sRule += rule.toString();
		}
		return sRule;
	}
}
