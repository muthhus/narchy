//    RED - A Java Editor Library
//    Copyright (C) 2003  Robert Lichtenberger
//
//    This library is free software; you can redistribute it and/or
//    modify it under the terms of the GNU Lesser General Public
//    License as published by the Free Software Foundation; either
//    version 2.1 of the License, or (at your option) any later version.
//
//    This library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//    Lesser General Public License for more details.
//
//    You should have received a copy of the GNU Lesser General Public
//    License along with this library; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
package red;

/** REDStyle event listener interface.
  * @author rli@chello.at
  * @tier API
  * @see REDStyleManager
  */
public interface REDStyleEventListener {
	/** Styles will change.
	  * @param style Array of REDStyles that will change.
	  */
	void beforeStyleChange(REDStyle[] style);

	/** Styles have changed.
	  * @param style Array of REDStyles that have changed.
	  */
	void afterStyleChange(REDStyle[] style);
	
	/** Theme will be set.
	  * @param oldTheme The current theme.
	  * @param newTheme The new theme that will be set.
	  */
	void beforeThemeChange(String oldTheme, String newTheme);

	/** Theme has been set.
	  * @param oldTheme The old theme theme.
	  * @param newTheme The new current theme.
	  */
	void afterThemeChange(String oldTheme, String newTheme);
}
