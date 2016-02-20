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

/** listener interface for editor events
  * @author rli@chello.at
  * @tier API
  */
public interface REDEditorEventListener extends EventListener {
	/** A new file is going to be loaded into the editor.
	  * @param filename the file which will be loaded
	  */
	void beforeFileLoad(String filename);
	
	/** A new file has been loaded into the editor.
	  * @param filename the file which has been loaded
	  */
	void afterFileLoad(String filename);
	
	/** File is going to be saved in the editor.
	  * This method is called upon all kinds of saving (save, save as, emergency save)
	  * @param filename the file which will be saved
	  */
	void beforeFileSave(String filename);

	/** File has been saved in the editor.
	  * This method is called upon all kinds of saving (save, save as, emergency save)
	  * @param filename the file which has be saved
	  */
	void afterFileSave(String filename);
}
