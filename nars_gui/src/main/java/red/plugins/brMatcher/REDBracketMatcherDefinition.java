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
import red.util.*;
import red.xml.*;

/** Bracket matcher definitions. Needed to create actual bracket matchers.
  * @author rli@chello.at
  * @tier system
  * @see REDBracketMatcherManager
  */
public class REDBracketMatcherDefinition implements REDXMLReadable {
	public REDBracketMatcherDefinition() {
		fName = "";
		fMaxLines = Integer.MAX_VALUE;
		fMaxChars = Integer.MAX_VALUE;
		fStyle = REDStyleManager.getStyle("BracketMatcher");
		fPatterns = new ArrayList();
	}
	
	public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
		handler.mapStart("#", "setName(#id)");
		handler.mapEnd("Style", "setStyle(#)");
		handler.mapEnd("MaxLines", "setMaxLines((int) # = '" + Integer.MAX_VALUE + "')");
		handler.mapEnd("MaxChars", "setMaxChars((int) # = '" + Integer.MAX_VALUE + "')");
	}
	
	public void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer) {
		if (obj instanceof REDBracketMatcherDefinitionPattern) {
			REDBracketMatcherDefinitionPattern pat = (REDBracketMatcherDefinitionPattern) obj;
			if (pat.isOk()) {
				fPatterns.add(pat);
				pat.setDefinition(this);
			}
			else {
				REDGLog.warning("Bracket Matcher", "Incomplete pattern in '" + getName() + "' ignored.");
			}
		}
	}

	/** XML callback routine. */
	public void setName(String name) {
		fName = name;
	}
	
	/** Get name of bracket matcher definition. */
	String getName() {
		return fName;
	}
	
	/** Set maximum numer of lines to be searched for a bracket match. If either the maximum number of lines or characters has been reached, searching will stop. */
	public void setMaxLines(int maxLines) {
		fMaxLines = maxLines;
	}
	
	/** Get maximum numer of lines to be searched for a bracket match. If either the maximum number of lines or characters has been reached, searching will stop. */
	int getMaxLines() {
		return fMaxLines;
	}
	
	/** Set maximum numer of characters to be searched for a bracket match. If either the maximum number of lines or characters has been reached, searching will stop. */
	public void setMaxChars(int maxChars) {
		fMaxChars = maxChars; 
	}
	
	/** Get maximum numer of characters to be searched for a bracket match. If either the maximum number of lines or characters has been reached, searching will stop. */
	int getMaxChars() {
		return fMaxChars;
	}
	
	/** XML callback routine. */
	public void setStyle(String styleName) {
		if (REDStyleManager.hasStyle(styleName)) {
			fStyle = REDStyleManager.getStyle(styleName);
		}
		else {
			REDGLog.warning("Bracket Matcher", "The style name '" + styleName + "' in '" + getName() + "' does not exist. Will use the style 'BracketMatcher' instead.");
		}
	}
	
	/** Get highlight style. */
	public REDStyle getStyle() {
		return fStyle;
	}

	/** Try and find a match. 
	  * @param editor The editor the matching should happen in.
	  * @param pos The current position of the caret.
	  * @param forDoubleClick if <Code>true</Code> evaluate patterns for double clicks.
	  * @param result If a non-null value is passed for this parameter, the result object is reused.
	  * @return The position matching, or -1 if no such position exists.
	  
	  */
	public REDBracketMatcherResult findMatch(REDEditor editor, int pos, boolean forDoubleClick, REDBracketMatcherResult result) {
		int retVal = -1;
		Iterator iter = fPatterns.iterator();
		REDBracketMatcherDefinitionPattern pat = null;
		while (iter.hasNext() && retVal == -1) {
			pat = (REDBracketMatcherDefinitionPattern) iter.next();
			retVal = pat.findMatch(editor, pos, forDoubleClick);
		}
		if (retVal != -1) {
			if (result == null) {
				result = new REDBracketMatcherResult();
			}
			result.fPosition = retVal;
			if (retVal > pos) {
				result.fLength = pat.fRight.length();
			}
			else {
				result.fLength = pat.fLeft.length();
			}
			return result;
		}
		else {
			return null;
		}
	}
	
	/** Get iterator over patterns. 
	  * @return An iterator which gives REDBracketMatcherDefinitionPattern objects.
	  */
	Iterator patternIterator() {
		return fPatterns.iterator();
	}
	
	boolean isOk() {
		return fPatterns.size() > 0;
	}
	
	/** The name of the definition. */
	String fName;	
	/** The maximum number of lines to be searched for a bracket match. */
	int fMaxLines;
	/** The maximum number of characters to be searched for a bracket match. */
	int fMaxChars;
	/** The style to highlight matches with. */
	REDStyle fStyle;
	/** The patterns to try and match. */
	ArrayList fPatterns;	// array of REDBracketMatcherDefinitionPattern
}
	