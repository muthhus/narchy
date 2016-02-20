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

import red.file.*;
import red.lineTree.*;
import red.rexParser.*;
import red.util.*;
import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.io.*;
import java.text.*;

/** Editor widget.
  * This class provides the basic/central API for clients of RED to access.
  * @author rli@chello.at
  * @tier API
  */
public class REDEditor {
	/** Create new REDEditor.
	  * @param filename Name of file to load into the editor. If set to "", editor starts with an empty file
	  * @param privateCopy <br>&nbsp;true: do not use a shared copy of the text
	    <br>&nbsp;false: use a shared copy of the text, if available; create a shareable copy otherwise
	  */
	public REDEditor(String filename, boolean privateCopy) {
		fText = REDTextServer.acquireText(filename, privateCopy);
		fView = new REDView(fText);
 		setController(new REDViewController());
		fListeners = new ArrayList(REDAuxiliary.fcListenerSize);
		fPluginQ = null;
		fPlugins = new ArrayList();
		fMacroCmd = null;
	}
	
	/** Create a new REDEditor with empty file. */
	public REDEditor() {
		this("", false);
	}
	
	protected void finalize() throws Throwable {
		REDTextServer.releaseText(fText);
	}
	
	/** Close editor.
	  * After calling this method you may no longer use the editor.
	  * It will also remove all registered listeners.
	  */
	public void close() {
		for (int x = fListeners.size()-1; x >= 0; x--) {
			removeREDEventListener((REDEventListener) fListeners.get(x));
		}
		REDTextServer.releaseText(fText);
		fText = null;
		fView = null;		
	}
	
	// --- listeners ---
	/** Add a listener to this editor.
	  * @param listener the listener to be added
	  * @see REDEventAdapter
	  */
	public void addREDEventListener(REDEventListener listener) {
		fText.addREDTextEventListener(listener);
		fText.getCommandProcessor().addREDCommandProcessorEventListener(listener);
		fView.addREDViewEventListener(listener);
		if (!fListeners.contains(listener)) {
			fListeners.add(listener);
		}
	}
	
	/** Remove a listener from this editor.
	  * @param listener the listener to be added
	  * @see REDEventAdapter
	  */
	public void removeREDEventListener(REDEventListener listener) {
		fText.removeREDTextEventListener(listener);
		fText.getCommandProcessor().removeREDCommandProcessorEventListener(listener);
		fView.removeREDViewEventListener(listener);	
		fListeners.remove(listener);
	}
	
	/** Internal constant for listeners; Before save event */
	private static final int LI_BEFORESAVE = 0;
	/** Internal constant for listeners; After save event */
	private static final int LI_AFTERSAVE = 1;
	/** Internal constant for listeners; Before load event */
	private static final int LI_BEFORELOAD = 2;
	/** Internal constant for listeners; After load event */
	private static final int LI_AFTERLOAD = 3;
	
	/** Auxiliary method to call listeners 
	  * @param op The operation to call listeners for. One of the <CODE>LI_...</CODE> constants
	  * @param filename The name of the file affected by the operation
	  */
	private void callListeners(int op, String filename) {
		for (int j = 0; j < fListeners.size(); j++) {
			REDEditorEventListener listener = (REDEditorEventListener) fListeners.get(j);
			switch(op) {
				case LI_BEFORESAVE:
					listener.beforeFileSave(filename);
				break;
				case LI_AFTERSAVE:
					listener.afterFileSave(filename);
				break;
				case LI_BEFORELOAD:
					listener.beforeFileLoad(filename);
				break;
				case LI_AFTERLOAD:
					listener.afterFileLoad(filename);
				break;
			}
		}
	}

	// --- text properties ---	
	/** Get complete text as string.
	  * @return A non-null string containing the whole text.
	  */
	public String asString() {
		return fText.asString();
	}
	
	/** Get contents of text.
	  * @param from start of stretch to get
	  * @param end end of stretch to get
	  * @return A non-null string containing [stretch, end[.
	  */
	public String copy(int from, int to) {
		return fText.asString(from, to);
	}
	
	/** Get single byte from text.
	  * @param pos byte to get
	  * @return the character [pos, pos+1]
	  */
	public byte charAt (int pos) {
		return fText.charAt(pos);
	}
	
