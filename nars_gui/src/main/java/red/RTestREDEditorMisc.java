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

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;
import junit.framework.*;

/** Miscellaneous test cases for REDEditor
  * @author rli@chello.at 
  * @tier test
  */
public class RTestREDEditorMisc extends RTestREDEditor {
	public RTestREDEditorMisc(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(RTestREDEditorMisc.class);
	}
			
	public void setUp() throws Exception {
		super.setUp();
	}	
	
	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testWordConstituents() {
		REDEditor editor = new REDEditor("A", false);
		assertTrue(editor.isWordConstituent((byte) 'A'));
		assertTrue(editor.isWordConstituent((byte) 'L'));
		assertTrue(editor.isWordConstituent((byte) 'Z'));
		assertTrue(editor.isWordConstituent((byte) 'a'));
		assertTrue(editor.isWordConstituent((byte) 'l'));
		assertTrue(editor.isWordConstituent((byte) 'z'));
		assertTrue(!editor.isWordConstituent((byte) '_'));
		assertTrue(!editor.isWordConstituent((byte) '&'));
		assertTrue(!editor.isWordConstituent((byte) ':'));

		editor.setWordConstituents("_&");
		assertTrue(editor.isWordConstituent((byte) 'A'));
		assertTrue(editor.isWordConstituent((byte) 'L'));
		assertTrue(editor.isWordConstituent((byte) 'Z'));
		assertTrue(editor.isWordConstituent((byte) 'a'));
		assertTrue(editor.isWordConstituent((byte) 'l'));
		assertTrue(editor.isWordConstituent((byte) 'z'));
		assertTrue(editor.isWordConstituent((byte) '_'));
		assertTrue(editor.isWordConstituent((byte) '&'));
		assertTrue(!editor.isWordConstituent((byte) ':'));		

		// word constituent settings are per-editor settings...
		REDEditor editor2 = new REDEditor("A", false);
		assertTrue(!editor2.isWordConstituent((byte) '_'));
		assertTrue(!editor2.isWordConstituent((byte) '&'));
		editor.close();
		editor2.close();
	}
	
	public void testChangeCount() {
		REDEditor editor = new REDEditor();
		assertEquals(0, editor.getChangeCount());
		editor.replace("Gandalf", 0, 0, null);
		assertEquals(1, editor.getChangeCount());
		editor.undo();
		assertEquals(0, editor.getChangeCount());
		editor.undo();
		assertEquals(0, editor.getChangeCount());
		editor.redo();
		assertEquals(1, editor.getChangeCount());
		editor.startMacroCommand("TestMacro");
		assertEquals(1, editor.getChangeCount());
		editor.replace("Balrog", 0, 0, null);
		assertEquals(1, editor.getChangeCount());
		editor.replace("Udun", 0, 0, null);
		assertEquals(1, editor.getChangeCount());
		editor.endMacroCommand();
		assertEquals(2, editor.getChangeCount());
		editor.undo();		
		editor.undo();		
		assertEquals(0, editor.getChangeCount());
		editor.redo();		
		editor.redo();		
		assertEquals(2, editor.getChangeCount());
		editor.redo();		
		assertEquals(2, editor.getChangeCount());		
	}
	
	public void testSavedChangeCount() {
		REDEditor editor = new REDEditor(TMP_FILE_NORMAL, false);
		assertEquals(0, editor.getSavedChangeCount());
		editor.replace("Gandalf", 0, 0, null);
		editor.startMacroCommand("TestMacro");
		editor.replace("Balrog", 0, 0, null);
		editor.replace("Udun", 0, 0, null);
		editor.endMacroCommand();
		editor.saveFile(null);
		assertEquals(2, editor.getSavedChangeCount());
		editor.undo();		
		editor.saveFile(null);
		assertEquals(1, editor.getSavedChangeCount());
		editor.close();
	}
	
