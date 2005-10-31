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

import java.io.StringWriter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.manalang.monkeygrease.utils.InsertAt;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * @author rmanalan
 * @version 0.10 Build 173 Oct 25, 2005 23:45 GMT
 */
public class Rule {

	private String name;

	private Pattern pattern;

	private boolean enabled;

	private String elements = "";

	private int insertAt;

	private String strInsertAt;

	private Transformer outerXmlTransformer;

	public Rule(Node ruleNode) throws TransformerFactoryConfigurationError,
			TransformerException {
		
		// sets up the xml transformer so we can serialize generic elements
		outerXmlTransformer = TransformerFactory.newInstance().newTransformer();
		outerXmlTransformer.setOutputProperty("omit-xml-declaration", "yes"); 
		
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
				if (item.getNodeName() == "item") {
					String type = getAttrValue(item, "type");
					if (type.equals("javascript")) {
						MonkeygreaseFilter.log.config("Found JavaScript item");
						Javascript js = new Javascript();
						js.setSrc(getNodeValue(item));
						elements += js.toString();
					} else if (type.equals("css")) {
						MonkeygreaseFilter.log.config("Found CSS item");
						CascadingStyleSheet css = new CascadingStyleSheet();
						css.setHref(getNodeValue(item));
						String media = getAttrValue(item, "media");
						if (media != null) {
							css.setMedia(media);
						}
						elements += css.toString();
					}
				} else {
					if (item.getNodeType() == Node.ELEMENT_NODE) {
						MonkeygreaseFilter.log.config("Found generic element");
						GenericElement ge = new GenericElement();
						ge.setElement(getOuterXml(item));
						elements += ge.toString();
					}
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

	private static String getNodeValue(Node node) {
		if (node == null)
			return null;
		NodeList nodeList = node.getChildNodes();
		if (nodeList == null)
			return null;
		Node child = nodeList.item(0);
		if (child == null)
			return null;
		if ((child.getNodeType() == Node.TEXT_NODE)) {
			String value = ((Text) child).getData();
			return value.trim();
		}
		return null;
	}

	public String getOuterXml(Node node) throws TransformerException {
		DOMSource nodeSource = new DOMSource(node);
		StringWriter resultStringWriter = new StringWriter();
		StreamResult streamResult = new StreamResult(resultStringWriter);
		outerXmlTransformer.transform(nodeSource, streamResult);
		return resultStringWriter.toString();
	}

	public String getInnerXml(Node node) throws TransformerException {
		StringBuffer innerXml = new StringBuffer();
		if (node.hasChildNodes()) {
			NodeList childNodes = node.getChildNodes();
			int i = childNodes.getLength();
			for (int c = 0; c < i; c++) {
				innerXml.append(getOuterXml(childNodes.item(c)));
			}
			return innerXml.toString();
		} else {
			return "";
		}
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
			return;
		}
		if (strInsertAt.equals("head-end".toLowerCase())) {
			insertAt = InsertAt.HEAD_END;
			return;
		}
		if (strInsertAt.equals("body-begin".toLowerCase())) {
			insertAt = InsertAt.BODY_BEGIN;
			return;
		}
		if (strInsertAt.equals("body-end".toLowerCase())) {
			insertAt = InsertAt.BODY_END;
			return;
		}
	}
}