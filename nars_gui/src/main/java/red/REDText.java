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

import java.io.*;
import java.util.*;

import red.lineTree.*;
import red.file.*;
import red.rexParser.*;

/** Piece list text implementation.
  * @author rli@chello.at
  * @tier system
  * @invariant testClassInvariant()
  * @invariant testClassInvariantStyle()
  */
class REDText implements REDRexLineSource {
	/** Create and a load a text.
	 * @param filename if this parameter is != "", then the text is loaded from disk. 
	 * @pre filename != null
	 */
	public REDText(String filename) {
		fScratchRider = new REDFileRider(REDFile.getUniqueTmpFile());
		fDefaultStyle = REDStyleManager.getDefaultStyle();
		fLineTree = new REDLineTree();
		fListeners = new ArrayList(REDAuxiliary.fcListenerSize);
		fCmdP = new REDCommandProcessor(this);	// TBD: make cmdP optional
		fModLock = false;
		fOpQ = new ArrayList(3);
		fCachePos = -1;
		fFilename = filename;
		fStyleBatchNotification = false;
		if (fFilename != "") {
			load();
		}
	}
	
	/** Get text as string. This method may require much time/memory if the text is long.
	 * @return the text as string
	 * @post return != null
	 * @post return.length() == length()
	 */
	public String asString() {
		StringBuffer buf = new StringBuffer(length());
		REDRun cur = fHead;
		while (cur != null) {
			buf.append(cur.asString());
			cur = cur.fNext;
		}
		return new String(buf);
	}
	
	/** Get part of the text as string.
	  * The parameters from and to are normalized to be in range: [0, fLength] 
	  * @param from The beginning of the stretch of text to be returned; for from == n, the nth character is included.
	  * @param to The end of the stretch of text to be returned; for to == n, the nth character is not included.
	  * @return The stretch [from, to[ as String.
	  * @post from >= 0 && to <= fLength implies return.length() == to - from
	  * @post return != null
	  */
	public String asString(int from, int to) {
		from = Math.max(from, 0); from = Math.min(from, fLength);
		to = Math.max(to, 0); to = Math.min(to, fLength); to = Math.max(to, from);
		if (from >= to) {
			return "";
		}
		
		int len = to - from;
		byte [] buf = new byte[len];
		
		REDRunSpec spec = findPrevRun(from, null);
		int start = spec.fRun.copyInto(buf, 0, len, spec.fOff);
		while (start != len && spec.fRun.fNext != null) {
			spec.fRun = spec.fRun.fNext;
			start += spec.fRun.copyInto(buf, start, len, 0);
		}
			
		return new String(buf);
	}

	/** Return part of the text as byte - array.
	  * @param from The beginning of the stretch of text to be returned; for from == n, the nth character is included.
	  * @param to The end of the stretch of text to be returned; for to == n, the nth character is not included.
	  * @param arr if this parameter != null, try to reuse the passed array
	  * @return An array containing [from, to[ or null, if [from, to[ would be empty.
	  */
	public byte[] asBytes(int from, int to, byte[] arr) {
		from = Math.max(from, 0); from = Math.min(from, fLength);
		to = Math.max(to, 0); to = Math.min(to, fLength); to = Math.max(to, from);
		if (from >= to) {
			return null;
		}
		int size = to - from;
		if (arr == null || arr.length < size) {
			arr = new byte[size];
		}
		REDRunSpec spec = findPrevRun(from, null);
		int done = spec.fRun.copyInto(arr, 0, size, spec.fOff);
		while (done < size) {
			spec.fRun = spec.fRun.fNext;
			done += spec.fRun.copyInto(arr, done, size, 0);
		}
		return arr;
	}

	/** Get character at position.
	  * @param pos The position to get character for
	  * @return The character at the given position or \0, if pos >= length
	  */
	public byte charAt(int pos) {
		if (pos >= length()) {
			return '\0';
		}
		else {
			pos = Math.max(0, pos);
			REDRunSpec spec = findNextRun(pos, null);
			return spec.fRun.charAt(spec.fOff);
		}
	}
	
