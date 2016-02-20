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

/** Stream class.
  * This class provides efficient, style-sensitive serial access to text contents. Use REDEditor.createStream to create a stream.
  * @author rli@chello.at
  * @tier API
  */
public class REDStream extends REDEventAdapter {
	/** Create a stream.
	  * @param editor The editor this stream operates on.
	  * @param position The position to start reading. This parameter has gap - semantics, i.e. if you want to start reading backwards from the end of an editor, use <Code>editor.length()</Code> here.
	  * @param direction The direction to read.
	  */
	REDStream(REDEditor editor, int position, REDStreamDirection direction) {
		fEditor = editor;
		fEditor.addREDEventListener(this);
		fPosition = position;
		fDirection = direction;
		fEof = false;
		fMarker = new Object();
	}
	
	/** Close stream and free resources. */
	public void close() {
		fEditor.removeREDEventListener(this);
	}
	
	private boolean styleOk() {
		REDStyle style = fEditor.getStyle(fPosition+1);
		return style.get(fMarker) == null;
	}
	
	/** Read character from stream.
	  * @return The next character from stream, or '\0' if any error has occured.
	  */
	public char readChar() {
		char c; 
		if (fDirection == REDStreamDirection.FORWARD) {
			while (fPosition < fEditor.length() && !styleOk()) {
				fPosition++;
			}
		}
		else {
			fPosition--;
			while (fPosition > 0 && !styleOk()) {
				fPosition--;
			}
		}
		if (fDirection == REDStreamDirection.BACKWARD && fPosition < 0 || 
			fDirection == REDStreamDirection.FORWARD && fPosition >= fEditor.length()) {
			c = '\0';
			fEof = true;
		}
		else {
			c = (char) fEditor.charAt(fPosition);
			if (fDirection == REDStreamDirection.FORWARD) {
				fPosition++;
			}
			
		}
		return c;
	}
	
	/** Check eof status.
	  * @return <Code>true</Code> if an attempt was made to read beyond the end of the stream. <Code>false</Code> otherwise.
	  */
	public boolean eof() {
		return fEof;
	}
	
	/** Set position of stream. As a side effect, this method will also reset the EOF flag.
	  * @param position The new position the stream will read at.
	  */
	public void setPosition(int position) {
		if (position < 0) position = 0;
		if (position > fEditor.length()) position = fEditor.length();
		fPosition = position;
		fEof = false;
	}
	
	/** Get position of stream. 
	  * @return The current position of the stream.
	  */
	public int getPosition() {
		return Math.max(0, fPosition);
	}
	
	/** Include a style. Only those characters are read from the text which match included styles.
	  * By default, all styles are included.
	  * @param style The name of the style to include.
	  * @param recursive If <Code>true</Code>, all derived styles will be included as well.
	  * @return <Code>true</Code> if the operation was successful. <Code>false</Code> if no style with the given name exists.
	  */
	public boolean includeStyle(String stylename, boolean recursive) {
		return REDStyleManager.remove(stylename, fMarker, recursive);
	}
	
	/** Exclude a style. Cf <Code>includeStyle</Code> for a documentation on style sensitivity. 
	  * @param style The name of the style to exclude.
	  * @param recursive If <Code>true</Code>, all derived styles will be excluded as well.
	  * @return <Code>true</Code> if the operation was successful. <Code>false</Code> if no style with the given name exists.
	  */
	public boolean excludeStyle(String stylename, boolean recursive) {
		return REDStyleManager.put(stylename, fMarker, fMarker, recursive);
	}

	// RED Event start	
	public void afterInsert(int from, int to) {
		if (fDirection == REDStreamDirection.FORWARD && from < fPosition ||
			fDirection == REDStreamDirection.BACKWARD && from <= fPosition) {
			fPosition += (to - from);
		}		
	}

	public void afterDelete(int from, int to) { 
		if (from < fPosition) {
			fPosition -= Math.min(fPosition, to) - from;
		}
	}
	
	public void afterLoad() {
		if (fDirection == REDStreamDirection.FORWARD) {
			fPosition = 0;
		}
		else {
			fPosition = fEditor.length();
		}
	}
	
	public void afterFileLoad(String filename) { }
	// RED Event end
	
	
	private final REDEditor fEditor;
	private int fPosition;
	private final REDStreamDirection fDirection;
	private boolean fEof;
	private final Object fMarker;
}
