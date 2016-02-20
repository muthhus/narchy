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

import java.awt.event.*;
import javax.swing.event.*;

/**  Defines standard editor feel and consumes input from a REDView 
  * @author rli@chello.at
  * @tier API
  */
public class REDViewController implements KeyListener, MouseInputListener {
	/** 
	  * @pre v != null
	  */
	public REDViewController() {
//		v.addKeyListener(this);
//		v.addMouseListener(this);
//		v.addMouseMotionListener(this);
	}
	
	/**
	  * @pre v != null
	  */
	static boolean needTypingCmd(REDView v, String cmdType) {
		REDText text = v.fText; 
		REDTextCommand cmd = text.getCurTypingCommand();
		
		return (cmd == null || !cmd.getDescription().equals(cmdType) || cmd.getView() != v);
	}

	public void crsLeft(REDView v, KeyEvent e) {
		int pace = REDView.PACE_CHAR;
		if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
			pace = REDView.PACE_WORD;
		}
		
		if ((e.getModifiers() & InputEvent.SHIFT_MASK	) == 0) {
			v.moveLeft(pace);
		}
		else {
			v.selectLeft(pace);
		}
		e.consume();
	}
	
	public void crsRight(REDView v, KeyEvent e) {
		int pace = REDView.PACE_CHAR;
		if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
			pace = REDView.PACE_WORD;
		}
		
		if ((e.getModifiers() & InputEvent.SHIFT_MASK	) == 0) {
			v.moveRight(pace);
		}
		else {
			v.selectRight(pace);
		}
		e.consume();
	}

	public void crsUp(REDView v, KeyEvent e) {
		int pace = REDView.PACE_LINE;
		if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
			pace = REDView.PACE_PAGEBOUND;
		}

		if ((e.getModifiers() & InputEvent.SHIFT_MASK	) == 0) {
			v.moveLeft(pace);
		}
		else {
			v.selectLeft(pace);
		}
		e.consume();
	}

	public void crsDown(REDView v, KeyEvent e) {
		int pace = REDView.PACE_LINE;
		if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
			pace = REDView.PACE_PAGEBOUND;
		}

		if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
			v.moveRight(pace);
		}
		else {
			v.selectRight(pace);
		}
		e.consume();
	}

	public void crsPageUp(REDView v, KeyEvent e) {
		int pace = REDView.PACE_PAGE;
		if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
			v.moveLeft(pace);
		}
		else {
			v.selectLeft(pace);
		}
                e.consume();
	}

	public void crsPageDown(REDView v, KeyEvent e) {
		int pace = REDView.PACE_PAGE;
		if ((e.getModifiers() & InputEvent.SHIFT_MASK) == 0) {
			v.moveRight(pace);
		}
		else {
			v.selectRight(pace);
		}
                e.consume();
	}

	public void crsHome(REDView v, KeyEvent e) {
		int pace = REDView.PACE_LINEBOUND;
		if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
			pace = REDView.PACE_DOCUMENT;
		}
		
		if ((e.getModifiers() & InputEvent.SHIFT_MASK	) == 0) {
			v.moveLeft(pace);
		}
		else {
			v.selectLeft(pace);
		}
		e.consume();
	}

	public void crsEnd(REDView v, KeyEvent e) {
		int pace = REDView.PACE_LINEBOUND;
		if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
			pace = REDView.PACE_DOCUMENT;
		}
		
		if ((e.getModifiers() & InputEvent.SHIFT_MASK	) == 0) {
			v.moveRight(pace);
		}
		else {
			v.selectRight(pace);
		}
		e.consume();
	}

	public void keyBackspace(REDView v, KeyEvent e) {
		REDTextCommand cmd;
		REDText text = v.fText;
		int selStart = v.fSelFrom;
		int selEnd = v.fSelTo;
		
		e.consume();
		if (selStart != selEnd) {	// we have a selection => delete it
			keyDelete(v, e);
			return;
		}
		
		if (selStart > 0) {
			selStart = v.charLeft(selStart);
			v.setSelection(selStart);
			cmd = text.getCurTypingCommand();
			if (needTypingCmd(v, "Backspace")) {
				cmd = new REDTextCommand("Backspace", v, text, selStart, v.charRight(selStart) - selStart, null);
				text.setCurTypingCommand(cmd);
				text.getCommandProcessor().perform(cmd);
			}
			else {
				cmd.backspace();
				text.replace(selStart, v.charRight(selStart), "");
			}
		}
		v.revealSelection();
	}

	public void keyDelete(REDView v, KeyEvent e) {
		REDTextCommand cmd;
		REDText text = v.fText;
		int selStart = v.fSelFrom;
		int selEnd = v.fSelTo;
		
		e.consume();
		if (selStart < text.length()) {
			if (!v.hasSelection()) {
				selEnd = v.charRight(selEnd);
			}
			v.setSelection(selStart);
			cmd = text.getCurTypingCommand();
			if (needTypingCmd(v, "Delete")) {
				cmd = new REDTextCommand("Delete", v, text, selStart, selEnd-selStart, null);
				text.setCurTypingCommand(cmd);
				text.getCommandProcessor().perform(cmd);
			}
			else {
				cmd.delete();
				text.replace(selStart, v.charRight(selStart), null);
			}
		}
		v.revealSelection();
	}

	public void keyInsert(REDView v, KeyEvent e) {
	}

	public void keyNewline(REDView v, KeyEvent e) {
		REDTextCommand cmd;
		REDText text = v.fText;
		
		cmd = new REDTextCommand("Newline", v, text, v.fSelFrom, 0, fLinebreak);
		text.setCurTypingCommand(cmd);
		text.getCommandProcessor().perform(cmd);
		e.consume();
		v.revealSelection();
	}

	public void keyEsc(REDView v, KeyEvent e) {
	}

	private static boolean atLineStart(REDText text, int pos) {
		if (pos <= 0) return true;
		pos--;
		byte c = text.charAt(pos);
		while (pos > 0 && (c == ' ' || c == '\t')) {
			pos--;
			c = text.charAt(pos);
		}
		return pos == 0 || REDAuxiliary.isLineBreak(c);
	}
	
	public void keyTab(REDView v, KeyEvent e) {
		if (v.hasSelection() || v.getMode() == REDAuxiliary.VIEWMODE_OVERWRITE || !atLineStart(v.fText, v.fSelFrom)) {
			key(v, e);
			return;
		}
		if ( (e.getModifiers() & (InputEvent.CTRL_MASK | InputEvent.ALT_MASK)) != 0) return;
	
		e.consume();
		REDText text = v.fText;
		int selStart = v.fSelFrom;
		text.setCurTypingCommand(null);

		REDTextCommand cmd = new REDTextCommand ("Indent", v, text, selStart, 0, v.getIndentString());
		text.getCommandProcessor().perform(cmd);
		v.adjustIndentation(text.getLineStart(text.getLineForPosition(selStart)), v.getIndentMode());
		v.revealSelection();
	}

	public void key(REDView v, KeyEvent e) {
		if ( (e.getModifiers() & (InputEvent.CTRL_MASK | InputEvent.ALT_MASK)) != 0) return;
	
		e.consume();
		REDText text = v.fText;
		int selStart = v.fSelFrom;
		int selEnd = v.fSelTo;
		REDTextCommand cmd = text.getCurTypingCommand();
	
		if (needTypingCmd(v, "Typing")) {
			if (v.hasSelection()) {
				cmd = new REDTextCommand ("Replace", v, text, selStart, selEnd-selStart, String.valueOf(e.getKeyChar()));
			}
			else {
				if (v.getMode() == REDAuxiliary.VIEWMODE_INSERT || selStart == text.length() || selStart == text.getLineEnd(text.getLineForPosition(selStart))) {	
					cmd = new REDTextCommand ("Typing", v, text, selStart, 0, String.valueOf(e.getKeyChar()));
				}
				else {
					cmd = new REDTextCommand("Replace", v, text, selStart, 1, String.valueOf(e.getKeyChar()));
				}
			}
			text.setCurTypingCommand(cmd);
			text.getCommandProcessor().perform(cmd);		
		}
		else {
			text.replace(selStart, selStart, String.valueOf(e.getKeyChar()));
			cmd.addChar();		
			v.setSelection(++selStart);
		}
		selStart = v.fSelFrom; 
		if (v.isInIndentArea(selStart)) {
			int lineNr = text.getLineForPosition(selStart);
			v.adjustIndentation(lineNr, v.getIndentMode());
		}
		v.revealSelection();		
	}
	
