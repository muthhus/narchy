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
 
package red.rexParser;

/** Regular expression parser line source interface.
  * @author rli@chello.at
  * @tier API  
  */
public interface REDRexLineSource {
	/** Get line as character with trailing linebreak character(s)
	  * @param lineNr The lineNr to get.
	  * @param reuse If a non-null value is passed, getLine may try to reuse this array in order to keep memory turnaround low.
	  * @return An array containin the requested line or null, if lineNr is out of range.
	  */
	char [] getLine(int lineNr, char[] reuse);
	
	/** Get length of line with trailing linebreak character(s)
	  * @param lineNr The line to get length for
	  * @return The length of the given line. Undefined, if lineNr is out of range.
	  */
	int getLineLength(int lineNr);
}
