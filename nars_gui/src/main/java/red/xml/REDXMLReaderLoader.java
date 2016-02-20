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
import red.util.*;

/** XML handler loader handler.
  * This handler processes &lt;LOAD tag="tagName"&gt;classname&lt;/LOAD&gt; entries,  loads and registers the fully qualified <CODE>
  * classname </CODE> to be the handler for &lt;tagName&gt;. <CODE>classname</CODE> must be a subclass of REDXMLHandlerReader. 
  * REDXMLReaderLoader is installed defaultedly within REDXMLManager.
  * @author rli@chello.at
  * @tier system
  */
public class REDXMLReaderLoader implements REDXMLReadable {
	public REDXMLReaderLoader() {
		super();
		fTagName = "";
	}
	
	public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
		handler.mapStart("#", "setTagName(#tag)");
		handler.mapEnd("#", "registerHandler(#, #&)");
	}
	
	public void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer) {
	}
	
	/** REDXMLHandlerReader callback routine. */
	public void setTagName(String tagname) {
		fTagName = tagname;
	}
	
	/** REDXMLHandlerReader callback routine. */
	public void registerHandler(String clName, REDXMLHandlerReader handler) throws SAXException {
		try {
			Class cl = Class.forName(clName);
			handler.getManager().registerHandler(fTagName, cl);
			REDTracer.info("red.xml", "REDXMLReaderLoader", "Registered handler " + cl + " for " + fTagName);
		}
		catch (Exception e) {
			throw new SAXException("Error while trying to load " + clName + ": " + e);
		}
	}	
	String fTagName;
}
