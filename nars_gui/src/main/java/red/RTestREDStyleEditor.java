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
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import red.util.*;

/** JUnit test class for REDStyleEditor, the GUI editor for styles.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDStyleEditor extends TestCase {

	final static String TMP_FILE_NORMAL = "RTestREDStyleEditor.1.tmp";
	
	public static Test suite() {
		return new TestSuite(RTestREDStyleEditor.class);
	}
	
	public RTestREDStyleEditor(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		readAdditionalStyleFile("red/RTestREDStyleEditor.1.xml", null);
		fStyleEditor = new REDStyleEditor();
	}
	
	public void testGUIParts() {
		assertNotNull(fStyleEditor.getHierarchyGUI());
		assertNotNull(fStyleEditor.getStyleEditGUI());
		assertNotNull(fStyleEditor.getSampleEditor());
		assertNotNull(fStyleEditor.getThemeChooser());
	}
	
	private static void assertSortedChildren(TreeNode node) {
		TreeNode child;
		String lastNode = "";
		for (int x = 0; x < node.getChildCount(); x++) {
			child = node.getChildAt(x);
			assertTrue("Last node '" + lastNode + "' is not less than '" + child + "'.", lastNode.compareTo(String.valueOf(child)) < 0);
			lastNode = String.valueOf(child);
			assertSortedChildren(child);
		}
	}
	
	public void testSorting() {
		assertSortedChildren(fStyleEditor.getHierarchyTreeRoot());
	}
	
	public void testIconsForTree() {
		assertNotNull(ClassLoader.getSystemResourceAsStream("red/icons/tree_collapsed.png"));
		assertNotNull(ClassLoader.getSystemResourceAsStream("red/icons/tree_expanded.png"));
	}
	
	static class Listener implements REDStyleEditorSelectionListener {
		public void styleSelected(REDStyle style) {
			fLastStyle = style;
		}
		REDStyle fLastStyle;
	}
	
	static void checkEvents(String expLog, RTestLogProxy proxy) {
		assertEquals("View Listener", expLog, String.valueOf(proxy));
	}
	
	public void testListener() {
		Listener a = new Listener();
		RTestLogProxy proxy = new RTestLogProxy(a);
		proxy.addLogClass(REDStyleEditorSelectionListener.class);
		REDStyleEditorSelectionListener listener = (REDStyleEditorSelectionListener) RTestLogProxy.newInstance(a, proxy);
		assertTrue(listener.equals(listener));
		
		assertTrue(fStyleEditor.addStyleEditorSelectionListener(listener));
		JTree tree = fStyleEditor.getHierarchyTree();

		// Select topmost style, which is "Default"
		tree.setSelectionRow(0);
		checkEvents("styleSelected(" + fStyleEditor.getStyleManager().doGetDefaultStyle() + ')', proxy);
		proxy.clear();
		checkEvents("", proxy);
		assertEquals(fStyleEditor.getStyleManager().doGetDefaultStyle(), a.fLastStyle);

		// Clear selection => must receive event too.
		tree.clearSelection();
		checkEvents("styleSelected(null)", proxy);
		assertEquals(null, a.fLastStyle);
		
		// Remove listener and check to see we don't have any events.
		assertTrue(fStyleEditor.removeStyleEditorSelectionListener(listener));
		tree.setSelectionRow(0);
		checkEvents("styleSelected(null)", proxy);
		assertEquals(null, a.fLastStyle);
	}
	
	public void testSelectionToEditUpdate() {
		Listener a = new Listener();
		
		assertTrue(fStyleEditor.addStyleEditorSelectionListener(a));
		JTree tree = fStyleEditor.getHierarchyTree();
		for (int x = 0; x < tree.getRowCount(); x++) {
			tree.setSelectionRow(x);
			REDStyle s = a.fLastStyle;
			assertEquals(s.getDisplayName(), fStyleEditor.fEditDisplayName.getText());
			assertEquals(s.getDescription(), fStyleEditor.fEditDescription.getText());
			assertEquals(s.getFontFace(), String.valueOf(fStyleEditor.fEditFontFace.getSelectedItem()));
			assertEquals(String.valueOf(s.getFontSize()), fStyleEditor.fEditFontSize.getEditor().getItem());
			assertTrue(s.getFontStyle().equalsIgnoreCase(String.valueOf(fStyleEditor.fEditFontStyle.getSelectedItem())));
			assertEquals(s.getForeground(), fStyleEditor.fEditForeground.getBackground());
			assertEquals(s.getBackground(), fStyleEditor.fEditBackground.getBackground());
			assertEquals(s.getLining(), fStyleEditor.fEditLining.getSelectedItem());
			assertEquals(!s.definesFontFace("Default"), fStyleEditor.fEditFontFaceInherited.isSelected());
			assertEquals(!s.definesFontSize("Default"), fStyleEditor.fEditFontSizeInherited.isSelected());
			assertEquals(!s.definesFontStyle("Default"), fStyleEditor.fEditFontStyleInherited.isSelected());
			assertEquals(!s.definesLining("Default"), fStyleEditor.fEditLiningInherited.isSelected());
			assertEquals(!s.definesForeground("Default"), fStyleEditor.fEditForegroundInherited.isSelected());
			assertEquals(!s.definesBackground("Default"), fStyleEditor.fEditBackgroundInherited.isSelected());
		}
		assertTrue(fStyleEditor.removeStyleEditorSelectionListener(a));
	}
	
	public void testSingleSelectionModel() {
		JTree tree = fStyleEditor.getHierarchyTree();
		assertEquals(0, tree.getSelectionCount());
		for (int x = 0; x < tree.getRowCount(); x++) {
			tree.addSelectionRow(x);
			assertEquals(1, tree.getSelectionCount());
			assertEquals(x, tree.getSelectionRows()[0]);
		}
	}
	
	public void testShowStyle() {
		boolean found[] = new boolean[2]; found[0] = false; found[1] = false;
		assertTrue(REDStyleManager.hasStyle("StyleEditorTest1"));
		assertTrue(fStyleEditor.showStyle("StyleEditorTest1"));
		assertTrue(fStyleEditor.showStyle("StyleEditorTest2"));

		Listener a = new Listener();
		assertTrue(fStyleEditor.addStyleEditorSelectionListener(a));
		JTree tree = fStyleEditor.getHierarchyTree();

		for (int x = 0; x < tree.getRowCount(); x++) {
			tree.setSelectionRow(x);
			if (a.fLastStyle.getName().equals("StyleEditorTest1")) {
				found[0] = true;
			}
			else if (a.fLastStyle.getName().equals("StyleEditorTest2")) {
				found[1] = true;
			}
		}
		
		for (int x = 0; x < 2; x++) {
			assertTrue(found[x]);
		}
	}
	
//	public void testInheritedCheckboxes() {
//		doTestInheritedCheckboxes("Default");
//	}
	
	public void testInheritedCheckboxesWithNonDefaultTheme() {	// need separate method, because will change styles.
		doTestInheritedCheckboxes("Balrog");
	}
	
	
	public void doTestInheritedCheckboxes(String theme) {
		fStyleEditor.fThemes.setSelectedItem(theme);
		assertEquals(theme, fStyleEditor.getTheme());
		assertTrue(fStyleEditor.showStyle("StyleEditorTest1"));
		assertTrue(fStyleEditor.showStyle("StyleEditorTest2"));
		assertTrue(fStyleEditor.selectStyle("StyleEditorTest2"));
		REDStyle s = fStyleEditor.fStyleManager.doGetStyle("StyleEditorTest2");

		Font oldFont = s.getFont();
	
		assertEquals("StyleEditorTest2", fStyleEditor.getSelectedStyle().getName());

		// Font face
		assertEquals("Monospaced", String.valueOf(fStyleEditor.fEditFontFace.getSelectedItem()));
		assertEquals(false, fStyleEditor.fEditFontFaceInherited.isSelected());
		assertTrue(s.definesFontFace(theme));
		fStyleEditor.fEditFontFaceInherited.doClick();
		assertEquals(true, fStyleEditor.fEditFontFaceInherited.isSelected());
		assertEquals("Helvetica", String.valueOf(fStyleEditor.fEditFontFace.getSelectedItem()));
		assertTrue(!s.definesFontFace(theme));
		assertEquals("Helvetica", s.getFontFace());
		assertTrue(oldFont != s.getFont());
		fStyleEditor.fEditFontFaceInherited.doClick();
		assertEquals(false, fStyleEditor.fEditFontFaceInherited.isSelected());
		assertTrue(s.definesFontFace(theme));		
		assertEquals("Helvetica", s.getFontFace());
		oldFont = s.getFont();
		
		// Font size
		assertEquals("10", String.valueOf(fStyleEditor.fEditFontSize.getEditor().getItem()));
		assertEquals(false, fStyleEditor.fEditFontSizeInherited.isSelected());
		assertTrue(s.definesFontSize(theme));
		fStyleEditor.fEditFontSizeInherited.doClick();
		assertEquals(true, fStyleEditor.fEditFontSizeInherited.isSelected());
		assertEquals("12", String.valueOf(fStyleEditor.fEditFontSize.getEditor().getItem()));
		assertTrue(!s.definesFontSize(theme));
		assertTrue(oldFont != s.getFont());
		assertEquals(12, s.getFontSize());
		fStyleEditor.fEditFontSizeInherited.doClick();
		assertEquals(false, fStyleEditor.fEditFontSizeInherited.isSelected());
		assertTrue(s.definesFontSize(theme));		
		assertEquals(12, s.getFontSize());
		oldFont = s.getFont();
		
		// Font style
		assertEquals("italic", String.valueOf(fStyleEditor.fEditFontStyle.getSelectedItem()));
		assertEquals(false, fStyleEditor.fEditFontStyleInherited.isSelected());
		assertTrue(s.definesFontStyle(theme));
		fStyleEditor.fEditFontStyleInherited.doClick();
		assertEquals(true, fStyleEditor.fEditFontStyleInherited.isSelected());
		assertEquals("plain", String.valueOf(fStyleEditor.fEditFontStyle.getSelectedItem()));
		assertTrue(!s.definesFontStyle(theme));
		assertTrue(oldFont != s.getFont());
		assertEquals("plain", s.getFontStyle());
		fStyleEditor.fEditFontStyleInherited.doClick();
		assertEquals(false, fStyleEditor.fEditFontStyleInherited.isSelected());
		assertTrue(s.definesFontStyle(theme));		
		assertEquals("plain", s.getFontStyle());
		oldFont = s.getFont();

		// Lining
		assertEquals("singleunder", String.valueOf(fStyleEditor.fEditLining.getSelectedItem()));
		assertEquals(false, fStyleEditor.fEditLiningInherited.isSelected());
		assertTrue(s.definesLining(theme));
		fStyleEditor.fEditLiningInherited.doClick();
		assertEquals(true, fStyleEditor.fEditLiningInherited.isSelected());
		assertEquals("none", String.valueOf(fStyleEditor.fEditLining.getSelectedItem()));
		assertTrue(!s.definesLining(theme));
		assertEquals(REDLining.NONE, s.getLining());
		fStyleEditor.fEditLiningInherited.doClick();
		assertEquals(false, fStyleEditor.fEditLiningInherited.isSelected());
		assertTrue(s.definesLining(theme));		
		assertEquals(REDLining.NONE, s.getLining());
		
		// Foreground
		assertEquals(new Color(30, 30, 30), fStyleEditor.fEditForeground.getBackground());
		assertEquals(false, fStyleEditor.fEditForegroundInherited.isSelected());
		assertTrue(s.definesForeground(theme));
		fStyleEditor.fEditForegroundInherited.doClick();
		assertEquals(true, fStyleEditor.fEditForegroundInherited.isSelected());
		assertEquals(new Color(0, 0, 0), fStyleEditor.fEditForeground.getBackground());
		assertTrue(!s.definesForeground(theme));
		assertEquals(new Color(0, 0, 0), s.getForeground());
		fStyleEditor.fEditForegroundInherited.doClick();
		assertEquals(false, fStyleEditor.fEditForegroundInherited.isSelected());
		assertTrue(s.definesForeground(theme));		
		assertEquals(new Color(0, 0, 0), s.getForeground());
		
		// Background
		assertEquals(new Color(55, 55, 55), fStyleEditor.fEditBackground.getBackground());
		assertEquals(false, fStyleEditor.fEditBackgroundInherited.isSelected());
		assertTrue(s.definesBackground(theme));
		fStyleEditor.fEditBackgroundInherited.doClick();
		assertEquals(true, fStyleEditor.fEditBackgroundInherited.isSelected());
		assertEquals(new Color(255, 255, 255), fStyleEditor.fEditBackground.getBackground());
		assertTrue(!s.definesBackground(theme));
		assertEquals(new Color(255, 255, 255), s.getBackground());
		fStyleEditor.fEditBackgroundInherited.doClick();
		assertEquals(false, fStyleEditor.fEditBackgroundInherited.isSelected());
		assertTrue(s.definesBackground(theme));		
		assertEquals(new Color(255, 255, 255), s.getBackground());
	}
	
	
	

	private void readAdditionalStyleFile(String filename, File backingStore) {
		InputStream is = getClass().getClassLoader().getResourceAsStream( filename);
		assertNotNull(is);
		REDStyleManager.readStyleFile(is, filename, backingStore);
	}
	
	public void testInheritedCheckboxesOnDefaultStyle() {
		assertTrue(fStyleEditor.selectStyle("Default"));
		assertEquals(false, fStyleEditor.fEditFontFaceInherited.isEnabled());
		assertEquals(false, fStyleEditor.fEditFontSizeInherited.isEnabled());
		assertEquals(false, fStyleEditor.fEditFontStyleInherited.isEnabled());
		assertEquals(false, fStyleEditor.fEditLiningInherited.isEnabled());
		assertEquals(false, fStyleEditor.fEditForegroundInherited.isEnabled());
		assertEquals(false, fStyleEditor.fEditBackgroundInherited.isEnabled());
		
		fStyleEditor.fEditBackgroundInherited.doClick();
		fStyleEditor.fEditFontFaceInherited.doClick();
		fStyleEditor.fEditFontSizeInherited.doClick();
		fStyleEditor.fEditFontStyleInherited.doClick();
		fStyleEditor.fEditForegroundInherited.doClick();
		fStyleEditor.fEditLiningInherited.doClick();
	}
	
	public void testEditToInheritedUpdate() {
		assertTrue(fStyleEditor.showStyle("StyleEditorTest3"));
		assertTrue(fStyleEditor.showStyle("StyleEditorTest3"));
		assertTrue(fStyleEditor.selectStyle("StyleEditorTest4"));
		REDStyle s = fStyleEditor.getStyleManager().doGetStyle("StyleEditorTest4");
		
		assertEquals(true, fStyleEditor.fEditFontFaceInherited.isSelected());
		fStyleEditor.fEditFontFace.setSelectedItem("Dialog");
		assertEquals(false, fStyleEditor.fEditFontFaceInherited.isSelected());
		assertEquals("Dialog", s.getFontFace());
		
		assertEquals(true, fStyleEditor.fEditFontSizeInherited.isSelected());
		fStyleEditor.fEditFontSize.setSelectedItem("14");
		assertEquals(false, fStyleEditor.fEditFontSizeInherited.isSelected());
		assertEquals(14, s.getFontSize());
		
		assertEquals(true, fStyleEditor.fEditFontStyleInherited.isSelected());
		fStyleEditor.fEditFontStyle.setSelectedItem("bold");
		assertEquals(false, fStyleEditor.fEditFontStyleInherited.isSelected());
		assertEquals("bold", s.getFontStyle());
		
		assertEquals(true, fStyleEditor.fEditLiningInherited.isSelected());
		fStyleEditor.fEditLining.setSelectedItem(REDLining.get("singlethrough"));
		assertEquals(false, fStyleEditor.fEditFontStyleInherited.isSelected());
		assertEquals(REDLining.SINGLETHROUGH, s.getLining());
		
		assertEquals(true, fStyleEditor.fEditForegroundInherited.isSelected());
		fStyleEditor.fEditForeground.changeColor(new Color(20, 20, 20));
		assertEquals(false, fStyleEditor.fEditForegroundInherited.isSelected());
		assertEquals(new Color(20, 20, 20), s.getForeground());
		
		assertEquals(true, fStyleEditor.fEditBackgroundInherited.isSelected());
		fStyleEditor.fEditBackground.changeColor(new Color(120, 20, 20));
		assertEquals(false, fStyleEditor.fEditBackgroundInherited.isSelected());
		assertEquals(new Color(120, 20, 20), s.getBackground());		
	}
	
	public void testThemeIterator() {
		readAdditionalStyleFile("red/RTestREDTextStyles.5.xml", null);
		REDStyle s = REDStyleManager.getStyle("ThemesTestStyle1");
		Iterator iter = s.themeIterator();
		assertTrue(iter.hasNext()); assertEquals("Default", iter.next());
		assertTrue(iter.hasNext()); assertEquals("TestTheme1", iter.next());
		assertTrue(iter.hasNext()); assertEquals("TestTheme2", iter.next());
		assertTrue(!iter.hasNext());
	}
	
	public void testThemeChooser() {
		assertTrue(fStyleEditor.selectStyle("StyleEditorTest5"));
		assertEquals("Helvetica", fStyleEditor.fEditFontFace.getSelectedItem());
		assertEquals("12", fStyleEditor.fEditFontSize.getEditor().getItem());
		assertEquals("plain", fStyleEditor.fEditFontStyle.getSelectedItem());
		assertEquals(REDLining.NONE, fStyleEditor.fEditLining.getSelectedItem());
		assertEquals(new Color(0, 0, 0), fStyleEditor.fEditForeground.getBackground());
		assertEquals(new Color(255, 255, 255), fStyleEditor.fEditBackground.getBackground());
		
		fStyleEditor.fThemes.setSelectedItem("Balrog");
		assertEquals("Mordor", fStyleEditor.fEditFontFace.getSelectedItem());
		assertEquals("32", fStyleEditor.fEditFontSize.getEditor().getItem());
		assertEquals("bold", fStyleEditor.fEditFontStyle.getSelectedItem());
		assertEquals(REDLining.DOUBLETHROUGH, fStyleEditor.fEditLining.getSelectedItem());
		assertEquals(new Color(255, 0, 0), fStyleEditor.fEditForeground.getBackground());
		assertEquals(new Color(0, 0, 0), fStyleEditor.fEditBackground.getBackground());
		
		fStyleEditor.fThemes.setSelectedItem("Gandalf");
		assertEquals("Gondor", fStyleEditor.fEditFontFace.getSelectedItem());
		assertEquals("8", fStyleEditor.fEditFontSize.getEditor().getItem());
		assertEquals("italic", fStyleEditor.fEditFontStyle.getSelectedItem());
		assertEquals(REDLining.SINGLEUNDER, fStyleEditor.fEditLining.getSelectedItem());
		assertEquals(new Color(127, 127, 127), fStyleEditor.fEditForeground.getBackground());
		assertEquals(new Color(0, 0, 0), fStyleEditor.fEditBackground.getBackground());		

		fStyleEditor.fThemes.setSelectedItem("Default");
		assertEquals("Helvetica", fStyleEditor.fEditFontFace.getSelectedItem());
		assertEquals("12", fStyleEditor.fEditFontSize.getEditor().getItem());
		assertEquals("plain", fStyleEditor.fEditFontStyle.getSelectedItem());
		assertEquals(REDLining.NONE, fStyleEditor.fEditLining.getSelectedItem());
		assertEquals(new Color(0, 0, 0), fStyleEditor.fEditForeground.getBackground());
		assertEquals(new Color(255, 255, 255), fStyleEditor.fEditBackground.getBackground());


		// test derived style
		assertTrue(fStyleEditor.showStyle("StyleEditorTest6"));
		assertTrue(fStyleEditor.selectStyle("StyleEditorTest6"));
		assertEquals("Helvetica", fStyleEditor.fEditFontFace.getSelectedItem());
		assertEquals("12", fStyleEditor.fEditFontSize.getEditor().getItem());
		assertEquals("plain", fStyleEditor.fEditFontStyle.getSelectedItem());
		assertEquals(REDLining.NONE, fStyleEditor.fEditLining.getSelectedItem());
		assertEquals(new Color(0, 0, 0), fStyleEditor.fEditForeground.getBackground());
		assertEquals(new Color(255, 255, 255), fStyleEditor.fEditBackground.getBackground());
		
		fStyleEditor.fThemes.setSelectedItem("Balrog");
		assertEquals("Mordor", fStyleEditor.fEditFontFace.getSelectedItem());
		assertEquals("32", fStyleEditor.fEditFontSize.getEditor().getItem());
		assertEquals("bold", fStyleEditor.fEditFontStyle.getSelectedItem());
		assertEquals(REDLining.DOUBLETHROUGH, fStyleEditor.fEditLining.getSelectedItem());
		assertEquals(new Color(255, 0, 0), fStyleEditor.fEditForeground.getBackground());
		assertEquals(new Color(0, 0, 0), fStyleEditor.fEditBackground.getBackground());
		
		fStyleEditor.fThemes.setSelectedItem("Gandalf");
		assertEquals("Gondor", fStyleEditor.fEditFontFace.getSelectedItem());
		assertEquals("8", fStyleEditor.fEditFontSize.getEditor().getItem());
		assertEquals("italic", fStyleEditor.fEditFontStyle.getSelectedItem());
		assertEquals(REDLining.SINGLEUNDER, fStyleEditor.fEditLining.getSelectedItem());
		assertEquals(new Color(127, 127, 127), fStyleEditor.fEditForeground.getBackground());
		assertEquals(new Color(0, 0, 0), fStyleEditor.fEditBackground.getBackground());		

		fStyleEditor.fThemes.setSelectedItem("Default");
		assertEquals("Helvetica", fStyleEditor.fEditFontFace.getSelectedItem());
		assertEquals("12", fStyleEditor.fEditFontSize.getEditor().getItem());
		assertEquals("plain", fStyleEditor.fEditFontStyle.getSelectedItem());
		assertEquals(REDLining.NONE, fStyleEditor.fEditLining.getSelectedItem());
		assertEquals(new Color(0, 0, 0), fStyleEditor.fEditForeground.getBackground());
		assertEquals(new Color(255, 255, 255), fStyleEditor.fEditBackground.getBackground());
	}
	
	private void assertNodeParent(String nodeName, String parent) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) fStyleEditor.getHierarchyTreeRoot();
		Enumeration e = node.breadthFirstEnumeration();
		boolean found = false;
		while (e.hasMoreElements() && !found) {
			node = (DefaultMutableTreeNode) e.nextElement();
			REDStyle s = (REDStyle) node.getUserObject();
			if (s.getName().equals(nodeName)) {
				found = true;
			}
		}
		assertTrue("No node with style '" + node + "'.", found);
		
		node = (DefaultMutableTreeNode) node.getParent();
		assertEquals(parent, ((REDStyle) node.getUserObject()).getName());
	}
	
	public void testThemedInheritance() {
		assertTrue(fStyleEditor.showStyle("StyleEditorTest5"));
		assertTrue(fStyleEditor.selectStyle("StyleEditorTest7"));
		assertTrue(fStyleEditor.selectStyle("StyleEditorTest7"));
		REDStyle s2 = fStyleEditor.getStyleManager().doGetStyle("StyleEditorTest2");
		REDStyle s5 = fStyleEditor.getStyleManager().doGetStyle("StyleEditorTest5");
		REDStyle s7 = fStyleEditor.getStyleManager().doGetStyle("StyleEditorTest7");

		assertEquals("Helvetica", fStyleEditor.fEditFontFace.getSelectedItem());
		assertEquals("12", fStyleEditor.fEditFontSize.getEditor().getItem());
		assertEquals("plain", fStyleEditor.fEditFontStyle.getSelectedItem());
		assertEquals(REDLining.NONE, fStyleEditor.fEditLining.getSelectedItem());
		assertEquals(new Color(0, 0, 0), fStyleEditor.fEditForeground.getBackground());
		assertEquals(new Color(255, 255, 255), fStyleEditor.fEditBackground.getBackground());
		assertEquals(s5, s7.getSuperStyle());
		assertNodeParent("StyleEditorTest7", "StyleEditorTest5");
		
		fStyleEditor.fThemes.setSelectedItem("Balrog");		
		assertEquals("Monospaced", fStyleEditor.fEditFontFace.getSelectedItem());
		assertEquals("10", fStyleEditor.fEditFontSize.getEditor().getItem());
		assertEquals("italic", fStyleEditor.fEditFontStyle.getSelectedItem());
		assertEquals(REDLining.SINGLEUNDER, fStyleEditor.fEditLining.getSelectedItem());
		assertEquals(new Color(30, 30, 30), fStyleEditor.fEditForeground.getBackground());
		assertEquals(new Color(55, 55, 55), fStyleEditor.fEditBackground.getBackground());
		assertEquals(s2, s7.getSuperStyle());
		assertNodeParent("StyleEditorTest7", "StyleEditorTest2");
	}
	
	public void testSaving() throws Exception {		
		REDStyle s1 = fStyleEditor.getStyleManager().doGetStyle("StyleEditorTest10");
		assertTrue(s1.setFontStyle("Default", "italic"));
		fStyleEditor.saveChangedStyles(new FileOutputStream(new File(TMP_FILE_NORMAL)));
		assertFileContent(
"<?xml version=\"1.0\"?>\n" +
		'\n' +
"<REDConfig>\n" +
"\t<Style id=\"StyleEditorTest10\">\n" +
"\t\t<FontStyle>italic</FontStyle>\n" +
"\t\t<Super>Default</Super>\n" +
"\t</Style>\n" +
"</REDConfig>\n"
		);

		assertTrue(s1.setLining("Default", REDLining.DOUBLEUNDER));
		fStyleEditor.saveChangedStyles(new FileOutputStream(new File(TMP_FILE_NORMAL)));
		assertFileContent(
"<?xml version=\"1.0\"?>\n" +
		'\n' +
"<REDConfig>\n" +
"\t<Style id=\"StyleEditorTest10\">\n" +
"\t\t<FontStyle>italic</FontStyle>\n" +
"\t\t<Lining>doubleunder</Lining>\n" +
"\t\t<Super>Default</Super>\n" +
"\t</Style>\n" +
"</REDConfig>\n"
		);
		
		assertTrue(s1.setFontFace("Default", "Monospaced"));
		fStyleEditor.saveChangedStyles(new FileOutputStream(new File(TMP_FILE_NORMAL)));
		assertFileContent(
"<?xml version=\"1.0\"?>\n" +
		'\n' +
"<REDConfig>\n" +
"\t<Style id=\"StyleEditorTest10\">\n" +
"\t\t<FontFace>Monospaced</FontFace>\n" +
"\t\t<FontStyle>italic</FontStyle>\n" +
"\t\t<Lining>doubleunder</Lining>\n" +
"\t\t<Super>Default</Super>\n" +
"\t</Style>\n" +
"</REDConfig>\n"
		);

		assertTrue(s1.setForeground("Default", 50, 60, 70));
		assertTrue(s1.setBackground("Default", 150, 160, 170));
		fStyleEditor.saveChangedStyles(new FileOutputStream(new File(TMP_FILE_NORMAL)));
		assertFileContent(
"<?xml version=\"1.0\"?>\n" +
		'\n' +
"<REDConfig>\n" +
"\t<Style id=\"StyleEditorTest10\">\n" +
"\t\t<FontFace>Monospaced</FontFace>\n" +
"\t\t<FontStyle>italic</FontStyle>\n" +
"\t\t<Lining>doubleunder</Lining>\n" +
"\t\t<Foreground red=\"50\" green=\"60\" blue=\"70\"/>\n" +
"\t\t<Background red=\"150\" green=\"160\" blue=\"170\"/>\n" +
"\t\t<Super>Default</Super>\n" +
"\t</Style>\n" +
"</REDConfig>\n"
		);
		
		assertTrue(s1.setFontSize("Balrog", 30));

		fStyleEditor.saveChangedStyles(new FileOutputStream(new File(TMP_FILE_NORMAL)));
		assertFileContent(
"<?xml version=\"1.0\"?>\n" +
		'\n' +
"<REDConfig>\n" +
"\t<Style id=\"StyleEditorTest10\" theme=\"Balrog\">\n" +
"\t\t<FontFace>Monospaced</FontFace>\n" +
"\t\t<FontSize>30</FontSize>\n" +
"\t\t<FontStyle>italic</FontStyle>\n" +
"\t\t<Lining>doubleunder</Lining>\n" +
"\t\t<Foreground red=\"50\" green=\"60\" blue=\"70\"/>\n" +
"\t\t<Background red=\"150\" green=\"160\" blue=\"170\"/>\n" +
"\t\t<Super>Default</Super>\n" +
"\t</Style>\n" +
"\t<Style id=\"StyleEditorTest10\">\n" +
"\t\t<FontFace>Monospaced</FontFace>\n" +
"\t\t<FontStyle>italic</FontStyle>\n" +
"\t\t<Lining>doubleunder</Lining>\n" +
"\t\t<Foreground red=\"50\" green=\"60\" blue=\"70\"/>\n" +
"\t\t<Background red=\"150\" green=\"160\" blue=\"170\"/>\n" +
"\t\t<Super>Default</Super>\n" +
"\t</Style>\n" +
"</REDConfig>\n"
		);
		File f = new File(TMP_FILE_NORMAL);
		assertTrue(f.delete());
	}		
	
	private static void assertFileContent(String content) throws IOException {
		RandomAccessFile file = new RandomAccessFile(TMP_FILE_NORMAL, "r");
		byte arr[] = new byte[(int) file.length()];
		file.read(arr);
		file.close();
		assertEquals(RTestAuxiliary.quoteTab(content), RTestAuxiliary.quoteTab(new String(arr)));
	}
	
	REDStyleEditor fStyleEditor;
}