	/** Replace text by string.
	 * This method deletes the stretch [from, to[ and inserts String s at from.
	 * from and to are normalized to be in range: [0, fLength] and to is normalized to be >= from
	 * @param from Position to replace from
	 * @param to Position to replace to
	 * @param s String to insert. may be null
	 * @return <br>&nbsp;true: operation has been executed
	   <br>&nbsp;false: operation has been queued for deferred execution
	 */
	public boolean replace(int from, int to, String s) {
		// parameter normalization
		from = Math.max(from, 0); from = Math.min(from, fLength);
		to = Math.max(to, 0); to = Math.min(to, fLength); to = Math.max(to, from);
		if (!acquireModificationLock()) {
			queueOperation(new ReplaceQueueEntry(from, to, s));
			return false;
		}
		
		if (to > from) {
			delete(from, to);
		}
		if (s == null || s.isEmpty()) {
			releaseModificationLock();
			return true;
		}
		
		to = from + s.length();
		callListeners(LI_BEFOREINSERT, from, to);				
		REDRunPair split = splitRun(from);
		REDStyle style;
		if (split.fBefore != null) {
			style = split.fBefore.fStyle;
		}
		else if (split.fAfter != null) {
			style = split.fAfter.fStyle;
		}
		else {
			style = getDefaultStyle();
		}
		REDRun r = new REDRun(fScratchRider, s, style);
		r.fPrev = split.fBefore;
		r.fNext = split.fAfter;
		fLength += r.length();
		if (split.fBefore != null) {
			split.fBefore.fNext = r;
		}
		else {
			fHead = r;
		}
		if (split.fAfter != null) {
			split.fAfter.fPrev = r;
		}
		fCache = r;
		fCachePos = from;
		if (split.fBefore != null) {
			mergeRuns(split.fBefore, split.fAfter);
		}
		else {
			mergeRuns(r, split.fAfter);
		}
		fLineTree.notifyInsert(from, s.length(), tokenize(from, to));
//		fLineTree.notifyInsert(from, s.length(), tokenize(s));
		callListeners(LI_AFTERINSERT, from, to);				
		releaseModificationLock();
		return true;
	}
	
	boolean hasStyle(int from, int to, REDStyle style) {
		REDRunSpec spec = findPrevRun(from, null);
		REDRun cur = spec.fRun;
		int curPos = spec.fOrg;
		while (curPos < to) {
			if (cur.fStyle != style) {
				return false;
			}
			curPos += cur.fLength;
			cur = cur.fNext;
		}
		return true;
	}
	
	/** Set style for a stretch of the text. 
	  * from and to have gap semantics, i.e. to set the style for the first character of a text you have to call setStyle(0, 1, style)
	  * from and to are normalized to be in range: [0, fLength] 	
	  * @param from startposition of stretch to set style for
	  * @param to endposition of stretch to set style for
	  * @param style to set
	  * @return <CODE>true</CODE>: operation has been executed; <CODE>false</CODE>: operation has been queued for deferred execution
	  * @pre style != null
	  * @post return == true implies forall int x in from+1 .. to | getStyle(x) == style
	  */
	public boolean setStyle(int from, int to, REDStyle style) {
		// parameter normalization
		from = Math.max(from, 0); from = Math.min(from, fLength);
		to = Math.max(to, 0); to = Math.min(to, fLength); to = Math.max(to, from);
		if (to <= from) {
			return true;
		}
		if (!acquireModificationLock()) {
			queueOperation(new SetStyleQueueEntry(from, to, style));
			return false;
		}
		if (hasStyle(from, to, style)) {
			releaseModificationLock();
			return true;
		}
		callListeners(LI_BEFORESTYLECHANGE, from, to, style);				
		REDRunPair start = splitRun(from);
		REDRunPair end = splitRun(to);
		REDRun cur = start.fAfter;
		fCachePos = from;
		fCache = start.fAfter;
		
		while (cur != end.fAfter) {
			cur.fStyle = style;
			fCachePos += cur.fLength;
			cur = cur.fNext;
			fCache = cur;
		}
		if (fCache == null) {
			fCache = fHead;
			fCachePos = 0;
		}
		REDRun s, e;
		s = start.fBefore;
		if (s == null) {
			s = start.fAfter;
		}
		e = end.fAfter;
		if (e == null) {
			e = end.fBefore;
		}
		REDAssert.ensure(s != null && e != null);
		mergeRuns(s, e);
		callListeners(LI_AFTERSTYLECHANGE, from, to, style);
		releaseModificationLock();
		return true;
	}

	/** Get style for text position.
	  * pos is normalized to be in range: [0, fLength] 	
	  * if pos lies between two runs the style of the first run is returned
	  * @param pos position to get style for
	  * @return style at given position 
	  * @post return != null
	  */
	REDStyle getStyle(int pos) {
		pos = Math.max(pos, 0); pos = Math.min(pos, fLength);
		REDRunSpec r = findPrevRun(pos, null);
		if (r.isValid()) {
			return r.fRun.fStyle;
		}
		else {
			return fDefaultStyle;
		}
	}

