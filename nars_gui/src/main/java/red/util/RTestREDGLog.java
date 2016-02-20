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

import junit.framework.*;
import java.io.*;

/** JUnit TestCase class for red.util.REDGLog.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDGLog extends TestCase {
	public RTestREDGLog(String name) {
		super(name);
	}

	static class Log implements REDLog {
		public void log(int severity, String group, String message) {
			fBuf.append("").append(severity).append('|').append(group).append('|').append(message).append('\n');
		}
		StringBuffer fBuf  = new StringBuffer();
	}
		
	public void testGLog() throws IOException {
		REDLog oldLog = REDGLog.fgLog;
		Log newLog = new Log();
		REDGLog.fgLog = newLog;
		REDGLog.info("g1", "m1");
		REDGLog.warning("g2", "m2");
		REDGLog.error("g3", "m3");
		REDGLog.fatal("g4", "m4");
		int SEV_INFO = 1;
		int SEV_WARNING = 2;
		int SEV_ERROR = 3;
		int SEV_FATAL = 4;
		assertEquals(REDLog.SEV_INFO + "|g1|m1\n" + REDLog.SEV_WARNING + "|g2|m2\n" + 
			REDLog.SEV_ERROR + "|g3|m3\n" + REDLog.SEV_FATAL + "|g4|m4\n", String.valueOf(newLog.fBuf));
		REDGLog.fgLog = oldLog;
	}
 	 
	public static Test suite() {
		return new TestSuite(RTestREDGLog.class);
	}
}
