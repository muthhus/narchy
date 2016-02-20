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

import red.rexParser.*;
import red.lineTree.*;
import javax.swing.*;
import java.util.*;

/** Finder for REDEditor objects.
  * This class allows to comfortably do find & replace operations.
  * It is intended to be used as a singleton. Multiple instances may however be allocated.
  * @author rli@chello.at
  * @tier API
  */
public class REDFinder implements REDRexLineSource {
	/** Get singleton instance. */
	public static REDFinder getInstance() {
		return fgInstance;
	}
	
	/** Default history size.
	  * This value specifies the history size of newly created REDFinder objects 
	  */
	public static int fcDefaultHistorySize = 10;
	
	/** Create finder.
	  * REDFinder is intended to be used as a singleton. Multiple instances may however be allocated by using this constructor.
	  */
	public REDFinder() {
		fParser = new REDRexParser();
		fEditor = null;
		fParseStart = 0;
		fParseReverse = false;
		fFindHistory = new DefaultComboBoxModel();
		fReplaceHistory = new DefaultComboBoxModel();
		fHistorySize = fcDefaultHistorySize;
		fReplaceAct = new ReplaceAction();
		fFactory = null;
	}
	
	/** Quote characters in pattern with backslash
	  * @param pattern The pattern to be quoted
	  * @param tbQuoted This string contains the characters which should be quoted.
	  * @return The passed pattern which each occurence of characters of tbQuoted preceded by a backslash
	  */
	static String quoteWithBackslash(String pattern, String tbQuoted) {
		StringBuilder buf = new StringBuilder(pattern);
		int idx;
		char c;
		for (int x = 0; x < tbQuoted.length(); x++) {
			c = tbQuoted.charAt(x);
			for (idx = 0; idx < buf.length(); idx++) {
				if (c == buf.charAt(idx)) {
					buf.insert(idx++, '\\');
				}
			}
		}
		return String.valueOf(buf);
	}
	
	/** Parser stopper which is implemented as decorator around real parser actions to return true, once a match has been found */
	private static class OneMatchStopper extends REDRexAction implements REDRexParserStopper {
		OneMatchStopper(REDRexAction decorated) {
			fDecorated = decorated;
			fMustStop = false;
		}
		
		public void patternMatch(REDRexParser parser, int line, REDRexParserMatch match) {
			fMustStop = true;
			fDecorated.patternMatch(parser, line, match);
		}
		
		public boolean mustStop(REDRexParser parser, int line, int offset, int state) {
			return fMustStop;
		}
		
		REDRexAction fDecorated;
		boolean fMustStop;
	}
	
	/** Find pattern.
	  * @param pattern The pattern to find.
	  * @param direction The direction to start the search.
	  * @param caseSensitive If this parameter is <CODE>true</CODE>, the search is performed case sensitively
	  * @param wholeWord If this parameter is <CODE>true</CODE>, the pattern must be delimited by non-word characters.
	  * @param useRegExp If this parameter is <CODE>true</CODE>, the pattern is interpreted as regular expression; If it is false
	  * it is matched as plain string.
	  * @param addToHistory If this parameter is <CODE>true</CODE>, the pattern is added to the find history of this finder.
	  * @param all If this parameter is <CODE>true</CODE>, the passed action is called for each occurence of pattern; If it is false
	  * it is called for the first mach only.
	  * @param action The action to be called upon matching the searched pattern.
	  */
	public void find(String pattern, int from, REDFinderDirection direction, boolean caseSensitive, 
		boolean wholeWord, boolean useRegExp, boolean addToHistory, boolean all, REDRexAction action) throws REDRexMalformedPatternException {
		if (pattern.length() == 0) return; // avoid problem with regexp engine
		fParser.clearRules();

		if (addToHistory) {
			updateHistory(fFindHistory, pattern);
		}	

		if (!useRegExp) {		
			pattern = quoteWithBackslash(pattern, "\\.*+?[^$");
		}
		
		if (wholeWord) {
			pattern = "\\b" + pattern + "\\b";
		}
		fParseStart = from;
		fParseStartLine = fEditor.getLineForPosition(from);
		fParseReverse = direction == REDFinderDirection.BACKWARD;
		
		OneMatchStopper stopper = null;
		if (!all) {
			action = stopper = new OneMatchStopper(action);
		}
		fParser.addRule(REDRexParser.defaultState(), pattern, caseSensitive, REDRexParser.defaultState(), this, action, false);
		fParser.parse(this, 0, REDRexParser.defaultState(), stopper, fParseReverse);
	}
	
	/** Replace action.
	  * This action is used to collect matches and replace them by the specified replace string.
	  * It handles backreferences
	  */
	private class ReplaceAction extends REDRexAction {
		class Entry implements Comparable {
			public Entry(int line, REDRexParserMatch details) {
				fLine = line;
				fDetails = details;
			}
			
