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

import java.util.*;

/** String finder for simple regexpes (a|b|c)
  * @author rli@chello.at
  * @tier system  
  * @tbd: greedy matching
  */
class REDRexStringFinder {
	public REDRexStringFinder(String simpleRegExp) {
		fDefaultState = new State();
		StringTokenizer tok = new StringTokenizer(simpleRegExp, "|");
		while (tok.hasMoreTokens()) {
			addNeedle(tok.nextToken());
		}
	}	
	
	void addNeedle(String needle) {
		State curState = fDefaultState;
		for (int x = 0; x < needle.length(); x++) {
			char c = needle.charAt(x);
			int idx = curState.fValidInput.indexOf(c);
			if (idx == -1) {
				curState.fValidInput = curState.fValidInput + c;
				curState.fStates.add(new State());
				curState = (State) curState.fStates.get(curState.fStates.size()-1);
			}
			else {
				curState = (State) curState.fStates.get(idx);
			}
		}
		curState.fMatch = true;
	}
	
	Iterator getMatches(char haystack[], int haystackLength) {
		State curState = fDefaultState;
		int length = 0;
		ArrayList matches = new ArrayList();
		for (int x = 0; x < haystackLength; x++) {
			char c = haystack[x];
			int idx = curState.fValidInput.indexOf(c);
			if (idx == -1) {
				if (curState.fMatch) {
					matches.add(new REDRexStringFinderMatch(x-length, x-1));
					idx = fDefaultState.fValidInput.indexOf(c);
				}
				if (idx == -1) {
					curState = fDefaultState;
					length = 0;
				}
				else {
					curState = (State) fDefaultState.fStates.get(idx);
					length = 1;
				}
			}
			else {
				curState = (State) curState.fStates.get(idx);
				length++;
			}			
		}
		if (curState.fMatch) {
			matches.add(new REDRexStringFinderMatch(haystackLength - length, haystackLength - 1));
		}
		
		return matches.iterator();
	}
	
	static class State {
		String fValidInput = "";
		Vector fStates = new Vector();
		boolean fMatch;
	}
	
	State fDefaultState;
}
