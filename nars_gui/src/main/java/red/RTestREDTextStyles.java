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
import java.io.*;
import java.util.*;
import junit.framework.*;

/** JUnit test class for the classes REDStyle, REDStyleManager and the style handling of REDText. 
  * @author gerald.czech@scch.at
  * @tier test
  */
public class RTestREDTextStyles extends RTestREDText {

	/** The default style from the REDStyleManager. */
	private REDStyle fDefaultMgrStyle;

	/** A super style object for testing purpose. */
	private REDStyle fSuperStyle;
	
	/** A style object for testing purpose with fSuperStyle as super style. */
	private REDStyle fStyle;
	
	/**
	 * Returns a test suite holding all the styles tests.
	 */
	public static Test suite() {
		return new TestSuite(RTestREDTextStyles.class);
	}
	
	/**
	 * Constructs a new RTestREDTextStyles object.
	 *
	 * @param name the name of the method to test, when for adding to a suite.
	 */
	public RTestREDTextStyles(String name) {
		super(name);
	}

	/**
	 * Initializes Testdata for the REDStyles.
	 */
	protected void setUp() throws Exception {
		super.setUp();
		fDefaultMgrStyle = REDStyleManager.getDefaultStyle();
		fSuperStyle = new REDStyle(Color.yellow, Color.blue, REDLining.SINGLEUNDER, "Serif", "ITALIC", 16, null);
		fStyle = new REDStyle(Color.lightGray, null, null, "Dialog", "BOLDITALIC", 10, fSuperStyle);
	}
	
	/**
	 * Tests the properties of the default style of REDStyleManager.
	 */
	public void testMgrDefaultStyle() {
		assertEquals("Mgr default style has wrong foreground color.",
				Color.black, fDefaultMgrStyle.getForeground());
		assertEquals("Mgr default style has wrong background color.",
				Color.white, fDefaultMgrStyle.getBackground());
		assertEquals("Mgr default style has wrong lining.",
				REDLining.NONE, fDefaultMgrStyle.getLining());
		assertEquals("Mgr default style has wrong font.",
				new Font("Monospaced", Font.PLAIN, 12),
				fDefaultMgrStyle.getFont());
	}
	
	/**
	 * Tests the getStyle() method of REDStyleManager.
	 */
	public void testMgrGetStyle() {
		assertEquals("Mgr returns wrong style for name 'Default'",
				fDefaultMgrStyle, REDStyleManager.getStyle("Default"));
	}
	
	/**
	 * Tests the getForeground() method of REDStyle.
	 */
	public void testGetForeground() {
		assertEquals("Wrong foreground color of super style.", Color.yellow,
				fSuperStyle.getForeground());
		assertEquals("Wrong foreground color of inherited style.",
				Color.lightGray, fStyle.getForeground());
	}
	
	/**
	 * Tests the getBackground() method of REDStyle.
	 */
	public void testGetBackground() {
		assertEquals("Wrong background color of super style.", Color.blue,
				fSuperStyle.getBackground());
		assertEquals("Wrong background color of inherited style.",
				Color.blue, fStyle.getBackground());
	}

	/**
	 * Tests the getLining() method of REDStyle.
	 */
	public void testGetLining() {
		assertEquals("Wrong lining of super style.", REDLining.SINGLEUNDER,
				fSuperStyle.getLining());
		assertEquals("Wrong lining of inherited style.", REDLining.SINGLEUNDER,
				fStyle.getLining());
	}

	/**
	 * Tests the getFont() method of REDStyle.
	 */
	public void testGetFont() {
		assertEquals("Wrong font of super style.",
				new Font("Serif", Font.ITALIC, 16), fSuperStyle.getFont());
		assertEquals("Wrong font of inherited style.",
				new Font("Dialog", Font.BOLD + Font.ITALIC, 10), fStyle.getFont());
	}
	
	/**
	 * Tests the inheritance of REDStyle objects.
	 */
	public void testInheritance() {
		REDStyle inherited = new REDStyle(null, null, null, null, null, REDStyle.INHERITED, fStyle);
		assertEquals("Inherited foreground color doesn't equal super style.",
				fStyle.getForeground(), inherited.getForeground());
		assertEquals("Inherited background color doesn't equal super (2 times) style.",
				fSuperStyle.getBackground(), inherited.getBackground());
		assertEquals("Inherited lining doesn't equal super (2 times) style.",
				fSuperStyle.getLining(), inherited.getLining());
		assertEquals("Inherited font doesn't equal super style.",
				fStyle.getFont(), inherited.getFont());
	}
	
