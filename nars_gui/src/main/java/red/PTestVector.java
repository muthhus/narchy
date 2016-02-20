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

import java.util.*;

/** Performance comparison between Vector and Array-List
  * @author rli@chello.at
  * @tier test
  */
public class PTestVector {
	static int iterations;
	public static void testVector() {
		Vector v = new Vector();
		
		PTestStopWatch sw = new PTestStopWatch();
		sw.start();
		for (int x = 0; x < iterations; x++) {
			v.add(x);
		}
		sw.stop("Inserting " + iterations + " elements into a vector");
	}
	
	public static void testArrayList() {
		ArrayList v = new ArrayList();
		
		PTestStopWatch sw = new PTestStopWatch();
		sw.start();
		for (int x = 0; x < iterations; x++) {
			v.add(x);
		}
		sw.stop("Inserting " + iterations + " elements into a array-list");
	}

	
	public static void main(String[] args) {
		try {
			iterations = Integer.valueOf(args[0]);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			iterations = 1000;
		}
		
		String test;
		try {
			test = args[1];
		}
		catch (ArrayIndexOutOfBoundsException e) {
			test = "Vector";
		}

		switch (test) {
			case "Vector":
				testVector();
				break;
			case "ArrayList":
				testArrayList();
				break;
			default:
				System.out.println("Unknown test case specified: '" + test + '\'');
				break;
		}
	}
}
