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

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;

import red.lineTree.*;

/** REDView - the visual (Swing) component used to display REDTexts 
  * @author rli@chello.at
  * @tier system
  * @invariant fText != null
  * @invariant fSelFrom >= 0
  */
public class REDView extends JPanel implements REDTextEventListener, REDStyleEventListener, Scrollable, PropertyChangeListener, FocusListener {
	public static final int PACE_CHAR = 0;
	public static final int PACE_WORD = 1;
	public static final int PACE_LINE = 2;
	public static final int PACE_LINEBOUND = 3;
	public static final int PACE_PAGE = 4;
	public static final int PACE_PAGEBOUND = 5;
	public static final int PACE_DOCUMENT = 6;
	private static final int CARET_BLINK_FREQUENCY = 400;
	static final int DIR_LEFT_TO_RIGHT = 0;
	static final int DIR_RIGHT_TO_LEFT = 1;

	REDView(REDText text) {
		super(false);	// no double buffering
		fHighlightColor = Color.yellow;
		fHighlightLine = -1;
		fWordConstituents = "";
		fLineHeightCache = new REDViewLineHeightCache();
		fText = text;
		fText.addREDTextEventListener(this);
		fPaintBatch = new ArrayList();
		fExtent = new Dimension();
		setTabWidth(REDAuxiliary.fcDefaultTabWidth);
		setIndentWidth(REDAuxiliary.fcDefaultIndentWidth);
		setMinTabWidth(REDAuxiliary.fcDefaultMinTabWidth);
		resetLineTops();
		checkLineWidth(0, fText.getNrOfLines());
		fXOffset = -1;
		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		setAutoscrolls(true);
		addFocusListener(this);
		setBackground(fText.getDefaultStyle().getBackground());
		fInsets = getInsets();
		REDStyleManager.addStyleEventListener(this);
	}

