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
 
package red.plugins.synHi;

import red.*;
import red.rexParser.*;
import red.util.*;
import red.xml.*;
import java.util.*;

/** Range rule. 
  * Range rules have a start and end regexp. They can be used for literals delimited by special characters (strings, comments, etc.)
  * @author rli@chello.at
  * @tier system
  * @see REDSyntaxHighlighterRule
  */
public class REDSyntaxHighlighterRange extends REDSyntaxHighlighterRule implements REDXMLReadable {
	public REDSyntaxHighlighterRange() {
		super();
		fSubParsers = new ArrayList();
		fRewind = false;
	}
	
	public REDSyntaxHighlighterRange(String startRegEx, String endRegEx, REDStyle style) {
		this();
		fStart = startRegEx; 
		fEnd = endRegEx;
		fStyle = style;
	}
	
	public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
		super.setMappings(handler);
		handler.mapEnd("Start", "setStart(#)");
		handler.mapEnd("End", "setEnd(#)");
		handler.mapEnd("Rewind", "setRewind()");
		handler.mapStart("SubParsers", "subparsersOn(#&)");
		handler.mapEnd("SubParsers", "subparsersOff(#&)");
	}
	
	public void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer) {
		if (outer.peekClientData("subparsers") != null) {
			if (obj instanceof REDSyntaxHighlighterRule) {
				addSubParser((REDSyntaxHighlighterRule) obj);
			}
			else {
				REDTracer.warning("red.plugins.synHi", "REDSyntaxHighlighterRange", "Unknown inner production ignored: " + obj);
			}				
		}
		else {
			REDTracer.warning("red.plugins.synHi", "REDSyntaxHighlighterRange", "Inner production ignored, because subparser tag not encountered: " + obj);
		}
	}

	
	public void setStart(String start) {
		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighterRange", "Start RegExp set for range: " + start);
		fStart = start;
	}
	
	public void setEnd(String end) {
		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighterRange", "End RegExp set for range: " + end);
		fEnd = end;
	}
	
	public void setRewind() {
		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighterRange", "Rewind turned on for range.");
		fRewind = true;
	}
	
	public static void subparsersOn(REDXMLHandlerReader handler) {
		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighterRange", "Subparsers turned on for range.");
		handler.pushClientData("subparsers", Boolean.TRUE);
	}
	
	public static void subparsersOff(REDXMLHandlerReader handler) {
		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighterRange", "Subparsers turned off for range.");
		handler.popClientData("subparsers");
	}

	void addSubParser(REDSyntaxHighlighterRule rule) {
		fSubParsers.add(rule);
	}
	
	void setRewind(boolean rewind) {
		fRewind = rewind;
	}
	
	boolean getRewind() {
		return fRewind;
	}
	
	void installInParser(int state, boolean caseSensitive, REDRexParser parser, REDStyle envStyle) throws REDRexMalformedPatternException {
		int emitState = parser.createState();
		parser.putClientProperty("style" + emitState, fStyle);
		parser.addRule(state, fStart, caseSensitive, emitState, fStyle, fgStartAction, false);
		Iterator iter = fSubParsers.iterator();
		while (iter.hasNext()) {
			REDSyntaxHighlighterRule rule = (REDSyntaxHighlighterRule) iter.next();
			rule.installInParser(emitState, caseSensitive, parser, fStyle);
		}
		parser.addRule(emitState, fEnd, caseSensitive, state, envStyle, fgEndAction, getRewind());
	}
	
	String fStart, fEnd;
	ArrayList fSubParsers;
	boolean fRewind;
	static REDRexAction fgStartAction = new REDSyntaxRangeStartAction();
	static REDRexAction fgEndAction = new REDSyntaxRangeEndAction();
}
	
/** Range start action. 
  * This action is called upon matching the start of a range.
  * @author rli@chello.at
  */
class REDSyntaxRangeStartAction extends REDRexAction {
	public void patternMatch(REDRexParser parser, int line, REDRexParserMatch match) {
		REDSyntaxHighlighterRule.updateLastLit(parser, line, match.getStart(0), match.getStart(0));
		parser.putClientProperty("envStyle", match.getEmitObj());
	}
}

/** Range end action. 
  * This action is called upon matching the end of a range.
  * @author rli@chello.at
  */
class REDSyntaxRangeEndAction extends REDRexAction {
	public void patternMatch(REDRexParser parser, int line, REDRexParserMatch match) {
		REDSyntaxHighlighterRule.updateLastLit(parser, line, match.getEnd(0), match.getEnd(0));
		parser.putClientProperty("envStyle", match.getEmitObj());
	}
}
