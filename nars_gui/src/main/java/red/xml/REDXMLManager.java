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

import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import red.util.*;


/** XML file manager.
  * This class is responsible for loading XML files.
  * @author rli@chello.at
  * @tier system
  */
final public class REDXMLManager extends REDXMLHandler {
	public REDXMLManager() {
		fHandlers = new HashMap(); 
		fHandlerStack = new Stack(); fHandlerStack.push(null);
		fInitialized = false;
	}

	/** Register handler class.
	  * @param tagName The tag to set the handler for.
	  * @param handler The handler to call if &lt;tagName&gt; is encountered. This has to be a subclass of REDXMLReadable.
	  */
	public void registerHandler(String tagName, Class handler) {
		REDTracer.info("red.xml", "REDXMLManager", "Registering " + handler + " for <" + tagName + ">.");
		fHandlers.put(tagName.toLowerCase(), handler);
	}
	
	/** Get handler class.
	  * @param tagName The tag to get the handler for.
	  * @return The handler registered for &lt;tagName&gt; or <CODE>null</CODE> if no handler is registered for &lt;tagName&gt;.
	  */
	public Class getHandler(String tagName) {
		return (Class) fHandlers.get(tagName.toLowerCase());
	}

	/** Push handler instance to handler stack.
	  * @param handler The new active handler.
	  */
	public void pushHandler(REDXMLHandlerReader handler) {
		fHandlerStack.push(handler);
	}
	
	/** Pop handler instance from handler stack.
	  * This method will remove the topmost handler instance from the handler stack and pass it to the next handler on stack, by calling 
	  * the method <CODE>innerProduction</CODE>, thus allowing the next handler to process the now removed handler in an appropriate way.
	  */
	public void popHandler() {
		REDXMLHandlerReader oldHandler = (REDXMLHandlerReader) fHandlerStack.pop();
		REDXMLHandlerReader curHandler = getCurrentHandler();
		if (curHandler != null) {
			curHandler.getObj().innerProduction(oldHandler.getObj(), oldHandler, curHandler);
		}
		else {
			fProducedObject = oldHandler.getObj();
		}
	}
	
	/** Get the last outmost produced object */
	public Object getProducedObject() {
		return fProducedObject;
	}
	
	/** Get current handler instance.
	  * @return The topmost handler instance from the handler stack or null if the stack is empty.
	  */
	private REDXMLHandlerReader getCurrentHandler() {
		return (REDXMLHandlerReader) fHandlerStack.peek();
	}
	
	// SAX interface
	/** Process start tags.
	  * This method overwrites the DefaultHandler in SAX to process start tags. It works in the following way:
	  * <OL>
	  * <LI>if there is a registered handler for the tagname encountered, instantiate the handler and push it on top of the handler stack, 
	  * i.e. the new handler instance will be used until either another start tag is found for which there exists a registered handler or the 
	  * the new handler instance decides to finish its work by calling the <CODE>stopHandling</CODE> method, which usually happens,
	  * if the end tag is found.</LI>
	  * <LI>if there is no current handler (i.e. handler stack is empty) throw an exception.</LI>
	  * <LI>else process the start tag by relaying it to the current handler.</LI>
	  * </OL>
	  */
	public void startElement (String uri, String name, String qName, Attributes atts) throws SAXException {
		if (fInitialized) {
			REDTracer.info("red.xml", "REDXMLManager", '<' + name +"> found.");
			REDXMLHandlerReader curHandler = getCurrentHandler();
			Class handlerCl = getHandler(name);
			if (handlerCl != null) {
				try {					
					REDXMLReadable readable = (REDXMLReadable) handlerCl.newInstance();
					curHandler = new REDXMLHandlerReader(this, name.toLowerCase(), readable);
					REDTracer.info("red.xml", "REDXMLManager", "New handler installed: " + curHandler.getObj().getClass());
				}
				catch (Exception e) {
					e.printStackTrace();
					throw new SAXException("Could not instantiate handler for <" + name + ">. " + e);
				}
				pushHandler(curHandler);
			}
				
			if (curHandler != null) {
				REDTracer.info("red.xml", "REDXMLManager", name + " handled by " + curHandler.getObj().getClass());
				curHandler.startElement(uri, name, qName, atts);
			}
			else {
				throw new SAXException("Could not find handler for <" + name + ">.");
			}
		}
		else {
			if (name.equalsIgnoreCase("REDConfig")) {
				fInitialized = true;
			}
			else {
				throw new SAXException("Error: <REDConfig> expected, <" + name + "> found.");
			}
		}
	}

	/** Process end tags.
	  * This method overwrites the DefaultHandler in SAX to relay end tags to the currently installed handler instance.
	  */
    public void endElement (String uri, String name, String qName) throws SAXException {
		if (getCurrentHandler() != null) {
			getCurrentHandler().endElement(uri, name, qName);
		}
    }
	
	/** Process character data.
	  * This method overwrites the DefaultHandler in SAX to relay character data to the currently installed handler instance.
	  */
    public void characters (char ch[], int start, int length) {
		if (getCurrentHandler() != null) {
			getCurrentHandler().characters(ch, start, length);
		}
    }
	
	/** Sax callback method. */
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}
	
	/** Get current line.
	  * @return The line, the parser currently parses, or -1 if either parsing has not started or the sax driver does not support position information.
	  */
	public int getCurLine() {
		if (fLocator == null) {
			return -1;
		}
		else {
			return fLocator.getLineNumber();
		}
	}
	
	/** Get current column.
	  * @return The line, the parser currently parses, or -1 if either parsing has not started or the sax driver does not support position information.
	  */
	public int getCurColumn() {
		if (fLocator == null) {
			return -1;
		}
		else {
			return fLocator.getColumnNumber();
		}
	}

	/** Get current name.
	  * @return The filename, the parser currently parses, or null if either parsing has not started or the sax driver does not support position information.
	  */
	public String getCurName() {
		if (fLocator == null) {
			return null;
		}
		else {
			return fLocator.getPublicId();
		}
	}
	
	
	/** Load from reader
	  * @param reader The reader to read from
	  * @param name The name of the file (stream, URL, etc.) which will be parsed. Will be used to give concise error information.
	  */
	public void parse(Reader r, String name) throws IOException, SAXException {
		XMLReader xr = null;
		// install default sax driver
		if (System.getProperty("org.xml.sax.driver") == null) {
			System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
		}
		try {
			xr = XMLReaderFactory.createXMLReader();
		}
		catch (SAXException se) {
			REDTracer.error("red.xml", "REDXMLManager", "Could not create XML reader: " + se);
			return;
		}
		xr.setContentHandler(this);
		xr.setErrorHandler(this);
		fInitialized = false;
		InputSource is = new InputSource(r);
		is.setPublicId(name);
		xr.parse(is);
	}
	
	/** Flag to indicate whether the &lt;RED&gt; tag has already be encountered. */
	private boolean fInitialized;
	
	/** A stack of handler objects. */
	private final Stack fHandlerStack;
	
	/** A Map tag -> handler class. */
	private final HashMap fHandlers;
	
	/** The last outmost produced object */
	private Object fProducedObject;
	
	/** The current parser location */
	private Locator fLocator;
}