	public void testCopy() {
		REDEditor editor = getTestEditor();
		editor.replace("Test", 0, editor.length(), null);
		assertEquals("es", editor.copy(1, 3));
		assertEquals("Te", editor.copy(0, 2));
		assertEquals("Te", editor.copy(-1, 2));
		assertEquals("st", editor.copy(2, 4));
		assertEquals("st", editor.copy(2, 5));
		assertEquals("Test", editor.copy(-17, 99));
		assertEquals("", editor.copy(3, 1));
		assertEquals("", editor.copy(-17, -19));
		assertEquals("", editor.copy(5, 1));
		assertEquals("", editor.copy(3, 3));		
	}
	
	public void testDefaultStyle() {
		REDEditor editor = getTestEditor();
		assertEquals(REDStyleManager.getDefaultStyle(), editor.getDefaultStyle());
		assertTrue(REDStyleManager.hasStyle("String"));	// Get some style.
		REDStyle s = REDStyleManager.getStyle("String");
		editor.setDefaultStyle(s);
		assertEquals(s, editor.getDefaultStyle());
		editor.setDefaultStyle(REDStyleManager.getDefaultStyle());
		assertEquals(REDStyleManager.getDefaultStyle(), editor.getDefaultStyle());
	}		
	
	public void testGetFilename() {
		REDEditor editor = getTestEditor();
		assertEquals(TMP_FILE_NORMAL, editor.getFilename());
	}
	
	public void testGetSelectedText() {
		REDEditor editor = getTestEditor();
		assertEquals("", editor.getSelectedText());
		for (int x = 0; x <= editor.length(); x++) {
			for (int y = x; y <= editor.length(); y++) {
				editor.setSelection(x, y);
				assertEquals(TEXT_CONTENT.substring(x, y), editor.getSelectedText());
			}
		}
	}
	
	public void testFocussedWord() {
		REDEditor editor = getTestEditor();
		editor.setSelection(0, 0);
		assertEquals("Don", editor.getFocussedWord());
		editor.setSelection(1, 1);
		assertEquals("Don", editor.getFocussedWord());
		editor.setSelection(2, 2);
		assertEquals("Don", editor.getFocussedWord());
		editor.setSelection(3, 3);
		assertEquals("Don", editor.getFocussedWord());
		editor.setSelection(0, 3);
		assertEquals("Don", editor.getFocussedWord());
		editor.setSelection(0, 5);
		assertEquals("Don", editor.getFocussedWord());
		editor.setSelection(5, 0);
		assertEquals("Don", editor.getFocussedWord());
		editor.setSelection(editor.length());
		assertEquals("", editor.getFocussedWord());
		
		editor.setWordConstituents("'");
		editor.setSelection(0, 0);
		assertEquals("Don't", editor.getFocussedWord());
		editor.setSelection(1, 1);
		assertEquals("Don't", editor.getFocussedWord());
		editor.setSelection(2, 2);
		assertEquals("Don't", editor.getFocussedWord());
		editor.setSelection(3, 3);
		assertEquals("Don't", editor.getFocussedWord());
		editor.setSelection(0, 3);
		assertEquals("Don't", editor.getFocussedWord());
		editor.setSelection(0, 5);
		assertEquals("Don't", editor.getFocussedWord());
		editor.setSelection(5, 0);
		assertEquals("Don't", editor.getFocussedWord());
		editor.setSelection(editor.length());
		assertEquals("", editor.getFocussedWord());
		editor.setSelection(editor.length() - 1);
		assertEquals("", editor.getFocussedWord());
		
		editor.setWordConstituents(".");
		editor.setSelection(editor.length());
		assertEquals("", editor.getFocussedWord());
		editor.setSelection(editor.length() - 1);
		assertEquals("Rings.", editor.getFocussedWord());
		
		editor.setWordConstituents("");
		editor.replace("Test", 0, editor.length(), null);
		editor.setSelection(editor.length());
		assertEquals("Test", editor.getFocussedWord());
	}
	
	public void testGetLineTopAndHeight() {
		REDEditor editor = getTestEditor();
		int lineHeight = new JPanel().getFontMetrics(REDStyleManager.getDefaultStyle().getFont()).getHeight();
		for (int x = 0; x < editor.getNrOfLines(); x++) {
			assertEquals(x * lineHeight, editor.getLineTop(x));
			assertEquals(lineHeight, editor.getLineHeight(x));
			assertEquals(x, editor.getLineAtHeight(x * lineHeight));
			if (x > 0) {
				assertEquals(x-1, editor.getLineAtHeight(x * lineHeight - 1));
			}
		}
	}
	