	/**
	 * Returns a string, which is the same contents as the toString() method
	 * of the given REDStyle should return.
	 *
	 * @param style the REDStyle object for which the string is constructed.
	 */
	private static String getSupposedStyleString(REDStyle style) {
		return "REDStyle (" + style.getName() + "): \n  Foreground: " + style.getForeground()
				+ "\n  Background: " + style.getBackground() + "\n  Lining: "
				+ style.getLining() + "\n  Font: " + style.getFont();
	}
	
	/**
	 * Tests the toString() method of REDStyle objects.
	 */
	public void testToString() {
		assertEquals("Super style object has wrong string representation.",
				getSupposedStyleString(fSuperStyle), fSuperStyle.toString());
		assertEquals("Inherited style object has wrong string representation.",
				getSupposedStyleString(fStyle), fStyle.toString());
	}
	
	/**
	 * Checks fi the test text has the given style on the given position.
	 *
	 * @param style the supposed style.
	 * @param pos the position in the REDText, whose style is to check.
	 */
	private void checkStyleForPos(REDStyle style, int pos) {
		REDText text = getTestText();
		assertEquals("Text has wrong style at pos " + pos + '.', style,
				text.getStyle(pos));
	}
	
	/**
	 * Tests the getDefaultStyle() and setDefaultStyle() of REDText objects.
	 */
	public void testDefaultStyle() {
		REDText text = getTestText();
		REDStyle defaultStyle = text.getDefaultStyle(); // supposed default style
		String replStr = "REPLACED";
		
		assertEquals("REDText default style doesn't equal REDStyleManager's default style.",
				fDefaultMgrStyle, defaultStyle);
		checkStyleForPos(defaultStyle, 0);
		checkStyleForPos(defaultStyle, text.length() / 2);
		checkStyleForPos(defaultStyle, text.length() - 1);
		checkStyleForPos(defaultStyle, -1);
		checkStyleForPos(defaultStyle, text.length());
		text.setDefaultStyle(fStyle);
		assertEquals("Wrong default style after setDefaultStyle().", fStyle,
				text.getDefaultStyle());
		// changing default style doen't change the existing text.
		assertEquals("Text style has change without replace to new default style.",
				defaultStyle, text.getStyle(0));
		text.replace(0, replStr.length(), replStr);
		assertEquals("Text style has changed after replace to new default style.",
				defaultStyle, text.getStyle(0));
		text.replace(0, text.length(), "");	//delete whole text
		text.replace(0, 0, replStr);		// insert text (adopt new style)
		assertEquals("New Text doesn't have new default style.", fStyle,
				text.getStyle(0));
	}
	
	/**
	 * Tests the getStyle() and setStyle() methods of REDText for the first
	 * character of REDText objects.
	 */
	public void testStyleFirstChar() {
		REDText text = getTestText();
		assertTrue("setStyle() operation has not been executed immediatly.",
				text.setStyle(0, 1, fStyle));
		assertEquals("First character has wrong style after style change.",
				fStyle, text.getStyle(0));
		assertEquals("Second character (right from gap) has wrong style after style change of first char.",
				fStyle, text.getStyle(1));
		assertEquals("Third character has wrong style after style change of first char.",
				text.getDefaultStyle(), text.getStyle(2));
	}
	
	/**
	 * Tests the getStyle() and setStyle() methods of REDText at the begin of
	 * REDText objects.
	 */
	public void testStyleBegin() {
		REDText text = getTestText();
		assertTrue("setStyle() operation has not been executed immediatly.",
				text.setStyle(0, text.length() / 3, fStyle));
		// returns fStyle cause no style is defined left from gap 0
		assertEquals("Gap left from changed part has wrong style.",	fStyle,
				text.getStyle(0));
		assertEquals("Gap in changed part has wrong style.", fStyle,
				text.getStyle(text.length() / 4));
		assertEquals("Gap right from changed part has wrong style.", fStyle,
				text.getStyle(text.length() / 3));
		assertEquals("Gap in unchanged part has wrong style.",
				text.getDefaultStyle(), text.getStyle(text.length() / 3 + 1));
	}
	
