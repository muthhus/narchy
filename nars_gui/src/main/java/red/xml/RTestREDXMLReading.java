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

import java.io.*;
import java.util.*;
import junit.framework.*;
import red.*;
import red.util.*;

/** Unit test for the XML reading infrastructure.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDXMLReading extends RTestLogObserver {
	public RTestREDXMLReading(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
		fManager = new REDXMLManager();
	}
	
	public static class ReadableBase {
		public ReadableBase() {
			fLog = new StringBuffer();
		}
		
		public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
			throw new Error("Abstract setMappings.");
		}

		/** Write the stacktrace to a string */
        public static String getStack(){
                StringWriter stringWriter= new StringWriter();
                PrintWriter writer= new PrintWriter(stringWriter);
                (new Throwable()).printStackTrace(writer);
                StringBuffer buffer= stringWriter.getBuffer();
				return String.valueOf(buffer);
        }
		
		/** Get calling method from stack */
		public static String getCallingMethod() {
			String stackDump = getStack();
			StringTokenizer tok = new StringTokenizer(stackDump, "\n");
			for (int dump = 0; dump < 4; dump++) {
				tok.nextToken();
			}
			String line = tok.nextToken();
			line = line.substring(0, line.indexOf('('));
			line = line.substring(line.lastIndexOf('.') + 1);
			return line;
		}
		
		/** Append the method calling log() to the call log. */ 
		public void log() {
			if (fLog.length() > 0) {
				fLog.append('\n');
			}
			fLog.append(getCallingMethod());
		}
		
		public String toString() {
			return String.valueOf(fLog);
		}
		
		public void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer) {
			throw new Error("Abstract innerProduction.");
		}		
		StringBuffer fLog;
	}
	
	public static class Readable1 extends ReadableBase implements REDXMLReadable {
		public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
			handler.mapStart("#", "foundDelimiterStart()");
			handler.mapEnd("#", "foundDelimiterEnd()");
		}
		public void foundDelimiterStart() { log(); }
		public void foundDelimiterEnd() { log(); }
	}
	
	ReadableBase readFile(String extension, Class handler) throws Exception {
		fManager.registerHandler("TopLevel", handler);
		REDResourceInputStreamIterator iter = REDResourceManager.getInputStreams("red/xml", extension);
		assertTrue(iter.hasNext());
		InputStream is = (InputStream) iter.next(); 
		assertTrue(!iter.hasNext());
		InputStreamReader reader = new InputStreamReader(is); 
		fManager.parse(reader, String.valueOf(iter.curName()));
		reader.close(); 
		return (ReadableBase) fManager.getProducedObject();
	}
	
	public void testDelimiterTag() throws Exception {
		Readable1 prod = (Readable1) readFile("RTestREDXMLReading.1.xml", Readable1.class);
		assertEquals("foundDelimiterStart\nfoundDelimiterEnd", String.valueOf(prod));
	}
	
	public static class Readable2 extends ReadableBase implements REDXMLReadable {
		public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
			handler.mapStart("#", "foundDelimiterStart(#, #id, #level, #&, #$)");
			handler.mapEnd("#", "foundDelimiterEnd(#, #!, #&, #$)");
			handler.mapStart("SubLevel", "foundSublevelStart(#, #!)");
		}
		
		public void foundSublevelStart(String content, String contentPrecise) {
			log();
			assertEquals("ccc", content);
			assertEquals("\tccc", contentPrecise);
		}
		
		public void foundDelimiterStart(String content, String id, String level, REDXMLHandlerReader reader, REDXMLManager manager) { 
			log(); 
			fManager = manager;
			assertEquals("", content);
			assertEquals("balrog", id);
			assertEquals("16", level);
			assertNotNull(manager);
			assertNotNull(reader);
		}
		
		public void foundDelimiterEnd(String content, String contentPrecise, REDXMLHandlerReader reader, REDXMLManager manager) { 
			log(); 
			assertEquals("bbb", content);
			assertEquals("\n\tbbb\n\t", contentPrecise);
			assertNotNull(manager);
			assertEquals(manager, fManager);
			assertNotNull(reader);
		}
		REDXMLManager fManager;
	}
	
	public void testParameters() throws Exception {
		Readable2 prod = (Readable2) readFile("RTestREDXMLReading.1.xml", Readable2.class);
		assertEquals(prod.fManager, fManager);
		assertEquals("foundDelimiterStart\nfoundSublevelStart\nfoundDelimiterEnd", String.valueOf(prod));
	}
	
	public static class Readable3 extends ReadableBase implements REDXMLReadable {
		public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
			handler.mapStart("#", "foundDelimiterStart(#, (String) #id, ( int )#level, (float) '3.5', (Integer) '-500', " + 
				"(boolean) 'true', 'foo', 'don''t', (short)'100',(long)'99999999', ( double) '1.5E22d', " + 
				"(char) 'r', (byte) '127', (float) #level, ' You cannot pass ! ')");
		}
		public void foundDelimiterStart(String content, String id, int level, float f, Integer i, boolean flag, String foo, 
			String dont, short s, long l, double d, char r, byte b, float levelFloat, String wsTest) { 
			log(); 
			assertEquals("", content);
			assertEquals("balrog", id);
			assertEquals(16, level);
			assertTrue(f == 3.5);
			assertEquals(new Integer(-500), i);
			assertTrue(flag);
			assertEquals("foo", foo);
			assertEquals("don't", dont);
			assertEquals(100, s);
			assertEquals(99999999, l);
			assertTrue(d == 1.5E22D);
			assertEquals('r', r);
			assertEquals(127, b);
			assertTrue(16f == levelFloat);
			assertEquals(" You cannot pass ! ", wsTest);
		}		
	}
	
	public void testTypeSafeParameters() throws Exception {
		Readable3 prod = (Readable3) readFile("RTestREDXMLReading.1.xml", Readable3.class);
		assertEquals("foundDelimiterStart", String.valueOf(prod));
	}
	
	public static class Readable4 extends ReadableBase implements REDXMLReadable {
		public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
			handler.mapStart("#", "foundDelimiterStart(#, (String) #idontexist, ( int )#level, (float) '', " + 
				"(boolean) '', '', '''', (short)'',(long)'', ( double) '', " + 
				"(char) '', (byte) '')");
			handler.mapStart("SubLevel", "foundSublevelStart((int) # = '42')");
			handler.mapEnd("SubLevel", "foundSublevelEnd(# = 'def1', #! = 'def2', (String) #idontexist = 'def3', " +
				"(int) #id = '4', (float) #noFloat = '3.7', (Integer) #foo = '45', (char) #nono = ''");
		}
		public void foundDelimiterStart(String content, String id, int level, float f, boolean flag, String foo, 
			String dont, short s, long l, double d, char r, byte b) { 
			log(); 
			assertEquals("", content);
			assertEquals("", id);
			assertEquals(16, level);
			assertTrue(0f == f);
			assertTrue(!flag);
			assertEquals("", foo);
			assertEquals("'", dont);
			assertEquals(0, s);
			assertEquals(0, l);
			assertTrue(0f == d);
			assertEquals('\0', r);
			assertEquals(0, b);
		}		
		
		public void foundSublevelStart(int theAnswer) {
			log();
			assertEquals(42, theAnswer);
		}
		
		public void foundSublevelEnd(String def1, String def2, String def3, int i4, float f37, Integer i45, char ch) {
			log();
			assertEquals("def1", def1);
			assertEquals("def2", def2);
			assertEquals("def3", def3);
			assertEquals(4, i4);
			assertTrue(f37 == 3.7f);
			assertEquals(new Integer(45), i45);
			assertEquals('\0', ch);
		}
	}
	
	public void testFallbackBehaviourAndDefaultParams() throws Exception {
		observeLog(true);
		ReadableBase prod = readFile("RTestREDXMLReading.1.xml", Readable4.class);
		assertEquals("foundDelimiterStart\nfoundSublevelStart\nfoundSublevelEnd", String.valueOf(prod));
		assertEquals(1, getLogCount());
		resetLog();
		observeLog(false);
	}
	
	public static class Readable5 extends ReadableBase implements REDXMLReadable {
		final static public int fcBalrog = 9;
		final static public int fcBbb = 10;
		static public RTestREDXMLReading fgBalrog = new RTestREDXMLReading("");
		final static public int fcHobbitsFrodo = 1 >> 1;
		final static public int fcHobbitsSam = 1 >> 2;
		final static public int fcHobbitsMerry = 1 >> 3;
		final static public int fcHobbitsPippin = 1 >> 4;
		
		
		public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
			handler.mapStart("#", "foundDelimiterStart((int) #id [fc], (red.xml.RTestREDXMLReading) #id[fg])");
			handler.mapEnd("#", "foundDelimiterEnd((int) #[fc] = 'just test syntax')");
			handler.mapStart("SubLevel", "foundSublevelStart((int) #hobbits[fcHobbits], (red.REDFinderDirection) #direction[red.REDFinderDirection.])");
		}

		public void foundDelimiterStart(int x, RTestREDXMLReading y) {
			log();
			assertEquals(fcBalrog, x);
			assertEquals(fgBalrog, y);
		}				
		
		public void foundDelimiterEnd(int x) {
			log();
			assertEquals(fcBbb, x);
		}

		public void foundSublevelStart(int flags, REDFinderDirection dir) {
			log();
			assertEquals(fcHobbitsFrodo + fcHobbitsSam + fcHobbitsMerry + fcHobbitsPippin, flags);
			assertSame(REDFinderDirection.FORWARD, dir);
		}
	}
	
	
	public void testConstantMapping() throws Exception {
		ReadableBase prod = readFile("RTestREDXMLReading.1.xml", Readable5.class);
		assertEquals("foundDelimiterStart\nfoundSublevelStart\nfoundDelimiterEnd", String.valueOf(prod));
	}
		
	public static Test suite() {
		return new TestSuite(RTestREDXMLReading.class);
	}
	REDXMLManager fManager;
}
