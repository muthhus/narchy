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
import junit.framework.*;

/** JUnit TestCase class for red.file.REDFile and red.file.REDFileRider. 
  * @author gerald.czech@scch.at
  * @tier test
  */
public class RTestREDFile extends TestCase {
	/** The name of the normal input data file. */
	private static final String fcInFileNormal = "RTestREDFile.1.in";

	/** The name of the empty input data file. */
	private static final String fcInFileEmpty = "RTestREDFile.2.in";
	
	/** The name of the normal output data file. */
	private static final String fcOutFile = "RTestREDFile.1.out"; 

	/**
	 * The content of RTestREDFile.1.in. Don't modify this string, cause it's
	 * content is used hardcoded sometimes directly in the code.
	 */
	private static final String fcFileContent = "Don't meddle in the affairs of wizards,"
			+ "\n" + "for they are subtle and quick to anger." + "\n\n"
			+ "The Lord of the Rings." + "\n";

	/**
	 * Initialize Testdata.
	 */
	public void setUp() {
		REDFile f = new REDFile(fcInFileEmpty, false);	
		f.purge();
		f.close();
		
		REDFileRider r = new REDFileRider(new REDFile(fcInFileNormal, false));
		r.getFile().purge();
		r.writeBytes(fcFileContent.getBytes(), fcFileContent.length());
		r.getFile().close();
	}
	
	public void tearDown() {
		File f = new File(fcInFileNormal); f.delete();
		f = new File(fcInFileEmpty); f.delete();
		f = new File(fcOutFile); f.delete();
	}
	
	/**
	 * Constructor.
	 * Constructs a new RTestRedFile object.
	 */
	public RTestREDFile(String name) {
		super(name);
	}
	
	/**
	 * Tests the read(), the eof() and the getFile() method of REDFileRider.
	 * The file RTestREDFile.1.in must exist in the current directory.
	 */
	public void testRead() {
		REDFile f = new REDFile(fcInFileNormal, true);
		REDFileRider r = new REDFileRider(f);
		
 		byte[] byteArray = new byte[fcFileContent.length()];
		byte c;
		int counter = 0;

		c = r.read();
		while (!r.eof()) {
			byteArray[counter] = c;
			c = r.read();
			counter++;
		}
		String testOutput = new String(byteArray);
		assertTrue("Rider returns correct file", f == r.getFile());
		assertTrue("Length of testOutput: ",
			testOutput.length() == fcFileContent.length());
		assertTrue("Content of testOutput", testOutput.equals(fcFileContent));
		assertTrue("All bytes read: ", r.getRes() ==  0);
		f.close();
	}
	
	/**
	 * Tests the readByte() and eof() method of REDFileRider on an empty file.
	 * The file RTestREDFile.2.in must exist in the current directory.
	 */
	public void testReadEmpty() {
		REDFile f = new REDFile(fcInFileEmpty, true);
		REDFileRider r = new REDFileRider(f);

		// first read on empty file causes eof() to be set
		byte c;
		assertTrue("Not eof before first read", !r.eof());
		c = r.read();
		assertTrue("eof after first read", r.eof());
		assertTrue("All bytes read: ", r.getRes() ==  0);
		f.close();
	}	

	/**
	 * Tests the readBytes() and eof() method of REDFileRider for requesting
	 * less data, as the file holds. The file RTestREDFile.1.in must exist in
	 * the current directory.
	 */
	public void testReadBytesShorter() {
		REDFile f = new REDFile(fcInFileNormal, true);
		REDFileRider r = new REDFileRider(f);
 		int res;
		int len = fcFileContent.length() / 2;
		String halfContent = fcFileContent.substring(0, len);
 		byte[] byteArray = new byte[len];
 		String output;

 		r.readBytes(byteArray);
 		output = new String(byteArray);
 		res = r.getRes();
 		assertTrue("Not on eof", !r.eof());
 		assertTrue("Bytes read equal half content", output.equals(halfContent));
 		assertTrue("All bytes read", res == 0);
		f.close();
	}

	/**
	 * Tests the readBytes() and eof() method of REDFileRider for requesting
	 * exactly the amount of data, as the file holds. The file RTestREDFile.1.in
	 * must exist in the current directory.
	 */
	public void testReadBytesExactLength() {
		REDFile f = new REDFile(fcInFileNormal, true);
		REDFileRider r = new REDFileRider(f);
		int len = fcFileContent.length();	// exact file's content
 		byte[] byteArray = new byte[len];
 		byte c;

 		r.readBytes(byteArray, 0, len);
 		assertTrue("Not on eof", !r.eof());
 		assertTrue("All bytes read", r.getRes() == 0);
 		c = r.read();		// read beyond eof
 		assertTrue("On eof", r.eof());
		f.close();
	}	

