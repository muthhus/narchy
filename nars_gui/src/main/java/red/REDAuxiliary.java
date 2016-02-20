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

import java.awt.*;

/** auxiliary functions and constants for the RED - System
  * @author rli@chello.at
  * @tier API
  */
public class REDAuxiliary
{
	/** Emit a beep.
	  * This method emits a beep if beeps are enabled.
	  */
	public static void beep() {
		if (fgBeepEnabled) {
			Toolkit.getDefaultToolkit().beep();
		}
	}
	
	/** Enable / Disable beep.
	  * @param enabled <Code>True</Code> if beeps are enabled; <Code>False</Code> if RED should be silent.
	  */
	public static void setBeepEnabled(boolean enabled) {
		fgBeepEnabled = enabled;
	}
	
	/** Is character part of a word.
	  * This routine only checks if c is letter or digit.
	  * @param c character to check
	  * @return <br>&nbsp;true: if part of a word
	    <br>&nbsp;false: otherwise
	  */
	public static boolean isWordConstituent(byte c) {
		return Character.isLetterOrDigit((char) c);
	}
	
	/** Is character a linebreak.
	  * @param c character to check
	  * @return <br>&nbsp;true: if this character breaks lines
	    <br>&nbsp;false: otherwise
	  */
	public static boolean isLineBreak(byte ch) {
		return  (ch == '\n') || (ch == '\r');
	}

	/** Insert view mode. 
	  * This is the default view mode. If this mode is set (using REDEditor.setViewMode(...)) the user can change the text.
	  * Keyboard input will insert characters.
	  */
	final public static int VIEWMODE_INSERT = 0;
	/** Overwrite view mode. <br> 
	  * If this mode is set (using REDEditor.setViewMode(...)) the user can change the text. 
	  * Keyboard input will overwrite existing characters (except when the caret is located at the end of the line).
	  */
	final public static int VIEWMODE_OVERWRITE = 1;
	/** Readonly view mode. <br>
	  * If this mode is set (using REDEditor.setViewMode(...)) the user can not change the text. 
	  * Keyboard input will be ignored.
	  */
	final public static int VIEWMODE_READONLY = 2;
	

	/** Default tab width */	
	final public static int fcDefaultTabWidth = 4;
	/** Default minimal width */	
	final public static int fcDefaultMinTabWidth = 1;
	/** Default indent width */	
	final public static int fcDefaultIndentWidth = 4;
	/** Default indentation mode */	
	final public static REDIndentMode fcDefaultIndentMode = REDIndentMode.ASIS;

	final static int fcListenerSize = 2;	
	
	private static boolean fgBeepEnabled = true;
}