			public int compareTo(Object o) {
				Entry e = (Entry) o;
				if (fLine == e.fLine) {
					return e.fDetails.getStart(0) - fDetails.getStart(0);
				}
				else {
					return e.fLine - fLine;
				}
			}
			int fLine;
			REDRexParserMatch fDetails;
		}

		ReplaceAction() {
			fMatches = new TreeSet();
		}
		
		public void setReplacement(String repl) {
			fReplacement = repl;
		}
		
		private String getBackref(Entry entry, int nr) {
			int lineStart = fEditor.getLineStart(entry.fLine);
			return fEditor.copy(lineStart + entry.fDetails.getStart(nr), lineStart + entry.fDetails.getEnd(nr));	
		}
		
		private String resolveBackrefs(Entry entry) {
			StringBuilder buf = new StringBuilder();
			int idx = fReplacement.indexOf('\\');
			int lastPos = 0;
			while (idx != -1 && idx+1 < fReplacement.length()) {
				int brNumber = fReplacement.charAt(idx+1) - '0';
				if (brNumber >= 0 && brNumber <= 9) {
					buf.append(fReplacement.substring(lastPos, idx));
					buf.append(getBackref(entry, brNumber));
					lastPos = idx + 2;
				}
				idx++; 
				idx = fReplacement.indexOf('\\', idx);
			}
			buf.append(fReplacement.substring(lastPos));
			return String.valueOf(buf);
		}
		
		/** expunges all entries in fMatches, which are not within the current selection */
		private void expungeMatchesOutsideSelection() {
			int selFrom = fEditor.getSelectionStart();
			int selTo = fEditor.getSelectionEnd();
			int from, to;
			Entry e;
			Iterator iter = fMatches.iterator();
			while (iter.hasNext()) {
				e = (Entry) iter.next();
				from = getRealPosition(e.fLine, e.fDetails.getStart(0));
				to = getRealPosition(e.fLine, e.fDetails.getEnd(0));								
				if (from < selFrom || to > selTo) {
					iter.remove();
				}
			}
		}
		
		private int executeReplacements(boolean withinSelection) {
			String actReplacement = "";
			Entry e;
			int from = 0, to;
			REDMark selStart = null, selEnd = null; 
			
			if (withinSelection) {
				expungeMatchesOutsideSelection();
				REDMarkTree markTree = fEditor.createMarkTree();
				selStart = markTree.createMark(fEditor.getSelectionStart(), null);
				selEnd = markTree.createMark(fEditor.getSelectionEnd(), null);				
			}
			int nrReplacements = fMatches.size();
			if (fMatches.size() > 1) {
				fEditor.startMacroCommand("Replace All");
			}
			Iterator iter = fMatches.iterator();			
			while (iter.hasNext()) {
				e = (Entry) iter.next();
				from = getRealPosition(e.fLine, e.fDetails.getStart(0));
				to = getRealPosition(e.fLine, e.fDetails.getEnd(0));
				if (fReplacement.contains("\\")) {	// we have backrefs
					actReplacement = resolveBackrefs(e);
				}
				else {
					actReplacement = fReplacement;
				}
				fEditor.replace(actReplacement, from, to, null);
			}
			if (withinSelection) {
				fEditor.setSelection(selStart.getPosition(), selEnd.getPosition());
			}
			else {
				if (fMatches.size() == 1) {
					fEditor.setSelection(from, from + actReplacement.length());
				}
			}
			if (fMatches.size() > 1) {
				fEditor.endMacroCommand();
			}
			fMatches.clear();
			return nrReplacements;
		}
		
		public void patternMatch(REDRexParser parser, int line, REDRexParserMatch match) {
			fMatches.add(new Entry(line, match));
		}
		
		String fReplacement;
		TreeSet fMatches;
	}
	
	/** Find and replace pattern.
	  * @param pattern The pattern to find.
	  * @param replacement The string to insert instead of the found pattern.
	  * @param direction The direction to start the search.
	  * @param caseSensitive If this parameter is <CODE>true</CODE>, the search is performed case sensitively
	  * @param wholeWord If this parameter is <CODE>true</CODE>, the pattern must be delimited by non-word characters.
	  * @param useRegExp If this parameter is <CODE>true</CODE>, the pattern is interpreted as regular expression; If it is false
	  * it is matched as plain string.
	  * @param addToHistory If this parameter is <CODE>true</CODE>, the pattern is added to the find history of this finder.
	  * @param replaceAll <CODE>OFF</CODE> first match is replaced; 
	  * <CODE>SELECTION</CODE>all matches in the current selection are replaced;
	  * <CODE>FILE</CODE>all matches in the current file are replaced;
	  * @return The number of replacements done.
	  */
	public int replace(String pattern, String replacement, int from, REDFinderDirection direction, boolean caseSensitive, 
		boolean wholeWord, boolean useRegExp, boolean addToHistory, REDFinderReplaceAllDirective replaceAll) throws REDRexMalformedPatternException { 
		fReplaceAct.setReplacement(replacement);
		find(pattern, from, direction, caseSensitive, wholeWord, useRegExp, addToHistory, replaceAll != REDFinderReplaceAllDirective.OFF, fReplaceAct);
		int nrReplacements = fReplaceAct.executeReplacements(replaceAll == REDFinderReplaceAllDirective.SELECTION);
		if (addToHistory) {
			updateHistory(fReplaceHistory, replacement);
		}
		return nrReplacements;
	}

	
	// history stuff
	/** Set history size.
	  * This method sets the size of the find and replace history of the finder.
	  * If there are more than entries in the history than the set history size, the oldes entry is removed
	  * @param size The new history size.
	  */
	public void setHistorySize(int size) {
		fHistorySize = size;
	}
	
