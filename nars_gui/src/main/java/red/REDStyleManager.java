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

import java.io.*;
import java.util.*;
import red.util.*;

/** Style manager.  Singleton pattern.
  * This class manages a hierarchy of styles loaded from xml - files. To be loaded an xml - file must fulfill these conditions:
  * <UL>
  * <LI>Reachable in classpath + "config/style"
  * <LI>End with ".[digit].xml", e.g. Styles.0.xml or Styles.7.xml. REDStyleManager loads files with lower digits first.
  * </UL>
  * Stylenames are case sensitive.
  * @author rli@chello.at
  * @tier API
  */
public class REDStyleManager {
	/** Get style by name. 
	  * @param name The name of the style to get.
	  * @return The style stored under the given name, or the default style, if no style is stored under the given name.
	  */
	static public REDStyle getStyle(String name) {
		return fgInstance.doGetStyle(name);
	}
	
	/** Get iterator over all registered styles.
	  * @return An iterator which will produce REDStyle objects.
	  */ 	
	static public Iterator getStyleIterator() {
		return fgInstance.doGetStyleIterator();
	}

	/** Get default style.
	  * @return The default style.
	  */
	static public REDStyle getDefaultStyle() {
		return fgInstance.doGetDefaultStyle();
	}
	
	/** Check availability of style. 
	  * @param name The name of the style to check.
	  * @return <Code>true</Code>, if a style is stored under the given name, <Code>false</Code> otherwise.
	  */
	static public boolean hasStyle(String name) {
		return fgInstance.doHasStyle(name);
	}
	
	static void addStyle(String name, REDStyle style) {
		fgInstance.doAddStyle(name, style);
	}
	
	/** Put key <-> value mapping into style(s).
	  * @param stylename The name of the style to put the mapping into.
	  * @param key The key of the mapping.
	  * @param value The value of the mapping.
	  * @param recursive If <Code>true</Code>, the mapping will be put into the given style and all its (current) derived substyles.
	  * @return <Code>true</Code> if the operation was successful. <Code>false</Code> if no style with the given name exists.
	  */
	static public boolean put(String stylename, Object key, Object value, boolean recursive) {
		return fgInstance.doPut(stylename, key, value, recursive);
	}
	
	/** Remove key <-> value mapping from style(s).
	  * @param stylename The name of the style to remove mapping from.
	  * @param key The key of the mapping to remove.
	  * @param recursive If <Code>true</Code>, the mapping will be removed from the given style and all its (current) derived substyles.
	  * @return <Code>true</Code> if the operation was successful. <Code>false</Code> if no style with the given name exists.
	  */
	static public boolean remove(String stylename, Object key, boolean recursive) {
		return fgInstance.doRemove(stylename, key, recursive);
	}
	
	/** Get value from style. 
	  * @param stylename The name of the style to get mapped value for.
	  * @param key The key to get mapped value for.
	  * @return The value associated with the given style and key, or <Code>null</Code> if either the style does not exist or it has not got a value for the given key.
	  */
	static public Object get(String stylename, Object key) {
		return fgInstance.doGet(stylename, key);
	}
	
	/** Install theme. 
	  * @param theme The name of the theme to be installed. Theme names are case sensitive.
	  */
	static public void setTheme(String theme) {
		fgInstance.doSetTheme(theme);
	}
	
	/** Get theme name. 
	  * @return The name of the theme currently installed. Theme names are case sensitive.
	  */
	static public String getTheme() {
		return fgInstance.doGetTheme();
	}

	/** Read style definitions from file.
	  * @param is Input stream to read style definitions from.
	  * @param location The location to be used for error messages.
	  * @param backingStore The file to write modified styles to, if they come from the specified input stream.
	  */
	static public void readStyleFile(InputStream is, String location, File backingStore) {
		REDStyleManagerImpl.doReadStyleFile(is, location, backingStore);
	}
	
	/** Read styles from xml - files config/style/*.[digit].xml */
	static private void readStyleFiles() {
		REDTracer.info("red", "REDStyleManager", "Reading style files.");
		for (int i = 0; i <= 9; i++) {
			REDResourceInputStreamIterator iter = REDResourceManager.getInputStreams("config/style", "." + i + ".xml");
			while (iter.hasNext()) {
				InputStream is = (InputStream) iter.next();
				readStyleFile(is, String.valueOf(iter.curName()), null);
			}
		}
	}
	
	/** Get iterator over styles.
	  * @return An iterator, which will iterate over all registered styles, in alphabetical order.
	  */
	static public Iterator iterator() {
		return fgInstance.doIterator();
	}

	// LIstener stuff	
	/** Add style event listener. This method will not create a reference to the added listener (weak listener pattern).
	  * @param listener The listener to receive style-related events.
	  */
	static public boolean addStyleEventListener(REDStyleEventListener listener) {
		return fgInstance.doAddStyleEventListener(listener);
	}
	
	/** Remove style event listener.
	  * @param listener The listener to remove.
	  */
	static public boolean removeStyleEventListener(REDStyleEventListener listener) {
		return fgInstance.doRemoveStyleEventListener(listener);
	}
	
	// Cannot instantiate REDStyleManagers from outside.
	private REDStyleManager() {
	}

	/** Get singleton instance.
	  * @return The REDStyleManagerImpl instance used for calls to static members of REDStyleManager.
	 */	
	static REDStyleManagerImpl getInstance() {
		return fgInstance;
	}
	
	
	static REDStyleManagerImpl fgInstance = new REDStyleManagerImpl();
	static REDStyleManagerImpl fgDevNull = new REDStyleManagerImpl();	// dummy instance, used as event sink.
	static {
		readStyleFiles();
	}		
}