	/** Get length of text. 
	  * @return The length of the text. An empty text has length 0.
	  */
	public int length() {
		return fLength;
	}
	
	/** (Re)load file from disk. */
	public void load() {
		// this routine could be much faster (O(1)), if only windows hadn't such a $&@! locking semantics
		callListeners(LI_BEFORELOAD);				
		File f = new File(fFilename); 
		if (!f.canRead()) return;	// @tbd throw an exception if we cannot read
		REDFile srcFile = new REDFile(fFilename, true);
		REDFile.copyFile(srcFile, fScratchRider.getFile());
		srcFile.close();
		fLength = fScratchRider.getFile().length();
		if (fLength > 0) {
			fHead = new REDRun(fScratchRider, 0, fLength, getDefaultStyle());
			fCache = fHead; fCachePos = 0;
			fLineTree.notifyInsert(0, fLength, tokenize(0, fLength));
		}
		else {
			fHead = null;
			fCache = fHead; fCachePos = 0;
			fLineTree = new REDLineTree();
		}
		setCurTypingCommand(null);
		fCmdP.finish();
		callListeners(LI_AFTERLOAD);
	}
	
	/** Save text into another file.
	  * Note that the text will remain associated with its original file, i.e. for <CODE>REDText t = new REDText("B"); </CODE> calling 
	  * <CODE>saveInto("A"); saveInto("A"); is not equal to
	  * these operations: <CODE>saveInto("A"); save();</CODE>,  because save() will still save into "B".
	  * @param filename The name of the file to save this text's content into.
	  */
	public void saveInto(String filename) {
		callListeners(LI_BEFORESAVEINTO, filename);				
		REDFile saveFile = new REDFile(filename);
		REDFileRider saveRider = new REDFileRider(saveFile); 
		saveFile.purge();
		REDRun cur = fHead;
		while (cur != null) {
			cur.copyInto(saveRider);
			cur = cur.fNext;
		}
		saveFile.close();
		callListeners(LI_AFTERSAVEINTO, filename);				
	}
	
	/** Save text into file.
	  * You may not call this operation for anonymous texts.
	  * @pre !fFilename.equals("")
	  */
	public void save() {
		callListeners(LI_BEFORESAVE);		
		REDFile saveFile = new REDFile(fFilename);
		REDFileRider saveRider = new REDFileRider(saveFile); 
		saveFile.purge();
		REDRun cur = fHead;
		while (cur != null) {
			cur.copyInto(saveRider);
			cur = cur.fNext;
		}
		saveFile.close();
		REDTextServer.reportSave(this);
		setCurTypingCommand(null);
		fCmdP.setCheckPoint();
		callListeners(LI_AFTERSAVE);				
	}
	
	/** Get default text style.
	  * This method returns the style a text has after loading.
	  * @return A REDStyle object representing the default style of this text.
	  */
	public REDStyle getDefaultStyle() {
		return fDefaultStyle;
	}
	
	/** Set default text style.
	  * @param style A REDStyle object representing the default style of this text.
	  */
	public void setDefaultStyle(REDStyle style) {
		fDefaultStyle = style;
	}
	
	/** Get start of line.
	 * lineNr is normalized to be in range: [0, nrLines] 
	 * @param lineNr Number of line to get start position for. The first line has number 0.
	 * @return the start position of the line
	 * @post return >= 0 && return <= length()
	 */
	public int getLineStart(int lineNr) {
		if (lineNr < 0) lineNr = 0;
		if (lineNr >= getNrOfLines()) {
			return length();
		}
		return fLineTree.getLineStart(lineNr);
	}
	
	/** Get end of line without linebreak character(s).
 	  * @param lineNr Number of line to get end position for. First line has number 0.
	  * @return The end position of the given line. If <Code>line &lt; 0</Code> 0 is returned. If <Code>line &gt; getNrOfLines(), length()</Code> is returned.
	  * @post return >= 0 && return <= length()
	  */
	public int getLineEnd(int lineNr) {
		if (lineNr < 0) return 0;
		if (lineNr+1 >= getNrOfLines()) {
			return length();
		}
		int retVal = getLineStart(lineNr+1);
		if (retVal > 1 && asString(retVal-2, retVal).equals("\r\n")) {
			retVal -= 2;
		}
		else if (retVal > 0 && asString(retVal-1, retVal).equals("\r") || asString(retVal-1, retVal).equals("\n")) {
			retVal--;
		}
		return retVal;
	}
	
