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
import java.io.*;
import red.util.*;
import red.xml.*;

/** Bracket matcher manager.
  * This class manages bracket matcher definitions and allows creation of bracket matchers.
  * @author rli@chello.at
  * @tier plugin
  * @see REDBracketMatcher
  */
public class REDBracketMatcherManager {
	static public void addDefinition(REDBracketMatcherDefinition def) {
		fDefinitions.put(def.getName(), def);
	}
	
	/** Create a bracket matcher.
	  * @param name Definition to create matcher from.
	  * @return A bracket matcher for the given definition name, or <Code>null</Code> if no such definition exists.
	  */
	static public REDBracketMatcher createMatcher(String name) {
		REDBracketMatcherDefinition def = getMatcherDefinition(name);
		if (def != null) {
			return new REDBracketMatcher(def);
		}
		else {
			return null;
		}
	}
	
	static REDBracketMatcherDefinition getMatcherDefinition(String name) {
		return (REDBracketMatcherDefinition) fDefinitions.get(name);
	}
	
	/** Read matcher definition.
	  * @param is The input stream to read the matcher definition from.
	  * @param location Location of the input stream. Used for error - output.
	  */
	static public void readMatcherDefinition(InputStream is, String location) {
		REDXMLManager manager = new REDXMLManager();
		manager.registerHandler("BracketMatcher", REDBracketMatcherDefinition.class);
		manager.registerHandler("StyleSet", REDBracketMatcherDefinitionStyleSet.class);
		manager.registerHandler("RuleSet", REDBracketMatcherDefinitionRuleSet.class);
		manager.registerHandler("Pattern", REDBracketMatcherDefinitionPattern.class);
		InputStreamReader reader = new InputStreamReader(is); 
		try {
			manager.parse(reader, location);
			REDBracketMatcherDefinition def = (REDBracketMatcherDefinition) manager.getProducedObject();
			if (def != null) {
				if (def.isOk()) {
					REDTracer.info("red.plugins.brMatcher", "REDBracketMatcherManager", "Added BMD '" + def.getName() + '\'');
					addDefinition(def);
				}
				else {
					REDGLog.error("Bracket Matcher", "Cannot add bracket matcher definition '" + def.getName() +"', since it contains no valid patterns.");
				}
			}
			else {
				REDGLog.error("Bracket Matcher", "Error while reading BMD from '" + location +"'.");
			}
		}
		catch (Exception e) {
			REDGLog.error("Bracket Matcher", "Exception while reading BMD from '" + location +"': "+ e);
		}
		try { reader.close(); } catch (IOException ioe) { }
	}
	
	static private void readMatcherDefinitions() {
		REDResourceInputStreamIterator iter = REDResourceManager.getInputStreams("config/brMatcher", ".xml");
		while (iter.hasNext()) {
			readMatcherDefinition((InputStream) iter.next(), String.valueOf(iter.curName()));
		}
	}
	
	private static final HashMap fDefinitions = new HashMap();
	static {
		readMatcherDefinitions();
	}
}
