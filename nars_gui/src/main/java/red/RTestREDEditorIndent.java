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

import junit.framework.*;

/**
  * JUnit TestCase class for the indentation/tab management of red.REDEditor. This class has it's own TestSuite.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDEditorIndent extends RTestREDEditor {
	public RTestREDEditorIndent(String name) {
		super(name);
	}
		
	public void testIndentWidth() {
		REDEditor editor = getTestEditor();
		assertEquals("Unexpected default indent width", REDAuxiliary.fcDefaultIndentWidth, editor.getIndentWidth());
		editor.setIndentWidth(6);
		assertEquals("IndentWidth not set", 6, editor.getIndentWidth());
		editor.setIndentWidth(-1);
		assertEquals("IndentWidth not normalized", 1, editor.getIndentWidth());
		editor.setIndentWidth(2000);
		assertEquals("IndentWidth not normalized", 256, editor.getIndentWidth());
		editor.setIndentWidth(0);
		assertEquals("IndentWidth not normalized", 1, editor.getIndentWidth());
	}
	
	public void testIndentMode() {
		REDEditor editor = getTestEditor();
		assertEquals("Unexpected default indent mode", REDAuxiliary.fcDefaultIndentMode, editor.getIndentMode());
		editor.setIndentMode(REDIndentMode.SPC);
		assertEquals("Unexpected indent mode", REDIndentMode.SPC, editor.getIndentMode());
		editor.setIndentMode(REDIndentMode.TAB);
		assertEquals("Unexpected indent mode", REDIndentMode.TAB, editor.getIndentMode());
		// TBD: modes as types
	}
	
	public void testTabWidth() {
		REDEditor editor = getTestEditor();
		assertEquals("Unexpected default tab width", REDAuxiliary.fcDefaultTabWidth, editor.getTabWidth());
		editor.setTabWidth(6);
		assertEquals("TabWidth not set", 6, editor.getTabWidth());
		editor.setTabWidth(-1);
		assertEquals("TabWidth not normalized", 1, editor.getTabWidth());
		editor.setTabWidth(2000);
		assertEquals("TabWidth not normalized", 256, editor.getTabWidth());
		editor.setTabWidth(0);
		assertEquals("TabWidth not normalized", 1, editor.getTabWidth());		
	}
	
	public void testIndentString() {
		REDEditor editor = getTestEditor();
		assertEquals("Indent string should be \\t defaultedly", "\t", editor.getIndentString());
		editor.setIndentWidth(8);
		editor.setTabWidth(4);
		editor.setIndentMode(REDIndentMode.ASIS);
		assertEquals("Indent string should be \\t\\t", "\t\t", editor.getIndentString());
		editor.setIndentMode(REDIndentMode.TAB);
		assertEquals("Indent string should be \\t\\t", "\t\t", editor.getIndentString());
		editor.setIndentMode(REDIndentMode.SPC);
		assertEquals("Indent string should be 8 spaces", "        ", editor.getIndentString());
		
		editor.setTabWidth(6);
		editor.setIndentMode(REDIndentMode.ASIS);
		assertEquals("Indent string should be \\t plus 2 spaces", "\t  ", editor.getIndentString());
		editor.setIndentMode(REDIndentMode.TAB);
		assertEquals("Indent string should be \\t plus 2 spaces", "\t  ", editor.getIndentString());
		editor.setIndentMode(REDIndentMode.SPC);
		assertEquals("Indent string should be 8 spaces", "        ", editor.getIndentString());
		
		editor.setIndentWidth(4);
		editor.setIndentMode(REDIndentMode.ASIS);
		assertEquals("Indent string should be 4 spaces", "    ", editor.getIndentString());
		editor.setIndentMode(REDIndentMode.TAB);
		assertEquals("Indent string should be 4 spaces", "    ", editor.getIndentString());
		editor.setIndentMode(REDIndentMode.SPC);
		assertEquals("Indent string should be 4 spaces", "    ", editor.getIndentString());
	}
	
	public void testAdjustIndentation() {
		REDEditor editor = new REDEditor();
		editor.setIndentWidth(4); editor.setTabWidth(4); 
		editor.replace("\t\t", 0, editor.length(), null);
		editor.adjustIndentation(0, REDIndentMode.SPC);
		assertEquals("Spacify doesn't work", "        ", editor.asString());
		
		editor.setTabWidth(2);
		editor.replace("\t\t", 0, editor.length(), null);
		editor.adjustIndentation(0, REDIndentMode.SPC);
		assertEquals("Spacify doesn't work", "    ", editor.asString());
		
		editor.setTabWidth(4);
		editor.replace("   \t ", 0, editor.length(), null);
		editor.adjustIndentation(0, REDIndentMode.TAB);
		assertEquals("Tabify doesn't work", "\t\t", editor.asString());

		editor.replace("   \t  ", 0, editor.length(), null);
		editor.adjustIndentation(0, REDIndentMode.TAB);
		assertEquals("Tabify doesn't work", "\t\t ", editor.asString());
	}
	
	public static Test suite() {
		return new TestSuite(RTestREDEditorIndent.class);
	}	
}
