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

/** Command for normal insert/delete/replace text operations
  * @author rli@chello.at
  * @tier system
  */
class REDTextCommand extends REDCommand {
	REDTextCommand(String description, REDView view, REDText text, int from, int len, String replacedText) {
		super(description);
		fText = text;
		fView = view;
		fFrom = from;
		fLength = Math.max(len, 0);
		fReplacedText = replacedText;
		fIsUndoRedo = false;
		fDelayed = false;
	}
	
	public void undoIt() {
		redoIt();
	}
	
	public void redoIt() {
		REDView v = fText.getUndoRedoView();
		fIsUndoRedo = true;
		doIt();
		if (v != null) {
			// if (v.getColumnSelectionMode()) {		// TBD
			if (false) {
				v.setSelection(fFrom, fFrom);
			}
			else {
				v.setSelection(fFrom, fFrom + fLength);
			}
		}
		fText.setUndoRedoView(null);
		fText.setCurTypingCommand(null);
	}
	
	public void doIt() {
		String replaced = null;
		
		// make backup of text to be destroyed
		if (fLength != 0) {
			replaced = fText.asString(fFrom, fFrom + fLength);
		}
		
		// replace
		fDelayed = !fText.replace(fFrom, fFrom + fLength, fReplacedText);
		
		// reset length
		if (fReplacedText != null) {
			fLength = fReplacedText.length();
		}
		else {
			fLength = 0;
		}
		
		fReplacedText = replaced;
		
		if (fView != null && !fIsUndoRedo) {
			fView.setSelection(Math.min(fFrom + fLength, fText.length()));
		}
	}
	
	public void addChar() {
		fLength++;
	}
	
	/**
	  * @pre fDescription.equals("Backspace")
	  * @pre fFrom > 0
	  */
	public void backspace() {
		int oldFrom = fFrom;
		fFrom = fView.charLeft(fFrom);
		fReplacedText = fText.asString(fFrom, oldFrom) + fReplacedText; 	// TBD: maybe use a stringbuf (?)
	}
	
	/**
	  * @pre fDescription.equals("Delete")
	  * @pre fFrom < fText.length()
	  */
	public void delete() {
		int pos = fReplacedText.length();
		fReplacedText = fReplacedText + fText.asString(fFrom, fView.charRight(fFrom));
	}
	
	public REDView getView() {
		return fView;
	}
	
	public boolean isDelayed() {
		return fDelayed;
	}
	
	boolean fIsUndoRedo;
	int fFrom, fLength;
	REDText fText;
	String fReplacedText;
	REDView fView;
	boolean fDelayed;
}
