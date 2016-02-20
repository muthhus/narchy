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
 
package red.xml;

import org.xml.sax.*;
import java.util.*;

/** XML reader. This class is responsible for reading XML. It has nothing to do with the REDXMLHandlerReader of SaX.
  * @author rli@chello.at
  * @tier system
  */
final public class REDXMLHandlerReader extends REDXMLHandler {
	/** Create XML reader.
	  * @param delimiterTag The delimiting tag of the object to read in.
	  * @param obj The object to read / write.
	  */
	public REDXMLHandlerReader(REDXMLManager manager, String delimiterTag, REDXMLReadable obj) throws REDXMLCallbackError {
		fManager = manager;
		fData = new StringBuffer();
		fDelimiter = delimiterTag;
		fStartMap = new HashMap();
		fEndMap = new HashMap();
		fObj = obj;
		fObj.setMappings(this);
	}
	
	/** Get object to read / write */
	REDXMLReadable getObj() {
		return fObj;
	}
	
	REDXMLManager getManager() {
		return fManager;
	}
	
	/** Get context for errors. 
	  * @return A string describing the ongoing action in handler. Examples: <UL>
	  * <LI>mapping <Test> to 'setId(#id)'</LI>
	  * <LI>reading <Test> at line: x, column: y</LI>
	  * </UL>
	  */
	public String getErrorContext() {
		return String.valueOf(getClientData(this + ".ErrorContext"));
	}
	
	private void setErrorContext(String context) {
		putClientData(this + ".ErrorContext", context);
	}

	/** Set tag mapping for start tags.
	  * This method will instruct the handler to call <CODE>methodName(...)</CODE> when &lt;tagEntry&gt;content&lt;/tagEntry&gt;
	  * is found in the XML file, thus providing a very easy way to parse simple XML entries.
	  * @param tagEntry The tag to map. If # ist passed, the delimiter tag is used.
	  * @param methodInvocation The method to invoke if the tag is parsed. 
	  * This parameter consists of the methodname, followed by a list of arguments. Each argument starts with a '#' character and is followed 
	  * by a list of tag attribute names. If the attribute name is empty, the content data is passed instead.
	  */
	public void mapStart(String tagEntry, String methodInvocation) throws REDXMLCallbackError {
		if (tagEntry.equals("#")) {
			tagEntry = fDelimiter;
		}
		setErrorContext("mapping <" + tagEntry + "> to '" + methodInvocation + '\'');
		REDXMLCallbackDescriptor desc = new REDXMLCallbackDescriptor(methodInvocation, fObj, this, fManager);
		fStartMap.put(tagEntry.toLowerCase(), desc);
	}
	
	/** Set tag mapping for end tags.
	  * This method will instruct the handler to call <CODE>methodName(...)</CODE> when &lt;/tagEntry&gt;content&lt;/tagEntry&gt;
	  * is found in the XML file, thus providing a very easy way to parse simple XML entries.
	  * @param tagEntry The tag to map. If # ist passed, the delimiter tag is used.
	  * @param methodInvocation The method to invoke if the tag is parsed.
	  * This parameter consists of the methodname, followed by a list of arguments. Each argument starts with a '#' character and is followed 
	  * by a list of tag attribute names. If the attribute name is empty, the content data is passed instead. Since end tags may not contain 
	  * attributes, you can only use content data as parameter in an end mapping.
	  */
	public void mapEnd(String tagEntry, String methodInvocation) throws REDXMLCallbackError {
		if (tagEntry.equals("#")) {
			tagEntry = fDelimiter;
		}
		setErrorContext("mapping </" + tagEntry + "> to '" + methodInvocation + '\'');
		REDXMLCallbackDescriptor desc = new REDXMLCallbackDescriptor(methodInvocation, fObj, this, fManager);
		fEndMap.put(tagEntry.toLowerCase(), desc);
	}
	
	/** Process start tag.
	  * This method is overwritten from SAX to reset content data and call the <CODE>start</CODE> method if appropriate.
	  */
	public void startElement (String uri, String name, String qName, Attributes atts) throws SAXException {
		REDXMLCallbackDescriptor desc = (REDXMLCallbackDescriptor) fStartMap.get(name.toLowerCase());
		setErrorContext("reading <" + name + "> at line: " + fManager.getCurLine() + ", column: " + fManager.getCurColumn() + " in '" + fManager.getCurName() + '\'');
		if (desc != null) {
			desc.invoke(fData, atts);
		}
		fData.delete(0, fData.length());
	}
	
	/** Process end tag.
	  * This method is overwritten from SAX. It does the following:
	  * <OL>
	  * <LI>Check whether the end tag equals the delimiter tag of this handler. If this is the case <CODE>finish()</CODE> and <CODE>stopHandling()</CODE> 
	  * are called to indicate the end for this handler and pop of the handler stack of REDXMLManager. </LI>
	  * <LI>Check whether the end tag is a registered method invokation map (cf. <CODE>map</CODE>). If this is the case invoke the mapped method.</LI>
	  * <LI>If the method is neither delimter tag nor registered for method mapping an error message is issued.
	  * </OL>
	  */
    public void endElement (String uri, String name, String qName) throws SAXException {
		REDXMLCallbackDescriptor desc = (REDXMLCallbackDescriptor) fEndMap.get(name.toLowerCase());
		setErrorContext("reading </" + name + "> at line: " + fManager.getCurLine() + ", column: " + fManager.getCurColumn() + " in '" + fManager.getCurName() + '\'');
		if (desc != null) {
			desc.invoke(fData, null);
		}
		
		if (name.equalsIgnoreCase(fDelimiter)) {
			stopHandling();
		}
		fData.delete(0, fData.length());
    }
	
	/** Process content data.
	  * This method is overwritten from SAX to manage content data.
	  */
	public void characters (char ch[], int start, int length) {
		fData.append(ch, start, length);
	}
	
	/** Stop handling.
	  * This method should not be called except when the delimiter end tag has been encountered.
	  */
	protected void stopHandling() {
		fManager.popHandler();
	}
	
	/** The current content data. */
	protected StringBuffer fData;
	
	/** The delimiter tag. */
	private final String fDelimiter;
	
	/** Start tag name -> callback descriptor. */
	private final HashMap fStartMap;

	/** End tag name -> callback descriptor. */
	private final HashMap fEndMap;
	
	/** The object to read. */	
	private final REDXMLReadable fObj;
	
	/** The XML Manager */
	private final REDXMLManager fManager;
}
