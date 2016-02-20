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


/** XML writer. This class is responsible for writing XML. 
  * @author rli@chello.at
  * @tier system
  */
final public class REDXMLHandlerWriter extends REDXMLHandler {
	/** Create XML writer.
	  * @param stream The output stream to write to.
	  */
	public REDXMLHandlerWriter(OutputStream stream) {
		fIndentLevel = 0;
		fTagStack = new Stack();
		fStream = stream;
	}
	
	private void writeStr(String str) throws IOException {
		fStream.write(str.getBytes());
	}
	
	private void writeTag(String tagname, String attributes, boolean newline, boolean empty) throws IOException {
		String emptySlash = empty ? "/" : "";
		for (int x = 0; x < fIndentLevel; x++) {
			writeStr("\t");
		}
		if (attributes == null || attributes.isEmpty()) {
			writeStr('<' + tagname + emptySlash + '>');
		}
		else {
			writeStr('<' + tagname + ' ' + attributes + emptySlash + '>');
		}
		if (newline) {
			writeStr("\n");
		}
	}
	
	/** Write an open tag.
	  * @param tagname The name of the tag
	  * @param attributes The attributes to be written into the open tag.
	  * @param newline Insert linebreak after tag.
	  */
	public void openTag(String tagname, String attributes, boolean newline) throws IOException {
		writeTag(tagname, attributes, newline, false);
		fIndentLevel++;
		fTagStack.push(tagname);
	}
	
	/** Write an open tag and break line,
	  * @param tagname The name of the tag
	  * @param attributes The attributes to be written into the open tag.
	  */
	public void openTag(String tagname, String attributes) throws IOException {
		openTag(tagname, attributes, true);
	}
	
	
	/** Write a closing tag.  */
	public void closeTag() throws IOException {
		closeTag(true);
	}
	
	/** Write a closing tag. 
	  * @param withIndent If <Code>true</Code> prepend indentation.
	  */
	void closeTag(boolean withIndent) throws IOException {
		String tagname = String.valueOf(fTagStack.pop());
		fIndentLevel--;
		if (withIndent) {
			for (int x = 0; x < fIndentLevel; x++) {
				writeStr("\t");
			}
		}
		writeStr("</" + tagname + ">\n");
	}
	
	/** Write entity. 
	  * @param tagname The tagname to be used.
	  * @param attributes The attributes to be written into open tag. May be <Code>null</Code>.
	  * @param content The content data to be written. If <Code>null</Code>, an empty tag (<Code>&lt;Tag/&gt;</Code>) will be written.
	  */
	public void writeEntity(String tagname, String attributes, String content) throws IOException {
		if (content != null) {
			openTag(tagname, attributes, false);
			writeStr(content);
			closeTag(false);
		}
		else {
			writeTag(tagname, attributes, true, true);
		}
	}
	
	public void write(REDXMLWritable obj) throws IOException {
		obj.writeXML(this);
	}
	
	public void writeXMLHeader() throws IOException {
		writeStr("<?xml version=\"1.0\"?>\n\n");
	}
	
	Stack fTagStack;
	OutputStream fStream;
	int fIndentLevel;
}
