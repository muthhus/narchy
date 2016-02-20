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

/** JUnit TestCase class for red.util.REDTracer.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDTracer extends TestCase {
	public RTestREDTracer(String name) {
		super(name);
	}
	
	
	public void setUp() {
		InputStream is = ClassLoader.getSystemResourceAsStream("red/RTestREDTracer.1.xml");
		REDTracer.readConfig(is, "RTestREDTracer.1.xml");
	}
	
	public void testGeneralLevel() {
		assertEquals(REDTracer.fcOff, REDTracer.fgLevel);
	}
	
	public void testRules() {
		assertEquals(true, REDTracer.wantTrace("pack1", "Class1"));
		assertEquals(true, REDTracer.wantTrace("pack1", "class1"));	// because we are case in-sensitive
		assertEquals(false, REDTracer.wantTrace("pack1", "Class2"));

		assertEquals(false, REDTracer.wantTrace("pack2", "Class1"));
		assertEquals(true, REDTracer.wantTrace("pack2", "Class2"));
		assertEquals(false, REDTracer.wantTrace("pack2", "class1"));	// because we are case in-sensitive
		assertEquals(true, REDTracer.wantTrace("pack2", "Class3"));

		assertEquals(true, REDTracer.wantTrace("pack3", "Class1"));
		assertEquals(true, REDTracer.wantTrace("pack3", "Class2"));
		assertEquals(true, REDTracer.wantTrace("pack3", "class1"));	
		assertEquals(true, REDTracer.wantTrace("pack3", "Class3"));
	}
		 	 
	/**
	 * Static method to construct the TestSuite of RTestREDFile.
	 */
	public static Test suite() {
		return new TestSuite(RTestREDTracer.class);
	}
}