	/**
	 * Tests the getStyle() and setStyle() methods of REDText in the middle of
	 * REDText objects.
	 */
	public void testStyleMiddle() {
		REDText text = getTestText();
		assertTrue("setStyle() operation has not been executed immediatly.",
				text.setStyle(text.length() / 3, text.length() / 3 * 2,
				fStyle));
		assertEquals("Gap left from changed part has wrong style.",
				text.getDefaultStyle(), text.getStyle(text.length() / 3));
		assertEquals("Gap in changed part has wrong style.", fStyle,
				text.getStyle(text.length() / 2));
		assertEquals("Gap right from changed part has wrong style.", fStyle,
				text.getStyle(text.length() / 3 * 2));
		assertEquals("Gap in unchanged part has wrong style.",
				text.getDefaultStyle(),
				text.getStyle(text.length() / 3  * 2 + 1));
	}
	
	/**
	 * Tests the getStyle() and setStyle() methods of REDText at the end of
	 * REDText objects.
	 */
	public void testStyleEnd() {
		REDText text = getTestText();
		assertTrue("setStyle() operation has not been executed immediatly.",
				text.setStyle(text.length() / 3 * 2, text.length(),	fStyle));
		assertEquals("Gap left from changed part has wrong style.",
				text.getDefaultStyle(), text.getStyle(text.length() / 3 * 2));
		assertEquals("Gap in changed part has wrong style.", fStyle,
				text.getStyle(text.length() / 4 * 3));
		assertEquals("Gap right from changed part has wrong style.", fStyle,
				text.getStyle(text.length()));
		assertEquals("Gap in unchanged part has wrong style.",
				text.getDefaultStyle(),	text.getStyle(text.length() / 2));
	}
	
	/**
	 * Tests the getStyle() and setStyle() methods of REDText for the last
	 * character of REDText objects.
	 */
	public void testStyleLastChar() {
		REDText text = getTestText();
		assertTrue("setStyle() operation has not been executed immediatly.",
				text.setStyle(text.length() - 1, text.length(), fStyle));
		assertEquals("Gap left from changed part has wrong style.",
				text.getDefaultStyle(), text.getStyle(text.length() - 1));
		assertEquals("Gap right from changed part has wrong style.", fStyle,
				text.getStyle(text.length()));
		assertEquals("Gap in unchanged part has wrong style.",
				text.getDefaultStyle(),	text.getStyle(text.length() / 2));
	}
	
	/**
	 * Tests the getStyle() and setStyle() methods of REDText before the begin
	 * of REDText objects.
	 */
	public void testStyleBeforeBegin() {
		REDText text = getTestText();
		assertTrue("setStyle() operation has not been executed immediatly.",
				text.setStyle(- 1, 0, fStyle));
		assertEquals("First Gap (left from first character) has wrong style.",
				text.getDefaultStyle(), text.getStyle(0));
		assertEquals("getStyle() on negative returned wrong style.",
				text.getDefaultStyle(), text.getStyle(-1));
		assertEquals("Gap in unchanged part has wrong style.",
				text.getDefaultStyle(),	text.getStyle(text.length() / 2));
	}
	
	/**
	 * Tests the getStyle() and setStyle() methods of REDText after the end of
	 * REDText objects.
	 */
	public void testStyleAfterEnd() {
		REDText text = getTestText();
		assertTrue("setStyle() operation has not been executed immediatly.",
				text.setStyle(text.length(), text.length() + 1, fStyle));
		assertEquals("Last Gap (right from last character) has wrong style.",
				text.getDefaultStyle(), text.getStyle(text.length()));
		assertEquals("getStyle() on index greater length returned wrong style.",
				text.getDefaultStyle(), text.getStyle(text.length() + 1));
		assertEquals("Gap in unchanged part has wrong style.",
				text.getDefaultStyle(),	text.getStyle(text.length() / 2));
	}
	
	/**
	 * Tests the getStyle() and setStyle() methods of REDText on empty REDText
	 * objects.
	 */
	public void testStyleEmptyText() {
		REDText text = getEmptyTestText();
		assertTrue("setStyle() operation has not been executed immediatly.",
				text.setStyle(0, 1, fStyle));
		assertEquals("First Gap of empty text has wrong style.",
				text.getDefaultStyle(), text.getStyle(0));
	}
	
