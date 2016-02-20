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
 
package red.plugins.synHi;

import junit.framework.*;
import red.*;
import java.util.*;
import java.awt.*;

/** Regression test for REDSyntaxHighlighterDefinition
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDSyntaxHighlighterDefinition extends TestCase {
	public RTestREDSyntaxHighlighterDefinition(String name) {
		super(name);
	}
	
	public void testName() {
		REDSyntaxHighlighterDefinition def1 = new REDSyntaxHighlighterDefinition("foo");
		assertEquals("foo", def1.getName());
	}
	
	public void testRuleManagement() {
		REDSyntaxHighlighterDefinition def1 = new REDSyntaxHighlighterDefinition("foo");
		Iterator iter = def1.iterator();
		
		// empty def => iterator empty
		assertEquals(false, iter.hasNext());
		
		// add one def
		REDSyntaxHighlighterKeyword k1 = new REDSyntaxHighlighterKeyword("", fStyle); def1.addRule(k1);
		iter = def1.iterator();
		assertEquals(true, iter.hasNext());
		assertEquals(k1, iter.next());		
		assertEquals(false, iter.hasNext());
		
		// add more defs
		REDSyntaxHighlighterKeyword k2 = new REDSyntaxHighlighterKeyword("", fStyle); def1.addRule(k2);
		REDSyntaxHighlighterKeyword k3 = new REDSyntaxHighlighterKeyword("", fStyle); def1.addRule(k3);
		iter = def1.iterator();
		assertEquals(true, iter.hasNext());
		assertEquals(k1, iter.next());		
		assertEquals(true, iter.hasNext());
		assertEquals(k2, iter.next());		
		assertEquals(true, iter.hasNext());
		assertEquals(k3, iter.next());		
		assertEquals(false, iter.hasNext());
	}			
	
	public void testIgnorePattern() {
		REDSyntaxHighlighterDefinition def = new REDSyntaxHighlighterDefinition("foo");
		assertEquals("[a-zA-Z0-9_]+", def.getIgnorePattern());
		def.setIgnorePattern("[a-zA-Z0-9_-]+");
		assertEquals("[a-zA-Z0-9_-]+", def.getIgnorePattern());		
	}
	
	public void testRewind() {
		REDSyntaxHighlighterRange range = new REDSyntaxHighlighterRange("foo", "moo", fStyle);
		assertEquals(false, range.getRewind());
		range.setRewind(true);
		assertEquals(true, range.getRewind());
	}
	
	public void testCaseSensitive() {
		REDSyntaxHighlighterDefinition def = new REDSyntaxHighlighterDefinition("foo");
		assertEquals(true, def.getCaseSensitive());
		def.setCaseSensitive(false);
		assertEquals(false, def.getCaseSensitive());
	}
			
	public static Test suite() {
		return new TestSuite(RTestREDSyntaxHighlighterDefinition.class);
	}
	
	REDStyle fStyle = new REDStyle(new Color(250, 100, 100), new Color(255, 255, 0), REDLining.SINGLEUNDER, "Monospaced", "ITALIC", 24, null);
}