	/** Test canUndo() and canRedo(). A more detailed test happens in RTestREDCommandProcessor. */
	public void testCommandQueue() {
		REDEditor editor = getTestEditor();
		assertEquals(false, editor.canUndo());
		assertEquals(false, editor.canRedo());
		editor.replace("Test", 0, 0, null);
		assertEquals(true, editor.canUndo());
		assertEquals(false, editor.canRedo());
		editor.replace("Foobar", 0, 0, null);
		assertEquals(true, editor.canUndo());
		assertEquals(false, editor.canRedo());
		editor.undo();
		assertEquals(true, editor.canUndo());
		assertEquals(true, editor.canRedo());
		editor.undo();
		assertEquals(false, editor.canUndo());
		assertEquals(true, editor.canRedo());
		editor.redo();
		assertEquals(true, editor.canUndo());
		assertEquals(true, editor.canRedo());
		editor.redo();
		assertEquals(true, editor.canUndo());
		assertEquals(false, editor.canRedo());
		editor.undo();
		assertEquals(true, editor.canUndo());
		assertEquals(true, editor.canRedo());
		editor.clearCommandQueue();
		assertEquals(false, editor.canUndo());
		assertEquals(false, editor.canRedo());
	}
	
	private String getClipboardAsString() throws Exception {
		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		return String.valueOf(c.getContents(this).getTransferData(DataFlavor.stringFlavor));
	}
	
	public void testClipboard() throws Exception {
		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		REDEditor editor = getTestEditor();
		
		// test copy
		editor.setSelection(6, 12);
		editor.clipboardCopy();
		assertEquals("meddle", getClipboardAsString());

		// test cut
		editor.setSelection(0, 5);
		editor.clipboardCut();
		assertEquals("Don't", getClipboardAsString());
		assertEquals(TEXT_CONTENT.substring(5), editor.asString());
		
		// test paste
		editor.clipboardPaste();
		assertEquals(TEXT_CONTENT, editor.asString());
		editor.clipboardPaste();
		assertEquals("Don't" + TEXT_CONTENT, editor.asString());
		editor.setSelection(editor.length());
		editor.clipboardPaste();
		assertEquals("Don't" + TEXT_CONTENT + "Don't", editor.asString());
	}

	public void testCaretBlink() {
		REDEditor editor = getTestEditor();
		editor.setCaretBlink(50);
		assertEquals(50, editor.getCaretBlink());
		editor.setCaretBlink(10000);
		assertEquals(10000, editor.getCaretBlink());
		editor.setCaretBlink(0);
		assertEquals(0, editor.getCaretBlink());
		editor.setCaretBlink(-10);
		assertEquals(0, editor.getCaretBlink());
		editor.setCaretBlink(60); editor.setCaretBlink(-10);
		assertEquals(0, editor.getCaretBlink());
	}

	public void testVisualizeWhitespace() {	// tbd: check if really painted. but how?
		REDEditor editor = getTestEditor();
		assertEquals(false, editor.getVisualizeWhitespace());	// off by default.
		editor.setVisualizeWhitespace(true);
		assertEquals(true, editor.getVisualizeWhitespace());
		editor.setVisualizeWhitespace(false);
		assertEquals(false, editor.getVisualizeWhitespace());
	}

	public void testHighlightLine() {
		REDEditor editor = getTestEditor();
		assertEquals(false, editor.hasHighlightLine());
		editor.setHighlightLine(1);
		assertTrue(editor.hasHighlightLine());
		assertEquals(1, editor.getHighlightLine());
		editor.replace("foo", 0, 0, null);
		assertEquals(false, editor.hasHighlightLine());
		
		editor.setHighlightLine(editor.getNrOfLines() + 1000);
		assertTrue(editor.hasHighlightLine());
		assertEquals(editor.getNrOfLines(), editor.getHighlightLine());

		editor.setHighlightLine(0);
		assertTrue(editor.hasHighlightLine());
		assertEquals(0, editor.getHighlightLine());

		editor.setHighlightLine(-1);
		assertEquals(false, editor.hasHighlightLine());

		editor.setHighlightLine(-20);
		assertEquals(false, editor.hasHighlightLine());

		int x = editor.getHighlightLine();	// may produce any result, but not throw an exception
		editor.setHighlightColor(Color.blue);	// tbd: check if really painted. But how?
	}
	
