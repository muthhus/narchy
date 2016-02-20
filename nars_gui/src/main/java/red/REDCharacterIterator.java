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

import java.text.*;

/** Character iterator implementation for RED.
  * @author rli@chello.at
  * @tier system
  */
class REDCharacterIterator implements CharacterIterator {
	REDCharacterIterator(REDEditor editor, int idx) {
		fEditor = editor;
		fIdx = idx;
	}
	
	// --- CharacterIterator impl begin
	public Object clone() { 
		return new REDCharacterIterator(fEditor, fIdx);
	}
	
	public char current() { 
		if (fIdx < 0 || fIdx >= fEditor.length()) {
			fIdx = Math.min(fEditor.length(), Math.max(0, fIdx));	// normalize
			return CharacterIterator.DONE;
		}
		return (char) fEditor.charAt(fIdx);
	}

	public char first() {
		return setIndex(getBeginIndex());
	}
	
	public int getBeginIndex() { 
		return 0;
	}
	
	public int getEndIndex() { 
		return fEditor.length();
	}
	
	public int getIndex() { 
		return fIdx;
	}
	
	public char last() { 
		return setIndex(getEndIndex() - 1);
	}
	
	public char next() { 
		return setIndex(fIdx + 1);
	}
	
	public char previous() { 
		return setIndex(fIdx - 1);
	}
	
	public char setIndex(int position) {
		fIdx = position;
		return current();
	}
	// --- CharacterIterator impl end
	
	REDEditor fEditor;
	int fIdx;
}
