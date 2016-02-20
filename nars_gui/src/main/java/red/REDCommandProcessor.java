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

/** command processor - stores a finite number of REDCommand objects
  * also manages a freeze point to determine whether a text is modified
  * @author rli@chello.at
  * @tier system
  */
class REDCommandProcessor {
	/** Default capacity for command processor. */
	public final static int fcDefaultCapacity = 30;

	/** Create a new command processor with specified capacity. */
	public REDCommandProcessor (REDText text, int capacity) {
		fChangeCount = 0;
		fCheckPoint = 0;
		fSize = 0;
		fStart = 0;
		fCapacity = capacity;
		fCmd = new REDCommand[capacity];
		fText = text;
		fLastState = isModified();
	}
	
	/** Create a new command processor with default capacity. */
	public REDCommandProcessor (REDText text) {
		this(text, fcDefaultCapacity);
	}
	
	/** Add a listener to this text 
	  * @param listener The listener to be added to the text
	  * @return true if successful, false otherwise	
	  * @pre listener != null
	  */
	public boolean addREDCommandProcessorEventListener(REDCommandProcessorEventListener listener) {
		if (!fListeners.contains(listener)) {
			return fListeners.add(listener);
		}
		return false;
	}
	
	/** Remove a listener from this text 
	  * @param listener The listener to be removed from the text
	  * @return true if successful, false otherwise
	  * @pre listener != null
	  */
	public boolean removeREDCommandProcessorEventListener(REDCommandProcessorEventListener listener) {
		return fListeners.remove(listener);
	}
	
	/** Perform a command in this processor.
	  * This method will add cmd to the queue of commands, so that a consequent call to undo will call the command's undo method.
	  * @post canUndo()
	  */
	public void perform(REDCommand cmd) {
		callListeners(REDCommandProcessorEventListener.DO, true);
		finishFrom(fChangeCount);
		if (fSize >= fCapacity) {
			fStart++;
			fSize--;
		}
		fSize++;
		fCmd[fChangeCount % fCapacity] = cmd;
		cmd.doIt();
		fChangeCount++;
		callListeners(REDCommandProcessorEventListener.DO, false);
	}

	/** Can a command be undone.
	  * @return true, if this processor has got a command; false, otherwise
	  */
	public boolean canUndo() {
		REDCommand c = getAt(fChangeCount-1);
		return c != null;
	}

	/** Undo a command.
	  * @return true, if processor was able to undo the last command; false, otherwise
	  */
	public boolean undo() {
		if(!canUndo()) return false;
		callListeners(REDCommandProcessorEventListener.UNDO, true);		
		REDCommand c = getAt(fChangeCount-1);
		c.undoIt();
		fChangeCount--; 
		callListeners(REDCommandProcessorEventListener.UNDO, false);
		return true;
	}
	
	/** Can an undone command be redone.
	  * @return true, if this processor has got an undone command; false, otherwise
	  */
	public boolean canRedo() {
		REDCommand c = getAt(fChangeCount);
		return c != null;
	}
	
	/** Redo a command.
	  * @return true, if processor was able to redo the last undone command; false, otherwise
	  */
	public boolean redo() {
		if(!canRedo()) return false;
		callListeners(REDCommandProcessorEventListener.REDO, true);
		REDCommand c = getAt(fChangeCount);
		c.redoIt();
		fChangeCount++; 
		callListeners(REDCommandProcessorEventListener.REDO, false);
		return true;
	}
	
	/** Set check point.
	  * The check point defines the unmodified state of this processor, any number of undo/redo operations that returns to the
	  * state marked by the check point will leave the processor in unmodified state.
	  * Upon creation the check point is the empty processor (i.e. no commands performed)
	  * @post !isModified() 
	  */
	public void setCheckPoint() {
		callListeners(REDCommandProcessorEventListener.CHECKPOINT, true);
		fCheckPoint = fChangeCount;
		callListeners(REDCommandProcessorEventListener.CHECKPOINT, false);
	}
	
	/** Get modified state.
	  * @return true, if state of processor differs from checkpoint; false, otherwise
	  */
	public boolean isModified() {
		return fCheckPoint != fChangeCount;
	}

	/** Finish processor.
	  * This command will empty the processor's command queue.
	  * @post !isModified()
	  * @post !canUndo()
	  * @post !canRedo()
	  */
	public void finish() {
		finishFrom(fStart);
		setCheckPoint();
	}
	
	/** Get current change count. 
	  * The change count is an integer indicating the current state of the command processor. 
	  * If an operation is done (or redone) the change count will increase. If an operation is undone it willd decrease.
	  * This method will return the current change count.
	  * @return The current change count.
	  */
	public int getChangeCount() {
		return fChangeCount;
	}
	
	/** Get change count for check point. 
	  * The change count is an integer indicating the current state of the command processor. 
	  * If an operation is done (or redone) the change count will increase. If an operation is undone it willd decrease.
	  * This method will return the change count for the last check point.
	  * @return The change count for the check point.
	  */
	public int getCheckPointChangeCount() {
		return fCheckPoint;
	}
	
	
	private void finishFrom(int n) {
		REDCommand c = null;
	
		if (n < fCheckPoint) {
			fCheckPoint = -1;	// we can never reach the checkpoint again, so we'll stay modified until a new check point is set
		}
		
		c = getAt(n);
		while (c != null) {
			fCmd[n % fCapacity] = null;
			n++;
			c = getAt(n);
		}
		
		fSize -= (n-fChangeCount);
	}

	/**
	  * @pre op == REDCommandProcessorEventListener.DO || op == REDCommandProcessorEventListener.UNDO || op == REDCommandProcessorEventListener.REDO 
	  */
	private void callListeners(int op, boolean before) {
		if (before) {
			fLastState = isModified();
		}
		else {
			if (fLastState != isModified()) {
				REDTextServer.reportStateChange(fText, isModified());
			}
		}
		for (int j = 0; j < fListeners.size(); j++) {
			REDCommandProcessorEventListener listener = (REDCommandProcessorEventListener) fListeners.get(j);
			if (before) {
				listener.beforeCmdProcessorChange(op);
			}
			else {
				listener.afterCmdProcessorChange(op);
			}
		}
	}
	
	private REDCommand getAt(int i) {
		if (i >= fStart && i < fStart+fSize) {
			return fCmd[i % fCapacity];
		}
		return null;
	}
	
	/** The number of changes performed. */
	int fChangeCount;
	/** The number of changes stored in the processor. 0 <= fSize <= fCapacity. */
	int fSize;
	/** The index of the oldest command stored in the processor. */
	int fStart;
	/** The maximum number of commands that can be stored in the processor. */
	int fCapacity;
	/** The ringbuffer array. */
	REDCommand [] fCmd;
	/** The freezepoint change index. */
	int fCheckPoint;
	/** The registered listeners. */
	ArrayList fListeners;
	/** The text that is associated with this processor. */
	REDText fText;
	/** Auxiliary variable to be able to report state transitions from "modified" to "not modified" and vice versa. */
	boolean fLastState;
	{
		fListeners = new ArrayList(REDAuxiliary.fcListenerSize);
	}
}
