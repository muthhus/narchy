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

//---- newclass ---------------------------------------------------------
/** View positions 
  * @author rli@chello.at
  * @tier system
  */
class REDViewPosition {
	public REDViewPosition() {
		fPosition = 0;
		fLine = 0;
		fBoundRect = new Rectangle();
	}
	
	int getTextPosition() {
		return fPosition;
	}
	
	int getLineNumber() {
		return fLine;
	}
		
	Point getLowerLeftPoint() {
		return new Point(fBoundRect.x, fBoundRect.y + fBoundRect.height - 1);
	}
	
	Point getUpperLeftPoint() {
		return new Point(fBoundRect.x, fBoundRect.y);
	}

	int fPosition;
	int fLine;
	Rectangle fBoundRect;
}