	/**
	 * Tests the getStructure() method of REDText. Further the appliance of
	 * several different styles to one REDText object ist tested.
	 */
	public void testGetStructure() {
		REDText text = getTestText();
		int len = TEXT_CONTENT.length();
		String supResult = TEXT_CONTENT.substring(0, len / 4) + "->\n"
				+ TEXT_CONTENT.substring(len / 4, len / 2) + "->\n"
				+ TEXT_CONTENT.substring(len / 2, len / 4 * 3) + "->\n"
				+ TEXT_CONTENT.substring(len / 4 * 3, len) + "->\n" + "null";
		assertTrue("First setStyle() operation has not been executed immediatly.",
				text.setStyle(len / 4, len / 2, fStyle));
		assertTrue("Second setStyle() operation has not been executed immediatly.",
				text.setStyle(len / 4 * 3, len, fSuperStyle));
		assertEquals("The structure of the test text is wrong.", supResult,
				text.getStructure());
	}
	
	/** Test fixup of superstyles when replacing an existing style. */
	public void testSuperstyleFixup() {
		REDStyle super1 = new REDStyle(Color.yellow, Color.red, REDLining.SINGLEUNDER, "Serif", "PLAIN", 16, null);
		REDStyleManager.addStyle("Super1", super1);
		REDStyle derived = new REDStyle(null, null, null, null, null, REDStyle.INHERITED, super1);
		REDStyleManager.addStyle("Derived", derived);
		assertTrue(derived.getForeground() == Color.yellow);
		assertTrue(derived.getFontFace().equals("Serif"));
		assertTrue(derived.getFontStyle().equals("PLAIN"));
		assertTrue(derived.getFontSize() == 16);
		REDStyle super2 = new REDStyle(Color.green, Color.red, REDLining.SINGLEUNDER, "Times", "BOLD", 12, null);
		REDStyleManager.addStyle("Super1", super2);
		assertTrue(derived.getForeground() == Color.green);
		assertTrue(derived.getFontFace().equals("Times"));
		assertTrue(derived.getFontStyle().equals("BOLD"));
		assertTrue(derived.getFontSize() == 12);		
	}
	
	/** Test key -> value mappings */
	public void testMappings() {
		REDStyle style1 = new REDStyle(Color.yellow, Color.red, REDLining.SINGLEUNDER, "Serif", "PLAIN", 16, REDStyleManager.getDefaultStyle());
		REDStyle style2 = new REDStyle(Color.green, Color.red, REDLining.SINGLEUNDER, "Times", "BOLD", 12, style1);
		REDStyleManager.addStyle("Style1", style1);
		REDStyleManager.addStyle("Style2", style2);
		assertEquals(style1, REDStyleManager.getStyle("Style1"));
		assertEquals(style2, REDStyleManager.getStyle("Style2"));
		Object key1 = new Object();
		Object value1 = new Object();
		
		// Must not have anything yet.
		assertEquals(null, REDStyleManager.get("Default", key1));
		
		// Test put/get/remove for non-existing style
		assertEquals(false, REDStyleManager.put("XXXNoSuchStyleHereXXX", key1, value1, true));
		assertEquals(false, REDStyleManager.put("XXXNoSuchStyleHereXXX", key1, value1, false));
		assertEquals(null, REDStyleManager.get("XXXNoSuchStyleHereXXX", key1));
		assertEquals(false, REDStyleManager.remove("XXXNoSuchStyleHereXXX", key1, true));
		assertEquals(false, REDStyleManager.remove("XXXNoSuchStyleHereXXX", key1, false));
		
		// Test simple put / get / remove
		assertEquals(true, REDStyleManager.put("Style1", key1, value1, false));
		assertEquals(value1, REDStyleManager.get("Style1", key1));
		assertEquals(null, REDStyleManager.get("Style2", key1));
		assertEquals(true, REDStyleManager.remove("Style1", key1, false));
		assertEquals(null, REDStyleManager.get("Style1", key1));
		assertEquals(null, REDStyleManager.get("Style2", key1));

		// Test recursive put / get / remove
		assertEquals(true, REDStyleManager.put("Style1", key1, value1, true));
		assertEquals(value1, REDStyleManager.get("Style1", key1));
		assertEquals(value1, REDStyleManager.get("Style2", key1));
		assertEquals(true, REDStyleManager.remove("Style1", key1, false));
		assertEquals(null, REDStyleManager.get("Style1", key1));
		assertEquals(value1, REDStyleManager.get("Style2", key1));
		assertEquals(true, REDStyleManager.remove("Style1", key1, true));
		assertEquals(null, REDStyleManager.get("Style1", key1));
		assertEquals(null, REDStyleManager.get("Style2", key1));		
	}
	