	/** Get length of line.
	 * lineNr is normalized to be in range: [0, nrLines]. The length of the line "nrLines" is always 0.
	 * @param lineNr Number of line to get start position for. First line has number 0.
	 * @param includingLinebreak true: return length including linebreak characters; false: return length without linebreak characters
	 * @return the length of the line
	 * @post return >= 0
	 */
	public int getLineLength(int lineNr, boolean includingLinebreak) {
		if (lineNr < 0) lineNr = 0;
		int retVal;
		if (includingLinebreak) {
			retVal = getLineStart(lineNr+1) - getLineStart(lineNr);
		}
		else {
			retVal = getLineEnd(lineNr) - getLineStart(lineNr);
		}
		return retVal;
	}
	
	/** Get line for a text position.
	  * This method returns the number of the line the given text position is currently in.
	  * @param pos The position to get line number for.
	  * @return The line number, <CODE>pos</CODE> is currently in.
	  */
	public int getLineForPosition(int pos) {
		if (pos < 0) pos = 0;
		return fLineTree.getLineForPosition(pos);
	}

	/** Add a listener to this text.
	  * @param listener The listener to be added to the text
	  * @return true if successful, false otherwise	
	  * @pre listener != null
	  */
	public boolean addREDTextEventListener(REDTextEventListener listener) {
		if (!fListeners.contains(listener)) {
			return fListeners.add(listener);
		}
		return false;
	}
	
	/** Remove a listener from this text.
	  * @param listener The listener to be removed from the text
	  * @return true if successful, false otherwise
	  * @pre listener != null
	  */
	public boolean removeREDTextEventListener(REDTextEventListener listener) {
		return fListeners.remove(listener);
	}
	
	/** Return number of lines in text.
	  * @return nr of lines in text; an empty text has 1 line.
	  * @post return >= 1
	  */
	public int getNrOfLines() {
		return fLineTree.getNrNodes();
	}

	/** Get view stretch.
	  * @param from start of stretch
	  * @param stretch if this parameter != null then the passed stretch object is reused
	  * @return a view stretch
	  * @pre pos >= 0
	  * @pre pos <= length()
	  * @post return != null
	  * @post stretch != null implies return == stretch
	  */
	public REDViewStretch getViewStretch(int pos, REDViewStretch stretch) {
		return getViewStretch(pos, stretch, false);
	}
	
	/** Get view stretch.
	  * @param from start of stretch
	  * @param stretch if this parameter != null then the passed stretch object is reused
	  * @param whiteSpaceViz if this parameter is true, tabs are returned one by one and spaces are returned as separate text stretches
	  * @return a view stretch
	  * @pre pos >= 0
	  * @pre pos <= length()
	  * @post return != null
	  * @post stretch != null implies return == stretch
	  */
	public REDViewStretch getViewStretch(int pos, REDViewStretch stretch, boolean whiteSpaceViz) {
		if (stretch == null) {
			stretch = new REDViewStretch();
		}
		if (pos >= length()) {
			stretch.fType = REDViewStretch.EOF;
			stretch.fRunSpec = findPrevRun(length(), stretch.fRunSpec);
			if (stretch.fRunSpec.isValid()) {
				stretch.fStyle = stretch.fRunSpec.fRun.fStyle;
			}
			else {
				stretch.fStyle = getDefaultStyle();
			}
			stretch.fLength = 0;
			return stretch;
		}
		stretch.fRunSpec = findNextRun(pos, stretch.fRunSpec);
		stretch.fStyle = stretch.fRunSpec.fRun.fStyle;
		stretch.fLength = 0;
		byte c = stretch.fRunSpec.fRun.getCharAt(stretch.fRunSpec.fOff);
		if (c == '\t') {
			stretch.fType = REDViewStretch.TAB;	
			while (c == '\t' && stretch.fRunSpec.fOff < stretch.fRunSpec.fRun.fLength && (!whiteSpaceViz || stretch.fLength == 0) ) {	
				stretch.fRunSpec.fOff++; stretch.fLength++;
				if (stretch.fRunSpec.fOff < stretch.fRunSpec.fRun.fLength) {
					c = stretch.fRunSpec.fRun.getCharAt(stretch.fRunSpec.fOff);
				}
			}
		}
		else if (c == '\r' || c == '\n') {
			stretch.fType = REDViewStretch.LINEBREAK;
			stretch.fLength = 1;
			if (c == '\r' && stretch.fRunSpec.fOff < stretch.fRunSpec.fRun.fLength && stretch.fRunSpec.fRun.getCharAt(stretch.fRunSpec.fOff+1) == '\n') {
				stretch.fLength++;
			}
		}
		else {
			stretch.fType = REDViewStretch.TEXT;
			if (whiteSpaceViz && c == ' ') {
				stretch.fLength = 1;
			}				
			else {
				stretch.fLength = stretch.fRunSpec.fRun.findWhitespace(stretch.fRunSpec.fOff, whiteSpaceViz) - stretch.fRunSpec.fOff;
			}
		}
		return stretch;
	}
	
