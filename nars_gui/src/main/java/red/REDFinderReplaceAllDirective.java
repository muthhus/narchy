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

/** Enumeration of replace all directives
  * @author rli@chello.at
  * @tier API
  */
public class REDFinderReplaceAllDirective {
	/** Do not replace all matches, i.e. replace the first match only */
	final public static REDFinderReplaceAllDirective OFF = new REDFinderReplaceAllDirective();
	/** Replace all matches within the current selection */
	final public static REDFinderReplaceAllDirective SELECTION = new REDFinderReplaceAllDirective();
	/** Replace all matches within the current file */
	final public static REDFinderReplaceAllDirective FILE = new REDFinderReplaceAllDirective();
	private REDFinderReplaceAllDirective() { }
}