	/**
	 * Tests the readBytes() and eof() method of REDFileRider for requesting
	 * more data, as the file holds. The file RTestREDFile.1.in must exist in
	 * the current directory.
	 */
	public void testReadBytesLonger() {
		REDFile f = new REDFile(fcInFileNormal, true);
		REDFileRider r = new REDFileRider(f);
 		int res;
		int len = fcFileContent.length() + 10;	// more than file's content
 		byte[] byteArray = new byte[len];

 		r.readBytes(byteArray, len);
 		res = r.getRes();
 		assertTrue("On eof", r.eof());
		assertTrue("10 bytes not read", res == 10);
 		assertTrue("getRes() > 0 => eof", res > 0 && r.eof());
		f.close();
	}

	/**
	 * Tests the readBytes() and eof() method of REDFileRider for inserting the
	 * contents in the middle of the resulting byte array. The file
	 * RTestREDFile.1.in must exist in the current directory.
	 */
	public void testReadBytesMiddle() {
		REDFile f = new REDFile(fcInFileNormal, true);
		REDFileRider r = new REDFileRider(f);
		int len = fcFileContent.length() + 20;
 		byte[] byteArray = new byte[len];
 		byte c;
 		String output;
 		String expected = new String(new byte[10]) + fcFileContent
 			+ new String(new byte[10]);

 		r.readBytes(byteArray, 10, fcFileContent.length());
 		output = new String(byteArray);
 		assertTrue("All bytes read", r.getRes() == 0);
 		assertTrue("Not on eof", !r.eof());
 		c = r.read();		// Read beyond eof
 		assertTrue("On eof", r.eof());
 		// substring shifted 10 bytes should be exactly file content
 		assertTrue("Content completely in read bytes",
 			fcFileContent.equals(output.substring(10, fcFileContent.length() + 10)));
 		assertTrue("Bytes read equal string holding content",
 			output.equals(expected));
		f.close();
	}
	
	/**
	 * Tests the readByte() and eof() method of REDFileRider on an empty file.
	 * The file RTestREDFile.2.in must exist in the current directory.
	 */
	public void testReadBytesEmpty() {
		REDFile f = new REDFile(fcInFileEmpty, true);
		REDFileRider r = new REDFileRider(f);
		int len = fcFileContent.length();
 		byte[] byteArray = new byte[len + 20];
 		String output;

		// first read on empty file causes eof() to be set
		assertTrue("Not eof before first read", !r.eof());
		r.readBytes(byteArray, 10, len);
		output = new String(byteArray);
		assertTrue("eof after first read", r.eof());
		assertTrue("Read bytes equal empty string with same length",
					output.equals(new String(new byte[len + 20]))); 
		assertTrue(len + " bytes not read", r.getRes() ==  len);
		f.close();
	}
	
	/**
	 * Tests the seek() method of REDFileRider. seek() ist tested for:
	 * <UL>
	 * <LI> a negative value
	 * <LI> the begin of the file
	 * <LI> a text in the middle of the file
	 * <LI> the end of the file
	 * <LI> beyond the end of the file
	 * </UL>
	 * The file RTestREDFile.1.in must exist in the current directory. 
	 */
	public void testSeek() {
		REDFile f = new REDFile(fcInFileNormal, true);
		REDFileRider r = new REDFileRider(f);
		int index; 		// the index to seek
 		byte[] byteArray = new byte[22];
		byte c;
		String output;
		
		// seek for a negative
		index = -1;
		r.seek(index);
		c = r.read();
		assertTrue("Seek on first posiiton for negative value", (char)c == 'D');

		// seek the begin of the file
		index = 0;
		r.seek(index);
		c = r.read();
		assertTrue("Seek on first posiiton", (char)c == 'D');
		
		// seek for text in the middle of the file and read some bytes
		index = fcFileContent.indexOf("The");
		assertTrue("Index in range of file", (0 <= index) && (index <= f.length()));
		r.seek(index);
		for (int i = 0; i < 22 && !r.eof(); i++) {
			c = r.read();
			byteArray[i] = c;
		}
		output = new String(byteArray);
		assertTrue("Read text after seek as supposed",
			output.equals("The Lord of the Rings."));

		// seek to eof
		r.seek(f.length());
		assertTrue("Not eof after seek", !r.eof());
		c = r.read();
		assertTrue("eof after seek", r.eof());		

		// seek beyond eof
		r.seek(f.length() + 10);
		assertTrue("Not eof after seek", !r.eof());
		c = r.read();
		assertTrue("eof after seek", r.eof());
		f.close();
	}

