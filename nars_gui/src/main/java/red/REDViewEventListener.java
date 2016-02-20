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

import java.util.*;

/** listener interface for view events
  * @author rli@chello.at
  * @tier API
  */
public interface REDViewEventListener extends EventListener {
	/** selection is going to be changed <br>
	  * at the time this event is fired, the old - values are still valid
	  * @param newFrom new beginning of selection
	  * @param newTo new end of selection
	  * @param oldFrom old beginning of selection
	  * @param oldTo old end of selection
	  */
	void beforeSelectionChange(int oldFrom, int oldTo, int newFrom, int newTo);

	/** selection is going to be changed  <br>
	  * at the time this event is fired, the new - values are already valid
	  * @param newFrom new beginning of selection
	  * @param newTo new end of selection
	  * @param oldFrom old beginning of selection
	  * @param oldTo old end of selection
	  */
	void afterSelectionChange(int oldFrom, int oldTo, int newFrom, int newTo);
	
	/** view has just gained focus */
	void gotFocus();

	/** view has just lost focus */
	void lostFocus();
	
	/** view mode is going to change
	  * @param oldMode old view mode
	  * @param newMode new view mode
	  */
	void beforeModeChange(int oldMode, int newMode);

	/** view mode has changed 
	  * @param oldMode old view mode
	  * @param newMode new view mode
	  */
	void afterModeChange(int oldMode, int newMode);
}