	/** Get line of text.
	  * @param line line to get
	  * @return a non-null string containing the requested line (including trailing linebreak)
	  */
	public String copyLine(int line) {
		return fText.asString(fText.getLineStart(line), fText.getLineStart(line+1));
	}
	
	/** Create a stream.
	  * @param pos Position to start stream at.
	  * @param dir The direction to read.
	  */
	public REDStream createStream(int pos, REDStreamDirection dir) {
		return new REDStream(this, pos, dir);
	}
	
	/** Create character iterator.
	  * @return A CharacterIterator implementation.
	  */
	public CharacterIterator createCharacterIterator() {
		return new REDCharacterIterator(this, 0);
	}

	/** Change contents of text.
	  * This method is used to insert / change / delete text.
	  * @param text Text to insert. If null or empty string, operation will delete only
	  * @param from Start of stretch to insert at/replace; Normalized to [0, length()]
	  * @param to End of stretch to replace. If <= from, operation will insert only; Normalized to [0, length()]
	  * @param cmdName Name of command to insert into command queue. if null, method determines a fitting description.
	  * @return <CODE>true</CODE>: operation has been executed. <CODE>false</CODE>: operation has been queued for deferred execution.
	  */
	public boolean replace (String text, int from, int to, String cmdName) {
		from = Math.max(0, from); from = Math.min(from, length());
		to = Math.max(from, to); to = Math.min(to, length());
		fText.setCurTypingCommand(null);
		if (cmdName == null) {
			if (text == null || text.isEmpty()) {
				cmdName = "Delete";
			}
			else if (to <= from) {
				cmdName = "Insert";
			}
			else {
				cmdName = "Replace";
			}
		}
		if (from != to || text != null && !text.isEmpty()) {
			REDTextCommand cmd = new REDTextCommand(cmdName, fView, fText, from, to-from, text);
			performCommand(cmd);
			return cmd.isDelayed();
		}
		return false;
	}
	
	/** Get line number for position.
	  * @param pos Position to find line number for. 
	  * @return the line the position is in
	  */
	public int getLineForPosition(int pos) {
		return fText.getLineForPosition(pos);
	}

	/** Return start of line.
	  * The parameter lineNr is normalized to be in range: [0, nrLines] 
	  * @param lineNr Number of line to get start position for. First line has number 0.
	  * @return the start position of the line
	  * @post return >= 0 && return <= length()
	  */
	public int getLineStart(int lineNr) {
		return fText.getLineStart(lineNr);
	}
	
	/** Get end of line without linebreak character(s).
 	  * @param lineNr Number of line to get end position for. First line has number 0.
	  * @return The end position of the given line. If <Code>line &lt; 0</Code> 0 is returned. If <Code>line &gt; getNrOfLines(), length()</Code> is returned.
	  * @post return >= 0 && return <= length()
	  */
	public int getLineEnd(int lineNr) {
		return fText.getLineEnd(lineNr);
	}

	/** return nr of lines in text
	  * @return nr of lines in text; an empty text has 1 line.
	  * @post return >= 1
	  */
	public int getNrOfLines() {
		return fText.getNrOfLines();
	}

	/** Set style for a stretch of the text.
	  * The parameters from and to have gap semantics, i.e. to set the style for the first character of a text you have to call setStyle(0, 1, style) <br>
	  * The parameters from and to are normalized to be in range: [0, fLength] 	
	  * @param from startposition of stretch to set style for
	  * @param to endposition of stretch to set style for
	  * @param style to set
	  * @return <CODE>true</CODE>: operation has been executed. <CODE>false</CODE>: operation has been queued for deferred execution.
	  * @pre style != null
	  * @post forall int x in from+1 .. to | getStyle(x) == style
	  */
	public boolean setStyle(int from, int to, REDStyle style) {
		return fText.setStyle(from, to, style);
	}

	/** Get style for text position.
	 * The parameter pos is normalized to be in range: [0, fLength] 	<br>
	 * if pos lies between two runs the style of the first run is returned
	 * @param pos position to get style for
	 * @return style at given position 
	 * @post return != null
	 */
	public REDStyle getStyle(int pos) {
		return fText.getStyle(pos);
	}

