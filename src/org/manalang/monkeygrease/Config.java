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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

public class Config {

	private static final String DEFAULT_WEB_CONF_FILE = "/WEB-INF/monkeygrease.xml";

	private static ServletContext context;

	private Rules rules;

	public Config(ServletContext sc) {
		context = sc;
		load();
	}

	public synchronized void load() {
		
		InputStream is = null;
		GetMethod method = null;

		if (MonkeygreaseFilter.remoteConfigURL != "" &&
				MonkeygreaseFilter.remoteConfigURL != null) {
			method = new GetMethod(MonkeygreaseFilter.remoteConfigURL);
			method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
					new DefaultHttpMethodRetryHandler(3, false));
			try {
				// Execute the method.
				int statusCode = MonkeygreaseFilter.client.executeMethod(method);

				if (statusCode != HttpStatus.SC_OK) {
					MonkeygreaseFilter.log.severe("Method failed: "
							+ method.getStatusLine());
				}
				is = method.getResponseBodyAsStream();
			} catch (HttpException e) {
				MonkeygreaseFilter.log.severe("Fatal protocol violation: "
						+ e.getMessage());
			} catch (IOException e) {
				MonkeygreaseFilter.log.severe("Fatal transport error: "
						+ e.getMessage());
			}

		} else {
			is = context.getResourceAsStream(DEFAULT_WEB_CONF_FILE);

			if (is == null) {
				System.out.println("unable to find monkeygrease conf file "
						+ DEFAULT_WEB_CONF_FILE);
			}
		}
		
		DocumentBuilder parser;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// factory.setValidating(true);
		// factory.setNamespaceAware(true);
		// factory.setIgnoringComments(true);
		// factory.setIgnoringElementContentWhitespace(true);
		try {
			parser = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			MonkeygreaseFilter.log
					.severe("Unable to setup XML parser for reading conf "
							+ e.toString());
			return;
		}

		// parser.setErrorHandler(handler);
		// parser.setEntityResolver(handler);

		try {
			Document doc = parser.parse(is);
			
			if (MonkeygreaseFilter.remoteConfigURL != "" &&
					MonkeygreaseFilter.remoteConfigURL != null )
				method.releaseConnection();

			NodeList rulesConf = doc.getElementsByTagName("rule");
			rules = new Rules();

			for (int i = 0; i < rulesConf.getLength(); i++) {
				Node ruleNode = rulesConf.item(i);
				Rule rule = new Rule(ruleNode);
				if (rule.isEnabled())
					rules.add(rule);
			}
			// processConfDoc(doc);
		} catch (SAXParseException e) {
			MonkeygreaseFilter.log.severe("Parse error on line "
					+ e.getLineNumber() + " " + e.toString());
		} catch (Exception e) {
			MonkeygreaseFilter.log.severe("Exception loading conf " + " "
					+ e.toString());
		}
	}

	public Rules getRules() {
		return rules;
	}

	public String getDEFAULT_WEB_CONF_FILE() {
		return DEFAULT_WEB_CONF_FILE;
	}
}
