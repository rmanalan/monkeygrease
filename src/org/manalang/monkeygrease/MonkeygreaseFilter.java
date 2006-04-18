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

import org.apache.commons.httpclient.HttpClient;
import org.manalang.monkeygrease.utils.InsertAt;
import org.manalang.monkeygrease.utils.LogFormatter;
import org.manalang.monkeygrease.utils.MonkeygreaseResponseWrapper;

/**
 * <strong>Monkeygrease servlet filter</strong>: the server-side Greasemonkey.
 * 
 * <p>
 * Monkeygrease is a simple servlet filter that will allow a web developer to
 * inject JavaScript, CSS or other elements within a web page. This concept was
 * inspired by the popular Greasemonkey extension for the Mozilla Firefox
 * browser. Greasemonkey empowers a user to alter and enhance any web page by
 * allowing them to execute user scripts (JavaScript) on any page.
 * </p>
 * 
 * <p>
 * Monkeygrease serves the same purpose as Greasemonkey, however, instead of
 * being a client-side solution, Monkeygrease is a server-side solution. This
 * allows all your users to benefit from enhancements you deploy to your site
 * through Monkeygrease. You might be wondering, “why not enhance the underlying
 * web page or web application?” Consider all of the packaged web applications
 * you have deployed. Many of these web applications are not that easy to
 * enhance. Some web applications are just not conducive to being customized.
 * Also, some of these web applications are “black-boxed” or closed source
 * products that just can’t be customized. Other products are just too
 * complicated and require expert knowledge to carry out interface based
 * customizations.
 * </p>
 * 
 * <p>
 * With Monkeygrease, you can forego customizations to the underlying web
 * application. Instead, you can rely on the power of JavaScript and Cascading
 * Style Sheets as a means to customizing a web application's interface. A few
 * examples of interface enhancements may include:
 * </p>
 * 
 * <ul>
 * <li>Changing the look and feel of a web application</li>
 * <li>Adding DHTML/Ajax features/effects on your site</li>
 * <li>Adding WYSIWYG editing to any textarea field on your site</li>
 * <li>Enhance pages by contextually adding content from external providers</li>
 * <li>Fix usability issues on any page</li>
 * </ul>
 * 
 * <p>
 * The enhancements you can make to your site with Monkeygrease are limitless.
 * Monkeygrease will enable you to move your web application to the Web 2.0
 * world. Forget about waiting for the next release of a packaged web
 * applications. Take matters into your own hands with Monkeygrease without
 * jeopordizing your existing web applications.
 * </p>
 * 
 * <p>
 * <strong>How to use Monkeygrease</strong>
 * </p>
 * 
 * <p>
 * This is the main servlet filter. You'll need to register this filter in your
 * web.xml file to use it. For more info, visit <a
 * href="http://monkeygrease.org" target="_blank">http://monkeygrease.org</a>.
 * </p>
 * 
 * @author Rich Manalang
 * @version 0.13 Build 294 Apr 12, 2006 02:40 GMT
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

	public static HttpClient client;

	public static boolean COMMENTS_ON;

	public static Logger log = Logger.getLogger("org.manalang.monkeygrease");

	private static FileHandler fh;

	public static String remoteConfigURL;

	/**
	 * Initializes Monkeygrease. Internal servlet filter method.
	 * 
	 * @param filterConfig
	 * @throws ServletException
	 */
	public void init(FilterConfig filterConfig) throws ServletException {

		try {
			fh = new FileHandler("monkeygrease_%u.log");
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
		remoteConfigURL = fc.getInitParameter("remoteConfigURL");
		if (remoteConfigURL != "" && remoteConfigURL != null)
			client = new HttpClient();

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
		
        HttpServletRequest hreq = (HttpServletRequest)request;
        
        // Exit if request is for XHR proxy servlet
        if(hreq.getRequestURI().equals("/monkeygreaseproxy"))
            return;

		log.info("*** Request Processing Begins ***");

		// Capture the stream which writes response to the client
		ServletOutputStream out = response.getOutputStream();

		// Create response wrapper so we can modify the original response
		MonkeygreaseResponseWrapper wrappedResponse = new MonkeygreaseResponseWrapper(
				(HttpServletResponse) response);
		
		// Exit if original response has a Content-Encoding header...
		// this prevents Monkeygrease from processing gzipped content.
		// There doesn't seem to be a way to get the actual value of 
		// a response header... need to look into this more.
		if (wrappedResponse.containsHeader("Content-Encoding"))
			return;

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
				
				if (remoteConfigURL == "") {
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
				} else {
					log.config("Conf file reloading from remote URL");
					cf.load();
					rules = cf.getRules();
				}

				confReloadInProgress = false;
			}

			// don't process if no rules available
			if (rules == null) {
				out.write(wrappedResponse.getData());
				out.close();
				return;
			}

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
						log.fine("<head> found... inserting rule");
					} else {
						log.fine("No <head> found");
					}
				} else {
					if (tagsToInsert != "") {
						tagsToInsert = tagsToInsert + headBeginMarker;
						modResponse = findAndReplace(tagsToInsert, modResponse,
								headBeginMarker);
						log.fine("<head> found... inserting rule");
					} else {
						log.fine("No <head> found");
					}
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
						log.fine("<body> found... inserting rule");
					} else {
						log.fine("No <body> found");
					}
				} else {
					if (tagsToInsert != "") {
						tagsToInsert = tagsToInsert + bodyBeginMarker;
						modResponse = findAndReplace(tagsToInsert, modResponse,
								bodyBeginMarker);
						log.fine("<body> found... inserting rule");
					} else {
						log.fine("No <body> found");
					}
				}
				break;

			case InsertAt.BODY_END:
				if (tagsToInsert != "") {
					tagsToInsert += "\n</body>";
					modResponse = findAndReplace(tagsToInsert, modResponse,
							"</body>");
					log.fine("</body> found... inserting rule");
				} else {
					log.fine("No </body> found");
				}
				break;
			}
		}

		if (COMMENTS_ON) {
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
					DateFormat.LONG);
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
