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

import java.io.*;
import junit.framework.*;
import red.file.*;

/** JUnit TestCase base class for red.REDEditor. This class has no own
  * TestSuite. Tests for editor event handling are implemented here for the
  * usage in extending classes.
  *
  * @author rli@chello.at, shamelessly stolen from a unit test by Gerald Czech - gerald.czech@scch.at
  * @tier test
  */
public class RTestREDEditor extends TestCase {
	/**
	 * The name of the normal temporary file. This file is created before the
	 * tests by setUp() as a copy of IN_FILE_NORMAL and deleted afterwards by
	 * tearDown(). Subclasses should use this file for modifications.
	 */
	static final String TMP_FILE_NORMAL = "RTestREDEditor.1.tmp";

	/** The string representation of the REDEditor to test. */
	static final String TEXT_CONTENT = "Don't meddle in the affairs of wizards,"
			+ "\n" + "for they are subtle and quick to anger." + "\n\n"
			+ "The Lord of the Rings." + "\n";
	
	/** The REDEditor object to test. */
	private REDEditor fEditor;
	
	/** The REDEditor with a shared text to test. */
	REDEditor fSharedEditor;
	
	/** The empty REDEditor object to test. */
	private REDEditor fEmptyTestEditor;
				
	/** View level Listener for editor events. */
	private REDEventListener fViewListener;
	private RTestLogProxy fViewProxy;

	/** Normal level Listener for editor events. */
	private REDEventListener fNormalListener;
	private RTestLogProxy fNormalProxy;

	/** Late level Listener for editor events. */
	private REDEventListener fLateListener;
	private RTestLogProxy fLateProxy;

	/**
	 * Static method to construct the TestSuite of RTestREDEditor. This suite
	 * is a cumulation of the test suites of all subclasses of RTestREDEditor.
	 * So it holds all test cases for the REDEditor class. 
	 */
	public static Test suite()	{
		TestSuite suite = new TestSuite("REDEditor all tests");
		suite.addTestSuite(RTestREDEditorFileIO.class);
		suite.addTestSuite(RTestREDEditorModification.class);
		suite.addTestSuite(RTestREDEditorIndent.class);
		suite.addTestSuite(RTestREDEditorMisc.class);
		return suite;
	}
	/**
	 * Constructor.
	 * Constructs a new RTestREDEditor object.
	 *
	 * @param name the name of the method to test, when for adding to a suite.
	 */
	public RTestREDEditor(String name) {
		super(name);
	}

	protected void logEventClass(Class eventListenerInterface) {
		fViewProxy.addLogClass(eventListenerInterface);
		fNormalProxy.addLogClass(eventListenerInterface);
		fLateProxy.addLogClass(eventListenerInterface);
	}
	
	protected void ignoreEventMethod(String methodName) {
		fViewProxy.addIgnoreMethod(methodName);
		fNormalProxy.addIgnoreMethod(methodName);
		fLateProxy.addIgnoreMethod(methodName);
	}		
	
	/**
	 * Initializes Testdata.
	 */
	protected void setUp() throws Exception {
		super.setUp();
		REDFile tmpFile = new REDFile(TMP_FILE_NORMAL);
		REDFileRider r = new REDFileRider(tmpFile);
		
		tmpFile.purge();
		r.writeBytes(TEXT_CONTENT.getBytes(), TEXT_CONTENT.length());
		assertTrue("Cannot write to current directory. Needed for testing.", r.getRes() == 0);
		tmpFile.close();
		fEditor = new REDEditor(TMP_FILE_NORMAL, true);
		fSharedEditor = new REDEditor(TMP_FILE_NORMAL, false);
		
		REDEventAdapter a = new RTestREDEventListener(REDTextEventListener.RLL_VIEW);
		fViewProxy = new RTestLogProxy(a);
		fViewListener = (REDEventListener) RTestLogProxy.newInstance(a, fViewProxy);
		
		a = new RTestREDEventListener(REDTextEventListener.RLL_NORMAL);
		fNormalProxy = new RTestLogProxy(a);
		fNormalListener = (REDEventListener) RTestLogProxy.newInstance(a, fNormalProxy);
		
		a = new RTestREDEventListener(REDTextEventListener.RLL_LATE);
		fLateProxy = new RTestLogProxy(a);
		fLateListener = (REDEventListener) RTestLogProxy.newInstance(a, fLateProxy);
		
		fEmptyTestEditor = new REDEditor();
		fEditor.addREDEventListener(fLateListener);		
		fEditor.addREDEventListener(fNormalListener);		
		fEditor.addREDEventListener(fViewListener);
		fEmptyTestEditor.addREDEventListener(fLateListener);
		fEmptyTestEditor.addREDEventListener(fNormalListener);
		fEmptyTestEditor.addREDEventListener(fViewListener);
	}
	
	/**
	 * Do some clean up. This is deleting the temp file (TMP_FILE_NORMAL).
	 */
	protected void tearDown() throws Exception {
		fEditor.close(); fEditor = null;
		fSharedEditor.close(); fSharedEditor = null;
		fEmptyTestEditor.close(); fEmptyTestEditor = null;
		// delete backup		
		File file = new File(TMP_FILE_NORMAL);
		assertTrue("Cannot delete " + file.getAbsolutePath(), file.delete());
		super.tearDown();
	}

	/**
	 * Returns the editor object to use for testing in all RTestREDEditor objects.
	 */
	REDEditor getTestEditor() {
		return fEditor;
	}
	
	/**
	 * Returns the editor object used for all tests concerning shared texts.
	 */
	REDEditor getSharedTestEditor() {
		return fSharedEditor;
	}

	/**
	 * Returns the empty text object to use for testing in all RTestREDEditor
	 * objects.
	 */
	REDEditor getEmptyTestEditor() {
		return fEmptyTestEditor;
	}
	
	/**
	 * Checks if the given event log matches those in the listeners.
	 * @param expLog The expected log of method calls 
	 */
	void checkEvents(String expLog) {
		assertEquals("View Listener", expLog, String.valueOf(fViewProxy));
		assertEquals("Normal Listener", expLog, String.valueOf(fNormalProxy));
		assertEquals("Late Listener", expLog, String.valueOf(fLateProxy));
	}

	/**
	 * Checks that no event has been generated accidently. In other words it is
	 * checked if there is no last event and the event queue is empty.
	 */
	void checkNoEvent() {
		checkEvents("");
	}
	
	void clearEvents() {
		fViewProxy.clear();
		fNormalProxy.clear();
		fLateProxy.clear();
	}
}