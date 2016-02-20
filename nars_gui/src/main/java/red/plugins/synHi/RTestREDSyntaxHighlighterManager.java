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

/** Regression test for REDSyntaxHighlighterManager
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDSyntaxHighlighterManager extends TestCase {
	public RTestREDSyntaxHighlighterManager(String name) {
		super(name);
	}
	
	public void testDefinitionManagement() {
		assertEquals(null, REDSyntaxHighlighterManager.createHighlighter("maynotexist"));
		REDSyntaxHighlighterDefinition def1 = new REDSyntaxHighlighterDefinition("foo");
		REDSyntaxHighlighterManager.addDefinition(def1);
		assertNotNull(REDSyntaxHighlighterManager.createHighlighter("foo"));
	}
	
	public void testHasHighlighter() {
		assertEquals(true, REDSyntaxHighlighterManager.hasHighlighter("C++"));
		assertEquals(true, REDSyntaxHighlighterManager.hasHighlighter("Java"));
		assertEquals(false, REDSyntaxHighlighterManager.hasHighlighter("Pterodactylus--"));
	}
	
		
	public static Test suite() {
		return new TestSuite(RTestREDSyntaxHighlighterManager.class);
	}
}
