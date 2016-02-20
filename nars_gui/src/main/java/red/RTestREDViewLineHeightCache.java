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

/** Regression test for REDViewLineHeightCache.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDViewLineHeightCache extends TestCase {
	public RTestREDViewLineHeightCache(String name) {
		super(name);
	}
	
	protected void setUp() {
		fCache = new REDViewLineHeightCache();
	}		
	
	protected void tearDown() {
	}
	
	public void testCache() {
		assertTrue(REDViewLineHeightCache.fgCacheSize >= 5);
		fCache.fillCache(1, 10);
		assertEquals(10, fCache.getHeight(1));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(0));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(2));
		fCache.fillCache(50, 12);
		assertEquals(10, fCache.getHeight(1));
		assertEquals(12, fCache.getHeight(50));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(0));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(2));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(40));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(60));
		fCache.fillCache(0, 16);
		assertEquals(10, fCache.getHeight(1));
		assertEquals(12, fCache.getHeight(50));
		assertEquals(16, fCache.getHeight(0));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(2));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(40));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(60));
	}
	
	public void testInvalidIndices() {
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(-1));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(-10));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(-100000));		
	}
	
	public void testLRUEviction() {
		for (int x = 0; x < REDViewLineHeightCache.fgCacheSize; x++) {
			fCache.fillCache(x, 10);
		}
		fCache.fillCache(100, 10);
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(0));
		for (int x = 1; x < REDViewLineHeightCache.fgCacheSize; x++) {
			assertEquals(10, fCache.getHeight(x));
		}
	}
	
	public void testInvalidateSingleLine() {
		assertTrue(REDViewLineHeightCache.fgCacheSize >= 5);
		for (int x = 0; x < 5; x++) {
			fCache.fillCache(x, 10);
		}
		fCache.invalidateLine(2);
		assertEquals(10, fCache.getHeight(0));
		assertEquals(10, fCache.getHeight(1));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(2));
		assertEquals(10, fCache.getHeight(3));
		assertEquals(10, fCache.getHeight(4));
		
		fCache.invalidateLine(4);
		assertEquals(10, fCache.getHeight(0));
		assertEquals(10, fCache.getHeight(1));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(2));
		assertEquals(10, fCache.getHeight(3));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(4));
		
		// invalidate what is not in cache
		fCache.invalidateLine(4);
		fCache.invalidateLine(30);
		assertEquals(10, fCache.getHeight(0));
		assertEquals(10, fCache.getHeight(1));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(2));
		assertEquals(10, fCache.getHeight(3));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(4));

		fCache.invalidateLine(1);		
		assertEquals(10, fCache.getHeight(0));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(1));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(2));
		assertEquals(10, fCache.getHeight(3));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(4));
		
		// assure evicted places are now available again
		fCache.fillCache(50, 10);
		assertEquals(10, fCache.getHeight(0));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(1));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(2));
		assertEquals(10, fCache.getHeight(3));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(4));
		assertEquals(10, fCache.getHeight(50));
	}
	
	public void testInvalidateMultipleLines() {
		assertTrue(REDViewLineHeightCache.fgCacheSize >= 5);
		for (int x = 0; x < 5; x++) {
			fCache.fillCache(x, 10);
		}
		
		fCache.invalidateLinesFrom(2);
		assertEquals(10, fCache.getHeight(0));
		assertEquals(10, fCache.getHeight(1));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(2));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(3));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(4));
		
		fCache.fillCache(50, 10);
		fCache.fillCache(40, 10);
		fCache.invalidateLinesFrom(15);
		assertEquals(10, fCache.getHeight(0));
		assertEquals(10, fCache.getHeight(1));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(2));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(3));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(4));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(40));
		assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(50));
	}
	
	public void testEmpty() {
		for (int x = 0; x < 100; x++) {
			assertEquals(REDViewLineHeightCache.fcInvalid, fCache.getHeight(x));
		}
	}

	public static Test suite() {
		return new TestSuite(RTestREDViewLineHeightCache.class);
	}
	
	REDViewLineHeightCache fCache;
}
