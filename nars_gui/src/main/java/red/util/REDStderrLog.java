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

/** A very simple log implementation which will print log entries to stderr
  * @author rli@chello.at
  * 
  * If the System property "com.windriver.rome.util.log.FullTrace" is set,
  * REDStderrLog will print a full trace of the log entry.
  * @tier system
  */
public class REDStderrLog implements REDLog {
	public REDStderrLog() { }
	
	static String getSeverityString(int severity) {
		switch(severity) {
			case REDLog.SEV_INFO:
				return "Info";
			case REDLog.SEV_WARNING:
				return "Warning";
			case REDLog.SEV_ERROR:
				return "Error";
			case REDLog.SEV_FATAL:
				return "Fatal";
		}
		return "UnkownSeverity";
	}
	
	public void log(int severity, String group, String message) {
		System.out.println(getSeverityString(severity) + ": " + group +": " + message);
	}	
}
