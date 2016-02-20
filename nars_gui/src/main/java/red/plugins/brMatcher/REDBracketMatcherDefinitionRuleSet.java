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

import red.xml.*;

/** Bracket matcher definition rule set. Rule sets define which bracket matching actions should be attempted.
  * @author rli@chello.at
  * @tier system
  * @see REDBracketMatcherManagerDefinition
  */
public class REDBracketMatcherDefinitionRuleSet implements REDXMLReadable {
	public REDBracketMatcherDefinitionRuleSet() {
		fBeforeLeft = true; fAfterLeft = false;
		fBeforeRight = false; fAfterRight = true;
		fDoubleClickSelect = true;
	}
	
	public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
		handler.mapStart("#", "register(#$, #id)");
		handler.mapEnd("BeforeLeft", "setBeforeLeft((boolean) # = 'true')");
		handler.mapEnd("AfterLeft", "setAfterLeft((boolean) # = 'false')");
		handler.mapEnd("BeforeRight", "setBeforeRight((boolean) # = 'false')");
		handler.mapEnd("AfterRight", "setAfterRight((boolean) # = 'true')");
		handler.mapEnd("DoubleClickSelect", "setDoubleClickSelect((boolean) # = 'true')");
	}
	
	public void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer) {
	}

	/** XML callback routine. */
	public void register(REDXMLManager manager, String id) {
		manager.putClientData("ruleSet." + id, this);
	}
	
	/** Get match before left bracket. */
	public void setBeforeLeft(boolean flag) {
		fBeforeLeft = flag;
	}
	
	/** Get match before left bracket. */
	boolean getBeforeLeft() {
		return fBeforeLeft;
	}
	
	/** Get match after left bracket. */
	public void setAfterLeft(boolean flag) {
		fAfterLeft = flag;
	}
	
	/** Get match after left bracket. */
	boolean getAfterLeft() {
		return fAfterLeft;
	}
	
	/** Set match before right bracket. */
	public void setBeforeRight(boolean flag) {
		fBeforeRight = flag;
	}

	/** Get match before right bracket. */
	boolean getBeforeRight() {
		return fBeforeRight;
	}
	
	/** Set match after right bracket. */
	public void setAfterRight(boolean flag) {
		fAfterRight = flag;
	}

	/** Get match after right bracket. */
	boolean getAfterRight() {
		return fAfterRight;
	}
	
	/** Set double click select. React upon double clicks after a left or before a right bracket by selecting the whole content of the bracket.  */
	public void setDoubleClickSelect(boolean flag) {
		fDoubleClickSelect = flag;
	}
	
	/** Get double click select. React upon double clicks after a left or before a right bracket by selecting the whole content of the bracket.  */
	boolean getDoubleClickSelect() {
		return fDoubleClickSelect;
	}
	
	/** Try to match if caret before a left bracket. */
	boolean fBeforeLeft;
	/** Try to match if caret after a left bracket. */
	boolean fAfterLeft;
	/** Try to match if caret before a right bracket. */
	boolean fBeforeRight;
	/** Try to match if caret after a right bracket. */
	boolean fAfterRight;
	/** React upon double clicks after a left or before a right bracket by selecting the whole content of the bracket. */
	boolean fDoubleClickSelect;
}
	