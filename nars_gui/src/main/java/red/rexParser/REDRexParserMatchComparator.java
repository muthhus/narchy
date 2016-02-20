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
	
/** Regular expression parser match comparator. This comparator compares to parser matches, based on the 
  * compareTo function of REDRexParserMatch.
  * @author rli@chello.at
  * @tier system
  */
class REDRexParserMatchComparator implements Comparator {
	REDRexParserMatchComparator(boolean reverse) {
		if (reverse) {
			fFactor = -1;
		}
		else {
			fFactor = 1;
		}
	}

	public int compare(Object o1, Object o2) {
		return ((REDRexParserMatch) o1).compareTo(o2) * fFactor;
	}
	
	public boolean equals(Object o) {
		return super.equals(o);
	}
	int fFactor;
}