	/** Get default style.
	  * @return the default style of this text
	  * @tbd: duplicate default style in REDEditor (?)
	  */
	public REDStyle getDefaultStyle() {
		return fText.getDefaultStyle();
	}
	
	/** Set default style.
	  * @param style the new default style of this text
	  * @tbd: duplicate default style in REDEditor (?)
	  */
	public void setDefaultStyle(REDStyle style) {
		fText.setDefaultStyle(style);
	}

	
	/** Get length of text.
	  * @return the length of the edited text
	  */
	public int length() {
		return fText.length();
	}
	
	/** Get modified status.
	  * @return <br>&nbsp;true: if text has been changed since last save
	    <br>&nbsp;false: otherwise
	  */
	public boolean isModified() {
		return fText.getCommandProcessor().isModified();
	}
	
	/** Get filename of edited text.
	  * @return a non-null string containing the filename of the edited text
	  */
	public String getFilename() {
		return fText.getFilename();
	}
	
	// --- view properties ---
	/** Get visual object of editor.
	  * This method may be used to get a JComponent object to integrate into the GUI
	  * @post return != null
	  */
	public JComponent getView() {
		return fView;
	}

	/** Get start of selection or position of caret.
	  * @return start of selection or position of caret
	  */
	public int getSelectionStart() {
		return fView.fSelFrom;
	}

	/** Get end of selection.
	  * @return end of selection or position of caret
	  */
	public int getSelectionEnd() {
		return fView.fSelTo;
	}
	
	/** Get selection as string.
	  * @return A non-null string which contains the current selection.
	  */
	public String getSelectedText() {
		return fView.getSelectedText();
	}
	
	/** Get focussed word. The focussed word is the identifier the caret (or start of selection) is in.
	  * @return A non-null String representing the focussed word.
	  */
	public String getFocussedWord() {
		return fView.getFocussedWord();
	}
	
	/** Set caret or selection.
	  * The parameters from and to are normalized to be in range: [0, fLength] 
	  * @param from start of selection
	  * @param to end of selection
	  */
	public void setSelection(int from, int to) {
		fView.setSelection(from, to);
	}
	
	/** Set caret to position 
	  * The parameter pos is normalized to be in range: [0, fLength] 
	  * @param pos position to caret to
	  */
	public void setSelection(int pos) {
		fView.setSelection(pos);
	}

	/** Used for delayed intelliselect */	
	class DelayedIntelliselect implements Runnable {
		DelayedIntelliselect(int from, int to, int percentFromTop, int percentToBottom) {
			fFrom = from; 
			fTo = to;
			fPercentFromTop = percentFromTop;
			fPercentToBottom = percentToBottom;
		}
		
		public void run() { 
			REDTracer.info("red", "REDEditor", "Delayed intelliselect.");
			doIntelliSelect(fFrom, fTo, fPercentFromTop, fPercentToBottom, true);
		}
		
		int fFrom, fTo, fPercentFromTop, fPercentToBottom;
	}
	
	private void doIntelliSelect(int from, int to, int percentFromTop, int percentToBottom, boolean delayed) {
		Rectangle r = fView.getVisibleRect();
		if (!delayed && r.height == 0 && SwingUtilities.isEventDispatchThread()) {	// if view is not yet displayed
			try {
				SwingUtilities.invokeLater(new DelayedIntelliselect(from, to, percentFromTop, percentToBottom));
			}
			catch (Exception ie) {
				REDTracer.error("red", "REDEditor", "Exception while waiting for event thread: " + ie);
			}
			return;
		}

		if (to < from) {
			to = from;
		}
		int lineTop = fView.getLineTop(getLineForPosition(from));
		int lineBottom = fView.getLineTop(getLineForPosition(to) + 1);
		int scrollHeight = lineTop - r.height * percentFromTop / 100;
		if (r.y > scrollHeight) {
			scrollTo(scrollHeight);
		}
		else {
			scrollHeight = lineBottom + r.height * percentToBottom / 100;
			scrollHeight -= r.height;
			if (r.y < scrollHeight) {
				scrollTo(scrollHeight);
			}
		}
		fView.setSelection(from, to);
		fView.revealSelection();	
	}
	
