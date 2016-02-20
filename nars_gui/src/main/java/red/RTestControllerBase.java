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
import java.awt.event.*;
import java.io.*;

/** Base class for regression tests that test view controllers and/or decorators.
  * @author rli@chello.at
  * @tier test
  */
public class RTestControllerBase extends TestCase {
	
	static final String TMP_FILE = "RTestControllerBase.tmp";
	protected static final int LEFT = InputEvent.BUTTON1_MASK;
	protected static final int MIDDLE = InputEvent.BUTTON2_MASK;
	protected static final int RIGHT = InputEvent.BUTTON3_MASK;
	
	public RTestControllerBase(String name) {
		super(name);
	}
	
	private static long now() {
		return System.currentTimeMillis();
	}
	
	protected void send(int keyCode, char keyChar) {
		send(keyCode, keyChar, 0);
	}

	protected void send(int keyCode, char keyChar, int modifiers) {
		REDViewController c = fEditor.getController();
		if (keyCode == KeyEvent.VK_UNDEFINED) {
			c.keyTyped(new KeyEvent(fEditor.getView(), KeyEvent.KEY_TYPED, now(), modifiers, keyCode, keyChar));
		}			
		else {
			c.keyPressed(new KeyEvent(fEditor.getView(), KeyEvent.KEY_PRESSED, now(), modifiers, keyCode));
		}
	}
	
	protected void tryWithAllModifiers(int keyCode) {
		int nrCombinations = 1 << fMasks.length;
		for (int i = 0; i < nrCombinations; i++) {
			int mValue = 0;
			int x = i;
			for (int j = fMasks.length - 1; j >= 0; j--) {
				if (x >= (1 << j)) {
					mValue += fMasks[j];
					x -= (1 << j);
				}
			}
			send(keyCode, '\0', mValue);
		}
	}
	
//	    public MouseEvent(Component source, int id, long when, int modifiers,
//                      int x, int y, int clickCount, boolean popupTrigger) {
	/** Simulate mouse click.
	  * @param gap The gap in which to click. This is a text position (0 <= gap <= editor.length())
	  * @param which The mouse button to click. Use LEFT, MIDDLE or RIGHT.
	  * @param clickCount The number of clicks to be sent.
	  */
	protected void mouseClick(int gap, int which, int clickCount) {
		REDView view = (REDView) fEditor.getView();
		REDViewPosition vp = view.locatePosition(gap, null);
		MouseEvent m = new MouseEvent(view, MouseEvent.MOUSE_PRESSED, now(), which, 
			vp.getUpperLeftPoint().x, vp.getUpperLeftPoint().y, clickCount, false);
		fEditor.getController().mousePressed(m);
	}
	
	public void setUp() throws Exception {
		super.setUp();
		fEditor = new REDEditor(TMP_FILE, false);
		fEditor.replace("Words are the voice of the heart", 0, fEditor.length(), null);
		assertTrue("Cannot save " + TMP_FILE, fEditor.saveFile(null));
	}
	
	public void tearDown() throws Exception {
		super.tearDown();
		fEditor.close(); fEditor = null;
		File file = new File(TMP_FILE);
		assertTrue("Cannot delete " + file.getAbsolutePath(), file.delete());
	}
	
	protected REDEditor fEditor;
	int [] fMasks = { InputEvent.SHIFT_MASK, InputEvent.CTRL_MASK, InputEvent.ALT_MASK, InputEvent.ALT_GRAPH_MASK, InputEvent.META_MASK };
}