	/** Test themes. */
	public void testThemes() {
		assertEquals("Mgr default style has wrong font.", "Monospaced", fDefaultMgrStyle.getFontFace());
		readAdditionalStyleFile("red/RTestREDTextStyles.1.xml", null);
		assertTrue(REDStyleManager.getDefaultStyle().hasTheme("Default"));
		assertTrue(REDStyleManager.getDefaultStyle().hasTheme("RTestREDTextStylesTheme"));
		assertTrue(!REDStyleManager.getStyle("Float").hasTheme("RTestREDTextStylesTheme"));
		assertEquals("Mgr default style has wrong font.", "Monospaced", fDefaultMgrStyle.getFontFace());
		REDStyleManager.setTheme("RTestREDTextStylesTheme");
		assertEquals("Mgr default style has wrong font.", "Helvetica", fDefaultMgrStyle.getFontFace());
		REDStyleManager.setTheme("Default");
		assertEquals("Mgr default style has wrong font.", "Monospaced", fDefaultMgrStyle.getFontFace());
	}
	
	public void testStyleObjectIdentity() {
		readAdditionalStyleFile("red/RTestREDTextStyles.1.xml", null);
		REDStyle before = REDStyleManager.getStyle("TestStyle1");
		assertEquals(20, before.getFontSize());
		readAdditionalStyleFile("red/RTestREDTextStyles.2.xml", null);
		REDStyle after = REDStyleManager.getStyle("TestStyle1");
		assertEquals(30, before.getFontSize());
		assertTrue(before == after);	// intentionally don't use equals here !
	}
	
	private static void readAdditionalStyleFile(String filename, File backingStore) {
		InputStream is = REDEditor.class.getClassLoader().getResourceAsStream(filename);
		assertNotNull(is);
		REDStyleManager.readStyleFile(is, filename, backingStore);
	}
	
	public void testBackingStore() {
		readAdditionalStyleFile("red/RTestREDTextStyles.1.xml", new File("MyBackingStore"));
		REDStyle s1 = REDStyleManager.getStyle("TestStyle1");
		assertEquals(new File("MyBackingStore"), s1.getBackingStore("Default"));
	}
	
	public void testChangingSuperStyle() {
		readAdditionalStyleFile("red/RTestREDTextStyles.1.xml", null);
		REDStyle s = REDStyleManager.getStyle("TestStyle2");
		assertEquals(REDStyleManager.getDefaultStyle(), s.getSuperStyle());
		readAdditionalStyleFile("red/RTestREDTextStyles.2.xml", null);
		assertEquals(REDStyleManager.getStyle("TestStyle1"), s.getSuperStyle());
	}
	
	public void testReverseLookup() {
		Iterator iter = REDStyleManager.getStyleIterator();
		while (iter.hasNext()) {
			REDStyle s = (REDStyle) iter.next();
			assertTrue(REDStyleManager.hasStyle(s.getName()));
			assertEquals(REDStyleManager.getStyle(s.getName()), s);
		}
	}
		
	public void testLogMessages() throws Exception {
		observeLog(true);
		readAdditionalStyleFile("red/RTestREDTextStyles.3.xml", null);
		assertEquals(2, getLogCount());
		observeLog(false);
	}
	
	public void testReduction() throws Exception {
		readAdditionalStyleFile("red/RTestREDTextStyles.1.xml", null);
		assertTrue(REDStyleManager.hasStyle("TestReduction"));
		REDStyle s = REDStyleManager.getStyle("TestReduction");
		assertEquals(99, s.getFontSize());
		assertEquals("XXX", s.getDescription());
		assertEquals("YYY", s.getDisplayName());		
		readAdditionalStyleFile("red/RTestREDTextStyles.4.xml", null);
		assertEquals(12, s.getFontSize());
		assertEquals("", s.getDescription());
		assertEquals("TestReduction", s.getDisplayName());		
	}		
	
	public void testIterator() throws Exception {
		String lastName = null;
		Iterator iter = REDStyleManager.iterator();
		while (iter.hasNext()) {
			REDStyle s = (REDStyle) iter.next();
			assertTrue("New name: " + s.getName() + " is not greater than last name: " + lastName, lastName == null || lastName.compareTo(s.getName()) < 0);
			lastName = s.getName();
		}
		assertNotNull("Iterator did not iterate over at least one style.", lastName);
	}
	