	/** Set caret or selection and scroll intelligently (aka IntelliSelect).
	  * The parameters from and to are normalized to be in range: [0, fLength] 
	  * @param from start of selection
	  * @param to end of selection
	  * @param percentFromTop The percentage given here is the fraction of the view's visible rectangle height which will be at least visible above the given selection.
	  * @param percentToBottom The percentage given here is the fraction of the view's visible rectangle height which will be at least visible below the given selection.
	  */
	public void setSelection (int from, int to, int percentFromTop, int percentToBottom) {
		doIntelliSelect(from, to, percentFromTop, percentToBottom, false);
	}
	
	/** Reveal selection. 
	  * You can use this method to make the editor scroll to a position where the current selection or caret is visible.
	  */
	public void revealSelection() {
		fView.revealSelection();
	}
	
	
	void scrollTo(int height) {
		Rectangle r = fView.getVisibleRect();
		r.y = height;
		fView.scrollRectToVisible(r);
	}		

	/** Get top of line (in pixel).
	  * @param lineNr The line to get top for.
	  * @return The top of the line. Line #0 has its top at 0.
	  */
	public int getLineTop(int lineNr) {
		return fView.getLineTop(lineNr);
	}

	/** Get height of line (in pixel).
	  * @param lineNr The line to get height for.
	  * @return The height of the line. 
	  */
	public int getLineHeight(int lineNr) {
		return fView.getLineHeight(lineNr);
	}
	
	/** Get line at pixel height.
	  * @param pixel The height in pixel for which to get line number for.
	  * @return The linenumber which contains the given pixel height.
	  */
	public int getLineAtHeight(int pixel) {
		return fView.getLineAtHeight(pixel);
	}		
	
	/** Get (gap) position for pixel coordinates.
	  * @param x The x coordinate to get position for.
	  * @param y The y coordinate to get position for.
	  * @return The position the given coordinates corresponds to.
	  */
	public int getPosition(int x, int y) {
		return fView.locatePoint(x, y, null, getViewMode() != REDAuxiliary.VIEWMODE_OVERWRITE).getTextPosition();
	}

	/** Get selection status.
	  * @return <BR>&nbsp; true, if view has got a selection 
	    <BR> &nbsp; false, otherwise
	  */
	public boolean hasSelection() {
		return fView.hasSelection();
	}

	/** Get caret blinking interval.
	  * @return the time (in milliseconds) the caret is displayed when blinking or 0 blinking is turned off
	  */
	public int getCaretBlink() {
		return fView.getCaretBlink();
	}

	/** Set caret blinking interval.
	  * @param millis The time (in milliseconds) to display / hide the caret
	  */
	public void setCaretBlink(int millis) {
		fView.setCaretBlink(millis);
	}
	
	/** Turn whitespace visualization on/off.
	  * @param visualize <br>&nbsp;true: visualize whitespace <br>&nbsp;false: do not visualize whitespace
	  */
	public void setVisualizeWhitespace(boolean visualize) {
		fView.setVisualizeWhitespace(visualize);
	}

	/** Get status of whitespace visualization.
	  * @return <br>&nbsp;true: whitespace is currently visualized <br>&nbsp;false: whitespace is currently not visualized
	  */
	public boolean getVisualizeWhitespace() {
		return fView.getVisualizeWhitespace();
	}
	
	/** Get word constituent status of character. By default only letters and digits are word constituents. This can be controlled using <Code>setWordConstituents</Code>.
	  * @return <Code>True</Code>, if the passed byte is a word constituent for this editor. <Code>false</Code> otherwise.
	  */
	public boolean isWordConstituent(byte c) {
		return fView.isWordConstituent(c);
	}

	/** Set word constituents. This method allows to specify characters which are considered to be part of words in addition to letters and digits.
	  * @param wordConstituents A string of characters to be considered word constituents.
	  */
	public void setWordConstituents(String wordConstituents) {
		fView.setWordConstituents(wordConstituents);
	}
	
	/** Set line to highlight.
	  * The highlit line will be displayed with the highlight background color.
	  * It will not float, but disappear upon changes in the text.
	  * @param line The line to highlight. Use -1 to remove highlighting.
	  */
	public void setHighlightLine(int line) {
		fView.setHighlightLine(line);
	}
	
