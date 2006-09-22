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
package org.manalang.monkeygrease.utils;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * LogFormatter is used to format the log file Monkeygrease creates
 * 
 * @author Rich Manalang
 * @version 0.20 Build 308 Sep 22, 2006 18:03 GMT
 */
public class LogFormatter extends Formatter {

	public LogFormatter() {
		super();
	}

	public String format(LogRecord lr) {
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT);
		String slr = df.format(new Date(lr.getMillis())) + " (" + lr.getLevel()
				+ "): " + lr.getMessage() + "\n";
		return slr;
	}

}
