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
import java.net.*;

/** File with buffered access.
  * @author rli@chello.at
  * @invariant fSwapper >= 0
  * @invariant fSwapper < fcNrBufs
  * @invariant fFile != null
  * @tier system
  */
public class REDFile {
	final static public int fcNrBufs = 4;

	private void setFile(String name, boolean readonly) throws FileNotFoundException {
		if (readonly) {
			fFile = new RandomAccessFile(name, "r");
		}
		else {
			fFile = new RandomAccessFile(name, "rw");
		}
	}
		
	public REDFile(String name, boolean readonly) {
		try {
			setFile(name, readonly);
		}
		catch (FileNotFoundException fnfe) {
			System.gc();	// hopefully we freed some descriptors here
			try {
				setFile(name, readonly);
			}
			catch (FileNotFoundException fnfe2) {
				fFile = null;
				throw new Error("Cannot create/read file: " + name + "\nException:" + fnfe2);
			}
		}
		try {
			fLength = (int) fFile.length();
		}
		catch (IOException ioe) {
			throw new Error("Cannot get filelength");
		}
		fReadonly = readonly;
	}

	public REDFile(String name) {
		this(name, false);
	}
	
	public void close() {
		for (int i = 0; i < fcNrBufs; i++) {
			if (fBuffer[i] != null) {
				fBuffer[i].flush();
			}
		}
		try {
			fFile.close();
		}
		catch (IOException ioe) {
			throw new Error("Cannot close file\n" + ioe);
		}
	}
	
	public boolean isReadonly() {
		return fReadonly;
	}
	
	public int length() {
		return fLength;
	}
	
	/** erase file content 
	  * @return true, if successful; false otherwise
	  * @post return == true implies length() == 0
	  */
	public boolean purge() {
		if (isReadonly()) {
			return false;
		}
		fLength = 0;
		try {
			fFile.setLength(0);
		}
		catch (IOException ioe) {
			throw new Error("Error in REDFile.purge: " + ioe);
		}
		for (int i = 0; i < fcNrBufs; i++) {
			if (fBuffer[i] != null) {
				fBuffer[i].fOrg = -1;
				fBuffer[i] = null;
			}
		}
		return true;
	}
	
	protected void finalize() {
		close();
	}
	
	boolean fReadonly;
	RandomAccessFile fFile;
	int fSwapper;
	int fLength;
	REDFileBuffer fBuffer[];
	{
		fBuffer = new REDFileBuffer[fcNrBufs];
		fSwapper = 0;
	}
	
	/** returns a tmp-file called sth. like /tmp/REDTmp_<hostname>_<username>_<timestamp>_<counter> */
	static synchronized public REDFile getUniqueTmpFile() {
		REDFile retVal = null;
		File f = new File(fcTmpPrefix + fcTmpFileCounter);
		f.deleteOnExit();
		retVal = new REDFile(f.getAbsolutePath(), false);
		fcTmpFileCounter++;
		return retVal;
	}
	static private int fcTmpFileCounter;
	static private final String fcTmpPrefix;
	static {
		String tmpDir = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator");
		String hostName;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException uhe) {
			hostName = "";
			// TBD: Where to put errors?
			System.err.println("Cannot determine hostname. This is a potential danger for tmp - files");
		}
		String userName = System.getProperty("user.name");
		fcTmpFileCounter = 0;
		fcTmpPrefix = tmpDir + "REDTmp_" + hostName + '_' + userName + '_' + System.currentTimeMillis() + '_';
	}
	
	/** copy file -- there is no API routine for that in Java :-(
	  * @pre src != null
	  * @pre dest != null
	  * @tbd further optimize by letting file buffers directly be written to dest file
	  */
	public static void copyFile (REDFile src, REDFile dest) {
		dest.purge();
		REDFile foo;
		byte buf[] = new byte[16000];
		REDFileRider r = new REDFileRider(src);
		REDFileRider w = new REDFileRider(dest);
		
		r.readBytes(buf, 16000);
		while (!r.eof()) {
			w.writeBytes(buf, 16000);
			r.readBytes(buf, 16000);
		}
		w.writeBytes(buf, 16000 - r.fRes);
	}
	
	public static boolean moveFile(String srcName, String destName) {
		File src = new File(srcName);
		return src.renameTo(new File(destName));
	}
}
