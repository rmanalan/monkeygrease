/**
 * Monkeygrease
 * Copyright (C) 2005 Rich Manalang
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
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;


/**
 * GenericResponseWrapper is used as a wrapper to capture the original
 * response object in the Monkeygrease filter.
 * 
 * @author Rich Manalang
 * @version 0.12 Build 270 Jan 24, 2006 17:40 GMT
 */
public class MonkeygreaseResponseWrapper
   extends HttpServletResponseWrapper
{
  private ByteArrayOutputStream output;

  private int contentLength;

  private String contentType;

  public MonkeygreaseResponseWrapper(HttpServletResponse response)
  {
    super(response);
    output = new ByteArrayOutputStream();
  }

  public byte[] getData()
  {
    return output.toByteArray();
  }

  public ServletOutputStream getOutputStream()
  {
    return new FilterServletOutputStream(output);
  }

  public void setContentLength(int length)
  {
    this.contentLength = length;
    super.setContentLength(length);
  }

  public int getContentLength()
  {
    return contentLength;
  }

  public String getContentType()
  {
    return contentType;
  }

  public void setContentType(String type)
  {
    this.contentType = type;
    super.setContentType(type);
  }

  public PrintWriter getWriter()
  {
    return new PrintWriter(getOutputStream(), true);
  }

}
