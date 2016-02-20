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

/** Adapter class for REDEventListener. <br> 
  * Extend and override the methods of interest. <br>
  * note: the text listener level for this adapter is RLL_NORMAL by default
  * @author rli@chello.at
  * @see REDEventListener
  * @tier API
  */
public class REDEventAdapter implements REDEventListener {
	/** get listening level of this listener
	  * @return the adapter returns REDTextEventListener.RLL_NORMAL by default.
	  */
	public int getListenerLevel() {
		return REDTextEventListener.RLL_NORMAL;
	}
	
	/** text is going to be inserted <br>
	  * the stretch [from, to] does not contain the insertion already. 
	  * In fact to may even contain an invalid position (beyond the current length of the text).
	  * @param from start of insertion
	  * @param to end of insertion
	  */
	public void beforeInsert(int from, int to) { }

	/** text has been inserted <br>
	  * the stretch [from, to] now contains the newly inserted text 
	  * @param from start of insertion
	  * @param to end of insertion
	  */
	public void afterInsert(int from, int to) { }

	/** text is going to be deleted <br>
	  * the stretch [from, to] still contains the text to be deleted
	  * @param from start of deletion
	  * @param to end of deletion
	  */
	public void beforeDelete(int from, int to) { }

	/** text has been deleted  <br>
	  * the stretch [from, to] no longer contains the deleted text
	  * @param from start of deletion
	  * @param to end of deletion
	  */
	public void afterDelete(int from, int to) { }
	
	/** style is going to be changed <br>
	  * the stretch [from, to] does not yet have newStyle
 	  * @param from start of stretch to be changed
	  * @param to end of stretch to be changed
	  */
	public void beforeStyleChange(int from, int to, REDStyle newStyle) { }

	/** style has been changed <br>
	  * the stretch [from, to] now has newStyle
	 * @param from start of changed stretch
	 * @param to end of changed stretch
	 */
	public void afterStyleChange(int from, int to, REDStyle newStyle) { }
	
	/** text is going to be loaded */
	public void beforeLoad() { }
	
	/** text has been loaded */
	public void afterLoad() { }
	
	/** text is going to be saved */
	public void beforeSave() { }

	/** text has been saved */
	public void afterSave() { }	

	/** text is going to be saved into another file <br>
	  * this event is fired in case of "Save As"-operations as well as in case of "Emergency Save"-operations
	  * @param filename the name of the file the text is going to be saved into
	  */
	public void beforeSaveInto(String filename) {}

	/** text has been saved into another file <br>
	  * this event is fired in case of "Save As"-operations as well as in case of "Emergency Save"-operations
	  * @param filename the name of the file the text is going to be saved into
	  */
	public void afterSaveInto(String filename) {}

	/** command processor is going to be changed 
	  * @param operation the type of operation (REDCommandProcessorEvent.DO, REDCommandProcessorEvent.UNDO, REDCommandProcessorEvent.REDO, REDCommandProcessorEvent.CHECKPOINT)
	  */
	public void beforeCmdProcessorChange(int operation) {}
	
	/** command processor has changed 
	  * @param operation the type of operation (REDCommandProcessorEvent.DO, REDCommandProcessorEvent.UNDO, REDCommandProcessorEvent.REDO, REDCommandProcessorEvent.CHECKPOINT)
	  */
	public void afterCmdProcessorChange(int operation) {}
	
	/** selection is going to be changed  <br>
	  * at the time this event is fired, the old - values are still valid
	  * @param newFrom new beginning of selection
	  * @param newTo new end of selection
	  * @param oldFrom old beginning of selection
	  * @param oldTo old end of selection
	  */
	public void beforeSelectionChange(int oldFrom, int oldTo, int newFrom, int newTo) { }

	/** selection is going to be changed  <br>
	  * at the time this event is fired, the new - values are already valid
	  * @param newFrom new beginning of selection
	  * @param newTo new end of selection
	  * @param oldFrom old beginning of selection
	  * @param oldTo old end of selection
	  */
	public void afterSelectionChange(int oldFrom, int oldTo, int newFrom, int newTo) { }
	
	/** view has just gained focus */
	public void gotFocus() { }

	/** view has just lost focus */
	public void lostFocus() { }
	
	/** view mode is going to change
	  * @param oldMode old view mode
	  * @param newMode new view mode
	  */
	public void beforeModeChange(int oldMode, int newMode) { }

	/** view mode has changed 
	  * @param oldMode old view mode
	  * @param newMode new view mode
	  */
	public void afterModeChange(int oldMode, int newMode) { }
	
	/** a new file is going to be loaded into the editor 
	  * @param filename the file which will be loaded
	  */
	public void beforeFileLoad(String filename) { }
	
	/** a new file has been loaded into the editor 
	  * @param filename the file which has been loaded
	  */
	public void afterFileLoad(String filename) { }
	
	/** file is going to be saved in the editor 
	  * @param filename the file which will be saved
	  */
	public void beforeFileSave(String filename) { }

	/** file has been saved in the editor 
	  * @param filename the file which has be saved
	  */
	public void afterFileSave(String filename) { }

	/** text will no longer send style notifications from now on */
	public void beforeStyleBatchNotification() { }
	
	/** text will send style notifications again from now on */
	public void afterStyleBatchNotification() { }
}
