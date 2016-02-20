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

/** listener interface for command processor events
  * @author rli@chello.at
  * @tier API
  */
public interface REDCommandProcessorEventListener extends EventListener
{
	/** DO operation, i.e. a change to the text */
	int DO = 1;
	/** UNDO operation, i.e. a change is undone */
	int UNDO = 2;
	/** REDO operation, i.e. an change to the text that has previously been undone is redone */
	int REDO = 3;
	/** CHECKPOINT operation, i.e. the text has been saved (and thus is no longer modified) */
	int CHECKPOINT = 4;
	
	/** command processor is going to be changed 
	  * @param operation the type of operation (DO, UNDO, REDO, CHECKPOINT)
	  */
	void beforeCmdProcessorChange(int operation);
	
	/** command processor has changed 
	  * @param operation the type of operation (DO, UNDO, REDO, CHECKPOINT)
	  */
	void afterCmdProcessorChange(int operation);
}
