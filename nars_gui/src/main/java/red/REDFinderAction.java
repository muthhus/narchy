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

import red.rexParser.*;

/** Wrapper class to translate match offsets 
  * @author rli@chello.at
  * @tier API
  */
abstract public class REDFinderAction extends REDRexAction {	
	abstract public void match(REDEditor editor, int from, int to);
	public void patternMatch(REDRexParser parser, int line, REDRexParserMatch match) {
		REDFinder finder = (REDFinder) match.getEmitObj();
		REDEditor editor = finder.getEditor();
		match(editor, finder.getRealPosition(line, match.getStart(0)), finder.getRealPosition(line, match.getEnd(0)));
	}
}

