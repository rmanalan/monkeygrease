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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.manalang.monkeygrease.utils.InsertAt;
import org.manalang.monkeygrease.utils.LogFormatter;
import org.manalang.monkeygrease.utils.MonkeygreaseResponseWrapper;

/**
 * <strong>Monkeygrease servlet filter</strong>: the server-side Greasemonkey.
 * This is the main servlet filter. You'll need to register this filter in your
 * web.xml file to use it. For more info, visit <a
 * href="http://monkeygrease.org" target="_blank">http://monkeygrease.org</a>.
 * 
 * @author Rich Manalang
 * @version 0.10 Build 173 Oct 25, 2005 23:45 GMT
 */
public class MonkeygreaseFilter implements Filter {

	private final static int SEVERE = 0;

	private final static int WARNING = 1;

	private final static int INFO = 2;

	private final static int CONFIG = 3;

	private final static int FINE = 4;

	private final static int FINER = 5;

	private final static int FINEST = 6;

	private FilterConfig fc = null;

	private ServletContext sc = null;

	private Config cf;

	private Rules rules;

	private int confReloadCheckInterval;

	private String confReloadCheckIntervalStr;

	private boolean confReloadCheckEnabled;

	private boolean confReloadInProgress;

	private long confReloadLastCheck;

	private long confLastLoad;

	public static boolean COMMENTS_ON;

	public static Logger log = Logger.getLogger("org.manalang.monkeygrease");

	private static FileHandler fh;

	/**
	 * Initializes Monkeygrease. Internal servlet filter method.
	 * 
	 * @param filterConfig
	 * @throws ServletException
	 */
	public void init(FilterConfig filterConfig) throws ServletException {

		try {
			fh = new FileHandler("monkeygrease%u.log");
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Send logger output to our FileHandler.
		log.addHandler(fh);
		log.getHandlers()[0].setFormatter(new LogFormatter());

		fc = filterConfig;
		confReloadCheckIntervalStr = fc
				.getInitParameter("confReloadCheckInterval");
		String commentsOnStr = fc.getInitParameter("commentsOn");
		COMMENTS_ON = commentsOnStr.toLowerCase().equals("true");
		String logLevel = fc.getInitParameter("logLevel");
		int logLevelInt;
		if (logLevel != null)
			logLevelInt = Integer.parseInt(logLevel);
		else
			logLevelInt = 0;

		switch (logLevelInt) {
		case SEVERE:
			log.setLevel(Level.SEVERE);
			break;
		case WARNING:
			log.setLevel(Level.WARNING);
			break;
		case INFO:
			log.setLevel(Level.INFO);
			break;
		case CONFIG:
			log.setLevel(Level.CONFIG);
			break;
		case FINE:
			log.setLevel(Level.FINE);
			break;
		case FINER:
			log.setLevel(Level.FINER);
			break;
		case FINEST:
			log.setLevel(Level.FINEST);
			break;
		default:
			log.setLevel(Level.SEVERE);
			break;
		}

		if (confReloadCheckIntervalStr != null
				&& !"".equals(confReloadCheckIntervalStr)) {
			// convert to millis
			confReloadCheckInterval = 1000 * Integer
					.parseInt(confReloadCheckIntervalStr);
			confReloadCheckEnabled = true;
			if (confReloadCheckInterval == 0) {
				log.config("Reload check performed on each request");
			} else {
				log.config("Reload check set to " + confReloadCheckInterval
						/ 1000 + "s");
			}
		} else {
			confReloadCheckEnabled = false;
		}

		sc = fc.getServletContext();
		cf = new Config(sc);
		rules = cf.getRules();

	}

	/**
	 * Destroys filter. Internal servlet filter method.
	 */
	public void destroy() {
		fc = null;
		fh.close();
	}

	/**
	 * Main filter process. Internal servlet filter method.
	 * 
	 * @param request
	 * @param response
	 * @param chain
	 * @throws IOException
	 * @throws ServletException
	 */
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (fc == null)
			return;

		log.info("*** Request Processing Begins ***");

		// Capture the stream which writes response to the client
		ServletOutputStream out = response.getOutputStream();

		// Create response wrapper so we can modify the original response
		MonkeygreaseResponseWrapper wrappedResponse = new MonkeygreaseResponseWrapper(
				(HttpServletResponse) response);

		// Process request, return response to wrappedResponse
		chain.doFilter(request, wrappedResponse);

		// Make sure we only process HTML content
		String ct = wrappedResponse.getContentType();

		log.fine("Request content type: " + ct);
		if (ct != null && ct.indexOf("text/html") != -1) {

			// check to see if the conf needs reloading
			long now = System.currentTimeMillis();
			if (confReloadCheckEnabled && !confReloadInProgress
					&& (now - confReloadCheckInterval) > confReloadLastCheck) {
				confReloadInProgress = true;
				confReloadLastCheck = now;

				log.fine("starting conf reload check");
				long confFileCurrentTime = getConfFileLastModified();
				if (confLastLoad < confFileCurrentTime) {
					// reload conf
					confLastLoad = System.currentTimeMillis();
					log.config("Conf file modified since last load, reloading");
					cf.load();
					rules = cf.getRules();
				} else {
					log.config("Conf is not modified");
				}

				confReloadInProgress = false;
			}

			// don't process if no rules available
			if (rules == null) {
				out.write(wrappedResponse.getData());
				out.close();
				return;
			}

			HttpServletRequest hreq = (HttpServletRequest) request;

			log.info("Number of rules loaded: " + rules.size());

			// filter rules based on request url
			Rules rulesToApply = new Rules();
			Iterator rulesIter = rules.iterator();
			while (rulesIter.hasNext()) {
				Rule rule = (Rule) rulesIter.next();
				log.info("Rule being evaluated: " + rule.getName());
				String uri = hreq.getRequestURI();
				String qry = hreq.getQueryString();
				String url = (qry != null) ? (uri + "?" + qry) : uri;

				log.info("Request URL to match: " + url);

				Pattern p = rule.getPattern();
				Matcher m = p.matcher(url);
				if (!m.matches()) {
					log.info("Request URL doesn't match pattern");
				} else {
					rulesToApply.add(rule);
					log
							.info("Request URL matches... adding rule to rulesToApply");
				}
			}
			log.info("Number of rules to apply: " + rulesToApply.size());

			// Don't bother processing if no rules apply
			if (rulesToApply.size() == 0) {
				out.write(wrappedResponse.getData());
				out.close();
				return;
			}

			// Process response
			String strResponse = new String((wrappedResponse.getData()));
			String newResponse = processResponse(strResponse, rulesToApply);

			// Reset content length header based on modified response
			wrappedResponse.setContentLength(newResponse.length());

			// write the modified response back out through the original
			// response stream
			out.write(newResponse.getBytes());
		} else
			// if not text/html write out original response
			out.write(wrappedResponse.getData());

		// finally, close the response writer
		out.close();
		log.info("*** Request Processing Ends ***");
	}