	/**
	 * Tests the seek() method of REDFileRider on an empty file. The file
	 * RTestREDFile.2.in must exist in the current directory.
	 */
	public void testSeekEmpty() {
		REDFile f = new REDFile(fcInFileEmpty, true);
		REDFileRider r = new REDFileRider(f);
		int index; 		// the index to seek
		byte c;
		
		index = fcFileContent.indexOf("The");
		assertTrue("Index out of range of file", index > f.length());
		assertTrue("Not eof before seek", !r.eof());
		r.seek(index);
		assertTrue("Not eof after seek", !r.eof());
		c = r.read();		// read beyond eof
		assertTrue("eof after first read", r.eof());
		f.close();
	}

	/**
	 * Tests the static copyFile() and the purg() method of REDFile. The file
	 * RTestREDFile.1.in must exist in the current directory and be writable. The file
	 * RTestREDFile.1.out is created in the current directory.
	 */
	public void testPurge() {
		REDFile inFile = new REDFile(fcInFileNormal);
		REDFile outFile = new REDFile(fcOutFile);
		REDFileRider inRider = new REDFileRider(inFile);
		REDFileRider r = new REDFileRider(outFile);
		byte[] inBytes = new byte[fcFileContent.length()];
		String inFileContent;
		String outFileContent;
		byte c;
		
		REDFile.copyFile(inFile, outFile);
		assertTrue("Copied files have equal length",
				inFile.length() == outFile.length());
		inRider.readBytes(inBytes);
		inFileContent = new String(inBytes);
		inFile.close();
		r.readBytes(inBytes);
		outFileContent = new String(inBytes);
		assertTrue("Copied file has same content as source",
				inFileContent.equals(outFileContent));
		r.seek(0);
		c = r.read();
		assertTrue("Not on eof", !r.eof());
		outFile.purge();
		r.seek(0);
		c = r.read();
		assertTrue("On eof - purge was successful", r.eof());
		outFile.close();
	}

	/**
	 * Tests the write() method of REDFileRider. Further the usage of two
	 * REDFileRiders on one REDFile is tested. The file RTestREDFile.1.out is
	 * created in the current directory and the file RTestREDFile.1.in must
	 * exist in the current directory.
	 */
	public void testWrite() {
		REDFile inFile = new REDFile(fcInFileNormal, true);
		REDFile outFile = new REDFile(fcOutFile, false);
		REDFileRider writer = new REDFileRider(outFile);
		REDFileRider reader = new REDFileRider(outFile);
		int index;
		String outText = "Hero";
		byte[] outBytes = new byte[outText.length()];
		byte[] inBytes = new byte[outText.length()];
		String inText;
		
		REDFile.copyFile(inFile, outFile);
		inFile.close();
		index = fcFileContent.indexOf("Lord");
		outBytes = outText.getBytes();
		writer.seek(index);
		for (int i = 0; i < outText.length(); i++) {
			writer.write(outBytes[i]);
			assertTrue("Write was successfull", writer.getRes() == 0);
		}
		
		// read the written part and check the result
		reader.seek(index);		
		reader.readBytes(inBytes);
		inText = new String(inBytes);
		assertTrue("Files of riders are equal", reader.getFile() == writer.getFile());
		assertTrue("Written and read bytes are equal", inText.equals(outText));
		outFile.close();
	}

	/**
	 * Tests the write() method of REDFileRider in an empty file. The file
	 * RTestREDFile.1.out is created in the current directory.
	 */
	public void testWriteEmpty() {
		REDFile f = new REDFile(fcOutFile);
		REDFileRider r = new REDFileRider(f);
		byte[] outBytes = fcFileContent.getBytes();
		byte[] inBytes = new byte[fcFileContent.length()];
		String inText;

		f.purge();
		for (int i = 0; i < fcFileContent.length(); i++) {
			r.write(outBytes[i]);
			assertTrue("getRes() is zero after write()", r.getRes() == 0);
		}
		
		r.seek(0);	// set rider back to the beginning of the file
		r.readBytes(inBytes);
		inText = new String(inBytes);
		assertTrue("Length of Written and read bytes are equal",
			fcFileContent.length() == inText.length());
		assertTrue("Written and read bytes are equal", fcFileContent.equals(inText));
		f.purge();
		f.close();
	}
 	 
