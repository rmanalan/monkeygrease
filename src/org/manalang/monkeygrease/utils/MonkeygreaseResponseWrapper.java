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
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.manalang.monkeygrease.MonkeygreaseFilter;
import org.manalang.monkeygrease.Rule;
import org.manalang.monkeygrease.Rules;

/**
 * MonkeygreaseResponseWrapper is used as a wrapper to capture the original
 * response object in the Monkeygrease filter. The rules to be applied are also
 * processed in this class.
 * 
 * @author Rich Manalang
 * @version 0.20 Build 308 Sep 22, 2006 18:03 GMT
 */
public class MonkeygreaseResponseWrapper extends HttpServletResponseWrapper {
	private ByteArrayOutputStream output;

	private PrintWriter mgWriter;

	private MonkeygreaseStream mgStream;

	private int contentLength;

	private String contentType;

	private Rules rulesToApply;

	public MonkeygreaseResponseWrapper(HttpServletRequest hreq,
			ServletResponse resp, Rules rules) throws IOException {
		super((HttpServletResponse) resp);

		// filter rules based on request url
		rulesToApply = new Rules();
		Iterator rulesIter = rules.iterator();
		while (rulesIter.hasNext()) {
			Rule rule = (Rule) rulesIter.next();
			MonkeygreaseFilter.log.info("Rule being evaluated: "
					+ rule.getName());
			String uri = hreq.getRequestURI();
			String qry = hreq.getQueryString();
			String url = (qry != null) ? (uri + "?" + qry) : uri;

			MonkeygreaseFilter.log.info("Request URL to match: " + url);

			Pattern p = rule.getPattern();
			Matcher m = p.matcher(url);
			if (!m.matches()) {
				MonkeygreaseFilter.log
						.info("Request URL doesn't match pattern");
			} else {
				rulesToApply.add(rule);
				MonkeygreaseFilter.log
						.info("Request URL matches... adding rule to rulesToApply");
			}
		}
		MonkeygreaseFilter.log.info("Number of rules to apply: "
				+ rulesToApply.size());

		// Process response
		mgStream = new MonkeygreaseStream(resp.getOutputStream(), rulesToApply,
				this);
		mgWriter = new PrintWriter(mgStream);

	}

	public byte[] getData() {
		return output.toByteArray();
	}

	public ServletOutputStream getOutputStream() {
		return mgStream;
	}

	public PrintWriter getWriter() throws IOException {
		return mgWriter;
	}

	public void setContentLength(int length) {
		this.contentLength = length;
		super.setContentLength(length);
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String type) {
		this.contentType = type;
		super.setContentType(type);
	}

}
