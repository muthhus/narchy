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
 
package red.plugins.brMatcher;

import java.awt.event.*;
import red.*;

/** This plugin performs bracket matching.
  * @author rli@chello.at
  * @tier plugin
  * @see REDBracketMatcherManager
  */
public class REDBracketMatcher extends REDPlugin {
	
	public REDBracketMatcher(REDBracketMatcherDefinition def) {
		fRestoreStyle = null;
		fDef = def;
		fResult = new REDBracketMatcherResult();
	}
	
	public void setEditor(REDEditor editor) {
		if (fEditor != null) {
			if (fController != null) {
				fEditor.removeControllerDecorator(fController);
			}
			restore();
		}
		super.setEditor(editor);
		fController = new Controller(fEditor.getController());
		fEditor.setController(fController);
	}

	void restore() {
		if (fRestoreStyle != null) {
			fEditor.setStyle(fFrom, fTo, fRestoreStyle);
			fRestoreStyle = null;
		}
	}
		
	void match() {
		if (fEditor.hasSelection()) return;
		
		int from = fEditor.getSelectionStart();
		REDBracketMatcherResult res = fDef.findMatch(fEditor, from, false, fResult); 
		if (res != null) {
			fFrom = res.fPosition; fTo = fFrom + res.fLength;	
			fRestoreStyle = fEditor.getStyle(fFrom+1);	// tbd: need to remember all styles!
			fEditor.setStyle(fFrom, fTo, fDef.getStyle());
		}
	}
	
	public void beforeInsert(int from, int to) {
		restore();
	}
	
	public void beforeDelete(int from, int to) {
		restore();
	}
	
	public void afterSelectionChange(int oldFrom, int oldTo, int newFrom, int newTo) {
		restore();
		match();
	}
	
	public void lostFocus() {
		restore();
	}

	class Controller extends REDViewControllerDecorator {
		public Controller(REDViewController decorated) {
			super(decorated);
		}
		
		public void mouseLeft(REDView v, MouseEvent e) {
			int from = fEditor.getPosition(e.getX(), e.getY());
			int to = -1; 
			REDBracketMatcherResult res = null;
			if (e.getClickCount() == 2) {
				res = fDef.findMatch(fEditor, from, true, fResult); 
			}
			if (res != null) {
				fEditor.setSelection(from, res.fPosition);
			}
			else {
				super.mouseLeft(v, e);
			}
		}	
	}
	
	REDStyle fRestoreStyle;
	int fFrom, fTo;
	REDBracketMatcherDefinition fDef;
	Controller fController;
	REDBracketMatcherResult fResult;
}