	/** Set highlight color.
	  * The default highlight color is yellow. 
	  * @param color The new highlight color. Only non-null values will be accepted.
	  */
	public void setHighlightColor(Color color) {
		fView.setHighlightColor(color);
	}
	
	/** Get highlight line status.
	  * @return <Code>true</Code> if view currently has a highlight line. <Code>false</Code> otherwise.
	  */
	public boolean hasHighlightLine() {
		return fView.hasHighlightLine();
	}
	
	/** Get highlight line number.
	  * @return The current highlight number or -1, if no line is currently highlit.
	  */
	public int getHighlightLine() {
		return fView.getHighlightLine();
	}
	
	// --- command queue ---
	/** Get status of command queue.
	  * @return <br>&nbsp;true: can undo last operation
	    <br>&nbsp;false: cannot undo any operation
	  */
	public boolean canUndo() {
		return fText.getCommandProcessor().canUndo();
	}

	/** Get status of command queue.
	  * @return <br>&nbsp;true: can redo last (undone) operation
	    <br>&nbsp;false: cannot redo any operation
	  */
	public boolean canRedo() {
		return fText.getCommandProcessor().canRedo();
	}
	
	/** Undo last operation.
	  * @return true, if successful
	  */
	public boolean undo() {
		fText.setUndoRedoView(fView);
		return fText.getCommandProcessor().undo();
	}
	
	/** Redo last (undone) operation.
	  * This method will not repeat the last operation performed, it only reestablishes the effects of undone operations
	  * @return true, if successful
	  */
	public boolean redo() {
		fText.setUndoRedoView(fView);
		return fText.getCommandProcessor().redo();
	}
    
	
	/** Start macro command mode.
	  * After starting the macro command mode, changes to the text are stored in a macro command, which will be executed when
	  * endMacroCommand() is called.
	  * You must not call startMacroCommand more than once without calling endMacroCommand first (such calls will be ignored).
	  * @param name The name of the macro command.
	  * @pre fMacroCmd == null
	  * @post fMacroCmd != null
	  */
	public void startMacroCommand(String name) {
		if (fMacroCmd != null) {
			REDGLog.error("RED", "Nested macro commands not supported.");
		}
		else {
			fMacroCmd = new REDMacroCommand(name);
		}
	}
	
	/** End macro command mode and execute macro.
	  * When calling this method, all added macro commands are executed.
	  * After that you may call startMacroCommand again.
	  */
	public void endMacroCommand() {
		if (fMacroCmd == null) {
			REDGLog.error("RED", "Tried to end macro command, while not having started one.");
		}
		else {
			fText.getCommandProcessor().perform(fMacroCmd);
		}
		fMacroCmd = null;
	}
	
	/** Get macro command mode. 
	  * @return <CODE>true</CODE>, if editor is currently in macro command mode.
	  */
	public boolean hasMacroCommand() {
		return fMacroCmd != null;
	}
	
	/** Auxiliary method to perform command. Will put command into macrocommand, if editor is in macro command mode. */
	private void performCommand(REDCommand cmd) {
		if (hasMacroCommand()) {
			fMacroCmd.add(cmd);
		}
		else {
			fText.getCommandProcessor().perform(cmd);
		}	
	}
    
	/** Clear command queue.
	  * After calling this method, canUndo() and canRedo() will return false.
	  */
    public void clearCommandQueue() {
        REDCommandProcessor p = fText.getCommandProcessor();
        p.finish();
    }
	
	/** Get current change count. 
	  * The change count is an integer indicating the current state of the command processor. 
	  * If an operation is done (or redone) the change count will increase. If an operation is undone it will decrease.
	  * @return The current change count.
	  */
	public int getChangeCount() {
		return fText.getCommandProcessor().getChangeCount();
	}
	
	/** Get change count at last save time.
	  * The change count is an integer indicating the current state of the command processor. 
	  * If an operation is done (or redone) the change count will increase. If an operation is undone it will decrease.
	  * This method will return the change count for the last save time.
	  * <Code>if (getChangeCount() == getSavedChangeCount() => isModified() == false</Code>
	  * @return The change count for the check point.
	  */
	public int getSavedChangeCount() {
		return fText.getCommandProcessor().getCheckPointChangeCount();
	}
		
