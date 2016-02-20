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


/** Memory performance test for Java objects 
  * @tier test
  */
public class PTestRun {
	static public void main(String arg[]) throws Exception {
		int iterations = Integer.valueOf(arg[0]);
		Runtime rt = Runtime.getRuntime();
		
		long s1 = rt.totalMemory() - rt.freeMemory();
		Object arr[] = new Object[iterations];
		long s2 = rt.totalMemory() - rt.freeMemory();

		for (int i = 0; i < iterations; i++) {
//			arr[i] = new REDRun(null, 0, 0, null);
			arr[i] = new PTestRun();
		}
		long s3 = rt.totalMemory() - rt.freeMemory();
		
		System.out.println("Array allocation: " + (s2 - s1) + " bytes. (" + ((s2 - s1) / iterations) + " per iteration)");
		System.out.println("Run allocation: " + (s3 - s2) + " bytes. (" + ((s3 - s2) / iterations) + " per iteration)");
	}
	long a, b, c, d, e, f;
}
