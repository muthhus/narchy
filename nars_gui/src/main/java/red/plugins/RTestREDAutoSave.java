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
 
package red.plugins;

import red.*;
import junit.framework.*;

/** Regression test for REDAutoSave
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDAutoSave extends TestCase {
	final static String fcContent = "All we have to decide is what to do with the time given to us.";
	
	public RTestREDAutoSave(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
		fEditor = new REDEditor("RTestREDAutoSave.1.tmp", false);
		fEditor.replace(fcContent, 0, 0, null);
		fAutoSave = new REDAutoSave();
	}
	
	public void tearDown() throws Exception {
		super.tearDown();
		fEditor.close();
	}
	
	public void testDefaults() {
		assertEquals(REDAutoSave.fcDefaultExtension, fAutoSave.getExtension());
		assertEquals(REDAutoSave.fcDefaultInterval, fAutoSave.getInterval());
	}
	
	public void testModifyingExtensionAndInterval() {
		fAutoSave.setExtension("autosave");
		assertEquals("autosave", fAutoSave.getExtension());
		fAutoSave.setInterval(5);
		assertEquals(5, fAutoSave.getInterval());
	}
	
	public static Test suite() {
		return new TestSuite(RTestREDAutoSave.class);
	}

	REDEditor fEditor;
	REDAutoSave fAutoSave;
}
