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

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
		InputStream is = context.getResourceAsStream(DEFAULT_WEB_CONF_FILE);

		if (is == null) {
			System.out.println("unable to find monkeygrease conf file "
					+ DEFAULT_WEB_CONF_FILE);
			return;
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
			MonkeygreaseFilter.log.severe("Unable to setup XML parser for reading conf "
					+ e.toString());
			return;
		}

		// parser.setErrorHandler(handler);
		// parser.setEntityResolver(handler);

		try {
			Document doc = parser.parse(is);
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
			MonkeygreaseFilter.log.severe("Parse error on line " + e.getLineNumber() + " "
					+ e.toString());
		} catch (Exception e) {
			MonkeygreaseFilter.log.severe("Exception loading conf " + " " + e.toString());
		}
	}

	public Rules getRules() {
		return rules;
	}
	
	public String getDEFAULT_WEB_CONF_FILE(){
		return DEFAULT_WEB_CONF_FILE;
	}
}