	/** Set current typing command.
	  * A typing command combines consecutive input characters.
	  * This method allows to set the current typing command. Most of the time this will be called with <CODE>null</CODE> to end the
	  * current typing command.
	  * @param cmd The new current typing command.
	  */
	public void setCurTypingCommand(REDTextCommand cmd) {
		fCurTypingCmd = cmd;
	}
	
	/** Get current typing command.
	  * A typing command combines consecutive input characters.
	  * @param The current typing command.
	  */
	public REDTextCommand getCurTypingCommand() {
		return fCurTypingCmd;
	}

	/** Set undo/redo view.
	  * The undo/redo view is used to visualize undo or redo operations (by selecting the affected part of the text)
	  * It must be reset prior to every und/redo operation.
	  * @param v The new undo/redo view.
	  */
	public void setUndoRedoView(REDView v) {
		fUndoRedoView = v;
	}

	/** Get undo/redo view.
	  * The undo/redo view is used to visualize undo or redo operations (by selecting the affected part of the text)
	  * @return The undo/redo view.
	  */
	public REDView getUndoRedoView() {
		return fUndoRedoView;
	}
	
	/** Get command processor.
	  * The command processor is also known as undo/redo queue. It manages REDCommand objects.
	  * @return The command processor of this text.
	  */
	public REDCommandProcessor getCommandProcessor() {
		return fCmdP;
	}
	
	/** Get filename.
	  * @return The name of the file this text is associated with.
	  */
	public String getFilename() {
		return fFilename;
	}	
	
	// --- REDRexLineSource interface
	public char [] getLine(int lineNr, char [] reuse) {
		if (lineNr < 0 || lineNr >= getNrOfLines()) {
			return null;
		}
		else {
			int len = getLineLength(lineNr);
			if (reuse == null || reuse.length < len) {
				reuse = new char[len];
			}
			// tbd: optimize me !!!
			int idx = 0;
			int pos = getLineStart(lineNr);
			char c = (char) charAt(pos);
			while (c != '\0' && !REDAuxiliary.isLineBreak((byte) c)) {
				reuse[idx++] = c; 
				pos++;
				c = (char) charAt(pos);
			}
			while (c != '\0' && idx < len && REDAuxiliary.isLineBreak((byte) c)) {
				reuse[idx++] = c; 
				pos++;
				c = (char) charAt(pos);
			}

			return reuse;
		}
	}
	
	public int getLineLength(int lineNr) {
		return getLineLength(lineNr, true);
	}
	
	/** Start style batch notification mode.
	  * After style batch notification mode has been entered,  notifications about style changes in the text will not be sent out to listeners until batchStyleNotificationEnd is called.
	  * You must not call batchStyleNotificationStart, if this mode is already in place (i.e. nested calls are disallowed).
	  */
	public void batchStyleNotificationStart() {
		if (fStyleBatchNotification) {
			throw new Error("Disallowed nested call of batchStyleNotificationStart.");
		}
		callListeners(LI_BEFOREBATCHNOTIFICATION);
		fStyleBatchNotification = true;
	}
	
	/** End style batch notification mode.
	  * This method ends style batch notification mode, thus (re-)enabling normal notifications.
	  * You must not call batchStyleNotificationEnd, without this mode in place
	  */
	public void batchStyleNotificationEnd() {
		if (!fStyleBatchNotification) {
			throw new Error("Call of batchStyleNotificationEnd without batch mode being active.");
		}
		fStyleBatchNotification = false;
		callListeners(LI_AFTERBATCHNOTIFICATION);
	}
	
	/** Get style batch notification mode status
	  * @return <CODE>true</CODE>, if style batch notification mode is in place, <CODE>false</CODE> otherwise
	  */
	public boolean hasStyleBatchNotification() {
		return fStyleBatchNotification;
	}
		
	// *******************************************************************************************************************************************************
	// P R I V A T E - L I N E
	// *******************************************************************************************************************************************************
	
