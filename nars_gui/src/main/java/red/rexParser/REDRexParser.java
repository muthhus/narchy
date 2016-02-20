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
 
package red.rexParser;

import java.util.*;
import java.util.regex.*;

/** Regular expression parser class
  * This class uses a regular expression package to implement parsers built at runtime.
  * @author rli@chello.at
  * @tier API  
  */
public class REDRexParser {
	
	public REDRexParser() {
		fNextState = 1;
		fNextActionId = 0;
		fRules = new ArrayList();
		fBuffer = null;
		fProperties = new HashMap();
	}
	
	/** Add rule to parser.
	  * @param reqState The state the parser must have in order for this rule to be relevant.
	  * @param pattern The regular expression pattern of this rule.
	  * @param caseSensitive This parameter specifies whether or not to match this rule case sensitive.
	  * @param emitState The state the parser will be in after this rule has been matched.
	  * @param emitObj The object that will be passed to the action associated with this rule.
	  * @param action The action to be executed when this rule matches
	  * @param rewind If this parameter is true, other rules will be considered as well on the text which has been matched by this rule.
	  */
	public void addRule(int reqState, String pattern, boolean caseSensitive, int emitState, Object emitObj, REDRexAction action, boolean rewind) throws REDRexMalformedPatternException {
		try {
			REDRexParserRule r = new REDRexParserRule(reqState, pattern, caseSensitive, emitState, emitObj, action, rewind, fNextActionId++);
			fRules.add(r);
		}
		catch (PatternSyntaxException mpe) {
			throw new REDRexMalformedPatternException(mpe.getMessage(), pattern);
		}
	}
	
	/** Remove all rules from parser. */
	public void clearRules() {
		fRules.clear();
	}
	
	/** Get the number of rules of this parser.
	  * @return The number of rules this parser has got.
	  */
	public int getNrOfRules() {
		return fRules.size();
	}
	
	/** The default state.
	  * This is the state a parser starts to look for matches if not specified otherwise
	  * @return The default parser state.
	  */
	public static int defaultState() {
		return 0;
	}
	
	/** Create state.
	  * This method creates and returns a new parser state to be used as required and/or emitted state.
	  * @return A new parser state.
	  */
	public int createState() {
		return fNextState++;
	}
	
	/** Parse using defaults.
	  * This method parses the passed line source, starting at line nr. 0 in default state without a stopper in forward direction.
	  * @param src The line source to parse.
	  */
	public void parse(REDRexLineSource src) {
		parse(src, 0, defaultState(), null, false);
	}
	
	private static boolean isPositionOK(REDRexParserMatch m, int minReq, boolean reverse) {
		if (reverse) {
			return m.getEnd(0) <= minReq;
		}
		else {
			return m.getStart(0) >= minReq;
		}
	}

	private static int getStartReqMinPos(boolean reverse, REDRexLineSource src, int line) {
		if (reverse) {
			return src.getLineLength(line)+1;
		}
		else {
			return 0;
		}			
	}
	
	private static int getReqMinPos(boolean reverse, REDRexParserMatch m) {
		if (reverse) {
			return m.getStart(0);
		}
		else {
			return m.getEnd(0);
		}
	}
	
	private SortedSet collectMatches(REDRexLineSource src, int line, boolean reverse) {
		REDRexParserRule rule;
		Pattern p;
		SortedSet matches = new TreeSet(new REDRexParserMatchComparator(reverse));

		Iterator iter = fRules.iterator();
		while (iter.hasNext()) {
			rule = (REDRexParserRule) iter.next(); 
			if (rule.fFinder != null) {	// dont do regexp do fast string search
				Iterator miter = rule.fFinder.getMatches(fBuffer, src.getLineLength(line));	
				while (miter.hasNext()) {
					REDRexStringFinderMatch stringMatch = (REDRexStringFinderMatch) miter.next();
					REDRexParserMatch match = new REDRexParserMatch(1, rule);
					match.setStart(0, stringMatch.beginOffset()); match.setEnd(0, stringMatch.endOffset()+1);
					matches.add(match);
				}
			}
			else {
				p = rule.fPattern;
				if (p == null) throw new Error("p null");
				if (fBuffer == null) throw new Error("Buffer null");
				Matcher matcher = p.matcher(new String(fBuffer, 0, src.getLineLength(line))); // tbd: do not copy, use CharBuffer or other interface!
				int pos = 0;
				while (matcher.find(pos)) {
					REDRexParserMatch match = new REDRexParserMatch(matcher.groupCount() + 1, rule);
					for (int i = 0; i < matcher.groupCount() + 1; i++) {
						match.setStart(i, matcher.start(i));
						match.setEnd(i, matcher.end(i));
					}
					matches.add(match);
					pos = matcher.end();
				}
			}
		}
		return matches;
	}
	
	private int processMatches(REDRexLineSource src, SortedSet matches, int line, int state, boolean reverse, REDRexParserStopper stopper) {
		int reqMinPos = getStartReqMinPos(reverse, src, line);	
		Iterator iter = matches.iterator();
		boolean stopped = false;
		while (iter.hasNext() && !stopped) {
			REDRexParserMatch m = (REDRexParserMatch) iter.next();
			if (isPositionOK(m, reqMinPos, reverse) && m.fRule.fReqState == state) {
				m.fRule.fAction.patternMatch(this, line, m);
				if (!m.fRule.fRewind) {
					reqMinPos = getReqMinPos(reverse, m);
				}
				state = m.fRule.fEmitState;
			}
			stopped = stopper != null && stopper.mustStop(this, line, m.getEnd(0), state);
		}
		matches.clear();
		return state;
	}
	
	/** Parse.
	  * @param src The line source to parse
	  * @param line The line to start parsing with
	  * @param state The state to start parsing with
	  * @param stopper The stopper which decides whether or not parsing continues after each line
	  * @param reverse If this parameter is true, matches are ordered from right to left (instead of left to right, which is the default). 
	  * This parameter does not affect the way lines are retrieved from the REDRexLineSource.
	  */
	public void parse(REDRexLineSource src, int line, int state, REDRexParserStopper stopper, boolean reverse) {
		SortedSet matches;
		
		fBuffer = src.getLine(line, fBuffer);
		// traverse lines
		while (fBuffer != null) {
			matches = collectMatches(src, line, reverse);
			state = processMatches(src, matches, line, state, reverse, stopper);
			
			line++;
			// test stopper
			if (stopper != null && stopper.mustStop(this, line, 0, state)) {
				fBuffer = null;
			}
			else {
				fBuffer = src.getLine(line, fBuffer);
			}
		}
	}
	
	public void putClientProperty(Object key, Object value) {
		fProperties.put(key, value);
	}
	
	public Object getClientProperty(Object key) {
		return fProperties.get(key);
	}
	
	int fNextState, fNextActionId;
	ArrayList fRules;
	char [] fBuffer;
	HashMap fProperties;
}
