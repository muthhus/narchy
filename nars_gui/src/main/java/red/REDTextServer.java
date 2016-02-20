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

/** REDTextServer - Singleton which manages pool of texts and deals with model sharing
  * @author rli@chello.at
  * @tier API
  */
public class REDTextServer {
	/** Acquire text for file by name. 
	  * @param fullFileName path and filename to return as text. If "" is passed a new instance of REDText is always returned, regardless of <CODE>privateCopy</CODE>.
	  * @param privateCopy <BR> &nbsp; true: a new REDText object will be allocated under all circumstances
	    <BR> &nbsp; false: if a REDText object already exists for this filename it will be returned (shared)
	  */
	public static REDText acquireText(String fullFileName, boolean privateCopy) {
		if (fullFileName.isEmpty()) {
			return new REDText("");
		}

		REDText text = null;
		REDTextWrapper w = (REDTextWrapper) fModels.get(fullFileName);
		if (w == null) {
			text = new REDText(fullFileName);			
			w = new REDTextWrapper(text, privateCopy);
			fModels.put(fullFileName, w);
		}
		else {
			w.incRefCount(privateCopy);
			if (privateCopy) {
				text = new REDText(fullFileName);
			}
			else {
				text = w.getText();
				if (text == null) {	// private copy acquired first, now we need to create the shared copy
					text = new REDText(fullFileName);
					w.setText(text);
				}
			}
		}
		return text;
	}
	
	/** Release acquired text.
	  * @param text The text that is no longer needed.
	  */
	public static void releaseText(REDText text) {
		if (text == null || text.getFilename().isEmpty()) return;
		String name = text.getFilename();
		REDTextWrapper w = (REDTextWrapper) fModels.get(name);
		w.decRefCount(text);
		if (w.getSharedRefCount() <= 0) {
			w.setText(null);
			reportStateChange(text, false);
			if (w.getPrivateRefCount() <= 0) {
				fModels.remove(name);
			}
		}
	}
	
	/** Get shared text ref count.
	  * @param fullFileName The name of the file to get shared ref count for.
	  * @return The number of shared text copies acquired for the given fullFileName
	  */
	public static int getSharedRefCount(String fullFileName) {
		REDTextWrapper w = (REDTextWrapper) fModels.get(fullFileName);
		if (w == null) {
			return 0;
		}
		else {
			return w.getSharedRefCount();
		}
	}
	
	/** Get load status of a specific text.
	  * @param fullFileName the name of the file to check for
	  * @param usePrivateCopies return true also if only a private copy of the text exists
	  * @return <br>&nbsp;true: if a copy of the text is currently in memory <br>&nbsp;false: otherwise
	  */
	public static boolean isTextLoaded(String fullFileName, boolean usePrivateCopies) {
		REDTextWrapper w = (REDTextWrapper) fModels.get(fullFileName);
		return w != null && (w.getText() != null || usePrivateCopies);
	}
	
	/** Get modified status of a specific text. Note that it is not possible to see if private copies of a text are loaded and modified.
	  * @param fullFileName the name of the file to check for
	  * @return <code>true</code> if a copy of the text is currently modified; <code>false</code> otherwise.
	  */
	public static boolean isTextModified(String fullFileName) {
		REDTextWrapper w = (REDTextWrapper) fModels.get(fullFileName);
		if (w != null) {
			REDText text = w.getText();
			if (text != null) {
				return text.getCommandProcessor().isModified();
			}
		}
		return false;
	}

	/** Get loaded text filenames Iterator.
	  * @param Iterator An Iterator over the loaded text names. Will iterate in ascending order.
	 */	
	public static Iterator getTextsFilenameIterator() {
		return fModels.keySet().iterator();
	}
	
	// Listeners start
	private final static int LI_STATE = 1;
	private final static int LI_SAVED = 2;
	
	/** Add a text server event listener. 
	  * @param listener The listener to add.
	  */
	public static void addTextServerEventListener(REDTextServerEventListener listener) {
		if (!fListeners.contains(listener)) {
			fListeners.add(listener);
		}
	}
	
	/** Remove a text server event listener.
	  * @param listener The listener to remove.
	  */
	public static void removeTextServerEventListener(REDTextServerEventListener listener) {
		fListeners.remove(listener);
	}
	
	/** Auxiliary method to call listeners 
	  * @param op The operation to call listeners for. One of the <CODE>LI_...</CODE> constants
	  * @param filename The name of the file affected by the operation
	  */
	static private void callListeners(int op, String filename, boolean modified) {
		for (int j = 0; j < fListeners.size(); j++) {
			REDTextServerEventListener listener = (REDTextServerEventListener) fListeners.get(j);
			switch(op) {
				case LI_STATE:
					listener.textStateChanged(filename, modified);
				break;
				case LI_SAVED:
					listener.textSaved(filename);
				break;
			}
		}
	}
	
	static void reportSave(REDText text) {
		callListeners(LI_SAVED, text.getFilename(), true);		
	}
	
	static void reportStateChange(REDText text, boolean modified) {
		callListeners(LI_STATE, text.getFilename(), modified);
	}
	// Listeners end
		
	/** Maps filenames to REDTextWrapper objects. */
	private static final Map fModels = new TreeMap();
	
	/** Holds event listeners. */
	private static final ArrayList fListeners = new ArrayList();
}
