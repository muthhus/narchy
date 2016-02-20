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
import red.util.*;

/** JUnit TestCase base class for red.REDText. This class has no own TestSuite.
  * @author gerald.czech@scch.at
  * @tier test
  */
public class RTestREDText extends RTestLogObserver {
	/**
	 * The name of the normal temporary file. This file is created before the
	 * tests by setUp() as a copy of IN_FILE_NORMAL and deleted afterwards by
	 * tearDown(). Subclasses should use this file for modifications.
	 */
	static final String TMP_FILE_NORMAL = "RTestREDText.1.tmp";

	/** The string representation of the REDText to test. */
	static final String TEXT_CONTENT = "Don't meddle in the affairs of wizards,"
			+ "\n" + "for they are subtle and quick to anger." + "\n\n"
			+ "The Lord of the Rings." + "\n";
	
	/** The REDText object to test. */
	private REDText fTestText;
	
	/** The empty REDText object to test. */
	private REDText fEmptyTestText;
	
	/** View level Listener for editor events. */
	REDEventListener fViewListener;
	private RTestLogProxy fViewProxy;

	/** Normal level Listener for editor events. */
	REDEventListener fNormalListener;
	private RTestLogProxy fNormalProxy;

	/** Late level Listener for editor events. */
	REDEventListener fLateListener;
	private RTestLogProxy fLateProxy;
	
	/**
	 * Static method to construct the TestSuite of RTestREDText. This suite
	 * is a cumulation of the test suites of all subclasses of RTestREDText.
	 * So it holds all test cases for the REDText class. 
	 */
	public static Test suite()	{
		TestSuite suite = new TestSuite("REDText all tests");
		suite.addTestSuite(RTestREDTextFileIO.class);
		suite.addTestSuite(RTestREDTextRepresentations.class);		
		suite.addTestSuite(RTestREDTextModification.class);
		suite.addTestSuite(RTestREDTextLineHandling.class);
		suite.addTestSuite(RTestREDTextStyles.class);
		return suite;
	}
	/**
	 * Constructs a new RTestREDText object.
	 *
	 * @param name the name of the method to test, when for adding to a suite.
	 */
	public RTestREDText(String name) {
		super(name);
	}
	
	/**
	 * Checks the timing of the occurance of the REDEvents, depending on their
	 * listener level.
	 */
	private void checkTiming() {
		long viewTime = fViewProxy.getTime();
		long normalTime = fNormalProxy.getTime();
		long lateTime = fLateProxy.getTime();

		assertTrue("REDEventListener RLL_LATE event occured before RLL_NORMAL event." + lateTime + " vs. " + normalTime, lateTime >= normalTime);
		assertTrue("REDEventListener RLL_NORMAL event occured before RLL_VIEW event." + normalTime + " vs. " + viewTime, normalTime >= viewTime);
	}	

	/**
	 * Checks if the given event log matches those in the listeners.
	 * @param expLog The expected log of method calls 
	 */
	void checkEvents(String expLog) {
		assertEquals("View Listener", expLog, String.valueOf(fViewProxy));
		assertEquals("Normal Listener", expLog, String.valueOf(fNormalProxy));
		assertEquals("Late Listener", expLog, String.valueOf(fLateProxy));
		checkTiming();
	}

	/**
	 * Checks that no event has been generated accidently. In other words it is
	 * checked if there is no last event and the event queue is empty.
	 */
	void checkNoEvent() {
		checkEvents("");
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
		tmpFile.purge();
		REDFileRider r = new REDFileRider(tmpFile);
		r.writeBytes(TEXT_CONTENT.getBytes(), TEXT_CONTENT.length());
		assertTrue("Cannot write to current directory. Needed for testing.", r.getRes() == 0);
		tmpFile.close();
		fTestText = new REDText(TMP_FILE_NORMAL);
		fEmptyTestText = new REDText("");
		
		REDEventAdapter a = new RTestREDEventListener(REDTextEventListener.RLL_VIEW);
		fViewProxy = new RTestLogProxy(a);
		fViewListener = (REDEventListener) RTestLogProxy.newInstance(a, fViewProxy);
		assertTrue(fViewListener.equals(fViewListener));
		
		a = new RTestREDEventListener(REDTextEventListener.RLL_NORMAL);
		fNormalProxy = new RTestLogProxy(a);
		fNormalListener = (REDEventListener) RTestLogProxy.newInstance(a, fNormalProxy);
		assertTrue(fNormalListener.equals(fNormalListener));
		
		a = new RTestREDEventListener(REDTextEventListener.RLL_LATE);
		fLateProxy = new RTestLogProxy(a);
		fLateListener = (REDEventListener) RTestLogProxy.newInstance(a, fLateProxy);
		assertTrue(fLateListener.equals(fLateListener));
		
		assertTrue(fTestText.addREDTextEventListener(fLateListener));
		assertTrue(fTestText.addREDTextEventListener(fViewListener));
		assertTrue(fTestText.addREDTextEventListener(fNormalListener));
		assertTrue(fEmptyTestText.addREDTextEventListener(fViewListener));
		assertTrue(fEmptyTestText.addREDTextEventListener(fLateListener));
		assertTrue(fEmptyTestText.addREDTextEventListener(fNormalListener));
	}
	
	/**
	 * Do some clean up. This is deleting the temp file (TMP_FILE_NORMAL).
	 */
	protected void tearDown() throws Exception {
		assertTrue(fTestText.removeREDTextEventListener(fViewListener));
		assertTrue(fTestText.removeREDTextEventListener(fLateListener));
		assertTrue(fTestText.removeREDTextEventListener(fNormalListener));
		assertTrue(fEmptyTestText.removeREDTextEventListener(fViewListener));
		assertTrue(fEmptyTestText.removeREDTextEventListener(fLateListener));
		assertTrue(fEmptyTestText.removeREDTextEventListener(fNormalListener));
		REDFile redFile = new REDFile(fTestText.getFilename());
		redFile.purge();
		redFile.close();
		fTestText = null;
		File file = new File(TMP_FILE_NORMAL);
		assertTrue("Cannot delete " + file.getAbsolutePath(), file.delete());
		super.tearDown();
	}

	/**
	 * Returns the text object to use for testing in all RTestREDText objects.
	 */
	REDText getTestText() {
		return fTestText;
	}

	/**
	 * Returns the empty text object to use for testing in all RTestREDText
	 * objects.
	 */
	REDText getEmptyTestText() {
		return fEmptyTestText;
	}
}
