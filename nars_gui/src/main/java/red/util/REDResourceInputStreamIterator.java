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
 
package red.util;

import java.util.*;
import java.io.*;

/** Resource input stream iterator.
  * @author rli@chello.at
  * @tier system
  */
public class REDResourceInputStreamIterator implements Iterator {
	REDResourceInputStreamIterator(ArrayList list) {
		fIter = list.iterator();
	}
	
	public boolean hasNext() {
		return fIter.hasNext();
	}
	
	public Object next() {
		fObj = fIter.next();
		if (fObj instanceof File) {
			try {
				return new FileInputStream((File) fObj);
			}
			catch (IOException ioe) {
				REDTracer.error("red.util", "REDResourceManager", "IO Exception while trying to access: " + fObj + ": " + ioe);
				return null;
			}
		}
		else {
			REDResourceJarEntry r = (REDResourceJarEntry) fObj;
			try {
				fObj = r.fEntry;
				return r.fFile.getInputStream(r.fEntry);
			}
			catch (IOException ioe) {
				REDTracer.error("red.util", "REDResourceManager", "IO Exception while trying to access: " + r.fEntry + " in " + r.fFile + ": "+  ioe);
				return null;
			}
		}
	}
	
	public Object curName() {
		return fObj;
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	Iterator fIter;
	Object fObj;
}