	private static final int LI_BEFOREDELETE = 0;
	private static final int LI_AFTERDELETE = 1;
	private static final int LI_BEFOREINSERT = 2;
	private static final int LI_AFTERINSERT = 3;
	private static final int LI_BEFORESTYLECHANGE = 4;
	private static final int LI_AFTERSTYLECHANGE = 5;
	private static final int LI_BEFORELOAD = 6;
	private static final int LI_AFTERLOAD = 7;
	private static final int LI_BEFORESAVE = 8;
	private static final int LI_AFTERSAVE = 9;
	private static final int LI_BEFORESAVEINTO = 10;
	private static final int LI_AFTERSAVEINTO = 11;
	private static final int LI_BEFOREBATCHNOTIFICATION = 12;
	private static final int LI_AFTERBATCHNOTIFICATION = 13;
	
	
	/** Auxiliary method to call listeners. */
	private void callListeners(int op, int from, int to) {
		callListeners(op, from, to, null, null);
	}
	
	/** Auxiliary method to call listeners. */
	private void callListeners(int op, int from, int to, REDStyle style) {
		callListeners(op, from, to, style, null);
	}

	/** Auxiliary method to call listeners. */
	private void callListeners(int op) {
		callListeners(op, 0, 0, null, null);
	}

	/** Auxiliary method to call listeners. */
	private void callListeners(int op, String filename) {
		callListeners(op, 0, 0, null, filename);
	}

	/** Auxiliary method to call listeners. */
	private void callListeners(int op, int from, int to, REDStyle style, String filename) {
//		REDTracer.info("red", "REDText", "Text call listeners: " + op);
		for (int i = REDTextEventListener.RLL_VIEW; i <= REDTextEventListener.RLL_LATE; i++) {
			for (int j = 0; j < fListeners.size(); j++) {
				REDTextEventListener listener = (REDTextEventListener) fListeners.get(j);
				if (listener.getListenerLevel() == i) {
					switch (op) {
						case LI_BEFOREDELETE:
							listener.beforeDelete(from, to);
						break;
						case LI_AFTERDELETE:
							listener.afterDelete(from, to);
						break;
						case LI_BEFOREINSERT:
							listener.beforeInsert(from, to);
						break;
						case LI_AFTERINSERT:
							listener.afterInsert(from, to);
						break;
						case LI_BEFORESTYLECHANGE:
							if (!fStyleBatchNotification) {
								listener.beforeStyleChange(from, to, style);
							}
						break;
						case LI_AFTERSTYLECHANGE:
							if (!fStyleBatchNotification) {
								listener.afterStyleChange(from, to, style);
							}
						break;
						case LI_BEFORELOAD:
							listener.beforeLoad();
						break;
						case LI_AFTERLOAD:
							listener.afterLoad();
						break;
						case LI_BEFORESAVE:
							listener.beforeSave();
						break;
						case LI_AFTERSAVE:
							listener.afterSave();
						break;
						case LI_BEFORESAVEINTO:
							listener.beforeSaveInto(filename);
						break;
						case LI_AFTERSAVEINTO:
							listener.afterSaveInto(filename);
						break;
						case LI_BEFOREBATCHNOTIFICATION:
							listener.beforeStyleBatchNotification();
						break;
						case LI_AFTERBATCHNOTIFICATION:
							listener.afterStyleBatchNotification();
						break;							
					}
				}
			}
		}
	}
	
	/** Auxiliary method to delete part of the text. 
	  * from and to have gap semantics.
	  * @param from start of the stretch to be deleted.
	  * @param to end of the stretch to be deleted.	  
	  */
	private void delete(int from, int to) {
		callListeners(LI_BEFOREDELETE, from, to);				
		REDRunPair start = splitRun(from);
		REDRunPair end = splitRun(to);
		if (start.fBefore != null) {
			start.fBefore.fNext = end.fAfter;
			if (end.fAfter != null) {
				end.fAfter.fPrev = start.fBefore;
			}
		}
		else {
			fHead = end.fAfter;
			if (end.fAfter != null) {
				end.fAfter.fPrev = fHead;
			}
		}
		if (end.fAfter != null) {
			fCache = end.fAfter;
			fCachePos = from;
		}
		else {
			fCache = fHead;
			fCachePos = 0;
		}
		fLength = fLength - to + from;
		if (fLength == 0) {
			fScratchRider.getFile().purge();
		}
		fLineTree.notifyDelete(from, to);
		callListeners(LI_AFTERDELETE, from, to);				
	}
	