	class CaretBlink implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (!hasSelection()) {
				if (isShowing()) {
					fCaretOn = !fCaretOn;
					repaintSelection();
				}
			}
		}
	}

	private final static int SP_NONE = 0;
	private final static int SP_TAB = 1;
	private final static int SP_SPC = 2;
	private final static int SP_LB = 3;
	private final static int SP_EOF = 4;

	static class PaintBatchEntry {
		public PaintBatchEntry() {}
		public int fX, fLength, fWidth, fSpecial;
		public REDStyle fStyle;
		public byte[] fStr;
		{
			fX = 0;
			fWidth = 0;
			fLength = 0;
			fStr = null;
			fStyle = null;
			fSpecial = SP_NONE;
		}
	}

	/** add a listener to this view
	  * @param listener The listener to be added to the view
	  * @return true if successful, false otherwise
	  * @pre listener != null
	  */
	boolean addREDViewEventListener(REDViewEventListener listener) {
		if (!fListeners.contains(listener)) {
			return fListeners.add(listener);
		}
		return false;
	}

	/** remove a listener from this view
	  * @param listener The listener to be removed from the view
	  * @return true if successful, false otherwise
	  * @pre listener != null
	  */
	boolean removeREDViewEventListener(REDViewEventListener listener) {
		return fListeners.remove(listener);
	}

	private static final int LI_BEFORESELCHANGE = 0;
	private static final int LI_AFTERSELCHANGE = 1;
	private static final int LI_GOTFOCUS = 2;
	private static final int LI_LOSTFOCUS = 3;
	private static final int LI_BEFOREMODECHANGE = 4;
	private static final int LI_AFTERMODECHANGE = 5;

	/** notify listeners about a view event
	  */
	private void callListeners(int op, int oldFrom, int oldTo, int newFrom, int newTo) {
		for (int j = 0; j < fListeners.size(); j++) {
			REDViewEventListener listener = (REDViewEventListener) fListeners.get(j);
			switch(op) {
				case LI_BEFORESELCHANGE:
					listener.beforeSelectionChange(oldFrom, oldTo, newFrom, newTo);
				break;
				case LI_AFTERSELCHANGE:
					listener.afterSelectionChange(oldFrom, oldTo, newFrom, newTo);
				break;
				case LI_GOTFOCUS:
					listener.gotFocus();
				break;
				case LI_LOSTFOCUS:
					listener.lostFocus();
				break;
				case LI_BEFOREMODECHANGE:
					listener.beforeModeChange(oldFrom, newTo);
				break;
				case LI_AFTERMODECHANGE:
					listener.afterModeChange(oldFrom, newTo);
				break;
			}
		}
	}

	private void callListeners(int op, int oldMode, int newMode) {
		callListeners(op, oldMode, -1, -1, newMode);
	}

	private void callListeners(int op) {
		callListeners(op, -1, -1, -1, -1);
	}


	int nextTabStop(int x, int nrTabs) {
		x += fTabMin - 1;
		return x + fTabWidth * nrTabs - (x % fTabWidth);
	}
	
	/** create one paint batch entry */
	private int makePaintBatchEntry(FontMetrics metrics, int batchEntries, int curX, int curPos) {
		if (fPaintBatch.size() <= batchEntries) {
			fPaintBatch.add(new PaintBatchEntry());
		}
		PaintBatchEntry e = null;
		try {
			e = ((PaintBatchEntry) fPaintBatch.get(batchEntries));
			e.fX = curX + fInsets.left;
			e.fStyle = fViewStretch.fStyle;
			e.fLength = fViewStretch.fLength;
			e.fStr = fText.asBytes(curPos, curPos + fViewStretch.fLength, e.fStr);
		}
		catch (ArrayIndexOutOfBoundsException aioobe) {
			throw new Error("Internal error in REDView.paintComponent");
		}
		switch (fViewStretch.fType) {
			case REDViewStretch.TAB:
				e.fWidth = nextTabStop(curX, fViewStretch.fLength) - curX;
				e.fSpecial = SP_TAB;
			break;
			case REDViewStretch.TEXT:
				e.fWidth = metrics.bytesWidth(e.fStr, 0, e.fLength);
				if (fVisualizeWhitespace && e.fLength == 1 && e.fStr[0] == ' ') {
					e.fSpecial = SP_SPC;
				}
				else {
					e.fSpecial = SP_NONE;
				}
			break;
			case REDViewStretch.LINEBREAK:
				e.fWidth = getPreferredSize().width;
				e.fSpecial = SP_LB;
			break;
			case REDViewStretch.EOF:
				e.fSpecial = SP_EOF;
				e.fWidth = getPreferredSize().width;
			break;
		}
		curX += e.fWidth;
		return curX;
	}		

	public void paintComponent (Graphics g) {
		super.paintComponent(g);

//		PTestStopWatch sw = new PTestStopWatch();
//		sw.start();
		int curX;
		int lineHeight, ascent, batchEntries;
		Rectangle r =  g.getClipBounds();
		int startLine = getLineAtHeight(r.y - fInsets.top);
		int endLine = getLineAtHeight(r.y + r.height - fInsets.top);
		int curY = fInsets.top + getLineTop(startLine);
		int curPos = fText.getLineStart(startLine);
//		REDTracer.info("red", "REDView", "Painting: " + startLine  + " to " + endLine);
		for (int line = startLine; line <= endLine; line++) {
			lineHeight = 0; ascent = 0; batchEntries = 0;
			curX = 0;
			// build line batch 
			do {
				fViewStretch = fText.getViewStretch(curPos, fViewStretch, fVisualizeWhitespace);
				FontMetrics metrics = g.getFontMetrics(fViewStretch.fStyle.getFont());
				curX = makePaintBatchEntry(metrics, batchEntries, curX, curPos);
				batchEntries++;
				lineHeight = Math.max(lineHeight, metrics.getHeight());
				ascent = Math.max(ascent, metrics.getAscent());
				curPos += fViewStretch.fLength;
			} while (fViewStretch.fType != REDViewStretch.LINEBREAK && fViewStretch.fType != REDViewStretch.EOF);

			// draw batched parts
			for (int b = 0; b < batchEntries; b++) {
				try {
					PaintBatchEntry e = ((PaintBatchEntry) fPaintBatch.get(b));
					g.setColor(e.fStyle.getBackground());
					g.fillRect(e.fX, curY, e.fWidth, lineHeight);
					g.setFont(e.fStyle.getFont());
					g.setColor(e.fStyle.getForeground());
					if (e.fSpecial != SP_NONE) {
						if (fVisualizeWhitespace) {
							String s = "";
							switch (e.fSpecial) {
								case SP_TAB:
									s = "\u00BB";
								break;
								case SP_SPC:
									s = "\u00B7";
								break;
								case SP_LB:
									if (e.fLength == 2) {
										s = "\u00FE\u00B6";
									}
									else if (e.fStr[0] == '\n') {
										s = "\u00B6";
									}
									else {
										s = "\u00FE";
									}
								break;
								case SP_EOF:	// @tbd: find visualization char
									s = "";
								break;
							}
							g.drawString(s, e.fX, curY + ascent);
						}
					}
					else {
						g.drawBytes(e.fStr, 0, e.fLength, e.fX, curY + ascent);
					}
					e.fStyle.getLining().paint(g, e.fX, curY, lineHeight, e.fWidth);
				}
				catch (ArrayIndexOutOfBoundsException aioobe) {
					throw new Error("Internal error in REDView.paintComponent");
				}
			}
			curY += lineHeight;
		}
		if (hasSelection()) {
			paintSelection(g);
		}
		else {
			paintCaret(g);
		}
		if (hasHighlightLine()) {
			paintHighlightLine(g);
		}
//		sw.stop("Custom painting");
	}

	private void paintCaret(Graphics g) {
		if (fCaretOn) {
			fCaretViewPosition = locatePosition(fSelFrom, fCaretViewPosition);	
			g.setColor(Color.black);	// TBD: make color customisable
			g.setXORMode(Color.white);
			if (fMode == REDAuxiliary.VIEWMODE_OVERWRITE && !(fSelFrom == fText.getLineEnd(fText.getLineForPosition(fSelFrom)))) {
				g.fillRect(fCaretViewPosition.fBoundRect.x, fCaretViewPosition.fBoundRect.y, fCaretViewPosition.fBoundRect.width, fCaretViewPosition.fBoundRect.height-1);
			}
			else {
				g.fillRect(fCaretViewPosition.fBoundRect.x-1, fCaretViewPosition.fBoundRect.y, 3, fCaretViewPosition.fBoundRect.height-1);
			}
			g.setPaintMode();
		}
	}
	
	private void paintHighlightLine(Graphics g) {
		REDViewPosition startVp;
		REDViewPosition endVp;
		
		g.setXORMode(fHighlightColor);
		g.setColor(Color.white);
		
		startVp = locateLineStart(fHighlightLine, null);
		g.fillRect(startVp.fBoundRect.x, startVp.fBoundRect.y, getPreferredSize().width, startVp.fBoundRect.height);
		g.setPaintMode();		
	}
	
	private void paintSelection(Graphics g) {
		REDViewPosition selStartVp;
		REDViewPosition selEndVp;
		int selStartLine, selEndLine;

		if (hasFocus()) {
			g.setXORMode(Color.blue.darker());	// TBD: make color customisable
		}
		else {
			g.setXORMode(Color.gray.brighter()); // TBD: make color customisable
		}
		g.setColor(Color.white);

		selStartVp = locatePosition(fSelFrom, null); selStartLine = selStartVp.getLineNumber();
		selEndVp = locatePosition(fSelTo, null); selEndLine = selEndVp.getLineNumber();
		if (selStartLine == selEndLine) {
			g.fillRect(selStartVp.fBoundRect.x, selStartVp.fBoundRect.y, 
				selEndVp.fBoundRect.x - selStartVp.fBoundRect.x,
				selEndVp.fBoundRect.height-1);
		}
		else {
//			g.setColor(Color.green);
			// insert column selection case here
			int width = getPreferredSize().width;
			g.fillRect(selStartVp.fBoundRect.x, selStartVp.fBoundRect.y, 
				width,
				selStartVp.fBoundRect.height);

			int fromHeight = selStartVp.fBoundRect.y + selStartVp.fBoundRect.height;
			int toHeight = selEndVp.fBoundRect.y;
			int lineHeights = toHeight - fromHeight;
			if (lineHeights > 0) {
//				g.setColor(Color.yellow);
				g.fillRect(fInsets.left, fromHeight, width, lineHeights);
			}
//			g.setColor(Color.red);
			g.fillRect(fInsets.left, selEndVp.fBoundRect.y, selEndVp.fBoundRect.x - fInsets.left, selEndVp.fBoundRect.height-1);
		}
		g.setPaintMode();
	}

    public void setBorder(Border border) {
		super.setBorder(border);
		fInsets = getInsets();
    }
	
	/** get caret blinking interval
	  * @return the time (in milliseconds) the caret is displayed/hidden when blinking or 0 blinking is turned off
	  */
	int getCaretBlink() {
		if (fCaretTimer != null) {
			return fCaretTimer.getDelay();
		}
		else {
			return 0;
		}
	}

	/** set caret blinking interval
	  * @param millis The time (in milliseconds) to display/hide the caret. If <= 0, blinking will stop
	  */
	void setCaretBlink(int millis) {
		fCaretTimer.stop();
		if (millis <= 0) {
			fCaretOn = true;
			fCaretTimer.setDelay(0);
		}
		else {
			fCaretOn = true; 
			fCaretTimer.setDelay(millis);
			fCaretTimer.start();
		}
		repaintSelection();
	}
	
	/** Get line top for a line.
	  * @param lineNr The number of the line to get line top for. The first line has number 0.
	  * @return The height (in pixel) this line starts.
	  */
	int getLineTop(int lineNr) {
		if (lineNr < fTopLines.getNrNodes()) {
			return fTopLines.getLineStart(lineNr);
		}
		else {
			return fExtent.height - fInsets.top - fInsets.bottom;
		}
	}
	
	/** get line at pixel height
	  */
	int getLineAtHeight(int pixel) {
		return fTopLines.getLineForPosition(pixel);
	}		
	
	/**
	  * tbd could be further optimised by making one pass only for line height and x/width of bound rect 
	  */
	REDViewPosition locatePosition(int position, REDViewPosition reuse) {
		FontMetrics metrics;
		position = Math.max(0, position);
		position = Math.min(position, fText.length());
		if (reuse == null) {
			reuse = new REDViewPosition();
		}
		reuse.fPosition = position;
		reuse.fLine = fText.getLineForPosition(position);		
		reuse.fBoundRect.y = getLineTop(reuse.fLine) + fInsets.top;
		reuse.fBoundRect.height = getLineHeight(reuse.fLine);
		int curX = 0;
		int curPos = fText.getLineStart(reuse.fLine);
		do {
			fViewStretch = fText.getViewStretch(curPos, fViewStretch);
			if (fViewStretch.fType == REDViewStretch.EOF) break;
			metrics = getFontMetrics(fViewStretch.fStyle.getFont());
			switch (fViewStretch.fType) {
				case REDViewStretch.TAB:
					curX = nextTabStop(curX, Math.min(fViewStretch.fLength, position - curPos));
				break;
				case REDViewStretch.TEXT:
					fScratchBuffer = fText.asBytes(curPos, curPos + fViewStretch.fLength, fScratchBuffer);
					curX += metrics.bytesWidth(fScratchBuffer, 0, Math.min(fViewStretch.fLength, position - curPos));
				break;
				default: break;
			}
			curPos += fViewStretch.fLength;
		} while (curPos < position && fViewStretch.fType != REDViewStretch.EOF);
		reuse.fBoundRect.x = curX + fInsets.left;
		
		fViewStretch = fText.getViewStretch(position, fViewStretch);
		switch (fViewStretch.fType) {
			case REDViewStretch.TAB:
				reuse.fBoundRect.width = nextTabStop(curX, 1) - curX;
			break;
			case REDViewStretch.TEXT:
				metrics = getFontMetrics(fViewStretch.fStyle.getFont());
				fScratchBuffer = fText.asBytes(position, position + 1, fScratchBuffer);
				reuse.fBoundRect.width = metrics.bytesWidth(fScratchBuffer, 0, 1);
			break;
			case REDViewStretch.LINEBREAK:
				metrics = getFontMetrics(fViewStretch.fStyle.getFont());
				reuse.fBoundRect.width = metrics.stringWidth("X") * fViewStretch.fLength;	// TBD: use real width of mapped chars.
			break;
			case REDViewStretch.EOF:
				reuse.fBoundRect.width = 0;
			break;
		}

		return reuse;
	}
	
	REDViewPosition locateLineStart(int line, REDViewPosition reuse) {
		return locatePosition(fText.getLineStart(line), reuse);
	}
	
	/** locate position by pixel coordinates
	  * @param x horizontal coordinate to locate
	  * @param y vertical coordinate to locate
	  * @param reuse if a non-null value is passed the REDViewPosition object is reused
	  */
	REDViewPosition locatePoint(int x, int y, REDViewPosition reuse) {
		return locatePoint(x, y, reuse, false);
	}

	/** locate position by pixel coordinates
	  * @param x horizontal coordinate to locate
	  * @param y vertical coordinate to locate
	  * @param reuse if a non-null value is passed the REDViewPosition object is reused
	  * @param midSplit true: return next position if x is larger than the horizontal middle of character
	  */
	REDViewPosition locatePoint(int x, int y, REDViewPosition reuse, boolean midSplit) {
		if (reuse == null) {
			reuse = new REDViewPosition();
		}
		x -= fInsets.left;
		y -= fInsets.top;
                x = Math.max(x, 0);
                y = Math.max(y, 0);
		reuse.fLine = getLineAtHeight(y);
		int curX = 0;
		int curPos = fText.getLineStart(reuse.fLine);
		reuse.fBoundRect.y = getLineTop(reuse.fLine) + fInsets.top;
		reuse.fBoundRect.height = getLineHeight(reuse.fLine);
		int prevPos = 0;
		int prevX = 0;
		int lastWidth = 0;

		fViewStretch = fText.getViewStretch(curPos, fViewStretch);
		while (curX < x && fViewStretch.fType != REDViewStretch.LINEBREAK && fViewStretch.fType != REDViewStretch.EOF) {
			FontMetrics metrics = getFontMetrics(fViewStretch.fStyle.getFont());
			prevX = curX;
			switch (fViewStretch.fType) {
				case REDViewStretch.TAB:
					curX = nextTabStop(curX, fViewStretch.fLength);
				break;
				case REDViewStretch.TEXT:
					fScratchBuffer = fText.asBytes(curPos, curPos + fViewStretch.fLength, fScratchBuffer);
					curX += metrics.bytesWidth(fScratchBuffer, 0, fViewStretch.fLength);
				break;
				default: break;
			}
			prevPos = curPos;
			curPos += fViewStretch.fLength;
			fViewStretch = fText.getViewStretch(curPos, fViewStretch);
		}

		if (curX > x) {
			curX = prevX;
			fViewStretch = fText.getViewStretch(prevPos, fViewStretch);
			int i = 0;
			switch (fViewStretch.fType) {
				case REDViewStretch.TAB:
					while (i < fViewStretch.fLength && (curX <= x || midSplit && curX - lastWidth / 2 <= x)) {
						prevX = curX;
						curX = nextTabStop(curX, 1);
						lastWidth = curX - prevX;
						i++;
					}
				break;
				case REDViewStretch.TEXT:
					FontMetrics metrics = getFontMetrics(fViewStretch.fStyle.getFont());
					fScratchBuffer = fText.asBytes(prevPos, prevPos + fViewStretch.fLength, fScratchBuffer);
					while (i < fViewStretch.fLength && (curX <= x || midSplit && curX - lastWidth / 2 <= x)) {
						prevX = curX;
						curX += metrics.bytesWidth(fScratchBuffer, i, 1);
						lastWidth = curX - prevX;
						i++;
					}
				break;
			}
			reuse.fPosition = prevPos + i - 1;
			reuse.fBoundRect.x = prevX + fInsets.left; reuse.fBoundRect.width = curX - prevX;
		}
		else {
			reuse.fPosition = curPos;
			reuse.fBoundRect.x = curX + fInsets.left;
			if (fViewStretch.fStyle != null) {
				FontMetrics metrics = getFontMetrics(fViewStretch.fStyle.getFont());
				reuse.fBoundRect.width = metrics.stringWidth("X") * fViewStretch.fLength;
			}
			else {
				reuse.fBoundRect.width = 0;
			}
		}
		return reuse;
	}
		
	int getLineWidth(int line) {
		int curPos = fText.getLineStart(line);
		int curX = 0;
	
		do {
			fViewStretch = fText.getViewStretch(curPos, fViewStretch);
			if (fViewStretch.fType == REDViewStretch.EOF) break;
			FontMetrics metrics = getFontMetrics(fViewStretch.fStyle.getFont());
			switch (fViewStretch.fType) {
				case REDViewStretch.TAB:
					curX = nextTabStop(curX, fViewStretch.fLength);
				break;
				case REDViewStretch.TEXT:
					fScratchBuffer = fText.asBytes(curPos, curPos + fViewStretch.fLength, fScratchBuffer);
					curX += metrics.bytesWidth(fScratchBuffer, 0, fViewStretch.fLength);
				break;
				case REDViewStretch.LINEBREAK:
					curX += metrics.charWidth('X');	// TBD: get real replacement character
				break;
			}				
			curPos += fViewStretch.fLength;
		} while (fViewStretch.fType != REDViewStretch.LINEBREAK && fViewStretch.fType != REDViewStretch.EOF);
		return curX;
	}
	
	/** Auxiliary method to check real line width and enlarge fExtent, if neccessary */
	private void checkRealLineWidth(int line) {
		int realWidth = getLineWidth(line);
		if (realWidth > fExtent.width) {
			fExtent.width = realWidth;
		}			
	}
	
	/** Check line widths and enlarge fExtent, if necessary */
	private void checkLineWidth(int startLine, int endLine) {
		int curPos = fText.getLineStart(startLine);
		int endPos = fText.getLineStart(endLine+1);
		int lineWidth = 0;
	
		fExtent.width -= fInsets.left + fInsets.right;
		fViewStretch = fText.getViewStretch(curPos, fViewStretch);
		while (fViewStretch.fType != REDViewStretch.EOF && curPos < endPos) {
			FontMetrics metrics = getFontMetrics(fViewStretch.fStyle.getFont());
			switch (fViewStretch.fType) {
				case REDViewStretch.TEXT:
					lineWidth += metrics.getMaxAdvance() * fViewStretch.fLength;
				break;
				case REDViewStretch.TAB:
					lineWidth = nextTabStop(lineWidth, fViewStretch.fLength);
				break;
				case REDViewStretch.LINEBREAK:
					if (lineWidth > fExtent.width) {
						checkRealLineWidth(startLine);
					}
					lineWidth = 0;
					startLine++;
				break;
			}				
			curPos += fViewStretch.fLength;
			fViewStretch = fText.getViewStretch(curPos, fViewStretch);
		}

		// Check if the last line is the widest
		if (fViewStretch.fType == REDViewStretch.EOF && lineWidth > fExtent.width) {
			checkRealLineWidth(startLine);
		}
		
		fExtent.width += fInsets.left + fInsets.right;
		revalidate();
	}
	
	// Listener interface
	public int getListenerLevel() {
		return REDTextEventListener.RLL_VIEW;
	}
	
	/**  
	  * @pre fBeforeCorrection == 0 
	  */
	public void beforeInsert(int from, int to) {
		fBeforeCorrection = getLineHeight(fText.getLineForPosition(from));
	}
	
	public void afterInsert(int from, int to) {
		int fromLine = fText.getLineForPosition(from);
		int toLine = fText.getLineForPosition(to);
		invalidateLineHeightCache(fromLine, toLine);
		to = fText.getLineEnd(toLine);
		ArrayList v = new ArrayList(toLine - fromLine);
		int curPos = fText.getLineStart(fromLine);
		int totalHeight = 0;
		int startHeight = getLineTop(fromLine);
		int lineHeight = 0;
		int lineNr = 1;
	
		fHighlightLine = -1;
		fViewStretch = fText.getViewStretch(curPos, fViewStretch);
		while (fViewStretch.fType != REDViewStretch.EOF && curPos < to) {
			FontMetrics metrics = getFontMetrics(fViewStretch.fStyle.getFont());
			lineHeight = Math.max(lineHeight, metrics.getHeight());
			switch (fViewStretch.fType) {
				case REDViewStretch.LINEBREAK:
					totalHeight += lineHeight;
					v.add(new REDLineTreeData(totalHeight + startHeight, lineNr));
					lineHeight = 0;
					lineNr++;
				break;
			}				
			curPos += fViewStretch.fLength;
			fViewStretch = fText.getViewStretch(curPos, fViewStretch);
		} 

		FontMetrics metrics = getFontMetrics(fViewStretch.fStyle.getFont());
		lineHeight = Math.max(lineHeight, metrics.getHeight());
		totalHeight += lineHeight;
		v.add(new REDLineTreeData(totalHeight, lineNr));

		fTopLines.notifyInsert(getLineTop(fromLine), totalHeight - fBeforeCorrection, v);
		fBeforeCorrection = 0;
		recalcExtentHeight();
		checkLineWidth(fromLine, toLine);
		if (fromLine == toLine) {
			repaintLine(fromLine);
		}
		else {
			repaintLines(fromLine, fText.getNrOfLines());
		}
	}
	
	public void beforeDelete(int from, int to) {
		fDeleteEndLine = fText.getLineForPosition(to);
		fDeleteBuffer = getLineHeight(fDeleteEndLine);
	}
	
	private void invalidateLineHeightCache(int fromLine, int toLine) {
		if (fromLine == toLine) {
			fLineHeightCache.invalidateLine(fromLine);
		}
		else {
			fLineHeightCache.invalidateLinesFrom(fromLine);
		}
	}

	public void afterDelete(int from, int to) {
		int startLine = fText.getLineForPosition(from);		
		invalidateLineHeightCache(startLine, fDeleteEndLine);
		fTopLines.notifyInsert(getLineTop(fDeleteEndLine), getLineHeight(startLine) - fDeleteBuffer, null);
		fTopLines.notifyDelete(getLineTop(startLine), getLineTop(fDeleteEndLine));
		fHighlightLine = -1;
		recalcExtentHeight();
		checkLineWidth(startLine, startLine);
		if (startLine == fDeleteEndLine) {
			repaintLine(startLine);
		}
		else {
			repaintLines(startLine, fText.getNrOfLines());
		}
	}

	/**  
	  * @pre fBeforeCorrection == 0 
	  * @tbd find a better way of handling style changes with respect to lineTree
	  */
	public void beforeStyleChange(int from, int to, REDStyle newStyle) {
		int startLine = fText.getLineForPosition(from);		
		int endLine = fText.getLineForPosition(to);
		
		if (fStyleChangeBuffer.length <= endLine - startLine) {
			fStyleChangeBuffer = new int[endLine - startLine + 1];
		}
		for (int x = startLine; x <= endLine; x++) {
			fStyleChangeBuffer[x-startLine] = getLineHeight(x);
		}
	}

	public void afterStyleChange(int from, int to, REDStyle newStyle) {
		int startLine = fText.getLineForPosition(from);		
		int endLine = fText.getLineForPosition(to);
		invalidateLineHeightCache(startLine, endLine);
		int val;
		for (int x = startLine; x <= endLine; x++) {
			val = getLineHeight(x) - fStyleChangeBuffer[x - startLine];
			fTopLines.notifyInsert(getLineTop(x), getLineHeight(x) - fStyleChangeBuffer[x - startLine], null);
		}
		recalcExtentHeight();
		checkLineWidth(startLine, endLine);
		if (startLine == endLine) {
			repaintLine(startLine);
		}
		else {
			repaintLines(startLine, fText.getNrOfLines());
		}
	}

	public void beforeLoad() {
	}
	
	public void afterLoad() {
		resetLineTops();
		fExtent.width = 0;
		checkLineWidth(0, fText.getNrOfLines());
	}

	public void beforeSave() {
	}

	public void afterSave() {
	}
	
	public void beforeSaveInto(String filename) {
	}

	public void afterSaveInto(String filename) {
	}
	
	public void beforeStyleBatchNotification() {
	}
	
	public void afterStyleBatchNotification() {
		fExtent.width = fExtent.height = 0;
		resetLineTops();
		checkLineWidth(0, fText.getNrOfLines());		
	}
	
	public void beforeStyleChange(REDStyle [] style) { 
	}

	public void afterStyleChange(REDStyle [] style) {
		afterStyleBatchNotification();
		repaint();
	}

	public void beforeThemeChange(String oldTheme, String newTheme) { 
	}

	public void afterThemeChange(String oldTheme, String newTheme) { 
		afterStyleBatchNotification();
		repaint();
	}
	
	
	
	/** reset the line top tree
	  */
	private void resetLineTops() {
		fHighlightLine = -1;
		fLineHeightCache.invalidateLinesFrom(0);
		ArrayList v = new ArrayList(fText.getNrOfLines());
		int curPos = 0;
		int totalHeight = 0;
		int lineHeight = 0;
		int lineNr = 1;
		FontMetrics metrics = null;
		
		fViewStretch = fText.getViewStretch(curPos, fViewStretch);
		while (fViewStretch.fType != REDViewStretch.EOF) {
			metrics = getFontMetrics(fViewStretch.fStyle.getFont());
			lineHeight = Math.max(lineHeight, metrics.getHeight());
			switch (fViewStretch.fType) {
				case REDViewStretch.LINEBREAK:
					totalHeight += lineHeight;
					v.add(new REDLineTreeData(totalHeight, lineNr));
					lineHeight = 0;
					lineNr++;
				break;
			}				
			curPos += fViewStretch.fLength;
			fViewStretch = fText.getViewStretch(curPos, fViewStretch);
		} 

		if (lineHeight == 0) {	// empty last line
			if (metrics == null) {	// no predecessor => take default font height
				metrics = getFontMetrics(fText.getDefaultStyle().getFont());
			}
			lineHeight = metrics.getHeight();
		}	
				
		totalHeight += lineHeight;
		v.add(new REDLineTreeData(totalHeight, lineNr));
		
		fTopLines = new REDLineTree();
		fTopLines.notifyInsert(0, totalHeight, v);
		recalcExtentHeight();
	}
	
	/** auxiliary method to set height of view */
	private void recalcExtentHeight() {
		fExtent.height = getLineTop(fText.getNrOfLines()-1) + getLineHeight(fText.getNrOfLines()) + fInsets.top + fInsets.bottom;
	}
	
	/** Auxiliary function to calculate line height, bypassing the line height cache. */
	int calculateLineHeight(int lineNr) {
		int curPos = fText.getLineStart(lineNr);
		int lineHeight = 0;
		FontMetrics metrics = null;
	
		fViewStretch = fText.getViewStretch(curPos, fViewStretch);
		while (fViewStretch.fType != REDViewStretch.EOF && fViewStretch.fType != REDViewStretch.LINEBREAK) {
			metrics = getFontMetrics(fViewStretch.fStyle.getFont());
			lineHeight = Math.max(lineHeight, metrics.getHeight());
			curPos += fViewStretch.fLength;
			fViewStretch = fText.getViewStretch(curPos, fViewStretch);
		} 
		if (fViewStretch.fStyle != null) {
			metrics = getFontMetrics(fViewStretch.fStyle.getFont());
			lineHeight = Math.max(lineHeight, metrics.getHeight());
		}
		return lineHeight;
	}

	/** calculate height of line 
	  * @pre lineNr < fText.getNrOfLines()
	  * @post return > 0
	  */
	public int getLineHeight(int lineNr) {
		int cacheVal = fLineHeightCache.getHeight(lineNr);
		if (cacheVal != REDViewLineHeightCache.fcInvalid) {
			return cacheVal;
		}
		
		int lineHeight = calculateLineHeight(lineNr);
		
		// fill cache
		fLineHeightCache.fillCache(lineNr, lineHeight);
		
		return lineHeight;
	}		
	
	// scrollable interface
	public Dimension getPreferredScrollableViewportSize() {
		return fExtent;
	}

	/**
	  * @tbd could be refined to align to line/column boundaries
	  */
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		REDStyle s = fText.getDefaultStyle();
		FontMetrics metrics = getFontMetrics(s.getFont());
		if (orientation == SwingConstants.HORIZONTAL) {
			return metrics.getMaxAdvance();
		}
		else {
			return metrics.getHeight();
		}
	}

	/**
	  * @tbd could be refined to align to line/column boundaries
	  */
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		REDStyle s = fText.getDefaultStyle();
		FontMetrics metrics = getFontMetrics(s.getFont());
		if (orientation == SwingConstants.HORIZONTAL) {
			return visibleRect.width - metrics.getMaxAdvance();
		}
		else {
			return visibleRect.height - metrics.getHeight();
		}
	}

	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	public void scrollPageUp() {
		Rectangle r = getVisibleRect();
		r.y -= getScrollableBlockIncrement(r, SwingConstants.VERTICAL, -1);
		scrollRectToVisible(r);
	}

	public void scrollPageDown() {
		Rectangle r = getVisibleRect();
		r.y += getScrollableBlockIncrement(r, SwingConstants.VERTICAL, +1);
		scrollRectToVisible(r);
	}

	// focus listener interface
	public void focusGained(FocusEvent e) {
		callListeners(LI_GOTFOCUS);
		fCaretOn = true;
		if (fCaretTimer.getDelay() > 0) {
			fCaretTimer.start();
		}
		repaintSelection();
	}
	
	public void focusLost(FocusEvent e) {
		callListeners(LI_LOSTFOCUS);
		fCaretOn = false;
		fCaretTimer.stop();
		repaintSelection();
	}
	
	void selectLeft(int pace) {
		int x;
		
		fText.setCurTypingCommand(null);

		repaintSelection();
		x = getSelectionHotSpot();
		
		switch(pace) {
			case PACE_CHAR: x = charLeft(x);	break;		
			case PACE_WORD: x = wordLeft(x); break;
			case PACE_LINE: x = lineAbove(x, fXOffset); break;
			case PACE_LINEBOUND: x = fText.getLineStart(fText.getLineForPosition(x)); break;
			case PACE_PAGE: x = pageAbove(x, fXOffset); scrollPageUp(); break;
			case PACE_PAGEBOUND: x = pageTop(fXOffset); break;
			case PACE_DOCUMENT: x = 0; break;
			default: throw new Error("Unknown pace in selectLeft");
		}
		setSelection(getSelectionColdSpot(), x);
		adjustXOffset(pace);
		repaintSelection();
		revealSelection();
	}

	void selectRight(int pace) {
		int x;

		fText.setCurTypingCommand(null);
		repaintSelection();

		x = getSelectionHotSpot();
		switch(pace) {
			case PACE_CHAR: x = charRight(x); break;
			case PACE_WORD: x = wordRight(x); break;
			case PACE_LINE: x = lineBelow(x, fXOffset); break;
			case PACE_LINEBOUND: x = fText.getLineEnd(fText.getLineForPosition(x)); break;
			case PACE_PAGE: x = pageBelow(x, fXOffset); scrollPageDown(); break;
			case PACE_PAGEBOUND: x = pageBottom(fXOffset); break;
			case PACE_DOCUMENT: x = fText.length(); break;
			default: throw new Error("Unknown pace in selectRight");
		}

		setSelection(getSelectionColdSpot(), x);
		adjustXOffset(pace);
		repaintSelection();
		revealSelection();
	}
	
	/** The point where the selection will change. Depending on selection direction */
	private int getSelectionHotSpot() {
		if (fSelDir == DIR_LEFT_TO_RIGHT) {
			return Math.max(fSelFrom, fSelTo);
		}
		else {
			return fSelFrom;
		}
	}
	
	/** The point where the selection will change. Depending on selection direction */
	private int getSelectionColdSpot() {
		if (fSelDir == DIR_LEFT_TO_RIGHT) {
			return fSelFrom;
		}
		else {
			return Math.max(fSelFrom, fSelTo);
		}
	}
	
	/** move caret to the left / down
	  * @param pace How much to go right / down. Must be one of the PACE_XXX constants!
	  */
	void moveLeft(int pace) {
		fText.setCurTypingCommand(null);
		repaintSelection();
		int newSelFrom = getSelectionHotSpot();
		fSelDir = DIR_RIGHT_TO_LEFT;

		switch(pace) {
			case PACE_CHAR:
				if (!hasSelection()) {
					newSelFrom = charLeft(fSelFrom);
				}
				else {
					newSelFrom = fSelFrom;
				}
			break;
			case PACE_WORD: newSelFrom = wordLeft(newSelFrom); break;
			case PACE_LINE: newSelFrom = lineAbove(newSelFrom, fXOffset); break;
			case PACE_LINEBOUND: newSelFrom = fText.getLineStart(fText.getLineForPosition(newSelFrom)); break;
			case PACE_PAGE: newSelFrom = pageAbove(newSelFrom, fXOffset); scrollPageUp(); break;
			case PACE_PAGEBOUND: newSelFrom = pageTop(fXOffset); break;
			case PACE_DOCUMENT: newSelFrom = 0; break;
			default: throw new Error("Unknown pace in EDLView::MoveLeft");
		}
		setSelection(newSelFrom);
		fCaretOn = true;
		normalizeSelection();
		adjustXOffset(pace);		
		repaintSelection();
		revealSelection();	
	}
	
	void moveRight(int pace) {
		fText.setCurTypingCommand(null);
		repaintSelection();
		int newSelFrom = getSelectionHotSpot();
		fSelDir = DIR_LEFT_TO_RIGHT;
	
		switch(pace) {
			case PACE_CHAR: 
				if (!hasSelection()) {
					newSelFrom = charRight(Math.max(fSelFrom, fSelTo));
				}
				else {
					newSelFrom = Math.max(fSelFrom, fSelTo);
				}
			break;		
			case PACE_WORD: newSelFrom = wordRight(newSelFrom); break;
			case PACE_LINE: newSelFrom = lineBelow(newSelFrom, fXOffset); break;
			case PACE_LINEBOUND: newSelFrom = fText.getLineEnd(fText.getLineForPosition(newSelFrom)); break;
			case PACE_PAGE: newSelFrom = pageBelow(newSelFrom, fXOffset); scrollPageDown();  break;
			case PACE_PAGEBOUND: newSelFrom = pageBottom(fXOffset); break;
			case PACE_DOCUMENT: newSelFrom = fText.length(); break;
			default: throw new Error("Unknown pace in EDLView::MoveRight"); 
		}
		setSelection(newSelFrom);
		fCaretOn = true;		
		normalizeSelection();
		adjustXOffset(pace);		
		repaintSelection();
		revealSelection();	
	}
	
	void adjustXOffset(int pace) {
		REDViewPosition vp;
		switch(pace) {
			case PACE_CHAR: case PACE_WORD: case PACE_LINEBOUND: case PACE_DOCUMENT:
				if (fSelDir == DIR_LEFT_TO_RIGHT) {
					vp = locatePosition(fSelTo, null);
				}
				else {
					vp = locatePosition(fSelFrom, null);
				}
				fXOffset = vp.getLowerLeftPoint().x;
			break;
			default: break;
		}		
	}

	void normalizeSelection() {
		int x;
		
		fSelFrom = Math.max(0, fSelFrom); fSelFrom = Math.min(fText.length(), fSelFrom);
		fSelTo = Math.max(0, fSelTo); fSelTo = Math.min(fText.length(), fSelTo);
 
		if (fSelTo < fSelFrom) {
			x = fSelFrom; fSelFrom = fSelTo; fSelTo = x;
			if (fSelDir == DIR_LEFT_TO_RIGHT) {
				fSelDir = DIR_RIGHT_TO_LEFT;
			}
			else  {
				fSelDir = DIR_LEFT_TO_RIGHT;
			}
		}
	}

	
	int charLeft(int pos) {
		pos--;
		if (pos > 0 && fText.charAt(pos) == '\n' && fText.charAt(pos-1) == '\r') {
			pos--;
		}
		return Math.max(pos, 0);
	}
	
	int charRight(int pos) {
		if (pos+1 < fText.length() && fText.charAt(pos) == '\r' && fText.charAt(pos+1) == '\n') {
			pos++;
		}
		pos++;
		return Math.min(pos, fText.length());
	}
	
	int wordLeft(int pos) {
		while (pos > 0 && !isWordConstituent(fText.charAt(pos-1))) pos--;
		while (pos > 0 && isWordConstituent(fText.charAt(pos-1))) pos--;
		return pos;
	}
	
	int wordRight(int pos) {
		while (pos < fText.length() && !isWordConstituent(fText.charAt(pos))) pos++;
		while (pos < fText.length() && isWordConstituent(fText.charAt(pos))) pos++;
		return pos;
	}
	
	int lineAbove(int pos, int xoff) {
		REDViewPosition cur;
		
		cur = locatePosition(pos, null);
		if (xoff == -1) {
			xoff = cur.getLowerLeftPoint().x;
		}
		if (cur.getLineNumber() > 0) {
			cur = locatePoint(xoff, cur.getUpperLeftPoint().y - 1, cur, true);	// TBD: last parameter false, if overwrite mode
			return cur.getTextPosition();
		}
		return 0;
	}
	
	/**
	  * @post return >= 0
	  * @post return <= fText.length()
	  */
	int lineBelow(int pos, int xoff) {
		REDViewPosition cur;
		
		cur = locatePosition(pos, null);
		if (xoff == -1) {
			xoff = cur.getLowerLeftPoint().x;
		}
		cur = locatePoint(xoff, cur.getLowerLeftPoint().y + 1, cur, true);	// TBD: last parameter false, if overwrite mode
		return cur.getTextPosition();
	}
	
	int pageAbove(int pos, int xoff) {
		REDViewPosition vp = locatePosition(pos, null);
		if (xoff == -1) {
			xoff = vp.getLowerLeftPoint().x;
		}
		REDStyle s = fText.getDefaultStyle();
		FontMetrics metrics = getFontMetrics(s.getFont());
		REDViewPosition np = locatePoint(xoff, vp.getLowerLeftPoint().y - getVisibleRect().height + metrics.getHeight(), null, true);	// TBD: last parameter false, if overwrite mode

		return np.getTextPosition();
	}

	int pageBelow(int pos, int xoff) {
		REDViewPosition vp = locatePosition(pos, null);
		if (xoff == -1) {
			xoff = vp.getLowerLeftPoint().x;
		}
		REDStyle s = fText.getDefaultStyle();
		FontMetrics metrics = getFontMetrics(s.getFont());
		REDViewPosition np = locatePoint(xoff, vp.getUpperLeftPoint().y + getVisibleRect().height - metrics.getHeight(), null, true);		// TBD: last parameter false, if overwrite mode

		return np.getTextPosition();
	}

	int pageTop(int xoff) {
		Rectangle r = getVisibleRect();
		REDViewPosition vp = locatePoint(xoff, r.y, null);
		if (r.contains(vp.fBoundRect)) {
			return vp.fPosition;
		}
		else {
			return lineBelow(vp.fPosition, xoff);
		}
	}
	
	int pageBottom(int xoff) {
		Rectangle r = getVisibleRect();
		REDViewPosition vp = locatePoint(xoff, r.y + r.height, null);
		if (r.contains(vp.fBoundRect)) {
			return vp.fPosition;
		}
		else {
			return lineAbove(vp.fPosition, xoff);
		}
	}

	boolean hasSelection() {
		return fSelTo > fSelFrom;
	}

	String getSelectedText() {
		if (hasSelection()) {
			return fText.asString(fSelFrom, fSelTo);
		}
		return "";
	}
	
	/** Get focussed word. The focussed word is the identifier the caret (or start of selection) is in.
	  * @return A non-null String representing the focussed word.
	  */
	String getFocussedWord() {
		int from = fSelFrom;
		int to = fSelFrom;
		while (from > 0 && isWordConstituent(fText.charAt(from-1))) from--;
		while (to < fText.length() && isWordConstituent(fText.charAt(to))) to++;
		return fText.asString(from, to);
	}
	
	private void deleteSelection(boolean putOnClipboard) {
		// set cmdName
		String cmdName;
		if (putOnClipboard) {
			cmdName = "Cut";
		}
		else {
			cmdName = "Delete";
		}
		
		fText.setCurTypingCommand(null);
		REDCommandProcessor cmp = fText.getCommandProcessor(); 
		REDCommand cmd = null;
		if (hasSelection()) {
			if (putOnClipboard) {
				Clipboard clipboard = getToolkit().getSystemClipboard();
				StringSelection sel = new StringSelection(getSelectedText());
				clipboard.setContents(sel, sel);
			}
//			if (GetColumnSelectionMode()) {	TBD
//				MacroCommand* mCmd = new MacroCommand(cmdName);
//				IterateColumnSelection(&EDLView::CutColumnSelectionLines, mCmd);
//				cmp.PerformCommand(mCmd);
//			}
//			else {
//				Assert(fSelTo >= fSelFrom);
				cmd = new REDTextCommand(cmdName, this, fText, fSelFrom, fSelTo-fSelFrom, null);
				cmp.perform(cmd);					
//			}
			setSelection(fSelFrom);
			revealSelection();
		}		
	}
	
	boolean deleteSelection() {
		deleteSelection(false);
		return true; // TBD: false, if readonly mode
	}

	boolean clipboardCopy() {	
		if (!hasSelection()) return false;
		Clipboard clipboard = getToolkit().getSystemClipboard();
		StringSelection sel = new StringSelection(getSelectedText());
		clipboard.setContents(sel, sel);
		return true;
	}
	
	boolean clipboardCut() {
		if (!hasSelection()) return false;
		deleteSelection(true);
		return true;	// TBD: false, if readonly mode
	}
	
	boolean clipboardPaste() {
		// TBD: check for readonly mode
		Clipboard clipboard = getToolkit().getSystemClipboard();
		Transferable toPaste = clipboard.getContents(this);
		String str = null;
		if (toPaste != null) {
			try {
				str = (String)(toPaste.getTransferData(DataFlavor.stringFlavor));
			}
			catch (Exception e) { str = null; }
		}
		if (str != null && !str.isEmpty()) {
			fText.setCurTypingCommand(null);
			fText.getCommandProcessor().perform(new REDTextCommand("Paste", this, fText, fSelFrom, fSelTo - fSelFrom, str));
			return true;
		}
		return false;
	}
	
	/** Set caret or selection.
	  * The parameters from and to are normalized to be in range: [0, fLength].
	  * If from > to, the selection direction is "right to left" and the values will be swaped. Otherwise it is "left to right".
	  * @param from start of selection
	  * @param to end of selection
	  */
	void setSelection(int from, int to) {
		from = Math.max(from, 0); from = Math.min(from, fText.length());
		to = Math.max(to, 0); to = Math.min(to, fText.length()); 
		if (from > to) {
			fSelDir = DIR_RIGHT_TO_LEFT;
			int tmp = from; from = to; to = tmp;
		}
		else {
			fSelDir = DIR_LEFT_TO_RIGHT;
		}
		callListeners(LI_BEFORESELCHANGE, fSelFrom, fSelTo, from, to);
		int oldFrom = fSelFrom;
		int oldTo = fSelTo;
		repaintSelection();
		fSelFrom = from;
		fSelTo = to;
		adjustXOffset(PACE_CHAR);
		repaintSelection();
		callListeners(LI_AFTERSELCHANGE, oldFrom, oldTo, from, to);
	}
	
	/** Set caret to position 
	  * The parameter pos is normalized to be in range: [0, fLength] 
	  * @param pos position to caret to
	  */
	void setSelection(int pos) {
		setSelection(pos, pos);
	}

	/** adjust scroller so that selection can be seen
	  */
	void revealSelection() {
		Rectangle r;
		if (!hasSelection() || fSelDir == DIR_RIGHT_TO_LEFT) {
			r = locatePosition(fSelFrom, null).fBoundRect;
		}
		else {
			r = locatePosition(fSelTo, null).fBoundRect;
		}
		r.grow(10, 0);
		scrollRectToVisible(r);
	}
	
	boolean isWordConstituent(byte c) {
		return REDAuxiliary.isWordConstituent(c) || fWordConstituents.indexOf(c) != -1;	
	}
	
	/** Set word constituents. This method allows to specify characters which are considered to be part of words in addition to letters and digits.
	  * @param wordConstituents A string of characters to be considered word constituents.
	  */
	void setWordConstituents(String wordConstituents) {
		fWordConstituents = wordConstituents;
	}
	
	public boolean isFocusTraversable() {
		return true;
	}
	
	public boolean isManagingFocus() {
		return true;
	}
	
	public void propertyChange(PropertyChangeEvent e) {
		recalcExtentHeight();
		fExtent.width = 0;
		checkLineWidth(0, fText.getNrOfLines());
	}

	void repaintSelection() {
		int fromLine, toLine;
		if (hasSelection()) {
			fromLine = fText.getLineForPosition(fSelFrom);
			toLine = fText.getLineForPosition(fSelTo);
		}
		else {
			fromLine = fText.getLineForPosition(fSelFrom);
			toLine = fromLine;
		}
		repaintLines(fromLine, toLine);
	}
	
	void repaintLines(int fromLine, int toLine) {
		int fromLineTop = getLineTop(fromLine);
		int toLineBottom = getLineTop(toLine + 1) - 1;
		repaint(fInsets.top, fromLineTop + fInsets.top, getPreferredSize().width, toLineBottom - fromLineTop);
	}	

	void repaintLine(int line) {
		repaintLines(line, line);
	}	
	
	void setText(REDText text) {
		fText.removeREDTextEventListener(this);
		fText = text;
		fText.addREDTextEventListener(this);
		fExtent.width = fExtent.height = 0;
		resetLineTops();
		checkLineWidth(0, fText.getNrOfLines());
		fXOffset = -1;
		repaint();
	}
	
	/** set view mode
	  * @pre mode == REDAuxiliary.VIEWMODE_READONLY || mode == REDAuxiliary.VIEWMODE_INSERT || mode == REDAuxiliary.VIEWMODE_OVERWRITE
	  */
	void setMode(int mode) {
		if (mode != fMode) {
			callListeners(LI_BEFOREMODECHANGE, fMode, mode);
			int oldMode = fMode;
			fMode = mode;
			repaintSelection();
			fText.setCurTypingCommand(null);
			callListeners(LI_AFTERMODECHANGE, oldMode, mode);
		}
	}
	
	/** get view mode 
	  * @pre return == REDAuxiliary.VIEWMODE_READONLY || return == REDAuxiliary.VIEWMODE_INSERT || return == REDAuxiliary.VIEWMODE_OVERWRITE
	  */
	int getMode() {
		return fMode;
	}
	
	void setVisualizeWhitespace(boolean visualize) {
		if (visualize != fVisualizeWhitespace) {
			fVisualizeWhitespace = visualize;
			repaint();
		}
	}

	boolean getVisualizeWhitespace() {
		return fVisualizeWhitespace;
	}
	
	void setTabWidth(int tabWidth) {
		FontMetrics metrics = getFontMetrics(fText.getDefaultStyle().getFont());
		fTabWidth = tabWidth * metrics.charWidth(' ');
		recalcIndentString();
		checkLineWidth(0, fText.getNrOfLines());
		repaint();
	}
	
	int getTabWidth() {
		FontMetrics metrics = getFontMetrics(fText.getDefaultStyle().getFont());
		return fTabWidth / metrics.charWidth(' ');
	}
	
	void setMinTabWidth(int minWidth) {
		FontMetrics metrics = getFontMetrics(fText.getDefaultStyle().getFont());
		fTabMin = minWidth * metrics.charWidth(' ');
		checkLineWidth(0, fText.getNrOfLines());
		repaint();
	}
	
	int getMinTabWidth() {
		FontMetrics metrics = getFontMetrics(fText.getDefaultStyle().getFont());
		return fTabMin / metrics.charWidth(' ');
	}
	
	void setIndentWidth(int indent) {
		fIndentWidth = indent;
		recalcIndentString();
		checkLineWidth(0, fText.getNrOfLines());
		repaint();
	}
	
	int getIndentWidth() {
		return fIndentWidth;
	}

	/** Set indentation mode  */
	void setIndentMode(REDIndentMode mode) {
		fIndentMode = mode;
		recalcIndentString();
	}
	
	/** Get indentation mode. */
	REDIndentMode getIndentMode() {
		return fIndentMode;
	}
	
	private void recalcIndentString() {
		StringBuffer buf = new StringBuffer(fIndentWidth);
		if (fIndentMode == REDIndentMode.SPC) {
			for (int i = 0; i < fIndentWidth; i++) {
				buf.append(' ');
			}
		}
		else {
			int tw = getTabWidth();
			int iw = getIndentWidth();
			while (iw >= tw) {
				buf.append('\t');
				iw -= tw;
			}
			while (iw > 0) {
				buf.append(' ');
				iw--;
			}
		}
		fIndentString = new String(buf);
	}

	String getIndentString() {
		return fIndentString;
	}
		
	void adjustIndentation(int pos, REDIndentMode mode) {
		if (mode == REDIndentMode.ASIS) return;	// don't need to adjust in as is mode
		int start = pos;
		int width = 0;
		int tw = getTabWidth();
		int dirty = 0;
		byte c = fText.charAt(pos);
		while (c == '\t' || c == ' ') {
			if (c == '\t') {
				width += tw;
				if (mode == REDIndentMode.SPC) {
					dirty++;
				}
			}
			else {
				width++;
				if (mode == REDIndentMode.TAB) {
					dirty++;
				}
			}
			pos++;
			c = fText.charAt(pos);
		}
		if (dirty >= tw || mode == REDIndentMode.SPC && dirty > 0) {
			fText.setCurTypingCommand(null);
			StringBuffer buf = new StringBuffer(width);
			if (mode == REDIndentMode.SPC) {
				for (int i = 0; i < width; i++) {
					buf.append(' ');
				}
			}
			else {
				for (int i = 0; i < width / tw; i++) {
					buf.append('\t');
				}
				for (int j = 0; j < width % tw; j++) {
					buf.append(' ');
				}
			}
			String s = new String(buf);
			REDTextCommand cmd = new REDTextCommand ("Indent adjustment", this, fText, start, pos - start, s);
			fText.getCommandProcessor().perform(cmd);
		}
	}
	
	boolean isInIndentArea(int pos) {
		if (pos <= 0) return true;
		pos--;
		byte c = fText.charAt(pos);
		while (pos > 0 && (c == ' ' || c == '\t')) {
			pos--;
			c = fText.charAt(pos);
		}	
		return pos == 0 || REDAuxiliary.isLineBreak(c);
	}

	/** get preferred size <br>
	  * this implementation is overwritten to return at least the size needed to display the view
	  * @post return != null
	  */
	public Dimension getPreferredSize() {
		Dimension ps = getParent() == null ? null : getParent().getSize();
		Dimension d = super.getPreferredSize();

		fPrefSize.width = fExtent.width;
		fPrefSize.height = fExtent.height;

		if (d != null) {
			fPrefSize.width = Math.max(d.width, fPrefSize.width);
			fPrefSize.height = Math.max(d.height, fPrefSize.height);
		}
		if (ps != null) {
			fPrefSize.width = Math.max(ps.width, fPrefSize.width);
			fPrefSize.height = Math.max(ps.height, fPrefSize.height);
		}
		return fPrefSize;
	}
	
	/** Set line to highlight.
	  * The highlit line will be displayed with the highlight background color.
	  * It will not float, but disappear upon changes in the text.
	  * @param line The line to highlight. Use -1 to remove highlighting.
	  */
	public void setHighlightLine(int line) {
		int oldLine = fHighlightLine;
		fHighlightLine = Math.min(Math.max(line, -1), fText.getNrOfLines());
		if (oldLine != -1) {
			repaintLine(oldLine);
		}
		if (fHighlightLine != -1) {
			repaintLine(fHighlightLine);
		}
	}
	
	/** Set highlight color.
	  * The default highlight color is yellow. 
	  * @param color The new highlight color. Only non-null values will be accepted.
	  */
	public void setHighlightColor(Color color) {
		if (color != null) {
			fHighlightColor = color;
		}
	}
	
	/** Get highlight line status.
	  * @return <Code>true</Code> if view currently has a highlight line. <Code>false</Code> otherwise.
	  */
	public boolean hasHighlightLine() {
		return fHighlightLine != -1;
	}
	
	/** Get highlight line number.
	  * @return The current highlight number or -1, if no line is currently highlit.
	  */
	public int getHighlightLine() {
		return fHighlightLine;
	}

	int debugGetLineTop(int lineNr) {
		int lineMax, topLine = 0;
		int lineStart = 0, lineEnd = 0;
		for (int i = 0; i < lineNr; i++) {
			lineStart = lineEnd;
			lineEnd = fText.getLineStart(i + 1);
			lineMax = 0;
			for (int j = lineStart+1; j <= lineEnd; j++) {
				REDStyle s = fText.getStyle(j); 
				FontMetrics metrics = getFontMetrics(s.getFont());
				lineMax = Math.max(lineMax, metrics.getHeight());
			}
			topLine += lineMax;
		}
		return topLine;
	}

	REDText fText;
	private REDViewStretch fViewStretch;
	private final ArrayList fPaintBatch;
	private int fTabMin, fTabWidth, fIndentWidth;
	private Graphics fGraphics;
	REDLineTree fTopLines;
	private int[] fStyleChangeBuffer;
	private int fDeleteBuffer, fDeleteEndLine;
	private int fBeforeCorrection;
	private Dimension fExtent;
	private final Dimension fPrefSize;
	int fSelFrom, fSelTo;	// caret position (fSelTo <= fSelFrom) or selection (otherwise)
	int fSelDir;
	private int fXOffset;
	private final javax.swing.Timer fCaretTimer;
	private boolean fCaretOn;
	private final ArrayList fListeners;
	private int fMode;
	private boolean fVisualizeWhitespace;
	private REDIndentMode fIndentMode;
	private String fIndentString;
	private byte [] fScratchBuffer;
	private REDViewPosition fCaretViewPosition;
	private Insets fInsets;
	private final REDViewLineHeightCache fLineHeightCache;
	private String fWordConstituents;
	private int fHighlightLine;
	private Color fHighlightColor;
	{
		fViewStretch = null;
		fTopLines = new REDLineTree();
		fStyleChangeBuffer = new int[2];
		fExtent = null;
		fPrefSize = new Dimension();
		fSelFrom = fSelTo = 0;
		fSelDir = DIR_LEFT_TO_RIGHT;
		fCaretOn = hasFocus();
		fCaretTimer = new javax.swing.Timer(CARET_BLINK_FREQUENCY, new CaretBlink());
		if (fCaretOn) {
			fCaretTimer.start();
		}
		addPropertyChangeListener("border", this);
		fListeners = new ArrayList(REDAuxiliary.fcListenerSize);
		fMode = REDAuxiliary.VIEWMODE_INSERT;
		fVisualizeWhitespace = false;
		fIndentMode = REDAuxiliary.fcDefaultIndentMode;
		fIndentString = null;
		fScratchBuffer = null;
		fCaretViewPosition = new REDViewPosition();
	}
}
