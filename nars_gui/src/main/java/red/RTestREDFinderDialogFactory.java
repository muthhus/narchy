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
import javax.swing.*;
import javax.swing.text.*;

/** Test case for REDFinderDialogFactory.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDFinderDialogFactory extends TestCase implements ActionListener {
	public RTestREDFinderDialogFactory(String name) {
		super(name);
	}

	public void actionPerformed(ActionEvent e) {
		fButtonCount++;
	}
	
	private void clickFindButton() {
		fButtonCount = 0;
		fFactory.getFindButton().doClick();
		assertEquals("Button click did not happen.", 1, fButtonCount);
	}
	
	public void setUp() throws Exception {
		super.setUp();
		fEditor = new REDEditor();
		fEditor.replace("If you want him -- come and claim him!", 0, fEditor.length(), null);
		assertEquals("If you want him -- come and claim him!", fEditor.asString());
		fFinder = new REDFinder();
		fFactory = fFinder.getREDFinderDialogFactory();
		fFinder.setEditor(fEditor);
		fFactory.getFindButton().addActionListener(this);
		
		// without these defaults the test cases will fail 
		assertTrue(fFactory.getMatchCaseCheckBox().isSelected());
		assertTrue(!fFactory.getWholeWordCheckBox().isSelected());
		assertTrue(fFactory.getUseRegExpCheckBox().isSelected());
		assertTrue(fFactory.getDirection() == REDFinderDirection.FORWARD);
		assertTrue(fFactory.getReplaceAllInFile().isSelected());
	}
	
	private String getComboString() {
		JTextComponent tc = ((JTextComponent) (fFactory.getFindCombo().getEditor().getEditorComponent()));
		return tc.getText();
	}
	
	public void testSetFindString() {
		JCheckBox cb = fFactory.getUseRegExpCheckBox();
		cb.setSelected(true);
		assertTrue(fFactory.isUseRegExp());
		fFactory.setFindString("\tWhat a cool thing.\n[tm]*");
		assertEquals("\\tWhat a cool thing\\.\\n\\[tm\\]\\*", getComboString());

		cb.setSelected(false);
		assertTrue(!fFactory.isUseRegExp());
		fFactory.setFindString("\tWhat a cool thing.\n[tm]*");
		assertEquals("\tWhat a cool thing.", getComboString());
	}
	
	public void testFindForwardCaseSensitive() {
		fFactory.setFindString("i");
		assertEquals(REDFinderDirection.FORWARD, fFactory.getDirection());
		assertTrue(fFactory.getDirectionForwardButton().isSelected());
		assertTrue(fFactory.isMatchCase());
		
		clickFindButton();
		assertEquals(13, fEditor.getSelectionStart());
		assertEquals(14, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(31, fEditor.getSelectionStart());
		assertEquals(32, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		// test wrap around
		clickFindButton();
		assertEquals(13, fEditor.getSelectionStart());
		assertEquals(14, fEditor.getSelectionEnd());
	}

	public void testLogLine() {
		JLabel log = fFactory.getLogLine();
		assertEquals(" ", log.getText());	// Not "", because that would not size the label correctly :-)
		fFactory.setFindString("Durin's Bane");
		
		clickFindButton();
		assertTrue(!log.getText().isEmpty());
		assertTrue(!log.getText().equals(" "));
	}

	public void testFindForwardCaseInsensitive() {
		fFactory.setFindString("i");
		fFactory.getMatchCaseCheckBox().setSelected(false);
		assertEquals(REDFinderDirection.FORWARD, fFactory.getDirection());
		assertTrue(!fFactory.isMatchCase());
		
		clickFindButton();
		assertEquals(0, fEditor.getSelectionStart());
		assertEquals(1, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(13, fEditor.getSelectionStart());
		assertEquals(14, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(31, fEditor.getSelectionStart());
		assertEquals(32, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		// test wrap around
		clickFindButton();
		assertEquals(0, fEditor.getSelectionStart());
		assertEquals(1, fEditor.getSelectionEnd());
	}

	public void testFindBackwardCaseSensitive() {		
		fFactory.setFindString("i");
		fFactory.getDirectionBackwardButton().setSelected(true);
		assertEquals(REDFinderDirection.BACKWARD, fFactory.getDirection());
		assertTrue(fFactory.isMatchCase());

		assertEquals(REDFinderDirection.BACKWARD, fFactory.getDirection());
		clickFindButton();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(31, fEditor.getSelectionStart());
		assertEquals(32, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(13, fEditor.getSelectionStart());
		assertEquals(14, fEditor.getSelectionEnd());
		// test wrap around
		clickFindButton();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
	}

	public void testFindBackwardCaseInsensitive() {		
		fFactory.setFindString("i");
		fFactory.getDirectionBackwardButton().setSelected(true);
		fFactory.getMatchCaseCheckBox().setSelected(false);
		assertEquals(REDFinderDirection.BACKWARD, fFactory.getDirection());
		assertTrue(!fFactory.isMatchCase());

		assertEquals(REDFinderDirection.BACKWARD, fFactory.getDirection());
		clickFindButton();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(31, fEditor.getSelectionStart());
		assertEquals(32, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(13, fEditor.getSelectionStart());
		assertEquals(14, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(0, fEditor.getSelectionStart());
		assertEquals(1, fEditor.getSelectionEnd());
		// test wrap around
		clickFindButton();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
	}
	
	public void testFindWholeWord() {
		fEditor.replace("We do e-business.", 0, fEditor.length(), null);
		fFactory.setFindString("e");
		fFactory.getWholeWordCheckBox().setSelected(true);
		
		clickFindButton();
		assertEquals(6, fEditor.getSelectionStart());
		assertEquals(7, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(6, fEditor.getSelectionStart());
		assertEquals(7, fEditor.getSelectionEnd());

		// test for case sensitiveness
		fFactory.setFindString("E");
		fEditor.setSelection(0);
		clickFindButton();
		assertEquals(0, fEditor.getSelectionStart());
		assertEquals(0, fEditor.getSelectionEnd());
			
		// now we should find again
		fFactory.getMatchCaseCheckBox().setSelected(false);
		clickFindButton();
		assertEquals(6, fEditor.getSelectionStart());
		assertEquals(7, fEditor.getSelectionEnd());
		clickFindButton();
		assertEquals(6, fEditor.getSelectionStart());
		assertEquals(7, fEditor.getSelectionEnd());		
	}
	
	public void testAgain() {
		fFactory.setFindString("i");
		clickFindButton();
		assertEquals(13, fEditor.getSelectionStart());
		assertEquals(14, fEditor.getSelectionEnd());
		fFactory.findAgain();
		assertEquals(31, fEditor.getSelectionStart());
		assertEquals(32, fEditor.getSelectionEnd());
		fFactory.findAgain();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		// test wrap around
		fFactory.findAgain();
		assertEquals(13, fEditor.getSelectionStart());
		assertEquals(14, fEditor.getSelectionEnd());		
	}
	
	public void testReplace() {
		fFactory.setFindString("i");
		fFactory.setReplaceString("X");
		
		// first click selects needle, second click modifies haystack
		fFactory.getReplaceButton().doClick();
		assertEquals(13, fEditor.getSelectionStart());
		assertEquals(14, fEditor.getSelectionEnd());
		fFactory.getReplaceButton().doClick();
		assertEquals("If you want hXm -- come and claim him!", fEditor.asString());
		
		// first click selects needle, second click modifies haystack
		fFactory.getReplaceButton().doClick();
		assertEquals(31, fEditor.getSelectionStart());
		assertEquals(32, fEditor.getSelectionEnd());
		fFactory.getReplaceButton().doClick();
		assertEquals("If you want hXm -- come and claXm him!", fEditor.asString());
		
		// first click selects needle, second click modifies haystack
		fFactory.getReplaceButton().doClick();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		fFactory.getReplaceButton().doClick();
		assertEquals("If you want hXm -- come and claXm hXm!", fEditor.asString());
		
		// can no longer find needle => no change in haystack or selection
		fFactory.getReplaceButton().doClick();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		fFactory.getReplaceButton().doClick();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		assertEquals("If you want hXm -- come and claXm hXm!", fEditor.asString());

		// Turn off case sensitiveness => Uppercase "I" gets replaced.
		fFactory.getMatchCaseCheckBox().setSelected(false);
		fFactory.getReplaceButton().doClick();
		assertEquals(0, fEditor.getSelectionStart());
		assertEquals(1, fEditor.getSelectionEnd());
		fFactory.getReplaceButton().doClick();
		assertEquals("Xf you want hXm -- come and claXm hXm!", fEditor.asString());
	}
	
	public void testReplaceAllCaseSensitive() {
		fFactory.setFindString("i");
		fFactory.setReplaceString("X");
		
		fFactory.getReplaceAllButton().doClick();
		assertEquals("If you want hXm -- come and claXm hXm!", fEditor.asString());
	}
	
	public void testReplaceAllCaseInsensitive() {
		fFactory.setFindString("i");
		fFactory.setReplaceString("X");
		fFactory.getMatchCaseCheckBox().setSelected(false);
		
		fFactory.getReplaceAllButton().doClick();
		assertEquals("Xf you want hXm -- come and claXm hXm!", fEditor.asString());
	}
	
	public void testReplaceAllInSelection() {
		fFactory.setFindString("i");
		fFactory.setReplaceString("X");
		fFactory.getReplaceAllInSelection().setSelected(true);
		assertTrue(fFactory.isReplaceAllInSelection());
		
		fEditor.setSelection(12, 15);
		fFactory.getReplaceAllButton().doClick();
		assertEquals("If you want hXm -- come and claim him!", fEditor.asString());
		
		fEditor.setSelection(0, 5);
		fFactory.getReplaceAllButton().doClick();
		assertEquals("If you want hXm -- come and claim him!", fEditor.asString());
		
		fEditor.setSelection(31, 36);
		fFactory.getReplaceAllButton().doClick();
		assertEquals("If you want hXm -- come and claXm hXm!", fEditor.asString());
		
		fFactory.getMatchCaseCheckBox().setSelected(false);
		fEditor.setSelection(0, 5);
		fFactory.getReplaceAllButton().doClick();
		assertEquals("Xf you want hXm -- come and claXm hXm!", fEditor.asString());
	}
	
	public void testReplaceAndFind() {
		fFactory.setFindString("i");
		fFactory.setReplaceString("X");
		
		// first click selects needle, second click modifies haystack
		fFactory.getReplaceButton().doClick();
		assertEquals(13, fEditor.getSelectionStart());
		assertEquals(14, fEditor.getSelectionEnd());
		fFactory.getReplaceAndFindButton().doClick();
		assertEquals("If you want hXm -- come and claim him!", fEditor.asString());
		
		assertEquals(31, fEditor.getSelectionStart());
		assertEquals(32, fEditor.getSelectionEnd());
		fFactory.getReplaceAndFindButton().doClick();
		assertEquals("If you want hXm -- come and claXm him!", fEditor.asString());
		
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		fFactory.getReplaceAndFindButton().doClick();
		assertEquals("If you want hXm -- come and claXm hXm!", fEditor.asString());
		
		// can no longer find needle => selection behind last change
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		fFactory.getReplaceAndFindButton().doClick();
		assertEquals(35, fEditor.getSelectionStart());
		assertEquals(36, fEditor.getSelectionEnd());
		assertEquals("If you want hXm -- come and claXm hXm!", fEditor.asString());

		// Turn off case sensitiveness => Uppercase "I" gets replaced.
		fFactory.getMatchCaseCheckBox().setSelected(false);
		fFactory.getReplaceAndFindButton().doClick();
		assertEquals(0, fEditor.getSelectionStart());
		assertEquals(1, fEditor.getSelectionEnd());
		fFactory.getReplaceAndFindButton().doClick();
		assertEquals("Xf you want hXm -- come and claXm hXm!", fEditor.asString());
		assertEquals(0, fEditor.getSelectionStart());
		assertEquals(1, fEditor.getSelectionEnd());
	}
	
	public void testBuildingBlocks() {
		// Hmm. cannot test a lot here. We'll just check for non-null return values ...
		assertNotNull(fFactory.getButtonPanel());
		assertNotNull(fFactory.getComboPanel());
		assertNotNull(fFactory.getDirectionPanel());
		assertNotNull(fFactory.getGeneralOptionsPanel());
		assertNotNull(fFactory.getLogPanel());
		assertNotNull(fFactory.getReplaceAllOptionsPanel());
	}
	
	public void testFaultyPattern() {
		fEditor.setSelection(0);
		fFactory.setFindString("[a-z");
		assertEquals(REDFinderDirection.FORWARD, fFactory.getDirection());
		assertTrue(fFactory.getDirectionForwardButton().isSelected());
		assertTrue(fFactory.isMatchCase());
		
		clickFindButton();
		assertEquals(0, fEditor.getSelectionStart());
		assertEquals(0, fEditor.getSelectionEnd());
	}

	public static Test suite() {
		return new TestSuite(RTestREDFinderDialogFactory.class);
	}

	REDFinderDialogFactory fFactory;
	REDFinder fFinder;
	REDEditor fEditor;
	private int fButtonCount;
}
