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
import red.lineTree.*;
import red.util.*;
import java.awt.event.*;
import java.util.*;

/** This plugin allows to protect (i.e. make readonly) parts of a text.
  * @author rli@chello.at
  * @tier plugin
  */
public class REDTextProtector extends REDPlugin {
	static class ProtectionStart extends Object { }
	static class ProtectionEnd extends Object { }
	
	private static Class fgProtectionStartClass;
	private static Class fgProtectionEndClass;
	
	class REDTextProtectorController extends REDViewControllerDecorator {
		REDTextProtectorController(REDViewController ctrl) {
			super(ctrl);
			if (fgProtectionStartClass == null) {
				fgProtectionStartClass = (new ProtectionStart()).getClass();
				fgProtectionEndClass = (new ProtectionEnd()).getClass();
			}
		}
		
		public void keyBackspace(REDView v, KeyEvent e) { 
			if (fEditor.hasSelection()) {
				keyDelete(v, e);
			}
			else {
				int selStart = fEditor.getSelectionStart();
				if (mayChange(selStart-1, selStart)) {
					super.keyBackspace(v, e);
				}
			}
		}
		
		public void keyDelete(REDView v, KeyEvent e) { 
			int start = fEditor.getSelectionStart();
			int end = fEditor.getSelectionEnd();
			if (mayChange(start, Math.max(start + 1, end))) {
				super.keyDelete(v, e);
			}
		}
		
		public void keyInsert(REDView v, KeyEvent e) { 
			if (mayChange()) {
				super.keyInsert(v, e);
			}
		}
		
		public void keyNewline(REDView v, KeyEvent e) { 
			if (mayChange()) {
				super.keyNewline(v, e);
			}
		}
		
		public void keyTab(REDView v, KeyEvent e) { 
			if (mayChange()) {
				super.keyTab(v, e);
			}
		}
		
		public void key(REDView v, KeyEvent e) { 
			if (mayChange()) {
				super.key(v, e);
			}
		}		
	}
	
	public REDTextProtector() {
		super();
		fController = null;
		fCollector = new ArrayList();
	}
	
	public void setEditor(REDEditor editor) {
		if (fEditor != null && fController != null) {
			fEditor.removeControllerDecorator(fController);
		}
		super.setEditor(editor);
		if (editor != null) {
			fMarks = editor.createMarkTree();
			fController = new REDTextProtectorController(editor.getController());
			editor.setController(fController);
		}
	}	
	
	/** Protect area.
	  * @param from Start of area to protect
	  * @param to End of area to protect. If <Code>to - from &lt; 2</Code> no protection will take place, i.e. the call will be ignored.
	  */
	public void protect(int from, int to) {
		if (to - from < 2) return;
		from++;
		REDMark m = fMarks.findMark(from, true, null);
		if (m == null || !(m.getValue() instanceof ProtectionStart) && m.getPosition() != from && m.getPosition() != from-1) {
			fMarks.createMark(from, new ProtectionStart());
		}

		m = fMarks.findMark(from+1, false, ProtectionStart.class);
		if (m != null && m.getValue() instanceof ProtectionStart && m.getPosition()-1 <= to) {
			fMarks.deleteMark(m);
		} 
		m = fMarks.findMark(to, true, null);
		if (m != null && m.getValue() instanceof ProtectionEnd) {
			fMarks.deleteMark(m);
		}
		m = fMarks.findMark(to+1, false, null);
		if (m == null || !(m.getValue() instanceof ProtectionEnd)) {
			fMarks.createMark(to, new ProtectionEnd());
		}	
	}
	
	public void protectLines(int fromLine, int toLine) {
		protect(fEditor.getLineEnd(fromLine-1), fEditor.getLineStart(toLine+1));
	}
	
	public boolean mayChange(int start, int end) {
		REDMark m1, m2;
		m1 = fMarks.findMark(start, true, null);
		if (m1 != null && m1.getValue() instanceof ProtectionStart) {
			return false;
		}
		
		if (end > start) {
			fCollector.clear();
			fMarks.collectMarks(start+1, end, null, fCollector);
			return fCollector.size() == 0;
		}
		return true;
	}
	
	public boolean mayChange() {
		return mayChange(fEditor.getSelectionStart(), fEditor.getSelectionEnd());
	}
	
	public void dumpProtection() {
		ArrayList v = fMarks.collectMarks(0, fEditor.length(), null, null);
		Iterator iter = v.iterator();
		while (iter.hasNext()) {
			REDMark m = (REDMark) iter.next();
			if (m.getValue() instanceof ProtectionStart) {
				REDTracer.info("red.plugins", "REDTextProtector", "Start at " + m.getPosition());
			}
			else if (m.getValue() instanceof ProtectionEnd) {
				REDTracer.info("red.plugins", "REDTextProtector", "End at " + m.getPosition());
			}
			else {
				REDTracer.info("red.plugins", "REDTextProtector", "Unknown mark value at " + m.getPosition() + ". This is bug!");
			}
		}	
	}
	
	public int getNrProtectedAreas() {
		ArrayList v1 = fMarks.collectMarks(0, fEditor.length(), fgProtectionStartClass, null);
		ArrayList v2 = fMarks.collectMarks(0, fEditor.length(), fgProtectionEndClass, null);
		if (v1.size() != v2.size()) {
			throw new Error("Internal error in REDTextProtector. Asymmetric protection boundaries.");
		}
		return v1.size();
	}

	public void afterFileLoad(String filename) { 
		fMarks = fEditor.createMarkTree();
	}

	REDTextProtectorController fController;
	REDMarkTree fMarks;
	ArrayList fCollector;
}