	public void testDisplayName() throws Exception {
		readAdditionalStyleFile("red/RTestREDTextStyles.4.xml", null);
		REDStyle s = new REDStyle(Color.yellow, Color.blue, REDLining.SINGLEUNDER, "Serif", "ITALIC", 16, null);
		REDStyleManager.addStyle("DisplayTestStyle", s);
		assertEquals("DisplayTestStyle", s.getDisplayName());
		s.setDisplayName("Test Display Style");
		assertEquals("Test Display Style", s.getDisplayName());
				
		s = REDStyleManager.getStyle("TestStyle1");
		assertEquals("TestStyle1", s.getDisplayName());
		
		s = REDStyleManager.getStyle("TestDisplayNameAndDescription");
		assertEquals("Test Description", s.getDisplayName());
	}
	
	public void testDescription() throws Exception {
		readAdditionalStyleFile("red/RTestREDTextStyles.4.xml", null);
		REDStyle s = new REDStyle(Color.yellow, Color.blue, REDLining.SINGLEUNDER, "Serif", "ITALIC", 16, null);
		REDStyleManager.addStyle("DisplayTestStyle", s);
		assertEquals("", s.getDescription());
		s.setDescription("Test Description");
		assertEquals("Test Description", s.getDescription());
				
		s = REDStyleManager.getStyle("TestStyle1");
		assertEquals("", s.getDescription());
		
		s = REDStyleManager.getStyle("TestDisplayNameAndDescription");
		assertEquals("This style is used to test the display name and description property of styles.", s.getDescription());
	}	
	
	private static void assertStyle(REDStyle s, String theme, String face, int size, String style, REDLining lining, Color fg, Color bg) {
		assertEquals(face, s.getFontFace(theme));
		assertEquals(size, s.getFontSize(theme));
		assertEquals(style, s.getFontStyle(theme));
		assertEquals(lining, s.getLining(theme));
		assertEquals(fg, s.getForeground(theme));
		assertEquals(bg, s.getBackground(theme));
	}
	
	private static void assertStyleDefines(REDStyle s, String theme, boolean face, boolean size, boolean style, boolean lining, boolean fg, boolean bg, boolean superStyle) {
		assertEquals(face, s.definesFontFace(theme));
		assertEquals(size, s.definesFontSize(theme));
		assertEquals(style, s.definesFontStyle(theme));
		assertEquals(lining, s.definesLining(theme));
		assertEquals(fg, s.definesForeground(theme));
		assertEquals(bg, s.definesBackground(theme));
		assertEquals(superStyle, s.definesSuperStyle(theme));
	}
	
	public void testThemeGetAndHas() throws Exception {
		readAdditionalStyleFile("red/RTestREDTextStyles.5.xml", null);
		REDStyle s = REDStyleManager.getStyle("ThemesTestStyle1");
		assertStyle(s, "Default", "Helvetica", 12, "plain", REDLining.NONE, Color.black, Color.white);
		assertStyle(s, "TestTheme1", "Monospaced", 10, "italic", REDLining.DOUBLEUNDER, new Color(10, 10, 10), new Color(245, 245, 245));
		assertStyle(s, "TestTheme2", "Tahoma", 8, "bolditalic", REDLining.DOUBLETHROUGH, new Color(20, 20, 20), new Color(55, 55, 55));
		assertStyleDefines(s, "Default", true, true, true, true, true, true, false);
		assertStyleDefines(s, "TestTheme1", true, true, true, true, true, true, false);
		assertStyleDefines(s, "TestTheme2", true, true, true, true, true, true, false);
		
		s = REDStyleManager.getStyle("ThemesTestStyle2");
		assertStyle(s, "Default", "Arial", 12, "plain", REDLining.NONE, Color.black, Color.white);
		assertStyle(s, "TestTheme1", "Helvetica", 10, "italic", REDLining.DOUBLEUNDER, new Color(10, 10, 10), new Color(245, 245, 245));
		assertStyle(s, "TestTheme2", "Monospaced", 18, "plain", REDLining.NONE, Color.black, Color.white);
		assertStyleDefines(s, "Default", true, false, false, false, false, false, true);
		assertStyleDefines(s, "TestTheme1", true, false, false, false, false, false, true);
		assertStyleDefines(s, "TestTheme2", false, true, false, false, false, false, true);
	}
	
	private static int nrRegisteredStyles(REDStyleManagerImpl m) {
		int x = 0;
		Iterator iter = m.doIterator();
		while (iter.hasNext()) {
			x++; iter.next();
		}
		return x;
	}
	
