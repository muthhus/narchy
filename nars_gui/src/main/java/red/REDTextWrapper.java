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

/** wrapper class for handling texts
  * @author rli@chello.at
  * @tier system
  * @see REDTextServer
  */
class REDTextWrapper {
	REDTextWrapper(REDText text, boolean privateCopy) {
		fPrivateRefCount = 0;
		fSharedRefCount = 0;
		incRefCount(privateCopy);
		if (!privateCopy) {
			fText = text;
		}
	}
	
	REDText getText() {
	    return fText;
	}
	
	void setText(REDText text) {
	    fText = text;
	}
	
	void incRefCount(boolean privateCopy) {
		if (privateCopy) {
			fPrivateRefCount++;
		}
		else {
			fSharedRefCount++;
		}
	}
	
	void decRefCount(REDText text) {
		if (text == fText) {
			fSharedRefCount--;
		}
		else {
			fPrivateRefCount--;
		}
	}

	int getSharedRefCount() {
		return fSharedRefCount;
	}
	
	int getPrivateRefCount() {
		return fPrivateRefCount;
	}
	
	
	private int fSharedRefCount;
	private int fPrivateRefCount;
	private REDText fText;
}
