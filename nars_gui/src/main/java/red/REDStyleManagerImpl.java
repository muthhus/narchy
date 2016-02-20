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
 
package red;

import java.awt.*;
import java.io.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;
import red.xml.*;
import red.util.*;

/** Style manager implementation.  
  * @author rli@chello.at
  * @tier API
  */
public class REDStyleManagerImpl {
	REDStyleManagerImpl() {
		fNameToStyleMap = new TreeMap();
		fDefaultStyle = new REDStyle(Color.black, Color.white, REDLining.NONE, "Monospaced", "PLAIN", 12, null);
		fDefaultStyle.setName("Default");
		fTheme = "Default";
		fNameToStyleMap.put("Default", fDefaultStyle);
		fListeners = new HashSet();
	}
	
	/** Get style by name. 
	  * @param name The name of the style to get.
	  * @return The style stored under the given name, or the default style, if no style is stored under the given name.
	  */
	REDStyle doGetStyle(String name) {
		REDStyle retVal = (REDStyle) fNameToStyleMap.get(name);
		if (retVal == null) {
			retVal = fDefaultStyle;
		}
		return retVal;
	}
		
	/** Get iterator over all registered styles.
	  * @return An iterator which will produce REDStyle objects.
	  */ 	
	Iterator doGetStyleIterator() {
		return fNameToStyleMap.values().iterator();
	}

	/** Get default style.
	  * @return The default style.
	  */
	REDStyle doGetDefaultStyle() {
		return fDefaultStyle;
	}
	
	/** Check availability of style. 
	  * @param name The name of the style to check.
	  * @return <Code>true</Code>, if a style is stored under the given name, <Code>false</Code> otherwise.
	  */
	boolean doHasStyle(String name) {
		return fNameToStyleMap.get(name) != null;
	}
	
	/** Add style. */
	void doAddStyle(String name, REDStyle style) {
		if (name.equals("Default")) {
			fDefaultStyle = style;
		}
		// adapt superstyles. This is O(n^2), but we don't care. This happen from test cases only.
		if (doHasStyle(name)) {
			REDStyle oldStyle = doGetStyle(name);
			HashMap map = new HashMap(); map.put(oldStyle, style);
			Iterator iter = fNameToStyleMap.values().iterator();
			while (iter.hasNext()) {
				REDStyle s = (REDStyle) iter.next();
				s.fixupSuperstyle(map);
			}
		}
		fNameToStyleMap.put(name, style);
		style.setName(name);
		style.setManager(this);
	}
	
	/** Put key <-> value mapping into style(s).
	  * @param stylename The name of the style to put the mapping into.
	  * @param key The key of the mapping.
	  * @param value The value of the mapping.
	  * @param recursive If <Code>true</Code>, the mapping will be put into the given style and all its (current) derived substyles.
	  * @return <Code>true</Code> if the operation was successful. <Code>false</Code> if no style with the given name exists.
	  */
	boolean doPut(String stylename, Object key, Object value, boolean recursive) {
		if (doHasStyle(stylename)) {
			REDStyle style = doGetStyle(stylename);
			if (recursive) {
				Iterator iter = fNameToStyleMap.values().iterator();
				while (iter.hasNext()) {
					REDStyle candidate = (REDStyle) iter.next();
					if (candidate.isA(style)) {
						candidate.put(key, value);
					}
				}
			}
			else {
				style.put(key, value);
			}
			return true;
		}
		return false;	
	}

	/** Remove key <-> value mapping from style(s).
	  * @param stylename The name of the style to remove mapping from.
	  * @param key The key of the mapping to remove.
	  * @param recursive If <Code>true</Code>, the mapping will be removed from the given style and all its (current) derived substyles.
	  * @return <Code>true</Code> if the operation was successful. <Code>false</Code> if no style with the given name exists.
	  */
	boolean doRemove(String stylename, Object key, boolean recursive) {
		if (doHasStyle(stylename)) {
			REDStyle style = doGetStyle(stylename);
			if (recursive) {
				Iterator iter = fNameToStyleMap.values().iterator();
				while (iter.hasNext()) {
					REDStyle candidate = (REDStyle) iter.next();
					if (candidate.isA(style)) {
						candidate.remove(key);
					}
				}
			}
			else {
				style.remove(key);
			}
			return true;
		}
		return false;	
	}
	
	/** Get value from style. 
	  * @param stylename The name of the style to get mapped value for.
	  * @param key The key to get mapped value for.
	  * @return The value associated with the given style and key, or <Code>null</Code> if either the style does not exist or it has not got a value for the given key.
	  */
	Object doGet(String stylename, Object key) {
		if (doHasStyle(stylename)) {
			REDStyle style = doGetStyle(stylename);
			return style.get(key);
		}
		return null;
	}
	