	/** Find the run which contains given position.
	 * caveat: if the given position lies between run a and b, a is returned
	 * @param pos The position to find the run for
	 * @return A run specification representing the found run. May be invalid (if given position was larger than text)
	 * @pre pos >= 0
	 * @pre pos <= length()
	 * @post return != null
	 * @post return.fOff > 0 || pos == 0
	 * @post return.fOff <= return.fRun.fLength
	 * @post return.fOrg >= 0
	 */
	private REDRunSpec findPrevRun(int pos, REDRunSpec spec) {
		REDRun cur = fCache;
		int curPos = fCachePos;
//		REDRun cur = fHead;
//		int curPos = 0;
		
		if (pos == 0) {
			cur = fHead;
			curPos = 0;
		}
		else {
			while (cur != null && pos - curPos > cur.fLength) {
				curPos += cur.fLength;
				cur = cur.fNext;
			}
			while (cur != null && pos - curPos <= 0) {
				cur = cur.fPrev; 
				curPos -= cur.fLength;
			}
		}
		
		fCache = cur;
		fCachePos = curPos;
		
		if (spec == null) {
			spec = new REDRunSpec();
		}
		spec.fRun = cur;
		spec.fOrg = curPos;
		spec.fOff = pos - curPos;
	
		return spec;	
	}
	
	/** Find the run which contains given position.
	 * caveat: if the given position lies between run a and b, b is returned
	 * @param pos The position to find the run for
	 * @return A run specification representing the found run. May be invalid (if given position was larger than text)
	 * @pre pos >= 0
	 * @pre pos <= length()
	 * @post return != null
	 * @post return.fOff >= 0
	 * @post return.fOff < return.fRun.fLength
	*/
	private REDRunSpec findNextRun(int pos, REDRunSpec spec) {
		if (pos < fLength) {
			spec = findPrevRun(pos+1, spec);
			spec.fOff--;
		}
		else {
			spec = findPrevRun(pos, spec);
		}
		return spec;
	}

	/** Split run at pos and return pair of runs. */
	private REDRunPair splitRun(int pos) {
		REDRunPair p = new REDRunPair();
		if (pos == 0) {
			p.fBefore = null;
			p.fAfter = fHead;
		}
		else {
			REDRunSpec spec = findPrevRun(pos, null); REDAssert.ensure(spec.isValid());
			p.fBefore = spec.fRun;
			int len = spec.fRun.length();
			if (spec.fOff != len) { // need to split
				p.fAfter = new REDRun(p.fBefore.fBuffer, p.fBefore.fOrg + spec.fOff, p.fBefore.fLength - spec.fOff, p.fBefore.fStyle);
				p.fBefore.fLength = spec.fOff;
				p.fAfter.fNext = p.fBefore.fNext;
				if (p.fAfter.fNext != null) {
					p.fAfter.fNext.fPrev = p.fAfter;
				}
				p.fBefore.fNext = p.fAfter;
				p.fAfter.fPrev = p.fBefore;
			}
			else { // we already have a split
				p.fAfter = p.fBefore.fNext;
			}
		}
		return p;
	}
	
	/** Merge all runs between start and end where possible.
	  * @pre start != null
	  */
	private void mergeRuns(REDRun start, REDRun end) {
		REDRun cur = start;
		REDRun next = cur.fNext;
		
		// tbd: what about three consecutive runs that could be merged ... can that be happening ?
		while (cur != end && next != null) {
			if (cur.isMergeableWith(next)) {
				if (next == fCache) {
					fCache = cur;
					fCachePos -= cur.fLength;
				}
				cur.fLength += next.fLength;
				cur.fNext = next.fNext;
				if (cur.fNext != null) {
					cur.fNext.fPrev = cur;
				}
			}
			if (next == end) {
				next = null;
			}
			else {
				cur = cur.fNext;
				if (cur != null) {
					next = cur.fNext;
				}
				else {
					next = null;
				}
			}
		}
	}
	
	/** Split String into lines including linebreak character, adapt line tree.
	  * StringTokenizer wont cut it: It cannot use \r\n as delimiter :-(
	  * @post forall REDLineTreeData e in return | e.getPosition() >= from@pre && e.getPosition() <= to@pre
	  */
	private ArrayList tokenize(int from, int to) {
		int nrLines = 1;
		ArrayList retVal = new ArrayList();
		REDRunSpec runSpec = null;
		while (from < to) {
			runSpec = findNextRun(from, runSpec);
			from = runSpec.fRun.findNextLine(runSpec.fOff) + runSpec.fOrg;

			retVal.add(new REDLineTreeData(Math.min(from, to), nrLines));
			nrLines++;
		}
		String lastChar = asString(to-1, to);
		if (lastChar.equals("\n") || lastChar.equals("\r") && !asString(to, to+1).equals("\n")) {
			retVal.add(new REDLineTreeData(to, nrLines));
		}
		return retVal;
	}
	
	// --- Deferred execution
	/** QueueEntry is an auxiliary class used to stored deferred calls to methods that will change the text. */
	abstract static class QueueEntry {
		abstract public void execute();
	}	