	public void testViewMode() {
		REDEditor editor = getTestEditor();
		editor.setViewMode(REDAuxiliary.VIEWMODE_READONLY);
		assertEquals(REDAuxiliary.VIEWMODE_READONLY, editor.getViewMode());
		editor.setViewMode(REDAuxiliary.VIEWMODE_OVERWRITE);
		assertEquals(REDAuxiliary.VIEWMODE_OVERWRITE, editor.getViewMode());
		editor.setViewMode(REDAuxiliary.VIEWMODE_INSERT);
		assertEquals(REDAuxiliary.VIEWMODE_INSERT, editor.getViewMode());
	}
	
	static class Controller extends REDViewControllerDecorator {
		Controller(REDViewController c, String id, StringBuffer log) {
			super(c); 
			fLog = log;
			fId = id;
		}
		
		public void key(REDView v, KeyEvent e) { 
			fLog.append(fId);
			super.key(v, e);
		}
		
		StringBuffer fLog;
		String fId;
	}
	
	private static void sendKey(REDEditor editor) {
		REDViewController c = editor.getController();
		c.keyTyped(new KeyEvent(editor.getView(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, 0, 'A'));
	}

	public void testControllerDecoratorManagement() {
		REDEditor editor = getTestEditor();
		StringBuffer log = new StringBuffer();
		Controller a = new Controller(editor.getController(), "A", log);
		editor.setController(a);
		Controller b = new Controller(editor.getController(), "B", log);
		editor.setController(b);
		Controller c = new Controller(editor.getController(), "C", log);
		editor.setController(c);
		sendKey(editor);
		assertEquals("CBA", String.valueOf(log)); log.delete(0, log.length());

		editor.removeControllerDecorator(b);
		sendKey(editor);
		assertEquals("CA", String.valueOf(log)); log.delete(0, log.length());
		
		editor.removeControllerDecorator(a);
		sendKey(editor);
		assertEquals("C", String.valueOf(log)); log.delete(0, log.length());
		
		editor.removeControllerDecorator(c);
		sendKey(editor);
		assertEquals("", String.valueOf(log));
	}
	
	class PluginAdder extends REDEventAdapter {
		PluginAdder(REDPlugin plugin) {
			fPlugin = plugin;
		}
		
		public void beforeFileLoad(String filename) {
			REDEditor editor = getTestEditor();
			editor.addPlugin(fPlugin);
		}
		REDPlugin fPlugin;
	}
	
	class PluginFlipper extends REDEventAdapter {
		PluginFlipper(REDPlugin plugin) {
			fPlugin = plugin;
		}
		
		public void beforeFileLoad(String filename) {
			REDEditor editor = getTestEditor();
			editor.addPlugin(fPlugin);
			editor.removePlugin(fPlugin);
		}
		REDPlugin fPlugin;
	}
	
	
	static class TestPlugin1 extends REDPlugin {
		public void afterFileLoad(String filename) {
			assertTrue("Must not call single method", false);
		}
	}
	
	public void testPluginQueue() {
		TestPlugin1 p1 = new TestPlugin1();
		REDEditor editor = getTestEditor();
		PluginAdder a = new PluginAdder(p1);
		editor.addREDEventListener(a);
		editor.loadFile(TMP_FILE_NORMAL, false);
		editor.removePlugin(p1);
		editor.removeREDEventListener(a);
		
		PluginFlipper f = new PluginFlipper(new TestPlugin1());
		editor.addREDEventListener(f);
		editor.loadFile(TMP_FILE_NORMAL, false);
		editor.removeREDEventListener(f);
	}
}
