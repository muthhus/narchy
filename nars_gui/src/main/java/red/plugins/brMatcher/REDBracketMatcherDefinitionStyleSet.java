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

/** Bracket matcher definition style sets. There are two kinds of style to a 
  * definition: <UL>
  * <LI>Ignore styles are those styles which should be ignored when searching
  * a matching bracket.</LI>
  * <LI>Precondition styles define what style must be there in order for a 
  * pattern to be applicable at all.
  * </UL>
  * @author rli@chello.at
  * @tier system
  * @see REDBracketMatcherManagerDefinition
  */
public class REDBracketMatcherDefinitionStyleSet implements REDXMLReadable {
	public REDBracketMatcherDefinitionStyleSet() {
		fIgnoreStyles = new HashSet();
		fPreStyle = null;
	}
	
	public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
		handler.mapStart("#", "register(#$, #id)");
		handler.mapEnd("IgnoreStyle", "addIgnoreStyle(#)");
		handler.mapEnd("PreStyle", "setPreStyle(#)");
	}
	
	public void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer) {
	}

	/** XML callback routine. */
	public void register(REDXMLManager manager, String id) {
		manager.putClientData("styleSet." + id, this);
	}
	
	/** XML callback routine. */
	public void addIgnoreStyle(String styleName) {
		if (REDStyleManager.hasStyle(styleName)) {
			fIgnoreStyles.add(styleName);
		}
		else {
			REDGLog.warning("Bracket Matcher", "The ignore style name '" + styleName + "' does not exist and will be ignored.");
		}
	}
	
	/** Get the number of ignored styles in style set. */
	int getNrIgnoreStyles() {
		return fIgnoreStyles.size();
	}
	
	/** Get an iterator over the excluded style names for this definition. */
	Iterator getIgnoreStylesIterator() {
		return fIgnoreStyles.iterator();
	}

	/** XML callback routine. */
	public void setPreStyle(String styleName) {
		if (REDStyleManager.hasStyle(styleName)) {
			fPreStyle = REDStyleManager.getStyle(styleName);
		}
		else {
			REDGLog.warning("Bracket Matcher", "The precondition style name '" + styleName + "' does not exist and will be ignored.");
		}

	}

	/** Get precondition style. */
	REDStyle getPreStyle() {
		return fPreStyle;
	}

	/** The set of ignored styles. */
	private final HashSet fIgnoreStyles;
	
	/** The precondition style. */
	private REDStyle fPreStyle;
}
	