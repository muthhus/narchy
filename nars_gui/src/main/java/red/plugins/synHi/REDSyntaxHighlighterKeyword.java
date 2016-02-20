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

import java.util.*;
import javax.swing.*;
import red.*;
import red.rexParser.*;
import red.util.*;
import red.xml.*;

/** Keyword rules. 
  * Keyword rules are simple regular expressions highlit with a certain style.
  * @author rli@chello.at
  * @tier system
  * @see REDSyntaxHighlighterRule
  */
public class REDSyntaxHighlighterKeyword extends REDSyntaxHighlighterRule implements REDXMLReadable {
	public REDSyntaxHighlighterKeyword() {
		super();
	}
	
	public REDSyntaxHighlighterKeyword(String regExp, REDStyle style) {
		super();
		fStyle = style;
		setRegExp(regExp);
	}
		
	
	public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
		super.setMappings(handler);
		handler.mapEnd("RegExp", "setRegExp(#)");
	}
	
	public void setRegExp(String regExp) {
		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighterKeyword", "RegExp set for pattern: " + regExp);
		fRegEx = regExp;
	}
	
	void installInParser(int state, boolean caseSensitive, REDRexParser parser, REDStyle envStyle) throws REDRexMalformedPatternException {
		parser.addRule(state, fRegEx, caseSensitive, state, fStyle, fgAction, false);
	}
	
	String fRegEx;
	static REDRexAction fgAction = new REDSyntaxHighlighterKeywordAction();
}
	
/** Keyword action. 
  * This parser action highlights keywords.
  * @author rli@chello.at
  */
class REDSyntaxHighlighterKeywordAction extends REDRexAction {
	public void patternMatch(REDRexParser parser, int line, REDRexParserMatch match) {
		REDSyntaxHighlighterRule.updateLastLit(parser, line, match.getStart(0), match.getEnd(0));
		REDEditor editor = (REDEditor) parser.getClientProperty("editor");
		REDStyle style = (REDStyle) match.getEmitObj();
		int lineStart = editor.getLineStart(line);
		LinkedList list = (LinkedList) parser.getClientProperty("batchQ");
		if (list == null) {
			editor.setStyle(lineStart + match.getStart(0), lineStart + match.getEnd(0), style);
		}
		else {
			synchronized (list) {
				int changeCount = (Integer) parser.getClientProperty("changeCount");
				list.add(new REDSyntaxHighlighterBatchEntry(lineStart + match.getStart(0), lineStart + match.getEnd(0), style, changeCount));
				Runnable executor = (Runnable) parser.getClientProperty("batchExecutor");
				if (executor != null) {
					parser.putClientProperty("batchExecutor", null);
					SwingUtilities.invokeLater(executor);
				}
			}
		}
	}
}
	