	private static void assertManagerHasNot(REDStyleManagerImpl m, REDStyle s) {
		Iterator iter = m.doIterator();
		while (iter.hasNext()) {
			assertTrue(iter.next() != s);
		}
	}
	
	public void testManagerDeepCopy() throws Exception {
		readAdditionalStyleFile("red/RTestREDTextStyles.5.xml", null);
		REDStyleManagerImpl src = REDStyleManager.getInstance();
		src.doPut("Default", "testMapping", this, false);
		REDStyleManagerImpl cp = src.deepCopy();
		REDStyle s;
		
		assertEquals(nrRegisteredStyles(src), nrRegisteredStyles(cp));
		
		// Check that no object from src is in cp
		Iterator iter = src.doIterator();
		while (iter.hasNext()) {
			s = (REDStyle) iter.next();
			assertManagerHasNot(cp, s);
		}
		
		// Check that no object from cp is in src
		iter = cp.doIterator();
		while (iter.hasNext()) {
			s = (REDStyle) iter.next();
			assertManagerHasNot(src, s);
		}
		
		// Mappings are not copied and independent
		assertNotNull(src.doGet("Default", "testMapping"));
		assertNull(cp.doGet("Default", "testMapping"));
		src.doRemove("Default", "testMapping", false);
		assertNull(src.doGet("Default", "testMapping"));
		cp.doPut("Default", "testMapping", this, false);
		assertNotNull(cp.doGet("Default", "testMapping"));
		assertNull(src.doGet("Default", "testMapping"));
		src.doPut("Default", "testMapping", this, false);
		assertNotNull(src.doGet("Default", "testMapping"));
		assertNotNull(cp.doGet("Default", "testMapping"));
		cp.doRemove("Default", "testMapping", false);
		assertNotNull(src.doGet("Default", "testMapping"));
		assertNull(cp.doGet("Default", "testMapping"));
		src.doRemove("Default", "testMapping", false);
		assertNull(src.doGet("Default", "testMapping"));
		assertNull(cp.doGet("Default", "testMapping"));
		
		// Check name identity		
		iter = src.doIterator();
		Iterator iter2 = cp.doIterator();
		while (iter.hasNext()) {
			REDStyle s1 = (REDStyle) iter.next();
			REDStyle s2 = (REDStyle) iter2.next();
			assertEquals(s1.getName(), s2.getName());
		}
		
		// If we change a style / theme in src it won't affect cp
		s = src.doGetStyle("ThemesTestStyle1");
		assertEquals("Helvetica", s.getFontFace());
		assertTrue(s.setFontFace("Default", "Tahoma"));
		assertEquals("Tahoma", s.getFontFace());
		s = cp.doGetStyle("ThemesTestStyle1");
		assertEquals("Helvetica", s.getFontFace());
		assertTrue(src.doGetStyle("ThemesTestStyle1").getFont() != cp.doGetStyle("ThemesTestStyle1").getFont());
		
		// If we change a style / theme in cp it won't affect src
		s = cp.doGetStyle("ThemesTestStyle1");
		assertEquals("Helvetica", s.getFontFace());
		assertTrue(s.setFontFace("Default", "Arial"));
		assertEquals("Arial", s.getFontFace());
		s = src.doGetStyle("ThemesTestStyle1");
		assertEquals("Tahoma", s.getFontFace());
		assertTrue(src.doGetStyle("ThemesTestStyle1").getFont() != cp.doGetStyle("ThemesTestStyle1").getFont());
		
		// If we add a style in src it won't be there in cp
		src.doAddStyle("testManagerDeepCopyStyle1", new REDStyle(Color.green, Color.blue, REDLining.NONE, "Tahoma", "plain", 12, src.doGetDefaultStyle()));
		assertTrue(!cp.doHasStyle("testManagerDeepCopyStyle1"));
		
		// If we add a style in cp it won't be there in src
		cp.doAddStyle("testManagerDeepCopyStyle2", new REDStyle(Color.green, Color.blue, REDLining.NONE, "Tahoma", "plain", 12, cp.doGetDefaultStyle()));
		assertTrue(!src.doHasStyle("testManagerDeepCopyStyle2"));
		
		// No superstyle of cp is in src
		iter = cp.doIterator();
		while (iter.hasNext()) {
			s = (REDStyle) iter.next();
			assertManagerHasNot(src, s.getSuperStyle());
		}		
	}
	
