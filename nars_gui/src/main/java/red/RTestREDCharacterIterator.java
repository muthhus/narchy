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
import java.text.*;

/** Tests for REDCharacterIterator
  * @author rli@chello.at 
  * @tier test
  */
public class RTestREDCharacterIterator extends TestCase {
	public RTestREDCharacterIterator(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(RTestREDCharacterIterator.class);
	}
			
	public void setUp() throws Exception {
		fEditor = new REDEditor();
		fEditor.replace("A Troll sat alone on his seat of stone.", 0, 0, null);
		fIter = fEditor.createCharacterIterator();
		super.setUp();
	}	
	
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testIndex() {
		assertEquals('T', fIter.setIndex(2));
		assertEquals('A', fIter.setIndex(0));
		assertEquals('.', fIter.setIndex(fEditor.length()-1));
		assertEquals(CharacterIterator.DONE, fIter.setIndex(-1));
		assertEquals(CharacterIterator.DONE, fIter.setIndex(-10));
		assertEquals(0, fIter.getIndex());
		assertEquals(' ', fIter.next());
		assertEquals(CharacterIterator.DONE, fIter.setIndex(fEditor.length()));
		assertEquals(CharacterIterator.DONE, fIter.setIndex(fEditor.length() + 3));
		assertEquals(fEditor.length(), fIter.getIndex());
		assertEquals('.', fIter.previous());
	}
	
	public void testClone() {
		fIter.setIndex(5);
		REDCharacterIterator iter2 = (REDCharacterIterator) fIter.clone();
		assertEquals(fIter.getIndex(), iter2.getIndex());
		assertEquals(fIter.next(), iter2.next());
	}
	
	public void testMisc() {
		assertEquals('A', fIter.first());
		assertEquals('.', fIter.last());
		assertEquals('.', fIter.current());
		assertEquals('e', fIter.previous());
		assertEquals('e', fIter.current());
		assertEquals(0, fIter.getBeginIndex());
		assertEquals(fEditor.length(), fIter.getEndIndex());
	}
	
	public void testTraverse() {
		StringBuilder buf = new StringBuilder();
		for(char c = fIter.first(); c != CharacterIterator.DONE; c = fIter.next()) {
			buf.append(c);
		}		
		assertEquals("A Troll sat alone on his seat of stone.", String.valueOf(buf));
	}
	
	public void testTraverseReverse() {
		StringBuilder buf = new StringBuilder();
		for(char c = fIter.last(); c != CharacterIterator.DONE; c = fIter.previous()) {
			buf.append(c);
		}		
		assertEquals(".enots fo taes sih no enola tas llorT A", String.valueOf(buf));
	}
	
	
	REDEditor fEditor;
	CharacterIterator fIter;
}
