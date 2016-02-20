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

/** Regular expression parser stopper interface.
  * @author rli@chello.at
  * @tier API  
  */
public interface REDRexParserStopper {
	/** Decide whether or not to stop.
	  * @param parser The parser needing a decision whether or not to stop
	  * @param line The line that will be parsed next if parsing continues
	  * @param offset The (line-relative) offset that will be parsed from if parsing continues
	  * @param state The state the parser is currently in
	  * @return <CODE>true</CODE>, if parsing should stop now; <CODE>false</CODE> otherwise
	  */
	boolean mustStop(REDRexParser parser, int line, int offset, int state);
}