	/** SetStyleQueueEntry represents deferred setStyle method calls */	
	class SetStyleQueueEntry extends QueueEntry {
		public SetStyleQueueEntry(int from, int to, REDStyle s) {
			fFrom = from; 
			fTo = to;
			fStyle = s;
		}
		
		public void execute() {
			setStyle(fFrom, fTo, fStyle);
		}
		int fFrom, fTo;
		REDStyle fStyle;
	}
	
	/** ReplaceQueueEntry represents deferred replace method calls */
	class ReplaceQueueEntry extends QueueEntry {
		public ReplaceQueueEntry(int from, int to, String s) {
			fFrom = from; 
			fTo = to;
			fString = s;
		}
		
		public void execute() {
			replace(fFrom, fTo, fString);
		}
		int fFrom, fTo;
		String fString;
	}
	
	/** Try to acquire the modification lock of this text.
	  * @return <CODE>true</CODE>, if lock could be acquired, <CODE>false</CODE> otherwise.
	  */
	private boolean acquireModificationLock() {
		if (fModLock) {
			return false;
		}
		else {
			fModLock = true;
			return true;
		}
	}
	
	/** Release the modification lock of this text.
	  * This method will also call all deferred operations.
	  * @pre fModLock
	  */
	private void releaseModificationLock() {
		fModLock = false;
		if (fOpQ.size() > 0) {
			((QueueEntry) fOpQ.remove(0)).execute();
		}
	}
	
	/** Queue operation for deferred execution 
	  * @param e The operation to be executed later.
	  */
	private void queueOperation(QueueEntry e) {
		fOpQ.add(e);
	}
	
//	/** Test the class invariant.
//	 * @return true, if invariant is ok, false otherwise.
//	 */
//	private boolean testClassInvariant() {
//		int actLen = 0; 
//		REDRun cur = fHead;
//		while (cur != null) {
//			actLen += cur.length();
//			if (cur.length() <= 0) {
//				REDTracer.error("red", "REDText", "Run with length 0 detected");
//				return false;
//			}
//			if (cur.length() != cur.asString().length()) {
//				REDTracer.error("red", "REDText", "Run has length differing from string representation");
//				return false;
//			}
//			if (cur.fNext != null && cur.fNext.fPrev != cur) {
//				REDTracer.error("red", "REDText", "Run linking not ok");
//				return false;
//			}
//			cur = cur.fNext;
//		}	
//		return actLen == length();
//	}
//
//	/** Test the class invariant for the styles of the text */
//	private boolean testClassInvariantStyle() {
//		REDRun cur = fHead;
//		while (cur != null) {
//			if (cur.fStyle == null) {
//				return false;
//			}
//			cur = cur.fNext;
//		}
//		return true;
//	}
	
	/** Get structure.
	 * Returns the structure (Runs) as a single string. The Runs are separated
	 * by "->\n". At the end the string "null" is added to indicate, that no
	 * more runs exist. This implies that the string "null" is returned for an
	 * empty REDText.
	 */
	String getStructure() {
		REDRun cur = fHead;
		String structure = "";
		while (cur != null) {
			structure += cur.asString() + "->\n";
			cur = cur.fNext;
		}
		structure += "null";
		return structure;
	}

//	/** Get structure with style information. */
//	String getStructureWithStyles() {
//		StringBuffer buf = new StringBuffer();
//		REDRun cur = fHead;
//		while (cur != null) {
//			buf.append("[" + cur.fStyle +"]" + cur.asString());
//			cur = cur.fNext;
//		}
//		return "" + buf;
//	}
	
	/** REDRunSpec represents a specification of a run, including the run itself, its origin and offset.
	  * It is used for findRun - operations.
	  */
	static class REDRunSpec {
		public REDRun fRun;
		public int fOrg;
		public int fOff;
		{
			fRun = null;
			fOrg = -1;
			fOff = -1;
		}
		public boolean isValid() {
			return fRun != null;
		}
	}
	
	private REDRun fHead;
	private REDRun fCache;
	private int fCachePos;
	private int fLength;
	private final REDFileRider fScratchRider;
	private final String fFilename;
	private REDStyle fDefaultStyle;
	protected REDLineTree fLineTree;	// @TBD make me private again.
	private final ArrayList fListeners;
	private REDTextCommand fCurTypingCmd;
	private REDView fUndoRedoView;
	private final REDCommandProcessor fCmdP;
	private boolean fModLock;	// modification locked
	private final ArrayList fOpQ;	// operation queue
	private boolean fStyleBatchNotification;
}
