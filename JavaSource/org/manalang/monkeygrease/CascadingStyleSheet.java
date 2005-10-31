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

public class CascadingStyleSheet {
	private String href;

	private String media;

	public CascadingStyleSheet() {
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getMedia() {
		return media;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public String toString() {
		String mediaAttrib = "";
		if (media != null) {
			mediaAttrib = "media=\"" + media + "\"";
		}
		if (href != null) {
			return "<link href=\"" + href + "\" rel=\"stylesheet\" "
					+ mediaAttrib + " type=\"text/css\">\n";
		} else {
			return "";
		}
	}
}