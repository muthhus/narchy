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

import java.util.*;

/** Auxiliary class for maintaining a small line height cache. 
  * @author rli@chello.at
  * @tier system
  */
class REDViewLineHeightCache {
	static final int fgCacheSize = 5;
	static final int fcInvalid = -1;

	static class Entry {
		Entry(int line, int value) {
			fLine = line;
			fValue = value;
		}
		int fLine, fValue;
	}
	
	REDViewLineHeightCache() {
		fCache = new LinkedList();
	}

	/** Fill line height cache. If there is not enough space in the cache to accomodate the new entry, the least recently used element will be kicked out.
	  * @param line The line to put into cache.
	  * @param height The height of the line to put into the cache.
	  */
	public void fillCache(int line, int height) {
		fCache.addFirst(new Entry(line, height));
		if (fCache.size() > fgCacheSize) {
			fCache.removeLast();
		}
	}
	
	/** Get line height from cache.
	  * @param line The line to get height for.
	  * @return The height of the line in cache, or <CODE>fcInvalid</CODE> if line is not in cache (cache miss).
	  */
	public int getHeight(int line) {
		ListIterator iterator = fCache.listIterator(0);
		Entry e = null;
		int retVal = fcInvalid;
		while (iterator.hasNext() && retVal == fcInvalid) {
			e = (Entry) iterator.next();
			if (e.fLine == line) {
				iterator.remove();
				fCache.addFirst(e);
				retVal = e.fValue;
			}
		}		
		return retVal;
	}
	
	/** Invalidate single line in cache. 
	  * @param line The line to kick out of the cache.
	  */
	public void invalidateLine(int line) {
		ListIterator iterator = fCache.listIterator(0);
		while (iterator.hasNext()) {
			Entry e = (Entry) iterator.next();
			if (e.fLine == line) {
				iterator.remove();
			}
		}			
	}
	
	/** Invalidate all lines from a certain line on.
	  * @param fromLine The line to kick out all lines from.
	  */
	public void invalidateLinesFrom(int fromLine) {
		ListIterator iterator = fCache.listIterator(0);
		while (iterator.hasNext()) {
			Entry e = (Entry) iterator.next();
			if (e.fLine >= fromLine) {
				iterator.remove();
			}
		}			
	}

	private final LinkedList fCache;
}
