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

import red.util.*;
import red.xml.*;
import java.util.*;

/** Syntax highlighter definitions. Needed to create actual highlighters.
  * Syntax highlighter definitions are a collection of Syntax highlighter rules.
  * @author rli@chello.at
  * @tier system
  * @see REDSyntaxHighlighterRule
  */
public class REDSyntaxHighlighterDefinition implements REDXMLReadable {
	public REDSyntaxHighlighterDefinition() {
		fName = "";
		fRules = new ArrayList();
		fIgnorePattern = "[a-zA-Z0-9_]+";
		fCaseSensitive = true;
	}
	
	public REDSyntaxHighlighterDefinition(String name) {
		this();
		setName(name);
	}

	
	public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
		handler.mapStart("#", "setName(#id)");
	}
	
	public void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer) {
		if (obj instanceof REDSyntaxHighlighterRule) {
			REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighterDefinition", "Adding rule to SHD: " + obj);
			addRule((REDSyntaxHighlighterRule) obj);
		}
		else {
			REDTracer.warning("red.plugins.synHi", "REDSyntaxHighlighterDefinition", "Unknown inner production ignored: " + obj);
		}
	}

	public void setName(String name) {
		fName = name;
		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighterDefinition", "SHD name set to: " + name);
	}
	
	String getName() {
		return fName;
	}
	
	String getIgnorePattern() {
		return fIgnorePattern;
	}
	
	void setIgnorePattern(String pattern) {
		fIgnorePattern = pattern;
	}
	
	boolean getCaseSensitive() {
		return fCaseSensitive;
	}
	
	void setCaseSensitive(boolean caseSensitive) {
		fCaseSensitive = caseSensitive;
	}
	
	void addRule(REDSyntaxHighlighterRule rule) {
		fRules.add(rule);
	}
	
	Iterator iterator() {
		return fRules.iterator();
	}
		
	ArrayList fRules;
	String fName;
	String fIgnorePattern;
	boolean fCaseSensitive;
}
	