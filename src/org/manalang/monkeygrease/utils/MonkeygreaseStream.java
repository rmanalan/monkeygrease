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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;

import org.manalang.monkeygrease.MonkeygreaseFilter;
import org.manalang.monkeygrease.Rule;
import org.manalang.monkeygrease.Rules;

/**
 * MonkeygreaseStream is used as the ServletOutputStream. This class performs
 * the actual insertion into the HTML.
 * 
 * @author Rich Manalang
 * @version 0.20 Build 308 Sep 22, 2006 18:03 GMT
 */
public class MonkeygreaseStream extends ServletOutputStream {
	private OutputStream intStream;

	private ByteArrayOutputStream baStream;

	private boolean closed = false;

	private Rules rulesToApply;

	private MonkeygreaseResponseWrapper wrapper;

	public MonkeygreaseStream(OutputStream outStream, Rules rules,
			MonkeygreaseResponseWrapper mgrWrapper) {
		intStream = outStream;
		baStream = new ByteArrayOutputStream();
		rulesToApply = rules;
		wrapper = mgrWrapper;
	}

	public void write(int i) throws IOException {
		baStream.write(i);
	}

	public void close() throws IOException {
		if (!closed) {
			processStream();
			intStream.close();
			closed = true;
		}
	}

	public void flush() throws IOException {
		if (baStream.size() != 0) {
			if (!closed) {
				processStream();
				baStream = new ByteArrayOutputStream();
			}
		}
	}

	public void processStream() throws IOException {
		// Don't bother processing if no rules apply
		if (rulesToApply.size() == 0) {
			intStream.write(baStream.toByteArray());
			intStream.flush();
		} else {
			intStream.write(processContent(baStream.toByteArray(),
					rulesToApply, wrapper));
			intStream.flush();
		}
	}

	public byte[] processContent(byte[] inBytes, Rules rulesToApply,
			MonkeygreaseResponseWrapper wrapper) {
		String strResponse = null;
		try {
			// force to UTF-8
			strResponse = new String(inBytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			strResponse = new String(inBytes);
		}
		String tagsToInsert = "";
		String modResponse = strResponse;
		String headBeginMarker = "<!-- mg#head#begin#marker -->";
		String bodyBeginMarker = "<!-- mg#body#begin#marker -->";
		boolean headBeginInserted = false;
		boolean bodyBeginInserted = false;

		Iterator rulesToApplyIter = rulesToApply.iterator();
		while (rulesToApplyIter.hasNext()) {
			Rule ruleToApply = (Rule) rulesToApplyIter.next();
			tagsToInsert = ruleToApply.toString();
			switch (ruleToApply.getInsertAt()) {
			case InsertAt.HEAD_BEGIN:
				if (!headBeginInserted) {
					if (tagsToInsert != "") {
						tagsToInsert = "<head>\n" + tagsToInsert
								+ headBeginMarker;
						modResponse = findAndReplace(tagsToInsert, modResponse,
								"<head>");
						headBeginInserted = true;
						MonkeygreaseFilter.log
								.fine("<head> found... inserting rule");
					} else {
						MonkeygreaseFilter.log.fine("No <head> found");
					}
				} else {
					if (tagsToInsert != "") {
						tagsToInsert = tagsToInsert + headBeginMarker;
						modResponse = findAndReplace(tagsToInsert, modResponse,
								headBeginMarker);
						MonkeygreaseFilter.log
								.fine("<head> found... inserting rule");
					} else {
						MonkeygreaseFilter.log.fine("No <head> found");
					}
				}
				break;

			case InsertAt.HEAD_END:
				if (tagsToInsert != "") {
					tagsToInsert += "</head>";
					modResponse = findAndReplace(tagsToInsert, modResponse,
							"</head>");
					MonkeygreaseFilter.log
							.fine("</head> found... inserting rule");
				} else {
					MonkeygreaseFilter.log.fine("No </head> found");
				}
				break;

			case InsertAt.BODY_BEGIN:
				if (!bodyBeginInserted) {
					if (tagsToInsert != "") {
						Pattern p = Pattern.compile("<body[^>]*>",
								Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
										| Pattern.DOTALL);
						Matcher m = p.matcher(modResponse);
						StringBuffer sb = new StringBuffer();
						do {
							if (!m.find())
								break;
							String origBody = m.group();
							if (origBody == null)
								continue;
							m.appendReplacement(sb, origBody.replaceAll("\\$",
									"\\\\\\$")
									+ "\n" + tagsToInsert + bodyBeginMarker);
							break;
						} while (true);
						m.appendTail(sb);
						modResponse = sb.toString();
						bodyBeginInserted = true;
						MonkeygreaseFilter.log
								.fine("<body> found... inserting rule");
					} else {
						MonkeygreaseFilter.log.fine("No <body> found");
					}
				} else {
					if (tagsToInsert != "") {
						tagsToInsert = tagsToInsert + bodyBeginMarker;
						modResponse = findAndReplace(tagsToInsert, modResponse,
								bodyBeginMarker);
						MonkeygreaseFilter.log
								.fine("<body> found... inserting rule");
					} else {
						MonkeygreaseFilter.log.fine("No <body> found");
					}
				}
				break;

			case InsertAt.BODY_END:
				if (tagsToInsert != "") {
					tagsToInsert += "\n</body>";
					modResponse = findAndReplace(tagsToInsert, modResponse,
							"</body>");
					MonkeygreaseFilter.log
							.fine("</body> found... inserting rule");
				} else {
					MonkeygreaseFilter.log.fine("No </body> found");
				}
				break;
			}
		}

		if (MonkeygreaseFilter.COMMENTS_ON) {
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
					DateFormat.LONG);
			modResponse = "<!-- This page has been Monkeygrease'd (Config file last loaded on "
					+ df.format(new Date(MonkeygreaseFilter.confLastLoad))
					+ ") -->\n" + modResponse;
		}
		wrapper.setContentLength(modResponse.length());
		return modResponse.getBytes();
	}

	private String findAndReplace(String tagsToInsert, String modResponse,
			String strPattern) {
		Pattern p = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(modResponse);
		modResponse = m.replaceFirst(tagsToInsert);
		return modResponse;
	}
}