	private long getConfFileLastModified() {
		File confFile = new File(sc.getRealPath(cf.getDEFAULT_WEB_CONF_FILE()));
		return confFile.lastModified();
	}

	/**
	 * Processes response rules. Returns the response as a string with rules
	 * applied.
	 * 
	 * @param sResponse
	 * @return Modified response string based on rules applied
	 */
	public String processResponse(String strResponse, Rules rulesToApply) {
		String tagsToInsert = "";
		String modResponse = strResponse;

		Iterator rulesToApplyIter = rulesToApply.iterator();
		while (rulesToApplyIter.hasNext()) {
			Rule ruleToApply = (Rule) rulesToApplyIter.next();
			tagsToInsert = ruleToApply.toString();
			switch (ruleToApply.getInsertAt()) {
			case InsertAt.HEAD_BEGIN:
				if (tagsToInsert != "") {
					tagsToInsert = "<head>\n" + tagsToInsert;
					modResponse = findAndReplace(tagsToInsert, modResponse,
							"<head>");
					log.fine("<head> found... inserting rule");
				} else {
					log.fine("No <head> found");
				}
				break;

			case InsertAt.HEAD_END:
				if (tagsToInsert != "") {
					tagsToInsert += "</head>";
					modResponse = findAndReplace(tagsToInsert, modResponse,
							"</head>");
					log.fine("</head> found... inserting rule");
				} else {
					log.fine("No </head> found");
				}
				break;

			case InsertAt.BODY_BEGIN:
				if (tagsToInsert != "") {
					Pattern p = Pattern.compile("<body[^>]*>",
							Pattern.CASE_INSENSITIVE);
					Matcher m = p.matcher(modResponse);
					StringBuffer sb = new StringBuffer();
					while (m.find()) {
						if (m.group() != null) {
							m.appendReplacement(sb, m.group() + "\n" + tagsToInsert);
						}
					}
					m.appendTail(sb);
					modResponse = sb.toString();
					log.fine("<body> found... inserting rule");
				} else {
					log.fine("No <body> found");
				}
				break;

			case InsertAt.BODY_END:
				if (tagsToInsert != "") {
					tagsToInsert += "</body>";
					modResponse = findAndReplace(tagsToInsert, modResponse,
							"</body>");
					log.fine("</body> found... inserting rule");
				} else {
					log.fine("No </body> found");
				}
				break;
			}
		}

		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
				DateFormat.LONG);

		if (COMMENTS_ON) {
			modResponse = "<!-- This page has been Monkeygrease'd (Config file last loaded on "
					+ df.format(new Date(confLastLoad))
					+ ") -->\n"
					+ modResponse;
		}
		return modResponse;
	}

	private String findAndReplace(String tagsToInsert, String modResponse,
			String strPattern) {
		Pattern p = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(modResponse);
		modResponse = m.replaceFirst(tagsToInsert);
		return modResponse;
	}
}
