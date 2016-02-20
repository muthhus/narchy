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
 
package red.plugins;

import red.*;
import java.awt.event.*;
import java.util.regex.*;

/** Automatic and intelligent indentation plugin.
  * This plugin provides intelligent behaviour for indentation.
  * @author rli@chello.at
  * @tier plugin
  */
public class REDAutoIndent extends REDPlugin {

	// @tbd language dependant, works for C/C++/Java only
	class REDAutoIndentController extends REDViewControllerDecorator {
		REDAutoIndentController(REDViewController decorated, REDAutoIndent indent) {
			super(decorated);
			fIndent = indent;
			
			try {
				fRexOpen = Pattern.compile("([ \t]*).*[{].*");
				fRexClose = Pattern.compile(".*[}][^{]*");
			}
			catch(PatternSyntaxException e) {
				throw new Error("Wrong patterns in REDAutoIndentController.");		
			}			
		}
		
		public void keyNewline(REDView v, KeyEvent e) {
			REDEditor editor = fIndent.getEditor();
			int mode = editor.getViewMode();
			if (mode == REDAuxiliary.VIEWMODE_INSERT || mode == REDAuxiliary.VIEWMODE_OVERWRITE) {
				int selStart = editor.getSelectionStart();
				int selEnd = editor.getSelectionEnd();
				int x, line;
	
				// get pre-indentation	
				x = selEnd;
				while (x < editor.length() && containsOnly(editor.copy(x, x+1), " \t")) {
					x++;
				}
				if (REDAuxiliary.isLineBreak(editor.charAt(x))) {
					x = selEnd;
				}
				
				line = editor.getLineForPosition(selStart);
				String indent = getLinebreak() + fIndent.getIndent(line, x - selEnd);
				editor.replace(indent, selStart, selEnd, "Newline");
				editor.adjustIndentation(editor.getLineForPosition(selStart)+1, editor.getIndentMode());
				editor.revealSelection();
			}
			else {
				super.keyNewline(v, e);
			}
		}

		// try to de-indent if necessary
		public void key(REDView v, KeyEvent e) {
			REDEditor editor = fIndent.getEditor();
			int mode = editor.getViewMode();
			if (e.getKeyChar() == '}' && (mode == REDAuxiliary.VIEWMODE_INSERT || mode == REDAuxiliary.VIEWMODE_OVERWRITE)) {
				int selStart = editor.getSelectionStart();
				int selEnd = editor.getSelectionEnd();
				int x, lineNr;
				String line;
				int depth = 1;
				
				lineNr = editor.getLineForPosition(selStart);
				line = editor.copyLine(lineNr);
				Matcher oMatcher = null;
				Matcher cMatcher = null;
				if (containsOnly(line, " \t\r\n")) {
					x = lineNr - 1;
					while (x >= 0 && depth > 0) {
						line = editor.copyLine(x--);
						oMatcher = fRexOpen.matcher(line);
						cMatcher = fRexClose.matcher(line);
						if (oMatcher.find()) {
							depth--;
						}
						if (cMatcher.find()) {
							depth++;
						}	
					}
					if (depth <= 0) {	// we've found it
						super.key(v, e);
						String indent = line.substring(oMatcher.start(1), oMatcher.end(1));
						line = editor.copyLine(lineNr);
						int lineStart = editor.getLineStart(lineNr);
						editor.replace(indent, lineStart, lineStart + findFirstNotOf(line, " \t"), "Unindent");
						editor.setSelection(fEditor.getSelectionStart() + 1, fEditor.getSelectionStart() + 1);
						return;	// avoid second super.key below
					}
				}
			}
			super.key(v, e);
		}
	
		private final REDAutoIndent fIndent;
		private final Pattern fRexOpen;
		private final Pattern fRexClose;
		private Matcher fMatcher;
	}


	public REDAutoIndent() {
		super();
		fController = null;
	}
		
	/** 
	  * @pre corr >= 0
	  */
	private String getLineIndent(int line, int corr) {
		String s = fEditor.copyLine(line) + 'x';	// append sentinel - x to prevent problems with blank lines
		int x = findFirstNotOf(s, " \t");
		if (x > corr) {
			return s.substring(0, x - corr);
		}
		else {
			return "";
		}
	}
	
	// TBD: this is language dependent
	private String getAutoIndent(int line) {
		String s = fEditor.copy(fEditor.getLineStart(line), fEditor.getSelectionStart());
		if (s.isEmpty()) {
			return "";
		}
		
		int len = s.length() - 1;
		
		// find first character before linebreak
		while (len > 0 && REDAuxiliary.isLineBreak((byte) s.charAt(len))) {
			len--;
		}
		
		if (s.charAt(len) == '{') {
			return fEditor.getIndentString();
		}
		
		return "";
	}
	
	public String getIndent(int line, int corr) {
		return getLineIndent(line, corr) + getAutoIndent(line);
	}
	
	public void setEditor(REDEditor editor)
	{
		if (fEditor != null) {
			fEditor.removeControllerDecorator(fController);
			fController = null;
		}
		super.setEditor(editor);
		if (fEditor != null) {
			fController = new REDAutoIndentController(fEditor.getController(), this);
			fEditor.setController(fController);
		}	
	}
		
	private static boolean containsOnly(String haystack, String needles) {
		return findFirstNotOf(haystack, needles) == -1;
	}

	private static int findFirstNotOf(String haystack, String needles) {
		int i = 0;
		while (i < haystack.length()) {
			if (needles.indexOf(haystack.charAt(i)) == -1) {
				return i;
			}
			i++;
		}
		return -1;				
	}

	REDAutoIndentController fController;
	private boolean fSpaces;

	
}

