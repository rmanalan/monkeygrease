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
 * @version 0.13 Build 294 Apr 12, 2006 02:40 GMT
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
			sRule += "<!-- Monkeygrease Rule End -->\n";
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