	/** Get history size.
	  * @return The current size of the find and replace history.
	  */
	public int getHistorySize() {
		return fHistorySize;
	}
	
	/** Get find history 
	  * @return The find history
	  */
	public DefaultComboBoxModel getFindHistory() {
		return fFindHistory;
	}
	
	/** Get replace history 
	  * @return The find history
	  */
	public DefaultComboBoxModel getReplaceHistory() {
		return fReplaceHistory;
	}

	/** Update history.
	  * This method updates the passed history to include entry in front.
	  * If entry is already part of the history it is moved in front.
	  * Otherwise it is added to front and the last entry removed, in case the history size is exceeded.
	  * @param history The history to add entry to
	  * @param entry The entry to add to the history,
	  */
	void updateHistory(DefaultComboBoxModel history, String entry) {
		int idx = history.getIndexOf(entry);
		if (idx == -1) {
			history.insertElementAt(entry, 0);
			if (history.getSize() > fHistorySize) {
				history.removeElementAt(fHistorySize);
			}			
		}
		else {
			history.removeElementAt(idx);
			history.insertElementAt(entry, 0);
		}
		history.setSelectedItem(history.getElementAt(0));
	}
	
	/** Set editor.
	  * You must associate a finder with an editor before calling find
	  * @param editor The editor to associate this finder with.
	  */
	public void setEditor(REDEditor editor) {
		fEditor = editor;
	}
	
	/** Get editor.
	  * @return  The editor associated with this finder.
	  */
	public REDEditor getEditor() {
		return fEditor;
	}

	/** Get (real) editor line.
	  * This method translates between virtual line numbers as processed by the REDRexParser and real editor linenumbers
	  */
	private int getRealLine(int lineNr) {
		int nrOfLines = fEditor.getNrOfLines();
		if (fParseReverse) {
			return (fParseStartLine  + nrOfLines - lineNr) % nrOfLines;	// must add nrOfLines, since -3 mod 6 == -3 in Java
		}
		else {
			return (fParseStartLine + lineNr) % nrOfLines;
		}
	}
	
	public int getRealPosition(int lineNr, int offset) {
		if (lineNr == 0 && !fParseReverse || lineNr == fEditor.getNrOfLines() && fParseReverse) {
			return fParseStart + offset;
		}
		else {
			return fEditor.getLineStart(getRealLine(lineNr)) + offset;
		}
	}
	
	// REDRexLineSource interface	
	public char [] getLine(int lineNr, char [] reuse) {
		int nrOfLines = fEditor.getNrOfLines();
		if (lineNr < 0 || lineNr > nrOfLines) {
			return null;
		}
		
		if (lineNr == 0 && !fParseReverse || lineNr == nrOfLines && fParseReverse) {
			String line = fEditor.copy(fParseStart, fEditor.getLineStart(fParseStartLine+1));
			return line.toCharArray();	// tbd: reuse
		}
		else {
			return fEditor.getLineSource().getLine(getRealLine(lineNr), reuse);
		}
	}
	
	public int getLineLength(int lineNr) {
		int nrOfLines = fEditor.getNrOfLines();
		if (lineNr == 0 && !fParseReverse || lineNr == nrOfLines && fParseReverse) {
			return fEditor.getLineStart(fParseStartLine+1) - fParseStart;
		}
		else if (lineNr == 0 && fParseReverse || lineNr == nrOfLines && !fParseReverse) {
			return fParseStart - fEditor.getLineStart(fParseStartLine);
		}
		else {
			return fEditor.getLineSource().getLineLength(getRealLine(lineNr));
		}
	}
	
	/** Get factory for find & replace dialog elements. */
	public REDFinderDialogFactory getREDFinderDialogFactory() {
		if (fFactory == null) {
			fFactory = new REDFinderDialogFactory(this);
		}
		return fFactory;
	}
	
	protected static REDFinder fgInstance = new REDFinder();
	private REDEditor fEditor;
	private final REDRexParser fParser;
	private int fParseStart, fParseStartLine;
	private boolean fParseReverse;
	private final DefaultComboBoxModel fFindHistory;
	private final DefaultComboBoxModel fReplaceHistory;
	private int fHistorySize;
	private final ReplaceAction fReplaceAct;
	private REDFinderDialogFactory fFactory;
}

