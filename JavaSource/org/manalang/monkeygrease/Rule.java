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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.manalang.monkeygrease.utils.InsertAt;
import org.w3c.dom.CDATASection;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Rich Manalang
 * @version 0.11 Build 249 Nov 07, 2005 19:21 GMT
 */
public class Rule {

	private String name;

	private Pattern pattern;

	private boolean enabled;

	private String elements = "";

	private int insertAt;

	private String strInsertAt;

	public Rule(Node ruleNode) throws TransformerFactoryConfigurationError,
			TransformerException {

		name = getAttrValue(ruleNode, "name");
		setPattern(getAttrValue(ruleNode, "url-pattern"));
		enabled = getAttrValue(ruleNode, "enabled").equals("true");
		strInsertAt = getAttrValue(ruleNode, "insert-at");
		setInsertAt(strInsertAt);
		MonkeygreaseFilter.log.config("Processing rule: " + name);

		if (enabled) {
			MonkeygreaseFilter.log.config("Rule enabled... looking for items");
			NodeList items = ruleNode.getChildNodes();
			for (int i = 0; i < items.getLength(); i++) {
				Node item = items.item(i);
				if (item.getNodeType() == Node.CDATA_SECTION_NODE) {
					CDATASection cdata = (CDATASection) item;
					elements += cdata.getNodeValue().trim() + "\n";
				}
			}

		} else {
			MonkeygreaseFilter.log.config("Rule disabled... moving on");
		}
	}

	private static String getAttrValue(Node n, String attrName) {
		if (n == null)
			return null;
		NamedNodeMap attrs = n.getAttributes();
		if (attrs == null)
			return null;
		Node attr = attrs.getNamedItem(attrName);
		if (attr == null)
			return null;
		String val = attr.getNodeValue();
		if (val == null)
			return null;
		return val.trim();
	}

	public String toString() {
		String sRule = "";
		if (MonkeygreaseFilter.COMMENTS_ON) {
			sRule += "<!-- Monkeygrease Rule Begin: " + this.getName() + " "
					+ this.strInsertAt + " -->\n" + this.elements;
			sRule += "<!-- Monkeygrease Rule End -->";
		} else {
			sRule += this.elements;
		}
		return sRule;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(String p) throws PatternSyntaxException {
		MonkeygreaseFilter.log.fine("Regex pattern to compile: " + p);
		try {
			pattern = Pattern.compile(p);
		} catch (Exception e) {
			MonkeygreaseFilter.log.fine(e.toString());
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int getInsertAt() {
		return insertAt;
	}

	public void setInsertAt(String strInsertAt) {
		if (strInsertAt.equals("head-begin".toLowerCase())) {
			insertAt = InsertAt.HEAD_BEGIN;
		}
		if (strInsertAt.equals("head-end".toLowerCase())) {
			insertAt = InsertAt.HEAD_END;
		}
		if (strInsertAt.equals("body-begin".toLowerCase())) {
			insertAt = InsertAt.BODY_BEGIN;
		}
		if (strInsertAt.equals("body-end".toLowerCase())) {
			insertAt = InsertAt.BODY_END;
		}
	}
}