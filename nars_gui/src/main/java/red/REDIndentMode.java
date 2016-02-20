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

/** Enumeration of indentation modes 
  * @author rli@chello.at
  * @tier API
  */
public class REDIndentMode {
	/** "As is" indentation mode.<br>
	  * In this mode indentation is usually done by tabs but no conversion is performed, if spaces are used for indentation 
	  */
	final public static REDIndentMode ASIS = new REDIndentMode();

	/** "Space" indentation mode.<br>
	  * In this mode indentation is done by spaces, converting any tab characters in lines that are indented
	  */
	final public static REDIndentMode SPC = new REDIndentMode();

	/** "Tab" indentation mode.<br>
	  * In this mode indentation is done by tabs (where possible), converting space characters in lines that are indented
	  */
	final public static REDIndentMode TAB = new REDIndentMode();
	
	private REDIndentMode() { }
}
