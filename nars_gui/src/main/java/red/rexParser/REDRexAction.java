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

/** Regular expression parser action.
  * Parser actions are associated with patterns in a parser. Whenever the parser finds a pattern it will execute the patternMatch 
  * method of the associated action object.
  * You may want to anonymously subclass REDRexAction.
  * @author rli@chello.at
  * @tier API
  */
public abstract class REDRexAction {
	
	public REDRexAction() {
	}
	
	/** Execute action due to a matching pattern.
	  * @param parser The parser which has found the match.
	  * @param line The line in which the match has happened.
	  * @param match The match descriptor.
	  */
	public abstract void patternMatch(REDRexParser parser, int line, REDRexParserMatch match);
}