//	boolean matchParenthesis(int clickPos, int& start, int& end) {
//		REDText* text = fView.GetText();
//		int textlength = text.GetLength();
//		int oldClickPos;
//		if (clickPos == 0 || clickPos == textlength) {
//			return false;
//		}
//		String before = text.GetCopy(clickPos-1, clickPos);
//		String after = text.GetCopy(clickPos, clickPos+1);
//		char t = '\0', u, ch, sch;
//		int direction, matchCount;
//			
//		if (before == "(") t = ')';
//		if (before == "[") t = ']';
//		if (before == "{") t = '}';
//		if (before == "<") t = '>';
//		if (t != '\0') {
//			direction = 1;
//			start = clickPos;
//			u = before[0];
//		}
//		else {
//			direction = -1;
//			end = clickPos;
//			if (after == ")") t = '(';
//			if (after == "]") t = '[';
//			if (after == "}") t = '{';
//			if (after == ">") t = '<';
//			u = after[0];
//			clickPos--;
//		}
//		
//		if (t != '\0' && clickPos > 0) {
//			matchCount = 1;
//			clickPos += direction;
//			ch = (*text)[clickPos];
//			while (matchCount && clickPos > 0 && clickPos < textlength) {
//				if (ch == '"' || ch == '\'') {
//					sch = ch;
//					oldClickPos = clickPos;
//					clickPos += direction;	
//					ch = (*text)[clickPos];
//					while (ch != sch && !REDTypes::IsLineBreak(ch) && clickPos > 0 && clickPos <= textlength) {
//						clickPos += direction;
//						ch = (*text)[clickPos];
//					}
//					if (REDTypes::IsLineBreak(ch)) {
//						clickPos = oldClickPos;
//					}
//					if (clickPos > 0 && clickPos < textlength) {
//						clickPos += direction;
//						ch = (*text)[clickPos];
//					}				
//				}
//				else {
//					if (ch == u) {
//						matchCount++;
//					}
//					else if (ch == t) {
//						matchCount--;
//					}
//					clickPos += direction;	
//					ch = (*text)[clickPos];
//				}
//			}
//			if (direction == 1) {
//				end = fView.CharLeft(clickPos);
//			}
//			else {
//				clickPos++;
//				start = fView.CharRight(clickPos);
//			}
//			return matchCount == 0;
//		}
//		else {
//			return false;
//		}
//	}	
		
	/** left mouse button event 
	  * @param v the view the event happened in 
	  * @param e the event (use e.getID() to determine kind of event)
	  */
	public void mouseLeft(REDView v, MouseEvent e) {
		REDViewPosition vp = null;
		v.requestFocus();
		v.fText.setCurTypingCommand(null);
		switch (e.getID()) {
			case MouseEvent.MOUSE_PRESSED:
			case MouseEvent.MOUSE_RELEASED:
				vp = v.locatePoint(e.getX(), e.getY(), null, v.getMode() != REDAuxiliary.VIEWMODE_OVERWRITE);	
			break; 
			case MouseEvent.MOUSE_DRAGGED:
				vp = v.locatePoint(e.getX(), e.getY(), null, true);		// we always want to use midSplit when making a selection!
			break;
		}

		int a, b, from, to;
	
		switch (e.getID()) {
			case MouseEvent.MOUSE_PRESSED:
				fClicks = e.getClickCount();
				if (fLineSelection)
				{
					fStart = v.locateLineStart(vp.getLineNumber(), null);
					fEnd = v.locateLineStart(vp.getLineNumber() + 1, null);
					fOStart = fStart; fOEnd = fEnd;
				} 
				else if ( (e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
					from = v.fSelFrom;
					to = v.fSelTo;
					if (vp.getTextPosition() >= from) {
						fStart = v.locatePosition(from, null); 
//						fEnd = v.locatePosition(to, fEnd);
					}
					else {
						fStart = v.locatePosition(to, null); 
//						fEnd = v.locatePosition(from, fEnd);
					}
					fEnd = vp;
				}
				else {
					if (fClicks == 1) {
						fStart = vp;
						fEnd = vp;
					}
					else if (fClicks == 2) {
						if (false) {	// TBD
//						if (matchParenthesis(vp.getTextPosition(), from, to)) {
							fStart = v.locatePosition(from, null);
							fEnd = v.locatePosition(to, null);
							fClicks = 0;
						}
						else {
							if (vp.getTextPosition() > 0 && v.isWordConstituent(v.fText.charAt(vp.getTextPosition() - 1))) {
								fStart = v.locatePosition(v.wordLeft(vp.getTextPosition()), null);
							}
							else {
								fStart = vp;
							}
							if (v.isWordConstituent(v.fText.charAt(vp.getTextPosition()))) {
								fEnd = v.locatePosition(v.wordRight(vp.getTextPosition()), null);
							}
							else {
								fEnd = vp;
							}
						}
					}
					else if (fClicks >= 3) {
						fStart = v.locateLineStart(vp.getLineNumber(), null);
						fEnd = v.locateLineStart(vp.getLineNumber()+1, null);
					}
				}
				if (fClicks >= 2) {
					fOStart = fStart; fOEnd = fEnd;
				}
			break;
			case MouseEvent.MOUSE_DRAGGED: 
			case MouseEvent.MOUSE_RELEASED:	
				if(fLineSelection) {
					if (fClicks == 1) {
						fStart = fOStart;
						fEnd = v.locateLineStart(vp.getLineNumber() + 1, null);
					}
				} 
				else {	
					if (fClicks == 1) {
						fEnd = vp; 
					}
					else if (fClicks == 2) {
						if (v.fSelDir == REDView.DIR_LEFT_TO_RIGHT) {
							fStart = fOStart;
							if (v.isWordConstituent(v.fText.charAt(vp.getTextPosition()))) {
								fEnd = v.locatePosition(v.wordRight(vp.getTextPosition()), null);
							}
							else {
								fEnd = vp;
							}
						}
						else {
							fStart = fOEnd;
							if (vp.getTextPosition() > 0 && v.isWordConstituent(v.fText.charAt(vp.getTextPosition() - 1))) {
								fEnd = v.locatePosition(v.wordLeft(vp.getTextPosition()), null);
							}
							else {
								fEnd = vp;
							}
						}
					}	
					else if (fClicks >= 3) {
						if (v.fSelDir == REDView.DIR_LEFT_TO_RIGHT) {
							fStart = fOStart;
							fEnd = v.locateLineStart(vp.getLineNumber()+1, null);
						}
						else {
							fStart = fOEnd;
							fEnd = v.locateLineStart(vp.getLineNumber(), null);
						}
					}
				}
			break;
			default: break;
		}
	
		a = fStart.getTextPosition();
		b = fEnd.getTextPosition();
		if (a <= b) {
			v.setSelection(a,b);
			v.fSelDir = REDView.DIR_LEFT_TO_RIGHT;
		}
		else {
			v.setSelection(b,a);
			v.fSelDir = REDView.DIR_RIGHT_TO_LEFT;
		}
		v.revealSelection();
	}

	public void mouseMiddle(REDView v, MouseEvent e) {
	}

	public void mouseRight(REDView v, MouseEvent e) {
	}

    public void setLinebreak(String lb) {
    	fLinebreak = lb;
	}

    public String getLinebreak() {
    	return fLinebreak;
	}
	
	public void keyTyped(KeyEvent e) {
		REDView v = (REDView) e.getSource();
		char c = e.getKeyChar();
		if (Character.isISOControl(c)) {
			switch(c) {
				case '\n': case '\r':
					keyNewline(v, e);
				break;
				case '\b':
					keyBackspace(v, e);
				break;
//				default:
//					REDTracer.info("red", "REDViewController", "Other: " + c + ", which is code: " + (int) c);
//				break;
			}
		}
		else {
			key(v, e);
		}
	}	 
	
	public void keyPressed(KeyEvent e) {
		REDView v = (REDView) e.getSource();
		switch (e.getKeyCode()) {
			case KeyEvent.VK_TAB: keyTab(v, e); break;
			case KeyEvent.VK_ESCAPE: keyEsc(v, e); break;
			case KeyEvent.VK_PAGE_UP: crsPageUp(v, e); break;
			case KeyEvent.VK_PAGE_DOWN: crsPageDown(v, e); break;
			case KeyEvent.VK_END: crsEnd(v, e); break;
			case KeyEvent.VK_HOME: crsHome(v, e); break;
			case KeyEvent.VK_LEFT: crsLeft(v, e); break;
			case KeyEvent.VK_UP: crsUp(v, e); break;
			case KeyEvent.VK_RIGHT: crsRight(v, e); break;
			case KeyEvent.VK_DOWN: crsDown(v, e); break;
			case KeyEvent.VK_DELETE: keyDelete(v, e); break;
			case KeyEvent.VK_INSERT: keyInsert(v, e); break;
//			default: 
//				REDTracer.info("red", "REDViewController", "Keypressed: " + e.getKeyCode());
//			break;
		}
	}	 
	
	public void keyReleased(KeyEvent e) {
	}	 
	
	public void mouseClicked(MouseEvent e) {
//		REDView v = (REDView) e.getSource();
//		int mod = e.getModifiers();
//		if ( (mod & InputEvent.BUTTON1_MASK) != 0) {
//			mouseLeft(v, e);
//		}
//		else if ( (mod & InputEvent.BUTTON2_MASK) != 0) {
//			mouseMiddle(v, e);
//		}
//		else if ( (mod & InputEvent.BUTTON3_MASK) != 0) {
//			mouseRight(v, e);
//		}
	}
	
	public void mousePressed(MouseEvent e) {
		REDView v = (REDView) e.getSource();
		int mod = e.getModifiers();
		if ( (mod & InputEvent.BUTTON1_MASK) != 0) {
			mouseLeft(v, e);
		}
		else if ( (mod & InputEvent.BUTTON2_MASK) != 0) {
			mouseMiddle(v, e);
		}
		else if ( (mod & InputEvent.BUTTON3_MASK) != 0) {
			mouseRight(v, e);
		}
	}
	
	public void mouseReleased(MouseEvent e) {
	}
	
	public void mouseEntered(MouseEvent e) {
	}
	
	public void mouseExited(MouseEvent e) {
	}
	
	public void mouseDragged(MouseEvent e) {
		REDView v = (REDView) e.getSource();
		int mod = e.getModifiers();
		if ( (mod & InputEvent.BUTTON1_MASK) != 0) {
			mouseLeft(v, e);
		}
		else if ( (mod & InputEvent.BUTTON2_MASK) != 0) {
			mouseMiddle(v, e);
		}
		else if ( (mod & InputEvent.BUTTON3_MASK) != 0) {
			mouseRight(v, e);
		}
	}
	
	public void mouseMoved(MouseEvent e) {
	}
	
    String fLinebreak;
    boolean fLineSelection;
	REDViewPosition fStart, fEnd, fOStart, fOEnd;	// mouse selection 
	int fClicks;
    {
    	fLineSelection = false;
    	fLinebreak = "\n";	// TBD
    	fStart = null;
    	fEnd = null;
    	fOStart = null;
    	fOEnd = null;
    	fClicks = 0;
    }
}
