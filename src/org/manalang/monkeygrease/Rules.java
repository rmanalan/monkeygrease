/**
 * Monkeygrease
 * Copyright (C) 2005-2006 Rich Manalang
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal 
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE.
 */
package org.manalang.monkeygrease;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * The Rules class contains both the rules defined in the configuration file and
 * the rules to be applied to the current URL
 * 
 * @author Rich Manalang
 * @version 0.20 Build 308 Sep 22, 2006 18:03 GMT
 */
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
