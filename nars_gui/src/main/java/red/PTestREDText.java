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


import java.io.*;

/** Performance tests for the REDText class 
  * @author Robert Lichtenberger, rli@chello.at
  * @see REDText
  * @tier test
  */
public class PTestREDText {
	static long iterations;
	static public void testInsertEnd() {
		REDText t = new REDText("");
		PTestStopWatch sw = new PTestStopWatch();
		sw.start();
		for (int x = 0; x < iterations; x++) {
			t.replace(x, x, "A");
		}
		sw.stop("Inserting " + iterations + " times at the end");
	}
	
	static public void testLoadLarge() {
		REDText t = new REDText("");
		PTestStopWatch sw = new PTestStopWatch();
		sw.start();
		for (int x = 0; x < iterations; x++) {
			t = new REDText("PTestREDText.1.in");
			sw.stop("Loading a text with length " + t.length() / 1000 + " kB");
		}
		sw.sum("loading a " + t.length() / 1000 + " kB text");
	}		
	
	static public void testFileLength() throws IOException {
		RandomAccessFile rf = new RandomAccessFile("PTestREDText.1.in", "r");
		PTestStopWatch sw = new PTestStopWatch();
		sw.start();
		for (int x = 0; x < iterations; x++) {
			rf.length();
		}
		// System.out.println("Filelength: " + rf.length());
		sw.stop("Getting file length: " + iterations + " times" );
	}
	
	static public void testRandomAccessFile() throws IOException {
		RandomAccessFile rf = new RandomAccessFile("PTestREDText.1.in", "r");
		PTestStopWatch sw = new PTestStopWatch();
		long length = rf.length();
		sw.start();
		for (long x = 0; x < length; x++) {
			byte c = rf.readByte();
		}
		sw.stop("Reading bytewise for: " + length / 1000 + " kB");
	}		
	
	static public void  main(String args[]) throws REDException, IOException {
		try {
			iterations = Integer.valueOf(args[0]);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			iterations = 1000;
		}
		
		String test;
		try {
			test = args[1];
		}
		catch (ArrayIndexOutOfBoundsException e) {
			test = "InsertEnd";
		}

		switch (test) {
			case "InsertEnd":
				testInsertEnd();
				break;
			case "LoadLarge":
				testLoadLarge();
				break;
			case "FileLength":
				testFileLength();
				break;
			case "ReadByte":
				testRandomAccessFile();
				break;
			default:
				System.out.println("Unknown test case specified: '" + test + '\'');
				break;
		}
	}
}
