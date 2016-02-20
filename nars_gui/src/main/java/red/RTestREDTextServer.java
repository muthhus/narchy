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

import junit.framework.*;
import java.util.*;
import java.io.*;

/** Regression test for REDTextServer
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDTextServer extends TestCase {
	/**
	 * The name of the normal temporary file. This file is created before the
	 * tests by setUp() as a copy of IN_FILE_NORMAL and deleted afterwards by
	 * tearDown(). Subclasses should use this file for modifications.
	 */
	static final String TMP_FILE_NORMAL = "RTestREDTextServer.1.tmp";

	public RTestREDTextServer(String name) {
		super(name);
	}
		
	public void testSharing() {
		REDText t1 = REDTextServer.acquireText("A", false);
		REDText t2 = REDTextServer.acquireText("A", false);
		
		assertEquals(t1.toString(), t2.toString());
		assertSame(t1, t2);
		assertTrue("A should be loaded", REDTextServer.isTextLoaded("A", false));
		assertTrue("B must not be loaded", !REDTextServer.isTextLoaded("B", false));

		REDTextServer.releaseText(t1); t1 = null;
		assertTrue("Text should still be loaded", REDTextServer.isTextLoaded("A", false));

		REDTextServer.releaseText(t2); t2 = null;
		assertTrue("Text should be unloaded", !REDTextServer.isTextLoaded("A", false));
		
		t1 = REDTextServer.acquireText("A", false);
		t2 = REDTextServer.acquireText("A", true);
		assertTrue(!t1.toString().equals(t2.toString()));
		assertTrue("A must be loaded (w/o private copies)", REDTextServer.isTextLoaded("A", false));
		assertTrue("A must be loaded (with private copies)", REDTextServer.isTextLoaded("A", true));
		
		REDTextServer.releaseText(t1); t1 = null;
		assertTrue("A must not be loaded (w/o private copies)", !REDTextServer.isTextLoaded("A", false));
		assertTrue("A must still be loaded (with private copies)", REDTextServer.isTextLoaded("A", true));
		
		REDTextServer.releaseText(t2); t2 = null;
		assertTrue("A must not be loaded (w/o private copies)", !REDTextServer.isTextLoaded("A", false));
		assertTrue("A must not be loaded (with private copies)", !REDTextServer.isTextLoaded("A", true));
	}

	public void testIterator() {	
		REDText t1 = REDTextServer.acquireText("A", false);
		REDText t2 = REDTextServer.acquireText("B", false);
		REDText t3 = REDTextServer.acquireText("C", false);
		REDText t4 = REDTextServer.acquireText("BB", false);
		REDText t5 = REDTextServer.acquireText("AB", true);
		
		Iterator iter = REDTextServer.getTextsFilenameIterator();
		assertTrue(iter.hasNext());
		assertEquals("A", iter.next());
		assertTrue(iter.hasNext());
		assertEquals("AB", iter.next());
		assertTrue(iter.hasNext());
		assertEquals("B", iter.next());
		assertTrue(iter.hasNext());
		assertEquals("BB", iter.next());
		assertTrue(iter.hasNext());
		assertEquals("C", iter.next());
		assertTrue(!iter.hasNext());
		REDTextServer.releaseText(t1);
		REDTextServer.releaseText(t2);
		REDTextServer.releaseText(t3);
		REDTextServer.releaseText(t4);
		REDTextServer.releaseText(t5);
	}
	
	public void testSharedRefCount() {
		assertEquals(0, REDTextServer.getSharedRefCount("foo"));
		REDEditor e1 = new REDEditor("foo", false);
		assertEquals(1, REDTextServer.getSharedRefCount("foo"));
		e1.close();
		assertEquals(0, REDTextServer.getSharedRefCount("foo"));
		
		e1 = new REDEditor("foo", false);
		REDEditor e2 = new REDEditor("foo", false);
		assertEquals(2, REDTextServer.getSharedRefCount("foo"));
		e1.close();
		assertEquals(1, REDTextServer.getSharedRefCount("foo"));
		e2.close();
		assertEquals(0, REDTextServer.getSharedRefCount("foo"));
	}
	
	public void testModified() {
		assertEquals(false, REDTextServer.isTextModified("A"));
		REDEditor e1 = new REDEditor("A", false);
		assertEquals(false, REDTextServer.isTextModified("A"));
		e1.replace("foobar", 0, 0, null);
		assertEquals(true, REDTextServer.isTextModified("A"));
		e1.undo();
		assertEquals(false, REDTextServer.isTextModified("A"));
		e1.redo();
		assertEquals(true, REDTextServer.isTextModified("A"));				
		e1.close();
	}

	// Listener tests start	
	static void checkEvents(String expLog, RTestLogProxy proxy) {
		assertEquals("View Listener", expLog, String.valueOf(proxy));
	}

	static class Listener implements REDTextServerEventListener {
		public void textStateChanged(String filename, boolean modified) { }
		public void textSaved(String filename) { }
	}
	
	public void testListeners() {
		Listener a = new Listener();
		RTestLogProxy proxy = new RTestLogProxy(a);
		proxy.addLogClass(REDTextServerEventListener.class);
		REDTextServerEventListener listener = (REDTextServerEventListener) RTestLogProxy.newInstance(a, proxy);
		
		REDTextServer.addTextServerEventListener(listener);
		REDEditor editor = new REDEditor(TMP_FILE_NORMAL, false);
		
		// must not have events 
		checkEvents("", proxy);
		
		// test change of state
		editor.replace("foo", 0, 0, null);
		checkEvents("textStateChanged(" + TMP_FILE_NORMAL + ", true)", proxy);
		
		// undo changes => text is no longer modified
		editor.undo();
		checkEvents("textStateChanged(" + TMP_FILE_NORMAL + ", true)\n" + 
			"textStateChanged(" + TMP_FILE_NORMAL + ", false)", proxy);
			
		// redo changes => text is modified again
		editor.redo();
		checkEvents("textStateChanged(" + TMP_FILE_NORMAL + ", true)\n" + 
			"textStateChanged(" + TMP_FILE_NORMAL + ", false)\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", true)", proxy);
			
		// text is already modified => must not receive additional notification
		editor.replace("bar", 0, 0, null);
		checkEvents("textStateChanged(" + TMP_FILE_NORMAL + ", true)\n" + 
			"textStateChanged(" + TMP_FILE_NORMAL + ", false)\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", true)", proxy);
		
		// test save event => also gives a change of state as saving removes the "modified" flag
		editor.saveFile(null);
		checkEvents("textStateChanged(" + TMP_FILE_NORMAL + ", true)\n" + 
			"textStateChanged(" + TMP_FILE_NORMAL + ", false)\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", true)\n" + 
			"textSaved(" + TMP_FILE_NORMAL + ")\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", false)", proxy);
			
		// This time, undo will send us before the checkpoint of the last save thus giving a modified state again.
		editor.undo();
		checkEvents("textStateChanged(" + TMP_FILE_NORMAL + ", true)\n" + 
			"textStateChanged(" + TMP_FILE_NORMAL + ", false)\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", true)\n" + 
			"textSaved(" + TMP_FILE_NORMAL + ")\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", false)\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", true)", proxy);
			
		// Closing the editor must change the state back to unmodified again.
		editor.close();
		assertTrue(!REDTextServer.isTextLoaded(TMP_FILE_NORMAL, false));
		checkEvents("textStateChanged(" + TMP_FILE_NORMAL + ", true)\n" + 
			"textStateChanged(" + TMP_FILE_NORMAL + ", false)\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", true)\n" + 
			"textSaved(" + TMP_FILE_NORMAL + ")\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", false)\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", true)\n" +
			"textStateChanged(" + TMP_FILE_NORMAL + ", false)", proxy);
		assertTrue(!REDTextServer.isTextModified(TMP_FILE_NORMAL));
		
		REDTextServer.removeTextServerEventListener(listener);
		File file = new File(TMP_FILE_NORMAL);
		assertTrue("Cannot delete " + file.getAbsolutePath(), file.delete());
	}
	// Listener tests end
	
	public static Test suite() {
		return new TestSuite(RTestREDTextServer.class);
	}	
}