	/**
	 * Tests the write() method of REDFileRider for writing on a readonly
	 * file. The file RTestREDFile.1.in must exist in the current directory.
	 * The file RTestREDFile.1.out is created in the current directory.
	 */
	public void testWriteReadOnlyFile() {
		REDFile inFile = new REDFile(fcInFileNormal, true);
		REDFile outFile = new REDFile(fcOutFile, false);
		REDFileRider r;
		byte outByte = (byte)'X';
		byte[] inBytes = new byte[fcFileContent.length()];
		String inText;
		
		REDFile.copyFile(inFile, outFile);
		inFile.close();
		outFile.close();
		outFile = new REDFile(fcOutFile, true);	// open file readonly
		r = new REDFileRider(outFile);		// set rider on readonly file
		r.write(outByte);
		assertTrue("Error indication must happen", r.getRes() != 0);
		
		r.seek(0);
		r.readBytes(inBytes);
		inText = new String(inBytes);
		assertTrue("Length of Readonly File after write not changed",
				fcFileContent.length() == inText.length());
		assertTrue("Readonly File after write not changed",
				fcFileContent.equals(inText));
		assertTrue("Purge must fail", outFile.purge() == false);
		outFile.close();
	}
	
	/**
	 * Tests the length() method (calculation) of REDFile. The file
	 * RTestREDFile.1.in must exist in the current directory. The file
	 * RTestREDFile.1.out is created in the current directory.
	 */
	public void testLength() {
		REDFile inFile = new REDFile(fcInFileNormal, true);
		REDFile outFile = new REDFile(fcOutFile, false);
		REDFileRider r = new REDFileRider(outFile);
		String outText = "XXXX";
		byte[] outBytes = new byte[outText.length()];
		byte[] inBytes = new byte[fcFileContent.length()];
		byte c;
		String inText;
		int result; 		// supposed result length
		
		REDFile.copyFile(inFile, outFile);
		inFile.close();
		r.seek(outFile.length());	// seek to length() == seek to end
		assertTrue("Not on eof", !r.eof());
		c = r.read();
		assertTrue("On eof", r.eof());
		result = fcFileContent.length() + 1;
		r.write((byte)'Z');
		assertTrue("Length of file equals supposed result after write()",
				outFile.length() == result);
		result = result + outText.length();
		outBytes = outText.getBytes();
		r.writeBytes(outBytes, outText.length());
		assertTrue("Length of file equals supposed result after writeBytes()",
				outFile.length() == result);
		outFile.close();
	} 
	
	/**
	 * Tests the writeBytes() method of REDFileRider. The file
	 * RTestREDFile.1.out is created in the current directory.
	 */
	public void testWriteBytesEmpty() {
		REDFile f = new REDFile(fcOutFile);
		REDFileRider r = new REDFileRider(f);
		byte[] outBytes = fcFileContent.getBytes();
		byte[] inBytes = new byte[fcFileContent.length()];
		String inText;

		f.purge();
		r.writeBytes(outBytes, fcFileContent.length());
		assertTrue("Write was successfull", r.getRes() == 0);
		
		r.seek(0);	// set rider back to the beginning of the file
		r.readBytes(inBytes);
		inText = new String(inBytes);
		assertTrue("Length of Written and read bytes are equal",
			fcFileContent.length() == inText.length());
		assertTrue("Written and read bytes are equal", fcFileContent.equals(inText));
		f.purge();
		f.close();		
	}
	 	 
	/**
	 * Tests the writeBytes() method of REDFileRider for writing on the begin
	 * of a file. The file RTestREDFile.1.in must exist in the current
	 * directory. The file RTestREDFile.1.out is created in the current
	 * directory.
	 */
	 public void testWriteBytesBegin() {
		REDFile inFile = new REDFile(fcInFileNormal, true);
		REDFile outFile = new REDFile(fcOutFile, false);
		REDFileRider r = new REDFileRider(outFile);
		String outText = "XXXX";
		byte[] outBytes = new byte[outText.length()];
		byte[] inBytes = new byte[fcFileContent.length()];
		String inText;
		String result;		// proposed result string
		
		result = outText + fcFileContent.substring(outText.length());
		REDFile.copyFile(inFile, outFile);
		inFile.close();
		outBytes = outText.getBytes();
		r.writeBytes(outBytes, outText.length());
		assertTrue("Write was successfull", r.getRes() == 0);
		
		// read the written part and check the result
		r.seek(0);
		r.readBytes(inBytes);
		inText = new String(inBytes);
		assertTrue("Length of text in file is unchanged",
				fcFileContent.length() == inText.length());
		assertTrue("Changes in proposed result and written bytes are equal",
				inText.equals(result));
		outFile.close();
	 }

