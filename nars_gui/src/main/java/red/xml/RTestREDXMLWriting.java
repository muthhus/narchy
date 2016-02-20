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
 
package red.xml;

import junit.framework.*;
import java.io.*;
import java.util.*;

/** Unit test for the XML writing infrastructure.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDXMLWriting extends TestCase {
	static final String TMP_FILE_NORMAL = "RTestREDXMLWriting.1.tmp";
	
	public RTestREDXMLWriting(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
		fFile = new FileOutputStream(TMP_FILE_NORMAL);
	}
	
	public void tearDown() throws Exception {
		super.tearDown();
		fFile.close();
		File f = new File(TMP_FILE_NORMAL);
		assertTrue(f.delete());
	}
			
	public static Test suite() {
		return new TestSuite(RTestREDXMLWriting.class);
	}
	
	static class Writable1 implements REDXMLWritable {
		public Writable1(int id, int value, int red, int green, int blue) {
			fId = id;
			fValue = value;
			fRed = red;
			fGreen = green;
			fBlue = blue;
		}
		
		public void writeXML(REDXMLHandlerWriter handler) throws IOException {
			handler.openTag("Writable1", "id=\"" + fId + '"');
			handler.writeEntity("Value", "", String.valueOf(fValue));
			handler.writeEntity("Color", "red=\"" + fRed + "\" green=\"" + fGreen + "\" blue=\"" + fBlue + '"', null);
			handler.closeTag();
		}

		int fId, fValue, fRed, fGreen, fBlue;
		
	}
	
	public void testSimpleWriting() throws Exception {
		Writable1 w1 = new Writable1(22, 1975, 10, 20, 30);
		REDXMLHandlerWriter writer = new REDXMLHandlerWriter(fFile);
		writer.write(w1);
		assertFileContent("<Writable1 id=\"22\">\n" + 
			"\t<Value>1975</Value>\n" +
			"\t<Color red=\"10\" green=\"20\" blue=\"30\"/>\n" +
			"</Writable1>\n");
			
	}
	
	private static void assertFileContent(String content) throws IOException {
		RandomAccessFile file = new RandomAccessFile(TMP_FILE_NORMAL, "r");
		byte arr[] = new byte[(int) file.length()];
		file.read(arr);
		file.close();
		assertEquals(content, new String(arr));
	}

	
	FileOutputStream fFile;
	HashSet fKeys;
}
