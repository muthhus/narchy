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
 
package red.plugins.synHi;

import red.*;

/** Auxiliary class which represents one entry in the operation batch produced by concurrent highlighting.
  * @author rli@chello.at
  * @tier system
  * @see REDSyntaxHighlighter
  */
public class REDSyntaxHighlighterBatchEntry {
	public REDSyntaxHighlighterBatchEntry(int from, int to, REDStyle style, int changeCount) {
		fFrom = from;
		fTo = to;
		fStyle = style;
		fChangeCount = changeCount;
	}
	
	public void execute(REDEditor editor, int changeCount) {
		if (changeCount == fChangeCount) {
			editor.setStyle(fFrom, fTo, fStyle);
		}
	}
	
	int fFrom, fTo, fChangeCount;
	REDStyle fStyle;
}
	