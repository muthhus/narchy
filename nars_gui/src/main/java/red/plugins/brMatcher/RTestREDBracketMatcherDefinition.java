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
 
package red.plugins.brMatcher;

import java.io.*;
import java.util.*;
import junit.framework.*;
import red.*;
import red.util.*;

/** Regression test for REDBracketMatcherDefinition
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDBracketMatcherDefinition extends RTestLogObserver {
	public RTestREDBracketMatcherDefinition(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
	}
	
	private static String flattenPatterns(REDBracketMatcherDefinition def) {
		StringBuilder buf = new StringBuilder();
		Iterator iter = def.patternIterator();
		while (iter.hasNext()) {
			REDBracketMatcherDefinitionPattern pat = (REDBracketMatcherDefinitionPattern) iter.next();
			buf.append(pat.fLeft);
			buf.append(pat.fRight);
		}			
		return String.valueOf(buf);
	}
	
	private static REDBracketMatcherDefinitionStyleSet getStyleSet(REDBracketMatcherDefinition def, int patNr) {
		return ((REDBracketMatcherDefinitionPattern) def.fPatterns.get(patNr)).fStyleSet;
	}
	
	private static REDBracketMatcherDefinitionRuleSet getRuleSet(REDBracketMatcherDefinition def, int patNr) {
		return ((REDBracketMatcherDefinitionPattern) def.fPatterns.get(patNr)).fRuleSet;
	}
		
	public void testDefinitionFields() throws Exception {
		readAdditionalMatcherDefs("ok.xml");
		REDBracketMatcherDefinition def1 = REDBracketMatcherManager.getMatcherDefinition("Test1");
		REDBracketMatcherDefinition def2 = REDBracketMatcherManager.getMatcherDefinition("Test2");
		assertEquals("Test1", def1.getName());
		assertEquals("Test2", def2.getName());
		assertEquals("()[]{}<>/**/\"\"", flattenPatterns(def1));
		assertEquals("|-", flattenPatterns(def2));
		assertEquals(REDStyleManager.getStyle("KeywordControl"), def1.getStyle());
		assertEquals(REDStyleManager.getStyle("BracketMatcher"), def2.getStyle());
		assertEquals(3, def1.getMaxLines());
		assertEquals(99, def2.getMaxLines());
		assertEquals(28, def1.getMaxChars());
		assertEquals(499, def2.getMaxChars());	
		assertEquals(true, getRuleSet(def1, 0).getBeforeLeft());
		assertEquals(true, getRuleSet(def1, 0).getAfterLeft());
		assertEquals(true, getRuleSet(def1, 0).getBeforeRight());
		assertEquals(true, getRuleSet(def1, 0).getAfterRight());
		assertEquals(false, getRuleSet(def2, 0).getBeforeLeft());
		assertEquals(false, getRuleSet(def2, 0).getAfterLeft());
		assertEquals(false, getRuleSet(def2, 0).getBeforeRight());
		assertEquals(false, getRuleSet(def2, 0).getAfterRight());
		assertEquals(true, getRuleSet(def1, 0).getDoubleClickSelect());
		assertEquals(false, getRuleSet(def2, 0).getDoubleClickSelect());
		assertEquals(3, getStyleSet(def1, 0).getNrIgnoreStyles());
		assertEquals(0, getStyleSet(def2, 0).getNrIgnoreStyles());

		HashSet targetSet = new HashSet(); 
		targetSet.add("Comment"); 
		targetSet.add("String");
		targetSet.add("Literal");
		Iterator iter = getStyleSet(def1, 0).getIgnoreStylesIterator();
		while (iter.hasNext()) {
			assertTrue(targetSet.remove(iter.next()));
		}
		assertEquals(0, targetSet.size());				
		
		iter = getStyleSet(def2, 0).getIgnoreStylesIterator();
		assertTrue(iter.hasNext() == false);
	}
	
	public void testExtremeCases() {
		observeLog(true);
		readAdditionalMatcherDefs("RTestREDBracketMatcher.1.tilt.xml");
		REDBracketMatcherDefinition def1 = REDBracketMatcherManager.getMatcherDefinition("Tilt1");
		assertNotNull(def1);
		assertEquals(5, getLogCount());
		resetLog();
		
		// Tilt2 has two pattern-related problems and is thus not added to the definitions, which makes for a third warning
		readAdditionalMatcherDefs("RTestREDBracketMatcher.2.tilt.xml");
		assertNull(REDBracketMatcherManager.getMatcherDefinition("Tilt2"));
		assertEquals(3, getLogCount());
		resetLog();

		// Tilt3 has an empty pattern and is thus not added to the definitions, which makes for the second warning
		readAdditionalMatcherDefs("RTestREDBracketMatcher.3.tilt.xml");
		assertNull(REDBracketMatcherManager.getMatcherDefinition("Tilt3"));
		assertEquals(2, getLogCount());
		observeLog(false);
	}
	
	private static void readAdditionalMatcherDefs(String pattern) {
		REDResourceInputStreamIterator iter = REDResourceManager.getInputStreams("red/plugins/brMatcher", pattern);
		while (iter.hasNext()) {
			InputStream is = (InputStream) iter.next();
			REDBracketMatcherManager.readMatcherDefinition(is, String.valueOf(iter.curName()));
		}
	}
	
	public static Test suite() {
		return new TestSuite(RTestREDBracketMatcherDefinition.class);
	}
}
