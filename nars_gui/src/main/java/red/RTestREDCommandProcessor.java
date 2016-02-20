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

/** Regression test for REDCommandProcessor
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDCommandProcessor extends TestCase implements REDCommandProcessorEventListener {
	public RTestREDCommandProcessor(String name) {
		super(name);
	}
	
	class DummyCommand extends REDCommand {
		DummyCommand(String name, boolean silent) {
			fName = name;
			fSilent = silent;
		}

		DummyCommand(String name) {
			this(name, false);
		}
		
		public void undoIt() {
			if (!fSilent) {
				out("undo" + fName + ' ');
			}
		}
		
		public void redoIt() {
			if (!fSilent) {
				out("redo" + fName + ' ');
			}
		}
		
		public void doIt() {
			if (!fSilent) {
				out("do" + fName + ' ');
			}
		}

		String fName;
		boolean fSilent;
	}

	public void out(String str) {
		fLog.append(str);
	}
	
	public void resetLog() {
		fLog = new StringBuffer();
	}
		
	public void setUp() {
		resetLog();
	}
	
	public void testCheckpoint() {
		REDCommandProcessor p = new REDCommandProcessor(new REDText(""), 8);
		assertEquals(false, p.isModified());
		p.perform(new DummyCommand("A"));
		assertEquals(true, p.isModified());
		p.undo();
		assertEquals(false, p.isModified());
		p.redo();
		assertEquals(true, p.isModified());
		p.setCheckPoint();
		assertEquals(false, p.isModified());
		p.perform(new DummyCommand("B"));
		p.perform(new DummyCommand("C"));
		assertEquals(true, p.isModified());
		p.undo();
		p.setCheckPoint();
		p.redo();
		assertEquals(true, p.isModified());
		p.undo();
		assertEquals(false, p.isModified());
		p.undo();
		assertEquals(true, p.isModified());
		p.undo();
		p.perform(new DummyCommand("D"));
		assertEquals(true, p.isModified());
	}

	public void testSimple() {
		REDCommandProcessor p = new REDCommandProcessor(new REDText(""), 8);
		for (int i = 0; i < 20; i++) {
			p.perform(new DummyCommand(Integer.toString(i)));
			assertEquals(true, p.canUndo());
			assertEquals(false, p.canRedo());
		}
		assertEquals("do0 do1 do2 do3 do4 do5 do6 do7 do8 do9 do10 do11 do12 do13 do14 do15 do16 do17 do18 do19 ", String.valueOf(fLog));

		resetLog();
		while (p.canUndo()) {
			p.undo();
		}
		assertEquals("undo19 undo18 undo17 undo16 undo15 undo14 undo13 undo12 ", String.valueOf(fLog));

		resetLog();
		while (p.canRedo()) {
			p.redo();
		}
		assertEquals("redo12 redo13 redo14 redo15 redo16 redo17 redo18 redo19 ", String.valueOf(fLog));

		resetLog();
		p.redo();	// should fail
		p.undo();
		p.redo();
		assertEquals("undo19 redo19 ", String.valueOf(fLog));
		
		resetLog();
		p.undo();
		p.undo();
		p.perform(new DummyCommand("Branch"));
		p.redo();		
		p.undo();
		p.undo();
		assertEquals("undo19 undo18 doBranch undoBranch undo17 ", String.valueOf(fLog));
		
		resetLog();
		p.finish();
		assertEquals(false, p.canUndo());
		assertEquals(false, p.canRedo());
		p.perform(new DummyCommand("A"));
		assertEquals(true, p.canUndo());
		assertEquals(false, p.canRedo());
		p.undo();
		assertEquals(false, p.canUndo());
		assertEquals(true, p.canRedo());
		p.redo();
		assertEquals(true, p.canUndo());
		assertEquals(false, p.canRedo());
		p.undo();
		p.perform(new DummyCommand("B"));
		assertEquals(true, p.canUndo());
		assertEquals(false, p.canRedo());
		p.undo();
		assertEquals(false, p.canUndo());
		assertEquals(true, p.canRedo());
		assertEquals("doA undoA redoA undoA doB undoB ", String.valueOf(fLog));
	}
	
	public void beforeCmdProcessorChange(int operation) {
		out("before");
		switch(operation) {
			case REDCommandProcessorEventListener.DO:
				out("Do");
			break;
			case REDCommandProcessorEventListener.UNDO:
				out("Undo");
			break;
			case REDCommandProcessorEventListener.REDO:
				out("Redo");
			break;
			case REDCommandProcessorEventListener.CHECKPOINT:
				out("Checkpoint");
			break;
		}
		out(" ");
	}
	
	public void afterCmdProcessorChange(int operation) {
		out("after");
		switch(operation) {
			case REDCommandProcessorEventListener.DO:
				out("Do");
			break;
			case REDCommandProcessorEventListener.UNDO:
				out("Undo");
			break;
			case REDCommandProcessorEventListener.REDO:
				out("Redo");
			break;
			case REDCommandProcessorEventListener.CHECKPOINT:
				out("Checkpoint");
			break;
		}
		out(" ");
	}

	public void testListener() {
		REDCommandProcessor p = new REDCommandProcessor(new REDText(""));
		p.addREDCommandProcessorEventListener(this);
		p.perform(new DummyCommand("A", true));
		p.undo();
		p.undo();
		p.redo();
		p.redo();
		p.setCheckPoint();
		p.setCheckPoint();
		p.removeREDCommandProcessorEventListener(this);
		p.perform(new DummyCommand("A", true));
		p.undo();
		p.undo();
		p.redo();
		p.redo();
		p.setCheckPoint();
		p.setCheckPoint();		
		assertEquals("beforeDo afterDo beforeUndo afterUndo beforeRedo afterRedo beforeCheckpoint afterCheckpoint beforeCheckpoint afterCheckpoint ", String.valueOf(fLog));
	}
	
	public void testDoubleListenerRegistration() {
		REDCommandProcessor p = new REDCommandProcessor(new REDText(""));
		assertTrue(p.addREDCommandProcessorEventListener(this));
		assertTrue(!p.addREDCommandProcessorEventListener(this));
	}	
		
	public static Test suite() {
		return new TestSuite(RTestREDCommandProcessor.class);
	}
	
	StringBuffer fLog;
}
