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
 
package red.util;

/** Auxiliary functions for test cases.
  * @author rli@chello.at
  * @tier test
  */	
public class RTestAuxiliary {
	private RTestAuxiliary() { } // no instances, just static methods

	/** Quoting \n, \t */
	public static String quote(String str) {
		StringBuilder buf = new StringBuilder();
		
		for (int x = 0; x < str.length(); x++) {
			char c = str.charAt(x);
			switch(c) {
				case '\n': buf.append("\\n"); break;
				case '\t': buf.append("\\t"); break;
				default: buf.append(c); break;
			}
		}
		return String.valueOf(buf);
	}

	/** Quoting \t */
	public static String quoteTab(String str) {
		StringBuilder buf = new StringBuilder();
		
		for (int x = 0; x < str.length(); x++) {
			char c = str.charAt(x);
			switch(c) {
				case '\t': buf.append("\\t"); break;
				default: buf.append(c); break;
			}
		}
		return String.valueOf(buf);
	}
}

