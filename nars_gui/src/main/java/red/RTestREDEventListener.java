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

/**
  * Helper class for testing the <Code>REDEventListener<Code>, allowing to define the listener level
  * 
  * @author gerald.czech@scch.at
  * @tier test
  */
public class RTestREDEventListener extends REDEventAdapter implements REDEventListener {
	/** The level of the listener. */
	private final int fLevel;
		
	/**
	 * Constructs a new RTestREDEventListener object for the given level and
	 * name of the source constructing it.
	 *
	 * @param level the level of the REDEventListener.
	 */
	public RTestREDEventListener(int level) {
		
		if (level == RLL_VIEW || level == RLL_NORMAL || level == RLL_LATE) {
			fLevel = level;
		}
		else {
			throw new Error("Illegal level passed to RTestREDEventListener.");
		}
	}
	
	public int getListenerLevel() {
		return fLevel;
	}
}