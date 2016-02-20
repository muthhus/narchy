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

/** Regular expression parser match descriptor.
  * @author rli@chello.at
  * @tier API
  */
public class REDRexParserMatch {
	public REDRexParserMatch(int groups, REDRexParserRule rule) {
		fStart = new int[groups];
		fEnd = new int[groups];
		fRule = rule; 
	}
	
	void setStart(int group, int offset) {
		fStart[group] = offset;
	}
	
	void setEnd(int group, int offset) {
		fEnd[group] = offset;
	}
	
	public int getStart(int group) {
		if (group < fStart.length) {
			return fStart[group];
		}
		return 0;
	}
	
	public int getEnd(int group) {
		if (group < fEnd.length) {
			return fEnd[group];
		}
		return 0;
	}
	
	public REDRexParserRule getRule() {
		return fRule;
	}
	
	public Object getEmitObj() {
		return fRule.fEmitObj;
	}

	public int compareTo(Object o) {
		REDRexParserMatch o2 = (REDRexParserMatch) o;
		if (fStart[0] == o2.fStart[0]) {
			if (fEnd[0] == o2.fEnd[0]) {
				return fRule.fId - o2.fRule.fId;
			}
			else {
				return o2.fEnd[0] - fEnd[0];	// sic! decide for larger match to be further in front 
			}
		}
		return fStart[0] - o2.fStart[0];
	}

	int [] fStart;
	int [] fEnd;
	REDRexParserRule fRule;
}
	
