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

/** listener interface for text events
  * for *Insert, *Delete and *StyleChange it is guaranteed that before and after are always called both
  * this is not so for *Load and *Save, since exceptions may interrupt the normal control flow
  * @author rli@chello.at
  * @tier API
  */
public interface REDTextEventListener extends EventListener {
	/** View listener level. Listener attached to this level are notified first */
	int RLL_VIEW = 0;
	/** Normal listener level. Listener attached to this level are notified second */
	int RLL_NORMAL = 1;
	/** Late listener level. Listener attached to this level are notified last */
	int RLL_LATE = 2;

	/** get listening level of this listener
	  * @post return == RLL_VIEW || return == RLL_NORMAL || return == RLL_LATE 
	  */
	int getListenerLevel();
	
	/** text is going to be inserted <br>
	  * note that the stretch [from, to] does not contain the insertion already. 
	  * In fact to may even contain an invalid position (beyond the current length of the text).
	  * @param from start of insertion
	  * @param to end of insertion
	  */
	void beforeInsert(int from, int to);

	/** text has been inserted <br>
	  * the stretch [from, to] now contains the newly inserted text 
	  * @param from start of insertion
	  * @param to end of insertion
	  */
	void afterInsert(int from, int to);

	/** text is going to be deleted <br>
	  * note that the stretch [from, to] still contains the text to be deleted
	  * @param from start of deletion
	  * @param to end of deletion
	  */
	void beforeDelete(int from, int to);

	/** text has been deleted <br>
	  * note that the stretch [from, to] no inter contains the deleted text
	  * @param from start of deletion
	  * @param to end of deletion
	  */
	void afterDelete(int from, int to);
	
	/** style is going to be changed <br>
	  * note that the stretch [from, to] does not yet have newStyle
 	  * @param from start of stretch to be changed
	  * @param to end of stretch to be changed
	  */
	void beforeStyleChange(int from, int to, REDStyle newStyle);

	/** style has been changed <br>
	  * note that the stretch [from, to] now has newStyle
	 * @param from start of changed stretch
	 * @param to end of changed stretch
	 */
	void afterStyleChange(int from, int to, REDStyle newStyle);
	

	/** text is going to be loaded */
	void beforeLoad();
	
	/** text has been loaded */
	void afterLoad();
	
	/** text is going to be saved */
	void beforeSave();

	/** text has been saved */
	void afterSave();
	
	/** text is going to be saved into another file */
	void beforeSaveInto(String filename);

	/** text has been saved into another file */
	void afterSaveInto(String filename);
	
	/** text will no longer send style notifications from now on */
	void beforeStyleBatchNotification();
	
	/** text will send style notifications again from now on */
	void afterStyleBatchNotification();
}