	/** Set tab width.
	  * @param width The tab width in SPC, where SPC is the width of this editor's current default style font.
	  *  This parameter is normalized to [1, 256]
	  */
	public void setTabWidth(int width) {
		width = Math.max(1, width); width = Math.min(256, width);
		fView.setTabWidth(width);
	}
	
	/** Set tab width.
	  * @return tab width in SPC, where SPC is the width of this editor's current default style font
	  */
	public int getTabWidth() {
		return fView.getTabWidth();
	}
	
	/** Set indent width.
	  * @param indent The indent width in SPC, where SPC is the width of this editor's current default style font. 
	  *  This parameter is normalized to [1, 256]
	  */
	public void setIndentWidth(int indent) {
		indent = Math.max(1, indent); indent = Math.min(256, indent);
		fView.setIndentWidth(indent);
	}
	
	/** Get indent width.
	  * @return indent width in SPC, where SPC is the width of this editor's current default style font
	  */
	public int getIndentWidth() {
		return fView.getIndentWidth();
	}

	/** Set indentation mode.
	  * @param mode The indentation mode to set.
	  */
	public void setIndentMode(REDIndentMode mode) {
		fView.setIndentMode(mode);
	}
	
	/** Get indentation mode.
	  * @return The indentation mode currently set in this editor.
	  */
	public REDIndentMode getIndentMode() {
		return fView.getIndentMode();
	}
	
	/** Get string used for indentation.
	  * @return the string inserted for indentation; contains spaces and tabs, depending on tabWidth, indentWidth and indentMode
	  */
	public String getIndentString() {
		return fView.getIndentString();
	}
	
	/** Adjust indentation for a single line.
	  * @param line number of line to adjust
	  * @param mode The mode to adjust this line to. ASIS will have no effect.
	  */
	public void adjustIndentation(int line, REDIndentMode mode) {
		fView.adjustIndentation(fText.getLineStart(line), mode);
	}

	// --- clipboard ---	
	
	/** Copy selection to clipboard.
	  * @return true, if successful
	  */
	public boolean clipboardCopy() {
		return fView.clipboardCopy();
	}
	
	/** Cut selection to clipboard.
	  * @return true, if successful
	  */
	public boolean clipboardCut() {
		return fView.clipboardCut();
	}
	
	/** Paste clipboard contents to editor.
	  * @return true, if successful
	  */	
	public boolean clipboardPaste() {
		return fView.clipboardPaste();
	}
	
	// --- file handling ---	
	/** Load a file into editor.
	  * @param filename file to load
	  * @param privateCopy <br>&nbsp;true: do not use a shared copy of the text
	    <br>&nbsp;false: use a shared copy of the text, if available; create a shareable copy otherwise
	  * @return true, if successful
	  * @pre filename != null
	  */
	public boolean loadFile(String filename, boolean privateCopy) {
		usePluginQ(true);
		callListeners(LI_BEFORELOAD, filename);
		doLoad(filename, privateCopy);
		callListeners(LI_AFTERLOAD, filename);
		usePluginQ(false);
		
		return true;	
	}
	
	/** Discard changes to text and reload from disk.
	  * @return true, if successful
	  * @post !isModified()
	  */
	public boolean revert() {
		fText.load();
		return true;
	}
	/** Save file to disk
	  * @param backupExtension filename extension (without leading dot) to save backup copy (i.e. previously saved version) under. 
	  * If null, no backup copy is made.
	  * @return true, if successful
	  * @post !isModified()
	  */
	public boolean saveFile(String backupExtension) {
		File file = new File(getFilename());
		if (file.exists() && !file.canWrite()) return false;
		usePluginQ(true);
		callListeners(LI_BEFORESAVE, fText.getFilename());
		if (backupExtension != null) {
			if (!REDFile.moveFile(fText.getFilename(), fText.getFilename() + '.' + backupExtension)) {
				REDGLog.error("RED", "Cannot move " + fText.getFilename() + " to " + fText.getFilename() + '.' + backupExtension);
			}
		}
		fText.save();
		callListeners(LI_AFTERSAVE, fText.getFilename());
		usePluginQ(false);
		return true;
	}
	
