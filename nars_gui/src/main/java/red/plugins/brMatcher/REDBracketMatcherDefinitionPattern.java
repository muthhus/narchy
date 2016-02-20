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
 
package red.plugins.brMatcher;

import java.util.*;
import red.*;
import red.xml.*;

/** Bracket matcher definition pattern. These define the patterns to match.
  * Note that, despite the name "bracket" matcher, patterns do not have to 
  * be brackets necessarily. They can be any string.
  * No regular expressions here.
  * @author rli@chello.at
  * @tier system
  * @see REDBracketMatcherDefinition
  */
public class REDBracketMatcherDefinitionPattern implements REDXMLReadable {
	public REDBracketMatcherDefinitionPattern() {
		fLeft = null;
		fRight = null;
		fStyleSet = null;
		fRuleSet = null;
		fNested = true;
	}
	
	public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
		handler.mapStart("#", "defineSets(#$, #StyleSet, #RuleSet)");
		handler.mapEnd("Left", "setLeft(#)");
		handler.mapEnd("Right", "setRight(#)");
		handler.mapEnd("Nested", "setNested((boolean) #='true'");
	}
	
	public void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer) {
	}

	/** XML callback routine. */
	public void defineSets(REDXMLManager manager, String styleSet, String ruleSet) {
		fStyleSet = (REDBracketMatcherDefinitionStyleSet) manager.getClientData("styleSet." + styleSet);
		fRuleSet = (REDBracketMatcherDefinitionRuleSet) manager.getClientData("ruleSet." + ruleSet);
	}

	/** XML callback routine. */
	public void setLeft(String left) {
		fLeft = left;
	}

	/** XML callback routine. */
	public void setRight(String right) {
		fRight = right;
	}

	/** XML callback routine. */
	public void setNested(boolean nested) {
		fNested = nested;
	}
	
	void setDefinition(REDBracketMatcherDefinition def) {
		fDef = def;
	}
	
	/** Is pattern fully defined and thus usable. */
	boolean isOk() {
		return fLeft != null && !fLeft.isEmpty() &&
			fRight != null && !fRight.isEmpty() &&
			fStyleSet != null && fRuleSet != null;
	}

	static String mirrorString(String str) {
		return String.valueOf(new StringBuffer(str).reverse());
	}

	private boolean nrLinesOk(int curLine, int startLine) {
		return Math.abs(curLine - startLine) <= fDef.getMaxLines();
	}
	
	private boolean nrCharsOk(int curPos, int startPos) {
		return Math.abs(curPos - startPos) <= fDef.getMaxChars();
	}
	
	private boolean isIgnoredStyle(REDStyle style) {
		Iterator iter = fStyleSet.getIgnoreStylesIterator();
		while (iter.hasNext()) {
			if (style.isA(REDStyleManager.getStyle(String.valueOf(iter.next())))) {
				return true;
			}
		}
		return false;
	}
	
	private boolean styleOk(REDEditor editor, int pos, int preOff) {
		REDStyle tbNotIgnored = editor.getStyle(pos);
		REDStyle tbPre = editor.getStyle(pos + preOff);
		REDStyle prestyle = fStyleSet.getPreStyle();
		return !isIgnoredStyle(tbNotIgnored) && (prestyle == null || tbPre.isA(prestyle));
	}
	
	
	private boolean mayAfterRight(REDEditor editor, int pos, boolean dblClick) {
		if (dblClick) return false;
		return fRuleSet.getAfterRight() && editor.copy(pos - fRight.length(), pos).equals(fRight) 
			&& styleOk(editor, pos, -1);
	}
	
	private boolean mayAfterLeft(REDEditor editor, int pos, boolean dblClick) {
		String s = editor.copy(pos - fLeft.length(), pos);
		return (dblClick || fRuleSet.getAfterLeft()) && editor.copy(pos - fLeft.length(), pos).equals(fLeft) 
			&& styleOk(editor, pos, 1);
	}
	
	private boolean mayBeforeRight(REDEditor editor, int pos, boolean dblClick) {
		return (dblClick || fRuleSet.getBeforeRight()) && editor.copy(pos, pos + fRight.length()).equals(fRight) 
			&& styleOk(editor, pos + 1, 0);
	}
	
	private boolean mayBeforeLeft(REDEditor editor, int pos, boolean dblClick) {
		if (dblClick) return false;
		return fRuleSet.getBeforeLeft() && editor.copy(pos, pos + fLeft.length()).equals(fLeft) 
			&& styleOk(editor, pos + 1, 1);
	}
	
	/** Try and find a match. 
	  * @param editor The editor the matching should happen in.
	  * @param pos The current position of the caret.
	  * @param forDoubleClick if <Code>true</Code> evaluate patterns for double clicks.
	  * @return The position matching, or -1 if no such position exists.
	  */
	public int findMatch(REDEditor editor, int pos, boolean forDoubleClick) {
		if (forDoubleClick && !fRuleSet.getDoubleClickSelect()) return -1;

		int sStart = 0, retVal = -1;
		String s = null, t = null;
		REDStreamDirection dir = null;
		if (mayAfterRight(editor, pos, forDoubleClick)) {
			s = fRight; t = fLeft;
			dir = REDStreamDirection.BACKWARD;
			sStart = pos - s.length();
		}
		else if (mayBeforeLeft(editor, pos, forDoubleClick)) {
			s = fLeft; t = fRight;
			dir = REDStreamDirection.FORWARD;
			sStart = pos + s.length();
		}
		else if (mayBeforeRight(editor, pos, forDoubleClick)) {
			s = fRight; t = fLeft;
			dir = REDStreamDirection.BACKWARD;
			sStart = pos;
		}
		else if (mayAfterLeft(editor, pos, forDoubleClick)) {
			s = fLeft; t = fRight;
			dir = REDStreamDirection.FORWARD;
			sStart = pos;
		}

		if (s != null) {
			if (!fNested) {
				s = "";
			}
	
			if (dir == REDStreamDirection.BACKWARD) {
				t = mirrorString(t);
			}
			retVal = doFindMatch(editor, sStart, s, t, dir);
			if (dir == REDStreamDirection.BACKWARD && retVal != -1 && forDoubleClick) {
				retVal += t.length();
			}
		}
		return retVal;
	}
	
	private static boolean bufEquals(char[] buf, int idx, String str) {
		int x = 0; idx = (idx + buf.length - str.length() + 1) % buf.length;
		while (x < str.length() && str.charAt(x) == buf[(idx + x) % buf.length]) {
			x++;
		}
		return x == str.length() && x > 0;
	}

	int doFindMatch(REDEditor editor, int sStart, String s, String t, REDStreamDirection dir) {
		REDStream stream = editor.createStream(sStart, dir);
		int mCount = 1;
		int startLine = editor.getLineForPosition(sStart);
		int curLine = startLine;
		char [] buf = new char[Math.max(s.length(), t.length())];
		int idx = -1;

		Iterator iter = fStyleSet.getIgnoreStylesIterator();
		while (iter.hasNext()) {
			stream.excludeStyle((String) iter.next(), true);
		}

		while (mCount != 0 && !stream.eof() && nrLinesOk(curLine, startLine) && nrCharsOk(stream.getPosition(), sStart)) {
			idx = (idx + 1) % buf.length;
			buf[idx] = stream.readChar();
			if (bufEquals(buf, idx, s)) {
				mCount++;
			}
			else if (bufEquals(buf, idx, t)) {
				mCount--;
			}
			else if (buf[idx] == '\r' || buf[idx]== '\n') {
				curLine = editor.getLineForPosition(stream.getPosition());
			}
		}
		
		if (mCount == 0) {
			if (dir == REDStreamDirection.FORWARD) {
				return stream.getPosition() - t.length();
			}
			else {
				return stream.getPosition();
			}
		}
		return -1;
	}

	/** The left (opening bracket) pattern. */
	String fLeft;

	/** The right (closing bracket) pattern. */
	String fRight;

	/** The style set to be used with this pattern. */
	REDBracketMatcherDefinitionStyleSet fStyleSet;
	
	/** The rule set to be used with this pattern. */
	REDBracketMatcherDefinitionRuleSet fRuleSet;	
	
	/** The definition this pattern belongs to. */
	REDBracketMatcherDefinition fDef;

	/** Take nested patterns into account. */
	boolean fNested;
}
	