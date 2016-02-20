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

import junit.framework.*;
import java.util.*;

/** Regression test for RED regular expression parser.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDRexStringFinder extends TestCase {
	public RTestREDRexStringFinder(String name) {
		super(name);
	}
	
	public void setUp() {
		fFinder = new REDRexStringFinder("if|for|while");
	}
	
	private String getMatchString(String haystack) {
		Iterator iter = fFinder.getMatches(haystack.toCharArray(), haystack.length());
		StringBuilder buf = new StringBuilder();
		while (iter.hasNext()) {
			if (buf.length() > 0) {
				buf.append(' ');
			}
			REDRexStringFinderMatch match = (REDRexStringFinderMatch) iter.next();
			buf.append("").append(match.beginOffset()).append('-').append(match.endOffset());
		}
		return String.valueOf(buf);
	}
	
	public void testFinder() {
		assertEquals("0-1 23-25 29-33", getMatchString("if you could hold this for a while"));
	}
	
	public void testGreediness() {
		fFinder = new REDRexStringFinder("if|end|endif");
		assertEquals("0-4", getMatchString("endif "));
		assertEquals("0-4", getMatchString("endif"));
		assertEquals("0-4 6-7", getMatchString("endif if"));
		assertEquals("0-4 5-6", getMatchString("endifif"));
	}
	
	public static Test suite() {
		return new TestSuite(RTestREDRexStringFinder.class);
	}
	
	REDRexStringFinder fFinder;
}
