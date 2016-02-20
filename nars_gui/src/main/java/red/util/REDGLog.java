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

/** Global log singleton
  * @author rli@chello.at
  * @tier system
  */
public class REDGLog {
	public static REDLog fgLog;

	/** Write a log message
	* @param severity constant defined in REDLog
	* @param group for selective enabling of messages
	* @param message the mesage text
	*/
	public static void log(int severity, String group, String message) {
		fgLog.log(severity, group, message);
	}

	/** Write a info message
	* @param group for selective enabling of messages
	* @param message the mesage text
	*/
	public static void info(String group, String message) {
		log(REDLog.SEV_INFO, group, message);
	}

	/** Write a warning message
	* @param group for selective enabling of messages
	* @param message the mesage text
	*/
	public static void warning(String group, String message) {
		log(REDLog.SEV_WARNING, group, message);
	}

	/** Write an error message
	* @param group for selective enabling of messages
	* @param message the mesage text
	*/
	public static void error(String group, String message) {
		log(REDLog.SEV_ERROR, group, message);
	}

	/** Write a fatal message
	* @param group for selective enabling of messages
	* @param message the mesage text
	*/
	public static void fatal(String group, String message) {
		log(REDLog.SEV_FATAL, group, message);
	}
	
	static {		
		fgLog = new REDStderrLog();
	}
}
