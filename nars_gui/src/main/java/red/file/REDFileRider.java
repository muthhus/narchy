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

/** Accessor to REDFiles.
  * @author rli@chello.at
  * @invariant fBuffer != null
  * @invariant fOffset <= REDFileBuffer.fcBufSize
  * @tier system
  */
public class REDFileRider {
	/** @pre f != null */
	public REDFileRider(REDFile f) {
		set(f, 0);
	}
	
	private REDFileRider() {
	}
	
	/** Set rider to position
	  * @param pos is normalized to be in range [0, f.length()]
	  */
	public void seek(int pos) {
		set(getFile(), pos);
	}
	
	/** Set rider to file and position
	  * @param pos is normalized to be in range [0, f.length()]
	  * @param f the file the rider should operate on
	  * @pre f != null
	  * @post fBuffer != null
	  */
	public void set (REDFile f, int pos) {
		int i;
		
		// normalize pos
		if (pos > f.fLength) {
			pos = f.fLength;
		}
		else if (pos < 0) {
			pos = 0;
		}
		
		fOffset = pos % REDFileBuffer.fcBufSize; fOrg = pos - fOffset; i = 0;
		while (i < REDFile.fcNrBufs && f.fBuffer[i] != null && fOrg != f.fBuffer[i].fOrg) {
			i++;
		}
		if (i < REDFile.fcNrBufs) {
			if (f.fBuffer[i] == null) {
				fBuffer = new REDFileBuffer(f); f.fBuffer[i] = fBuffer;
			}
			else {
				fBuffer = f.fBuffer[i];
			}
		}
		else {
			f.fSwapper = (f.fSwapper + 1) % REDFile.fcNrBufs;
			fBuffer = f.fBuffer[f.fSwapper];
			fBuffer.flush();
		}
		
		if (fBuffer.fOrg != fOrg) {
			fBuffer.fill(fOrg);
		}
		fEof = false; fRes = 0;
	}

	/** read a byte from the file 
	  * @post fEof implies return == 0
	  */
	public byte read() {
		byte x;
		if (fOrg != fBuffer.fOrg) {
			set(fBuffer.fFile, fOrg + fOffset);
		}
		if (fOffset < fBuffer.fSize) {
			x = fBuffer.fData[fOffset]; fOffset++;
		}
		else if (fOrg + fOffset < fBuffer.fFile.fLength) {
			set(fBuffer.fFile, fOrg + fOffset);
			x = fBuffer.fData[0]; fOffset = 1;
		}
		else {
			x = 0;
			fEof = true;
		}
		fRes = 0;
		return x;
	}


	/** read up to x.length bytes into byte array
	  * this.fRes will contain the number of bytes read
	  * @param x array to copy into
	  * @post fRes > 0 implies fEof
	  */
	public void readBytes(byte[] x) {
		readBytes(x, 0, x.length);
	}

	/** read up to n bytes into byte array
	  * this.fRes will contain the number of bytes read
	  * @param x array to copy into
	  * @param n number of bytes to copy into x
	  * @post fRes > 0 implies fEof
	  * @pre x.length >= n
	  */
	public void readBytes(byte[] x, int n) {
		readBytes(x, 0, n);
	}
	
	/** read up to n bytes into byte array
	  * this.fRes will contain the number of bytes read
	  * @param x array to copy into
	  * @param off array-offset to begin copying at
	  * @param n number of bytes to copy into x
	  * @post fRes > 0 implies fEof
	  * @pre x.length >= n + off
	  */
	public void readBytes(byte[] x, int off, int n) {
		int min, restInBuf;
		while (n > 0) {
			if (fOrg != fBuffer.fOrg || fOffset >= REDFileBuffer.fcBufSize) {
				set(fBuffer.fFile, fOrg + fOffset);
			}
			restInBuf = fBuffer.fSize - fOffset;
			if (restInBuf == 0) {
				fRes = n; fEof = true; return;
			}
			else if (n > restInBuf) {
				min = restInBuf;
			}
			else {
				min = n;
			}
			try {
				System.arraycopy(fBuffer.fData, fOffset, x, off, min);
			}
			catch (Exception e) {
				throw new Error("Internal error in REDFileRider.readBytes");
			}
			fOffset += min; off += min; n -= min;
		}
		fRes = 0;
	}

	/** write a byte to file */
	public void write(byte x) {
		if (getFile().isReadonly()) {
			fRes = 1;
		}
		else {
			if (fOrg != fBuffer.fOrg || fOffset >= REDFileBuffer.fcBufSize) {
				set(fBuffer.fFile, fOrg + fOffset);
			}	
			fBuffer.fData[fOffset] = x;
			fBuffer.fDirty = true;
			if (fOffset == fBuffer.fSize) {
				fBuffer.fSize++; 
				fBuffer.fFile.fLength++;
			}
			fOffset++; fRes = 0;
		}
	}
	
	/** Write n bytes from byte array
	  * @pre x.length >= n
	  */
	public void writeBytes(byte[] x, int n) {
		if (getFile().isReadonly()) {
			fRes = 1;
		}
		else {
			int xpos, min, restInBuf;
			xpos = 0; 
			while (n > 0) {
				if (fOrg != fBuffer.fOrg || fOffset >= REDFileBuffer.fcBufSize) {
					set(fBuffer.fFile, fOrg + fOffset);
				}
				restInBuf = REDFileBuffer.fcBufSize - fOffset;
				if (n > restInBuf) min = restInBuf; else min = n;
				try {
					System.arraycopy(x, xpos, fBuffer.fData, fOffset, min);
				}
				catch (Exception e) {
					throw new Error("Internal error in REDFileRider.writeBytes: " + e);
				}
				fOffset += min; 
				if (fOffset > fBuffer.fSize) {
					fBuffer.fFile.fLength += fOffset - fBuffer.fSize;
					fBuffer.fSize = fOffset;
				}
				xpos += min; n -= min; fBuffer.fDirty = true;
			}
			fRes = 0;
		}
	}	
	
	/** get end of file status
	  * @return true, if rider has tried to read beyond the end of file
	  */
	public boolean eof() {
		return fEof;
	}
	
	/** get result of last operation 
	  * will be 0 after successful write operation
	  * will contain nr. of bytes requested but unavailable read after read operation
	  */
	public int getRes() {
		return fRes;
	}
	
	/** get file the rider operates on 
	  * @post return != null
	  */
	public REDFile getFile() {
		return fBuffer.fFile;
	}
	
	int fRes, fOrg, fOffset;
	boolean fEof;
	REDFileBuffer fBuffer;
}
