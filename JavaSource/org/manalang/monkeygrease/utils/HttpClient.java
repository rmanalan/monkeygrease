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
package org.manalang.monkeygrease.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

/**
 * HttpClient for Monkeygrease. Allows for cross-domain XMLHttpRequest using the
 * Monkeygrease JavaScript library which relies on Prototype.
 * 
 * This servlet is dependent on the following Apache Jakarta Commons libraries:
 * <ul>
 * <li><a href="http://jakarta.apache.org/commons/httpclient/"
 * target="_blank">HttpClient<a></li>
 * <li><a href="http://jakarta.apache.org/commons/logging/"
 * target="_blank">Logging<a></li>
 * <li><a href="http://jakarta.apache.org/commons/codec/" target="_blank">Codec<a></li>
 * </ul>
 * You will need to download these libraries in order to use this feature of
 * Monkeygrease. Visit the <a
 * href="http://monkeygrease.org/documentation/">Monkeygrease</a> website for
 * more documentation on this feature.
 * 
 * @author Rich Manalang
 * @version 0.12 Build 260 Nov 11, 2005 01:11 GMT
 */
public class HttpClient extends javax.servlet.http.HttpServlet implements
		javax.servlet.Servlet {
	private HostConfiguration hc;

	private org.apache.commons.httpclient.HttpClient client;

	public HttpClient() {
		super();
	}

	/*
	 * Initializes servlet with parameters from web.xml file
	 * 
	 * @see javax.servlet.GenericServlet#init()
	 */
	public void init() throws ServletException {
		// Sets HTTP proxy host and port if it exists in the web.xml
		super.init();
		String proxyHost = this.getInitParameter("ProxyHost");
		String proxyPortStr = this.getInitParameter("ProxyPort");
		System.out.println(proxyHost);
		int proxyPort = 0;
		if (proxyPortStr != null || proxyPortStr != "")
			proxyPort = Integer.parseInt(proxyPortStr);
		if (proxyHost != null && proxyHost != "" && proxyPort != 0) {
			hc = new HostConfiguration();
			hc.setProxy(proxyHost, proxyPort);
		}

		// Create an instance of HttpClient.
		client = new org.apache.commons.httpclient.HttpClient();

		// Sets proxy if it exists
		if (hc != null)
			client.setHostConfiguration(hc);
	}

	/*
	 * This is the main HttpClient method. This servlet only allows the post
	 * method.
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String url = request.getParameter("u");
		String method = request.getParameter("m");
		String params = request.getParameter("p");
		InputStream is = null;

		if (url == null || url == "")
			return;

		if (method != null && method.toLowerCase().equals("post")) {
			// Create a method instance.
			PostMethod postMethod = new PostMethod(url);

			// If params avail, set post data
			if (params != null && params != "") {
				String[] paramArray = params.split("&");
				NameValuePair[] postData = new NameValuePair[paramArray.length];
				for (int i = 0; i < paramArray.length; i++) {
					String[] nameVal = paramArray[i].split("=");
					String name = nameVal[0];
					String value = nameVal[1];
					postData[i] = new NameValuePair(name, value);
				}
				postMethod.setRequestBody(postData);
			}
			
			// Provide custom retry handler is necessary
			postMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
					new DefaultHttpMethodRetryHandler(2, false));

			try {
				// Execute the method.
				int statusCode = client.executeMethod(postMethod);

				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: "
							+ postMethod.getStatusLine());
				}

				// Read the response body.
				is = postMethod.getResponseBodyAsStream();
				Header[] headers = postMethod.getResponseHeaders();
				for (int i = 0; i < headers.length; i++) {
					Header header = headers[i];
					response.setHeader(header.getName(), header.getValue());
				}
				PrintWriter out = response.getWriter();
				BufferedReader in = new BufferedReader(
						new InputStreamReader(is));
				String line;
				while ((line = in.readLine()) != null) {
					out.println(line);
				}
				out.close();

			} catch (HttpException e) {
				System.err.println("Fatal protocol violation: "
						+ e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Fatal transport error: " + e.getMessage());
				e.printStackTrace();
			} finally {
				// Release the connection.
				postMethod.releaseConnection();
			}

		} else {
			if (params != null) {
				if (url.indexOf("?") == -1) {
					url = url + "?" + params;
				} else {
					url = url + "&" + params;
				}
			}

			// Create a method instance.
			GetMethod getMethod = new GetMethod(url);

			// Provide custom retry handler is necessary
			getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
					new DefaultHttpMethodRetryHandler(2, false));

			try {
				// Execute the method.
				int statusCode = client.executeMethod(getMethod);

				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: "
							+ getMethod.getStatusLine());
				}

				// Read the response body.
				is = getMethod.getResponseBodyAsStream();
				Header[] headers = getMethod.getResponseHeaders();
				for (int i = 0; i < headers.length; i++) {
					Header header = headers[i];
					response.setHeader(header.getName(), header.getValue());
				}
				PrintWriter out = response.getWriter();
				BufferedReader in = new BufferedReader(
						new InputStreamReader(is));
				String line;
				while ((line = in.readLine()) != null) {
					out.println(line);
				}
				out.close();

				// Deal with the response.
				// Use caution: ensure correct character encoding and is not
				// binary data

			} catch (HttpException e) {
				System.err.println("Fatal protocol violation: "
						+ e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Fatal transport error: " + e.getMessage());
				e.printStackTrace();
			} finally {
				// Release the connection.
				getMethod.releaseConnection();
			}
		}
	}
}