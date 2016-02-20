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
import java.io.*;

/** Syntax highlighter manager.
  * This class manages highlighter definitions and allows creation of syntax highlighters.
  * @author rli@chello.at
  * @tier plugin
  * @see REDSyntaxHighlighter
  */
public class REDSyntaxHighlighterManager {
	static public void addDefinition(REDSyntaxHighlighterDefinition def) {
		fDefinitions.put(def.getName(), def);
	}
	
	/** Create highlighter from definition.
	  * @param name The (case-sensitive) name of the syntax highlighter definition to create highlighter from.
	  * @return A syntax highlighter to be added as plugin to a REDEditor or <Code>null</Code> if no such definition exists.
	  */
	static public REDSyntaxHighlighter createHighlighter(String name) {
		REDSyntaxHighlighterDefinition def = (REDSyntaxHighlighterDefinition) fDefinitions.get(name);
		if (def != null) {
			return new REDSyntaxHighlighter(def);
		}
		else {
			return null;
		}
	}
	
	/** Check availability of highlighter definition.
	  * @param name The (case-sensitive) name of the syntax highlighter definition to check availability for.
	  * @return <Code>true</Code> if such a definition exists; <Code>false</Code> otherwise
	  */
	static public boolean hasHighlighter(String name) {
		return fDefinitions.get(name) != null;
	}
	
	static private void readHighlighters() {
		REDXMLManager manager = new REDXMLManager();
		manager.registerHandler("Syntaxhighlighter", REDSyntaxHighlighterDefinition.class);
		manager.registerHandler("Pattern", REDSyntaxHighlighterKeyword.class);
		manager.registerHandler("Range", REDSyntaxHighlighterRange.class);
		REDResourceInputStreamIterator iter = REDResourceManager.getInputStreams("config/synHi", ".shd");
		while (iter.hasNext()) {
			InputStream is = (InputStream) iter.next();
			InputStreamReader reader = new InputStreamReader(is); 
			try {
				manager.parse(reader, String.valueOf(iter.curName()));
				REDSyntaxHighlighterDefinition def = (REDSyntaxHighlighterDefinition) manager.getProducedObject();
				if (def != null) {
					REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighterManager", "Added SHD '" + def.getName() + '\'');
					addDefinition(def);
				}
				else {
					REDTracer.error("red.plugins.synHi", "REDSyntaxHighlighterManager", "Error while reading SHD from '" + iter.curName() +"'.");
				}
			}
			catch (Exception e) {
				REDTracer.error("red.plugins.synHi", "REDSyntaxHighlighterManager", "Exception while reading SHD from '" + iter.curName() +"': "+ e);
			}
			try { reader.close(); } catch (IOException ioe) { }
		}
	}
	
	private static final HashMap fDefinitions = new HashMap();
	static {
		readHighlighters();
	}
}