	// Listener tests start	
	static void checkEvents(String expLog, RTestLogProxy proxy) {
		assertEquals(expLog, String.valueOf(proxy));
	}

	static class Listener implements REDStyleEventListener {
		public void beforeStyleChange(REDStyle [] style) { } 
		public void afterStyleChange(REDStyle [] style) { } 
		public void beforeThemeChange(String oldTheme, String newTheme) { }
		public void afterThemeChange(String oldTheme, String newTheme) { }
	}
	
	public void testListener() {
		readAdditionalStyleFile("red/RTestREDTextStyles.1.xml", new File("MyBackingStore"));
		Listener a = new Listener();
		RTestLogProxy proxy = new RTestLogProxy(a);
		proxy.addLogClass(REDStyleEventListener.class);
		REDStyleEventListener listener = (REDStyleEventListener) RTestLogProxy.newInstance(a, proxy);
		assertTrue(REDStyleManager.addStyleEventListener(listener));
		REDStyle registered = new REDStyle(null, null, null, null, null, REDStyle.INHERITED, REDStyleManager.getDefaultStyle());
		REDStyleManager.addStyle("RegisteredTestListenerStyle", registered);
		String arr = "[" + registered + ']';
		REDStyle unRegistered = new REDStyle(null, null, null, null, null, REDStyle.INHERITED, REDStyleManager.getDefaultStyle());

		// check for each method of changing style		
		checkEvents("", proxy);
		registered.setFontFace("Default", "Monospaced");
		checkEvents("beforeStyleChange(" + arr + ")\nafterStyleChange([" + registered + "])", proxy); proxy.clear(); arr = "[" + registered + ']';
		registered.setFontSize("Default", 12);
		checkEvents("beforeStyleChange(" + arr + ")\nafterStyleChange([" + registered + "])", proxy); proxy.clear(); arr = "[" + registered + ']';
		registered.setFontStyle("Default", "plain");
		checkEvents("beforeStyleChange(" + arr + ")\nafterStyleChange([" + registered + "])", proxy); proxy.clear(); arr = "[" + registered + ']';
		registered.setLining("Default", REDLining.SINGLEUNDER);
		checkEvents("beforeStyleChange(" + arr + ")\nafterStyleChange([" + registered + "])", proxy); proxy.clear(); arr = "[" + registered + ']';
		registered.setForeground("Default", 12, 13, 14);
		checkEvents("beforeStyleChange(" + arr + ")\nafterStyleChange([" + registered + "])", proxy); proxy.clear(); arr = "[" + registered + ']';
		registered.setBackground("Default", 15, 16, 17);
		checkEvents("beforeStyleChange(" + arr + ")\nafterStyleChange([" + registered + "])", proxy); proxy.clear(); arr = "[" + registered + ']';
		registered.setSuper("Default", "Literal");
		checkEvents("beforeStyleChange(" + arr + ")\nafterStyleChange([" + registered + "])", proxy); proxy.clear();

		// unregistered style must not cause event
		unRegistered.setFontFace("Default", "Monospaced");
		unRegistered.setFontSize("Default", 12);
		unRegistered.setFontStyle("Default", "plain");
		unRegistered.setLining("Default", REDLining.SINGLEUNDER);
		unRegistered.setForeground("Default", 12, 13, 14);
		unRegistered.setBackground("Default", 15, 16, 17);
		unRegistered.setSuper("Default", "Literal");
		checkEvents("", proxy);
		
		REDStyleManager.setTheme("RTestREDTextStylesTheme");
		REDStyleManager.setTheme("Default");
		checkEvents("beforeThemeChange(Default, RTestREDTextStylesTheme)\nafterThemeChange(Default, RTestREDTextStylesTheme)\n" +
			"beforeThemeChange(RTestREDTextStylesTheme, Default)\nafterThemeChange(RTestREDTextStylesTheme, Default)", proxy); proxy.clear();

		// After listener has been removed nothing may be reported anymore		
		assertTrue(REDStyleManager.removeStyleEventListener(listener));
		registered.setFontFace("Default", null);
		registered.setFontSize("Default", REDStyle.INHERITED);
		registered.setFontStyle("Default", null);
		registered.setLining("Default", null);
		registered.setForeground("Default", null);
		registered.setBackground("Default", null);
		registered.setSuper("Default", "Default");
		REDStyleManager.setTheme("RTestREDTextStylesTheme");
		REDStyleManager.setTheme("Default");
		checkEvents("", proxy);
	}
}
