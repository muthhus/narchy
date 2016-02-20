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

/** Macro command class.
  * This class takes encapsulates multiple REDCommands.
  * @author rli@chello.at
  * @tier system
  */
class REDMacroCommand extends REDCommand {
	public REDMacroCommand(String description) {
		super(description);
		fCommands = new ArrayList();
	}
	
	public void doIt() {
		Iterator iter = fCommands.iterator();
		while (iter.hasNext()) {
			((REDCommand) iter.next()).doIt();
		}
	}
	
	public void undoIt() {
		for (int x = fCommands.size() - 1; x >= 0; x--) {
			((REDCommand) fCommands.get(x)).undoIt();
		}
	}
	
	public void redoIt() {
		Iterator iter = fCommands.iterator();
		while (iter.hasNext()) {
			((REDCommand) iter.next()).redoIt();
		}
	}
	
	/** Add command to macro 
	  * @param cmd The command to be added to this macro.
	  */
	public void add(REDCommand cmd) {
		fCommands.add(cmd);
	}
	
	ArrayList fCommands;
}
