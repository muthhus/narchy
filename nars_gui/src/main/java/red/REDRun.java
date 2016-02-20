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

import red.file.*;

// TBD: Refine exception handling

/**
  * @author rli@chello.at
  * @tier system
  * @invariant fLength > 0
  * @invariant fBuffer != null
  */
final class REDRun {

	/**
	 * @pre buffer != null
	 * @pre style != null
	 * @pre length > 0
	*/
	public REDRun(REDFileRider buffer, int org, int length, REDStyle style)  {
		fBuffer = buffer;
		fOrg = org;
		fLength = length;
		fStyle = style;
	}
	
	/**
	 * @pre buffer != null
	 * @pre style != null
	 * @pre str.length() > 0
	*/
	public REDRun(REDFileRider buffer, String str, REDStyle style) {
		fLength = str.length();
		fBuffer = buffer;
		fStyle = style;
		fOrg = fBuffer.getFile().length();
		fBuffer.seek(fOrg);
		fBuffer.writeBytes(str.getBytes(), str.length());
	}
	
	/**
	 * @post return.length() == length()
	 */
	public String asString() {
		String retVal;
		byte [] buf = new byte[fLength];
		fBuffer.seek(fOrg);
		fBuffer.readBytes(buf, fLength);
		retVal = new String(buf);
		return retVal;
	}
	
	public byte charAt(int pos) {
		fBuffer.seek(fOrg + pos);
		return fBuffer.read();
	}
		
	
	/** copy parts of run into char-array 
	  * @param arr array to copy into 
	  * @param from offset of arr to copy bytes to
	  * @param arrSize max offset of arr to write into
	  * @param myOff offset of run to start reading at
	  * @return the number of bytes read
	  */
	public int copyInto(byte[] arr, int from, int arrSize, int myOff) {
		int retVal = 0;
		fBuffer.seek(fOrg + myOff);
		int readAmount = Math.min(arrSize - from, fLength - myOff);
		fBuffer.readBytes(arr, from, readAmount);
		return readAmount - fBuffer.getRes();
	}
	
	/** tbd make more efficient */
	public void copyInto(REDFileRider dest) {
		byte [] buf = new byte[fLength];
		fBuffer.seek(fOrg);
		fBuffer.readBytes(buf, fLength);
		dest.writeBytes(buf, fLength);
	}
	
	public int length() {
		return fLength;
	}
	
	/** @pre pos < fLength */
	public byte getCharAt(int pos) {
		fBuffer.seek(fOrg + pos);
		return fBuffer.read();
	}
	
	int findWhitespace(int pos, boolean alsoSpaces) {
		fBuffer.seek(fOrg + pos);
		byte c = fBuffer.read();
		while (pos < fLength && c != '\t' && c != '\n' && c != '\r' && (c != ' ' || !alsoSpaces)) {
			pos++;
			c = fBuffer.read();
		}
		return pos;
	}
	
	/** find position of next line start
	  * @return the start position of next line, relative to pos
	  * @param pos Position to start search at
	  * @pre pos >= 0
	  * @pre pos < fLength
	  */
	int findNextLine(int pos) {
		fBuffer.seek(fOrg + pos);
		byte c;
		do {
			c = fBuffer.read(); pos++;
		}
		while (pos < fLength && c != '\r' && c != '\n');
		if (c == '\r') {
			if (pos < fLength && fBuffer.read() == '\n' || fNext != null && fNext.getCharAt(0) == '\n') {
				pos++;
			}
		}
		else if (c == '\n') {
		}
		else if (fNext != null) {
			pos += fNext.findNextLine(0);
		}			
		return pos;
	}
	
	/**
	 * @pre r != null
	 */
	public boolean isMergeableWith(REDRun r) {
		return r.fBuffer == fBuffer && r.fOrg == fOrg + fLength && r.fStyle == fStyle;
	}
	
	protected REDFileRider fBuffer;
	protected int fOrg;
	protected int fLength;
	protected REDStyle fStyle;
	protected REDRun fNext;
	protected REDRun fPrev;
}

	
