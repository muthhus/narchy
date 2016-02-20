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
 
package red.file;

import java.io.*; 

/** File cache object used by REDFile and REDFileRider.
  * @author rli@chello.at
  * @see REDFile
  * @see REDFileRider 
  * @invariant fSize <= fcBufSize 
  * @invariant fSize >= 0 
  * @invariant fOrg <= fFile.fLength
  * @tier system
  */
class REDFileBuffer {
		final static public int fcBufSize = 4096;
	
		public REDFileBuffer(REDFile f) {
			fFile = f;
		}			
			
		/** write back modified contents if necessary 
		  * @post !fDirty
		  */
		void flush() {
			if (fDirty) {
				try {
					fFile.fFile.seek(fOrg);
					fFile.fFile.write(fData, 0, fSize);
					fDirty = false;
				}
				catch (IOException e) {
					throw new Error("Internal error in REDFileBuffer.flush: " + e);
				}
			}
		}
		
		/** 
		  * @pre org % fcBufSize == 0
		  */
		void fill(int org) {
			try {
				fOrg = org;
				fFile.fFile.seek(fOrg);
				fSize = Math.max(0, fFile.fFile.read(fData, 0, fcBufSize));
				fDirty = false;
			}
			catch (Exception e) {
				throw new Error("Internal error in REDFileBuffer.flush");
			}
		}
		
		REDFile fFile;
		boolean fDirty;
		int fOrg, fSize;
		byte fData[];
		{
			fFile = null;
			fDirty = false;
			fOrg = -1;
			fSize = 0;
			fData = new byte[fcBufSize];
		}
	}
