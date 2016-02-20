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

import org.xml.sax.helpers.*;
import java.util.*;


/** XML handler base class.
  * This base class provides support for client data and client data stacks to REDXMLHandlerReader and XMLWriter.
  * @author rli@chello.at
  * @tier system
  */
public class REDXMLHandler extends DefaultHandler {
	public REDXMLHandler() {
		fClientData = new HashMap();
	}
	
	/** Associate client data.
	  * @param key The key to store client data under. 
	  * @param data The data to associate.
	  */
	public void putClientData(String key, Object data) {
		fClientData.put(key, data);
	}
	
	/** Get client data.
	  * @param key The key to get client data for.
	  * @return The data associated or null if no client data is stored under the given key.
	  */
	public Object getClientData(String key) {
		return fClientData.get(key);
	}
	
	/** Remove client data.
	  * @param key The key of the client data to remove.
	  * @return The removed data or null, if not data was stored under the given key.
	  */
	public Object removeClientData(String key) {
		return fClientData.remove(key);
	}
	
	private String getFullStackName(String stackName) {
		return stackName + ':' + this + ":Stacks";
	}
	
	private Stack getStack(String stackName) {
		return (Stack) getClientData(getFullStackName(stackName));
	}
	
	/** Push client data to named stack
	  * @param stackName The name of the stack to push client data to
	  * @param data The data to push onto the stack.
	  */
	public void pushClientData(String stackName, Object data) {
		Stack s = getStack(stackName);
		if (s == null) {
			s = new Stack();
			putClientData(getFullStackName(stackName), s);
		}
		s.push(data);
	}
	
	/** Pop client data from named stack
	  * @param stackName The name of the stack to pop client data from
	  * @return The topmost element of the named stack or null if the stack is empty / does not exist.
	  */
	public Object popClientData(String stackName) {
		Stack s = getStack(stackName);
		if (s != null) {
			try {
				return s.pop();
			}
			catch (EmptyStackException ese) { }
		}
		return null;
	}
	
	/** Peek client data from named stack
	  * @param stackName The name of the stack to peek client data from
	  * @return The topmost element of the named stack or null if the stack is empty / does not exist.
	  */
	public Object peekClientData(String stackName) {
		Stack s = getStack(stackName);
		if (s != null) {
			try {
				return s.peek();
			}
			catch (EmptyStackException ese) { }
		}
		return null;
	}

	/** client data map */
	private final HashMap fClientData;
}