	/** Save text into another file. 
	  * After the operation is finished the editor will contain the new file.
	  * @param filename file to save into
	  * @param privateCopy <br>&nbsp;true: do not use a shared copy of the text
	  * @return true, if successful
	  * @pre filename != null
	  * @pre !filename.equals("")
	  */
	public boolean saveFileAs(String filename, boolean privateCopy) {
		usePluginQ(true);
		callListeners(LI_BEFORESAVE, filename);
		if (!privateCopy && REDTextServer.isTextLoaded(filename, false)) {
			REDText into  = REDTextServer.acquireText(filename, false);
			into.replace(0, into.length(), asString());
			into.getCommandProcessor().finish();
			into.save();
			REDTextServer.releaseText(into);
		}
		else {
			fText.saveInto(filename);
		}
		doLoad(filename, privateCopy);
		callListeners(LI_AFTERSAVE, filename);
		usePluginQ(false);
		return true;
	}
	
	/** Create an emergency backup of text.
	  * Note that in contrast to saveFileAs this method will no load the newly created file (but keep the one that is currently loaded).
	  * @param filename file to save into
	  * @return true, if successful
	  * @pre extension != null
	  * @pre !extension.equals("")
	  */
	public boolean saveEmergency(String extension) {
		String filename = fText.getFilename() + '.' + extension;
		callListeners(LI_BEFORESAVE, filename);
		fText.saveInto(filename);
		callListeners(LI_AFTERSAVE, filename);
		return true;
	}

	private void doLoad(String filename, boolean privateCopy) {
		Iterator iter = fListeners.iterator();
		while (iter.hasNext()) {
			REDEventListener l = (REDEventListener) iter.next();
			fText.removeREDTextEventListener(l);
			fText.getCommandProcessor().removeREDCommandProcessorEventListener(l);
			fView.removeREDViewEventListener(l);
		}			
		REDTextServer.releaseText(fText);
		fText = REDTextServer.acquireText(filename, privateCopy);
		iter = fListeners.iterator();
		while (iter.hasNext()) {
			REDEventListener l = (REDEventListener) iter.next();
			fText.addREDTextEventListener(l);
			fText.getCommandProcessor().addREDCommandProcessorEventListener(l);
			fView.addREDViewEventListener(l);
		}			
		fView.setText(fText);
	}
	

	// --- view mode ---
	
	/** Set view mode.
	  * @param mode use one of the REDAuxiliary.VIEWMODE_* - constants
	  */
	public void setViewMode(int mode) {
		REDViewController c;
		fView.setMode(mode);

		switch (mode) {
			case REDAuxiliary.VIEWMODE_INSERT:
			case REDAuxiliary.VIEWMODE_OVERWRITE:
				c = getController();
				while (c instanceof REDViewControllerDecorator) {
					REDViewControllerDecorator d = (REDViewControllerDecorator) c;
					if (d instanceof REDViewReadonlyController) {
						removeControllerDecorator(d);
						break;
					}
					c = d.getDecorated();
				}
			break;
			case REDAuxiliary.VIEWMODE_READONLY:
				c = getController();
				while (c != null && c instanceof REDViewControllerDecorator && !(c instanceof REDViewReadonlyController)) {
					c = ((REDViewControllerDecorator) c).getDecorated();
				}
				if (!(c instanceof REDViewReadonlyController)) {
					setController(new REDViewReadonlyController(getController()));
				}
			break;
		}
	}

	/** Get view mode.
	  * @return one of the REDAuxiliary.VIEWMODE_* - constants
	  */
	public int getViewMode() {
		return fView.getMode();
	}
	
	// --- controller methods ---
	
	/** Set controller for this editor.
	  * Note: to add a controller decoration use editor.setController(new MyControllerDecorator(editor.getController())).
	  * @param controller Controller to be set for this editor
	  */
	public void setController(REDViewController controller) {
		if (fController != null) {
			fView.removeKeyListener(fController);
			fView.removeMouseListener(fController);
			fView.removeMouseMotionListener(fController);
		}		
		fController = controller;
		if (fController != null) {
			fView.addKeyListener(fController);
			fView.addMouseListener(fController);
			fView.addMouseMotionListener(fController);
		}
	}
	
	/** Get controller for this editor.
	  * @return the current controller of the editor
	  */
	public REDViewController getController() {
		return fController;
	}
	
