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

/** JUnit TestCase class for the file IO handling of red.REDText. 
  * @author gerald.czech@scch.at
  * @tier test
  */
public class RTestREDTextFileIO extends RTestREDText {
	/** Constant holding the name for a second temporary file. */
	private static final String fcTmpFile = "RTestREDText.2.tmp";
	
	/**
	 * Static method to construct the TestSuite of RTestREDTextFileIO.
	 */
	public static Test suite() {
		return new TestSuite(RTestREDTextFileIO.class);
	}
	
	/**
	 * Constructor.
	 * Constructs a new RTestREDTextFileIO object.
	 */
	public RTestREDTextFileIO(String name) {
		super(name);
	}
	
	/**
	 * Initializes test data. Here a REDTextEventListener is attached to the
	 * test text.
	 */
	public void setUp() throws Exception {
		super.setUp();
		logEventClass(REDTextEventListener.class);
		ignoreEventMethod("getListenerLevel");
	}
	
	/**
	 * Do some clean up. This is removing the REDTextEventListener.
	 */
	public void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Tests the load(), the getStructure() and the isModified() methods of
	 * REDText. Also the REDTextListener load events are tested.
	 */
	public void testLoad() {
		String structure;
		String structureString;
		REDText text = getTestText();
		
		// The file was already loaded when the test text was initialized in
		// setUp method of the base class. But load again to test the listener.
		text.load();
		
		structureString = TEXT_CONTENT + "->\nnull";
		assertEquals("Loaded file doesn't equal ref. data", TEXT_CONTENT,
				text.asString()); 
		structure = text.getStructure();
		assertEquals("Structure doesn't equal ref. data structure",
				structureString, structure);
		checkEvents("beforeLoad()\n" +
			"afterLoad()");
	}
	
	/**
	 * Tests the load() and the getStructure() methods of REDText for a non
	 * existant file. Also the REDTextListener load events are tested.
	 */
	public void testLoadEmpty() {
		String structure;
		REDText text = getTestText();

		// load non existing text (load is done automatically in constructor).	
		text = new REDText("maynotexist.txt");
		text.addREDTextEventListener(fViewListener);
		text.addREDTextEventListener(fNormalListener);
		text.addREDTextEventListener(fLateListener);
		text.load();
		checkEvents("beforeLoad()");
		structure = text.getStructure();
		assertEquals("Structure of non existant file wasn't \"null\"",
				"null", structure);
	}

	/**
	 * Tests the method save() of REDText and the REDTextListener save events.
	 * Further the method isModified() is tested.
	 */
	public void testSave() {
		// proposed result string
		String result = "But it is said: " + TEXT_CONTENT;
		String actResult;
		REDFile file = new REDFile(TMP_FILE_NORMAL);
		REDText text = getTestText();
		REDFileRider rider;
		int len;
		byte[] byteArray;
		
		// insert some text at the beginning
		text.replace(0, 0, "But it is said: ");
		assertEquals("Insert text at beginning failed.", result, text.asString());
		text.save();

		rider = new REDFileRider(file);
		len = result.length();
		byteArray = new byte[len];
		rider.readBytes(byteArray, 0, len);
		file.close();
		actResult = new String(byteArray);
		checkEvents("beforeInsert(0, 16)\n"+
			"afterInsert(0, 16)\n" +
			"beforeSave()\n" + 
			"afterSave()");
	}		
	
	/** Checks whether save and load cause the current typing command to be reset */
	public void testTypingCommandReset() {
		REDText text = getTestText();
		REDTextCommand cmd = new REDTextCommand("dummy", null, null, 0, 0, "");
		text.setCurTypingCommand(cmd);
		assertNotNull(text.getCurTypingCommand());
		text.save();
		assertNull(text.getCurTypingCommand());
		
		text.setCurTypingCommand(cmd);
		assertNotNull(text.getCurTypingCommand());
		text.load();
		assertNull(text.getCurTypingCommand());
	}

	/**
	 * Tests the saveInto() method of REDText and the REDTextListener saveInTo
	 * events. Further the method isModified() is tested.
	 */
	public void testSaveInto() {
		String result = "But it is said: " + TEXT_CONTENT;
		String actResult;
		String actFilename;
		REDFile file = new REDFile(fcTmpFile);
		REDText text = getTestText();	// test text from IN_FILE_NORMAL
		REDFileRider rider;
		int len;
		byte[] byteArray;

		// save file into new tmp file
		text.replace(0, 0, "But it is said: ");
		assertEquals("Insert text at beginning failed.", result, text.asString());
		text.saveInto(fcTmpFile);

		// read file to get contents to check it success of saveInto()
		rider = new REDFileRider(file);
		len = result.length();
		byteArray = new byte[len];
		rider.readBytes(byteArray, 0, len);
		actResult = new String(byteArray);
		
		checkEvents("beforeInsert(0, 16)\n"+
			"afterInsert(0, 16)\n" +
			"beforeSaveInto(" + fcTmpFile + ")\n" + 
			"afterSaveInto(" + fcTmpFile + ')');

		// delete second tmp file		
		file.purge();
		file.close();
		file = null;
		File fileToDel = new File(fcTmpFile);
		assertTrue("Temp file doesn't exist", fileToDel.exists());
		assertTrue("Temp file cannot be read", fileToDel.canRead());
		assertTrue("Temp file delete failed", fileToDel.delete());
	}
}