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

import junit.framework.*;
import java.io.*;
import java.util.*;

/** JUnit TestCase class for red.util.REDResourceManager.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDResourceManager extends TestCase {
	public RTestREDResourceManager(String name) {
		super(name);
	}
		
	public void testResourceManager() throws IOException {
		boolean [] testArr = new boolean[3];
		for (int x = 0; x < 3; x++) {
			testArr[x] = false;
		}
		
		Iterator iter = REDResourceManager.getInputStreams("red/util", ".rmtest.in");
		while (iter.hasNext()) {
			InputStream is = (InputStream) iter.next();
			int x = is.read() - 65;
			assertTrue(!testArr[x]);	// must not already be true;
			testArr[x] = true;
			is.close();
		}
		
		for (int x = 0; x < 3; x++) {
			assertTrue(testArr[x]);
		}
	}
	
	public void testIteratorNoRemove() throws IOException {
		boolean flag = false;
		Iterator iter = REDResourceManager.getInputStreams("red/util", ".rmtest.in");
		try {
			iter.remove();
		}
		catch (UnsupportedOperationException onse) {
			flag = true;
		}
		assertTrue("Did not throw UnsupportedOperationException.", flag);
	}
 	 
	/**
	 * Static method to construct the TestSuite of RTestREDFile.
	 */
	public static Test suite() {
		return new TestSuite(RTestREDResourceManager.class);
	}
}