	/**
	 * Tests the writeBytes() method of REDFileRider for writing on the begin
	 * of a file. The file RTestREDFile.1.in must exist in the current
	 * directory. The file RTestREDFile.1.out is created in the current
	 * directory.
	 */
	public void testWriteBytesMiddle() {
		REDFile inFile = new REDFile(fcInFileNormal, true);
		REDFile outFile = new REDFile(fcOutFile, false);
		REDFileRider r = new REDFileRider(outFile);
		int index;
		String outText = "Hero";
		byte[] outBytes = new byte[outText.length()];
		byte[] inBytes = new byte[outText.length()];
		String inText;
		
		REDFile.copyFile(inFile, outFile);
		inFile.close();
		index = fcFileContent.indexOf("Lord");
		outBytes = outText.getBytes();
		r.seek(index);
		r.writeBytes(outBytes, outText.length());
		assertTrue("Write was successfull", r.getRes() == 0);
		
		// read the written part and check the result
		r.seek(index);		
		r.readBytes(inBytes);
		inText = new String(inBytes);
		assertTrue("Written and read bytes are equal", inText.equals(outText));
		outFile.close();
	}
	 
	/**
	 * Tests the writeBytes() method of REDFileRider for writing on the end
	 * of a file. The file RTestREDFile.1.in must exist in the current
	 * directory. The file RTestREDFile.1.out is created in the current
	 * directory.
	 */
	 public void testWriteBytesEnd() {
		REDFile inFile = new REDFile(fcInFileNormal, true);
		REDFile outFile = new REDFile(fcOutFile, false);
		REDFileRider r = new REDFileRider(outFile);
		String outText = "XXXX";
		byte[] outBytes = new byte[outText.length()];
		byte[] inBytes = new byte[fcFileContent.length() + outText.length()];
		String inText;
		String result;					// proposed result string
		
		result = fcFileContent + outText;
		REDFile.copyFile(inFile, outFile);
		inFile.close();
		r.seek(outFile.length() + 1);		// set rider to the end of the file
		outBytes = outText.getBytes();
		r.writeBytes(outBytes, outText.length());
		assertTrue("Write was successfull", r.getRes() == 0);
		
		// read the written part and check the result
		r.seek(0);
		r.readBytes(inBytes);
		inText = new String(inBytes);
		assertTrue("Length of text in file is unchanged",
				result.length() == inText.length());
		assertTrue("Changes in proposed result and written bytes are equal: <<" + inText + ">> vs. <<"+result+">>",
				inText.equals(result));
		outFile.close();
	 }
	 
	 /**
	  * Test writeBytes() method of REDFileRider on an empty file with very
	  * much data. The file RTestREDFile.1.out is created in the current
 	  * directory.
 	  */
 	 public void testWriteBytesLong() {
 	 	REDFile outFile = new REDFile(fcOutFile);
 	 	REDFileRider r = new REDFileRider(outFile);
 	 	byte[] outBytes;
 	 	byte[] inBytes;
 	 	byte c;
 	 	int len;
 	 	int numberOfWrites = 10;		// how often fcFileContent is written
 	 	String result = "";	// supposed result
 	 	String inText;
 	 	
 	 	len = fcFileContent.length();
 	 	outBytes = fcFileContent.getBytes();
 	 	outFile.purge();			// ensure file is empty
 	 	
 	 	// write in file and compose result string
 	 	for (int i = 0; i < numberOfWrites; i++) {
 	 		r.writeBytes(outBytes, len);
 	 		assertTrue("Write was successfull", r.getRes() == 0);
 	 		result += fcFileContent;
 	 	}
 	 	
 	 	// read file
 	 	inBytes = new byte[len * numberOfWrites];
 	 	r.seek(0);
 	 	r.readBytes(inBytes);
 	 	assertTrue("Not on eof", !r.eof());
 	 	c = r.read();
 	 	assertTrue("On eof", r.eof());
 	 	inText = new String(inBytes);
 	 	assertTrue("Length of supposed result and read bytes are equal",
 	 			result.length() == inText.length());
 	 	assertTrue("Read bytes equal supposed result", result.equals(inText));
 	 	outFile.purge();		// clean up
 	 	outFile.close();
 	 }
 	 
 	 /** Test getUniqueTmpFile.
	  * This test tries to find out if tmp - files are collected properly
	  */
 	 public void testUniqueTmpFile() {
 	 	for (int x = 0; x < 300; x++) {
 	 		REDFile.getUniqueTmpFile();
 	 	}
 	 }
 	 
	/**
	 * Static method to construct the TestSuite of RTestREDFile.
	 */
	public static Test suite() {
		return new TestSuite(RTestREDFile.class);
	}
}