	/** Install theme. 
	  * @param theme The name of the theme to be installed. Theme names are case sensitive.
	  */
	void doSetTheme(String theme) {
		send("beforeThemeChange", fTheme, theme);
		String oldTheme = fTheme;
		fTheme = theme;
		Iterator iter = fNameToStyleMap.values().iterator();
		while (iter.hasNext()) {
			REDStyle s = (REDStyle) iter.next();
			s.installTheme(fTheme);
		}
		send("afterThemeChange", oldTheme, fTheme);
	}
	
	/** Get theme name. 
	  * @return The name of the theme currently installed. Theme names are case sensitive.
	  */
	String doGetTheme() {
		return fTheme;
	}
	
	/** Read style definitions from file.
	  * @param is Input stream to read style definitions from.
	  * @param location The location to be used for error messages.
	  * @param backingStore The file to write modified styles to, if they come from the specified input stream.
	  */
	static void doReadStyleFile(InputStream is, String location, File backingStore) {
		REDXMLManager manager = new REDXMLManager();
		manager.registerHandler("Style", REDStyle.class);
		manager.putClientData("backingStore", backingStore);
		InputStreamReader reader = new InputStreamReader(is);
		try {
			manager.parse(reader, location);
		}
		catch (Exception e) {
			e.printStackTrace();
			REDTracer.error("red", "REDStyleManagerImpl", "Exception while reading style definition from '" + location + "': " + e);
		}
		try { reader.close(); } catch (IOException ioe) { }
	}
	
	/** Get iterator over styles.
	  * @return An iterator, which will iterate over all registered styles, in alphabetical order.
	  */
	Iterator doIterator() {
		return fNameToStyleMap.values().iterator();
	}
	
	/** Create a deep copy (new REDStyle instances). */
	REDStyleManagerImpl deepCopy() {
		REDStyleManagerImpl manager = new REDStyleManagerImpl();
		HashMap map = new HashMap();
		Iterator iter = fNameToStyleMap.keySet().iterator();
		while (iter.hasNext()) {
			String name = String.valueOf(iter.next());
			REDStyle src = doGetStyle(name);
			REDStyle copy = src.copy();	// will return a copy, whose superStyle still points to the original style hierarchy
			copy.installTheme(fTheme);
			manager.doAddStyle(name, copy);
			map.put(src, copy);
		}
		
		// fixup super styles
		iter = manager.doIterator();
		while (iter.hasNext()) {
			REDStyle s = (REDStyle) iter.next();
			s.fixupSuperstyle(map);
		}
		
		return manager;
	}
	
	// Listener stuff
	static class EventListenerReference extends WeakReference {
		EventListenerReference(Object referent) {
			super(referent);
		}

		public boolean equals(Object obj) {
			if (obj instanceof EventListenerReference) {
				return get().equals(((EventListenerReference) obj).get());
			}
			return super.equals(obj);
		}
		
		public int hashCode() {
			Object obj = get();
			if (obj != null) {
				return obj.hashCode();
			}
			else {
				return 0;
			}
		}
	}
	
	public boolean doAddStyleEventListener(REDStyleEventListener listener) {
		return fListeners.add(new EventListenerReference(listener));
	}
	
	public boolean doRemoveStyleEventListener(REDStyleEventListener listener) {
		return fListeners.remove(new EventListenerReference(listener));
	}
	
	private void callListeners(Method m, Object [] arg) {
		Iterator iter = fListeners.iterator();
		while (iter.hasNext()) {
			WeakReference ref = (WeakReference) iter.next();
			Object receiver = ref.get();
			if (receiver != null) {
				try {
					m.invoke(receiver, arg);
				}
				catch (Exception e) {
					REDGLog.error("RED", "Error while trying to invoke '" + m + "' on '" + receiver + "': " + e);
				}
			}
			else {
				iter.remove();
			}
		}
	}
	
	private void send(String methodName, REDStyle s) {
		Class [] argTypes = {REDStyle[].class};
		REDStyle [] styles = { s };
		Object [] arg = { styles };
		try {
			callListeners(REDStyleEventListener.class.getMethod(methodName, argTypes),  arg);
		}
		catch (Exception e) {
			throw new Error(String.valueOf(e));
		}
	}
	
	private void send(String methodName, String paramOne, String paramTwo) {
		Class [] argTypes = {String.class, String.class};
		Object [] arg = { paramOne, paramTwo };
		try {
			callListeners(REDStyleEventListener.class.getMethod(methodName, argTypes), arg);
		}
		catch (Exception e) {
			throw new Error(String.valueOf(e));
		}
	}
	
	public void doSendBeforeStyleChange(REDStyle s) {
		send("beforeStyleChange", s);
	}
	
	public void doSendAfterStyleChange(REDStyle s) {
		send("afterStyleChange", s);
	}
	
	private final TreeMap fNameToStyleMap;
	private REDStyle fDefaultStyle;
	private String fTheme;
	private final HashSet fListeners;
}