	/** Remove a specific controller decoration from the controller stack of this editor.
	  * @param decorator Decorator to be removed
	  */
	public void removeControllerDecorator(REDViewControllerDecorator decorator) {
		REDViewController c = getController();
		REDViewControllerDecorator prev = null;
		while (c instanceof REDViewControllerDecorator) {
			REDViewControllerDecorator d = (REDViewControllerDecorator) c;
			if (d == decorator) {
				if (d == getController()) {
					setController(d.getDecorated());
					break;
				}
				else {
					prev.setDecorated(d.getDecorated());
					break;
				}
			}
			prev = d;
			c = d.getDecorated();
		}
	}
	
	// --- Plugins ---
	private void usePluginQ(boolean useIt) {
		if (useIt) {
			fPluginQ = new ArrayList();
		}
		else {
			Iterator iter = fPluginQ.iterator();
			while (iter.hasNext()) {
				doAddPlugin((REDPlugin) iter.next());
			}
			fPluginQ = null;
		}
	}
	
	private void doAddPlugin(REDPlugin plugIn) {
		fPlugins.add(plugIn);
		plugIn.setEditor(this);
//		if (plugIn.IsVisualPlugin() && fTextView) {
//			fTextView.AddPlugin(plugIn);
//		}
//		// else the plugin will be added in DoMakeVObject 
	}
	
	/** Add a plugin to this editor.
	  * @param plugIn the plugin to add
	  */
	public void addPlugin(REDPlugin plugIn) {
		if (fPluginQ != null) {
			fPluginQ.add(plugIn);
		}
		else {
			doAddPlugin(plugIn);
		}
	}
	
	/** Remove a plugin from this editor.
	  * @param plugIn the plugin to remove
	  */
	public void removePlugin(REDPlugin plugIn) {
		if (fPluginQ == null || !fPluginQ.remove(plugIn)) {
			fPlugins.remove(plugIn);
			plugIn.setEditor(null);
		}
	}
	
	public REDMarkTree createMarkTree() {
		REDMarkTree t = new REDMarkTree(this);
		fText.addREDTextEventListener(t);
		return t;
	}
	
	
	/** Get line source for regular expression parsing.
	  * @return A line source object
	  */
	public REDRexLineSource getLineSource() {
		return fText;
	}
	
	/** Start style batch notification mode.
	  * After style batch notification mode has been entered,  notifications about style changes in the text will not be sent out to listeners until batchStyleNotificationEnd is called.
	  * You must not call batchStyleNotificationStart, if this mode is already in place (i.e. nested calls are disallowed).
	  */
	public void batchStyleNotificationStart() {
		fText.batchStyleNotificationStart();
	}
	
	/** End style batch notification mode.
	  * This method ends style batch notification mode, thus (re-)enabling normal notifications.
	  * You must not call batchStyleNotificationEnd, without this mode in place
	  */
	public void batchStyleNotificationEnd() {
		fText.batchStyleNotificationEnd();
	}
	
	/** Get style batch notification mode status
	  * @return <CODE>true</CODE>, if style batch notification mode is in place, <CODE>false</CODE> otherwise
	  */
	public boolean hasStyleBatchNotification() {
		return fText.hasStyleBatchNotification();
	}


// Printing delayed RLI 26 Feb 2001	
//	// --- printing 
//	public boolean printWithDialog() {
//		PrinterJob printerJob = PrinterJob.getPrinterJob();
//		PageFormat pageFormat = printerJob.defaultPage();
//		printerJob.setPrintable(fView, pageFormat);
//
//		if (printerJob.printDialog()) {
//			try {
//				printerJob.print();
//			} 
//			catch (PrinterException pe) {
//				return false;
//			}
//		}
//		return true;
//	}
//	
//	public boolean print() {
//		PrinterJob printerJob = PrinterJob.getPrinterJob();
//		PageFormat pageFormat = printerJob.defaultPage();
//		printerJob.setPrintable(fView, pageFormat);
//		try {
//			printerJob.print();
//		}
//		catch (PrinterException pe) {
//			return false;
//		}
//		return true;
//	}

	private REDText fText;
	private REDView fView;
	private REDViewController fController;
	private final ArrayList fListeners;
	private ArrayList fPluginQ;
	private final ArrayList fPlugins;
	private REDMacroCommand fMacroCmd;
}
