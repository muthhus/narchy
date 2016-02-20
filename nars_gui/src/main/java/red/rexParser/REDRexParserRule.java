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
 
package red.rexParser;

import java.util.regex.*;

/** Regular expression parser class rule
  * @author rli@chello.at
  * @tier system
  */
class REDRexParserRule {
	public REDRexParserRule(int reqState, String pattern, boolean caseSensitive, int emitState, Object emitObj, REDRexAction action, boolean rewind, int id) throws PatternSyntaxException {
		fReqState = reqState;
		if (caseSensitive) {
			fPattern = Pattern.compile(pattern);
		}
		else {
			fPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		}
		fEmitState = emitState;
		fEmitObj = emitObj;
		fAction = action;
		fRewind = rewind;
		fId = id;
		if (isOptimisableRule(pattern, caseSensitive)) {
			fFinder = new REDRexStringFinder(pattern);
		}
	}
	
	private static boolean isOptimisableRule(String pattern, boolean caseSensitive) {
		if (!caseSensitive) return false;	// tbd: allow case - insensitive stuff to be optimizable as well
		int x = 0;
		while (x < pattern.length()) {
			char c = pattern.charAt(x);
			if (!Character.isLetterOrDigit(c) && c != '|') {
				return false;
			}
			x++;
		}
		return true;
	}
		
	int fReqState, fEmitState, fId;
	Pattern fPattern;
	Object fEmitObj;
	REDRexAction fAction;
	boolean fRewind;
	REDRexStringFinder fFinder;
}
