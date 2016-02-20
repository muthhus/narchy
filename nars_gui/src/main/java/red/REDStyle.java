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
import java.util.*;
import red.xml.*;
import red.util.*;

/** REDStyle objects contain foreground and background color, font and lining information
  * @author rli@chello.at
  * @tier API
  * @see REDStyleManager
  * @invariant fForeground != null || fSuper != null
  * @invariant fBackground != null || fSuper != null
  * @invariant fLining != null || fSuper != null
  * @invariant fFontFace != null || fSuper != null
  * @invariant fFontSize != INHERITED || fSuper != null
  * @invariant fFontStyle != INHERITED || fSuper != null
  */
public class REDStyle implements REDXMLReadable {
	/** Inherited from superstyle. */
	final public static int INHERITED = -1;

	/** Create a style.
	  * You should not call this constructor directly. Ask the REDStyleManager for a shared style.
	  * @param foreground The foreground color of the style.
	  * @param background The background color of the style.
	  * @param lining The lining of the style (cf. the LC_* constants).
	  * @param font The font of the style.
	  * @param superStyle The style this style is derived from. May be <Code>null</Code>.
	  * @pre (foreground != null && background != null && lining != INHERITED && font != null) || superStyle != null
	  */
	public REDStyle(Color foreground, Color background, REDLining lining, String fontFace, String fontStyle, int fontSize, REDStyle superStyle) {
		fName = "";
		fManager = REDStyleManager.fgDevNull;
		fMappings = new HashMap();
		fThemes = new TreeMap();
		fCurTheme = getOrCreateThemeEntry("Default");
		fCurTheme.fForeground = foreground; 
		fCurTheme.fBackground = background;
		fCurTheme.fLining = lining;
		fCurTheme.fFontFace = fontFace;
		fCurTheme.fFontStyle = fontStyle;
		fCurTheme.fFontSize = fontSize;
		fCurTheme.fSuper = superStyle;
	}
	
	/** Create empty style.
	  * You should not call this constructor directly. It is used for reading in styles from .xml - files.
	  */
	public REDStyle() {
		this(null, null, null, null, null, INHERITED, null);
	}
	
	REDStyle copy() {
		REDStyle s = new REDStyle();
		Iterator iter = fThemes.keySet().iterator();
		while (iter.hasNext()) {
			String name = String.valueOf(iter.next());
			ThemeEntry e = getThemeEntry(name);
			ThemeEntry eCopy = e.copy();
			s.fThemes.put(name, eCopy);
		}
		s.fDisplayName = fDisplayName;
		s.fDescription = fDescription;
		return s;
	}
	
	public String toString() {
		return "REDStyle (" + getName() + "): \n  Foreground: " + getForeground() + "\n  Background: " + getBackground() + "\n  Lining: " + getLining() + "\n  Font: " + getFont();
	}
	
	/** Get name of style. 
	  * @return The name under which this style is registered in REDStyleManager, or <Code>""</Code> if it is not registered at all.
	  */
	public String getName() {
		return fName;
	}
	
	/** Set name of style. This method must be called by REDStyleManager only.
	  * @param name The name of the style.
	  */
	void setName(String name) {
		fName = name;
	}
	
	public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
		handler.mapStart("Foreground", "setForeground('Default', (int) #red, (int) #green, (int) #blue)");
		handler.mapStart("Background", "setBackground('Default', (int) #red, (int) #green, (int) #blue)");
		handler.mapEnd("Lining", "setLining('Default', (red.REDLining) #[red.REDLining.])");
		handler.mapEnd("Super", "setSuper('Default', #)");
		handler.mapEnd("FontFace", "setFontFace('Default', #)");
		handler.mapEnd("FontSize", "setFontSize('Default', (int) # = '-1')");
		handler.mapEnd("FontStyle", "setFontStyle('Default', #)");
		handler.mapEnd("DisplayName", "setDisplayName(#)");
		handler.mapEnd("Description", "setDescription(#)");
		handler.mapEnd("#", "registerStyle(#&, #$)");
		handler.mapStart("#", "setId(#id, #theme='Default', #&, #$)");
	}
	
	public void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer) {
	}

	/** Set foreground color.  */
	public boolean setForeground(String theme, int red, int green, int blue) {
		return setForeground(theme, new Color(red, green, blue));
	}
	
	/** Set foreground color.  */
	public boolean setForeground(String theme, Color c) {
		ThemeEntry e = getOrCreateThemeEntry(theme);
		fManager.doSendBeforeStyleChange(this);
		e.fForeground = c;
		fManager.doSendAfterStyleChange(this);
		return true;		
	}
	
	/** Set background color.  */
	public boolean setBackground(String theme, int red, int green, int blue) {
		return setBackground(theme, new Color(red, green, blue));
	}
	
	/** Set background color.  */
	public boolean setBackground(String theme, Color c) {
		ThemeEntry e = getOrCreateThemeEntry(theme);
		fManager.doSendBeforeStyleChange(this);
		e.fBackground = c;
		fManager.doSendAfterStyleChange(this);
		return true;
	}
	
	
	/** Set lining. */
	public boolean setLining(String theme, REDLining lining) {
		ThemeEntry e = getOrCreateThemeEntry(theme);
		fManager.doSendBeforeStyleChange(this);
		e.fLining = lining;
		fManager.doSendAfterStyleChange(this);
		return true;
	}
	
	private boolean setSuper(String theme, REDStyle newSuper) {
		ThemeEntry e = getOrCreateThemeEntry(theme);
		if (newSuper != e.fSuper) {
			fManager.doSendBeforeStyleChange(this);
			e.fSuper = newSuper;
			e.fFontCache = null;
			fManager.doSendAfterStyleChange(this);
		}
		return true;
	}
	
	/** Set super-style. */
	public void setSuper(String theme, String superStyle) {
		if (!REDStyleManager.hasStyle(superStyle)) {
			REDTracer.error("red", "REDStyle", "Unknown superstyle '" + superStyle + "' ignored. Using 'Default' instead.");
		}
		setSuper(theme, REDStyleManager.getStyle(superStyle));
	}
	
	/** Set font face. */
	public boolean setFontFace(String theme, String fontFace) {
		ThemeEntry e = getOrCreateThemeEntry(theme);
		if (fontFace != e.fFontFace) {
			fManager.doSendBeforeStyleChange(this);
			e.fFontFace = fontFace;
			e.fFontCache = null;
			fManager.doSendAfterStyleChange(this);
		}
		return true;
	}
	
	/** Set font size. */
	public boolean setFontSize(String theme, int fontSize) {
		ThemeEntry e = getOrCreateThemeEntry(theme);
		if (fontSize != e.fFontSize) {
			fManager.doSendBeforeStyleChange(this);
			e.fFontSize = fontSize;
			e.fFontCache = null;
			fManager.doSendAfterStyleChange(this);
		}
		return true;
	}

	/** Set font style. */
	public boolean setFontStyle(String theme, String fontStyle) {
		ThemeEntry e = getOrCreateThemeEntry(theme);
		if (fontStyle != e.fFontStyle) {
			fManager.doSendBeforeStyleChange(this);
			e.fFontStyle = fontStyle;
			e.fFontCache = null;
			fManager.doSendAfterStyleChange(this);
		}
		return true;
	}

	/** Set style id (name). */
	public static void setId(String id, String theme, REDXMLHandlerReader handler, REDXMLManager manager) {
		handler.putClientData("id", id);
		handler.putClientData("theme", theme);
	}
	
	/** Set display name.
	  * The display name is a short, concise description of the style to be used as label for editing purposes.
	  * @param displayName The display name to be used.
	  */
	public void setDisplayName(String displayName) {
		fDisplayName = displayName;
	}
	
	/** Get display name.
	  * The display name is a short, concise description of the style to be used as label for editing purposes.
	  * @return The display name to be used for this style. If no display name has been specified, the name of the style will be returned.
	  */
	public String getDisplayName() {
		if (fDisplayName != null) {
			return fDisplayName;
		}
		return getName();
	}
	
	/** Set description.
	  * The description is a verbose, but single-lined definition of the purpose of a style.
	  * @param description The description to be set.
	  */
	public void setDescription(String description) {
		fDescription = description;
	}
	
	/** Get description.
	  * The description is a verbose, but single-lined definition of the purpose of a style.
	  * @return The description be used for this style. If no description has been specified, <Code>""</Code> will be returned.
	  */
	public String getDescription() {
		if (fDescription != null) {
			return fDescription;
		}
		return "";
	}
	
	
	private ThemeEntry getOrCreateThemeEntry(String theme) {
		ThemeEntry e = (ThemeEntry) fThemes.get(theme);
		if (e == null) {
			if (theme.equals("Default")) {
				e = new ThemeEntry();
			}
			else {
				e = getThemeEntry("Default").copy();
			}
			fThemes.put(theme, e);
		}
		return e;
	}
	
	private ThemeEntry getThemeEntrySafe(String theme) {
		ThemeEntry e = (ThemeEntry) fThemes.get(theme);
		if (e == null) {
			e = (ThemeEntry) fThemes.get("Default");
		}
		return e;
	}
	
	private ThemeEntry getThemeEntry(String theme) {
		return (ThemeEntry) fThemes.get(theme);
	}
	

	private void merge(String theme, ThemeEntry source) {
		ThemeEntry e = getOrCreateThemeEntry(theme);
		e.fForeground = source.fForeground;
		e.fBackground = source.fBackground;
		e.fLining = source.fLining;
		e.fFontFace = source.fFontFace;
		e.fFontStyle = source.fFontStyle;
		e.fFontSize = source.fFontSize;
		e.fSuper = source.fSuper;
	}
	
	void installTheme(String theme) {
		fCurTheme = getThemeEntrySafe(theme);		
	}
	
	/** Set backing store for theme of style. If the given theme name does not exist, the method has no effect.
	  * @param theme The name of the theme to set backing store for.
	  * @param backingStore The file to write this theme back, if requested.
	  */
	public void setBackingStore(String theme, File backingStore) {
		ThemeEntry e = getThemeEntry(theme);
		if (e != null) {
			e.fBackingStore = backingStore;
		}
	}
	
	/** Get backing store. 
	  * @param theme The name of the theme to get backing store for.
	  * @return The file this style/theme is stored into, or <Code>null</Code> if the given theme name does not exist.
	  */
	public File getBackingStore(String theme) {
		ThemeEntry e = getThemeEntry(theme);
		if (e != null) {
			return e.fBackingStore;
		}
		return null;
	}
	
	private boolean styleOk() {
		return fCurTheme.fSuper != null ||
			(fCurTheme.fFontFace != null && fCurTheme.fFontSize != INHERITED && fCurTheme.fFontStyle != null 
				&& fCurTheme.fLining != null && fCurTheme.fForeground != null && fCurTheme.fBackground != null);
	}
	
	/** Register style at REDStyleManager. XML callback method. */
	public void registerStyle(REDXMLHandlerReader handler, REDXMLManager manager) {
		String id = (String) handler.getClientData("id");
		String theme = String.valueOf(handler.getClientData("theme"));
		if (styleOk()) {
			if (!REDStyleManager.hasStyle(id)) {
				REDStyleManager.addStyle(id, this);
			}
			REDStyle target = REDStyleManager.getStyle(id);
			target.merge(theme, this.fCurTheme);
			target.setBackingStore(theme, (File) manager.getClientData("backingStore"));
			handler.removeClientData("id");
			handler.removeClientData("theme");
			if (theme.equals("Default")) {
				target.fDisplayName = fDisplayName;
				target.fDescription = fDescription;
			}
			else if (fDisplayName != null || fDescription != null) {
				REDGLog.warning("RED", "Display name and description are ignored in theme '" + theme + "' of style '" + id + '\'');
			}
		}
		else {
			REDGLog.error("RED", "Incomplete style '" + id + "' (theme '" + theme + "') ignored.");
		}
	}

	/**
	 * Get foreground color of style.
	 *
	 * @param theme The theme to look foreground color up for. If the given theme does not exist, the default theme is used.
	 * @return Foreground color object.
	 */
	public Color getForeground(String theme) {
		REDStyle other = this;
		while (true) {
			ThemeEntry e = other.getThemeEntrySafe(theme);
			if (e.fForeground == null && e.fSuper != null) {
				other = e.fSuper;
				continue;
			}
			return e.fForeground;
		}
	}

	/**
	 * Get foreground color of style for currently active theme.
	 *
	 * @return Foreground color object.
	 */
	public Color getForeground() {
		REDStyle other = this;
		while (true) {
			if (other.fCurTheme.fForeground == null && other.fCurTheme.fSuper != null) {
				other = other.fCurTheme.fSuper;
				continue;
			}
			return other.fCurTheme.fForeground;
		}
	}

	/**
	 * Get background color of style.
	 *
	 * @param theme The theme to look in. If the theme does not exist, the default theme is used.
	 * @return Background color object.
	 */
	public Color getBackground(String theme) {
		REDStyle other = this;
		while (true) {
			ThemeEntry e = other.getThemeEntrySafe(theme);
			if (e.fBackground == null && e.fSuper != null) {
				other = e.fSuper;
				continue;
			}
			return e.fBackground;
		}
	}

	/**
	 * Get background color of style for active theme.
	 *
	 * @return Background color object.
	 */
	public Color getBackground() {
		REDStyle other = this;
		while (true) {
			if (other.fCurTheme.fBackground == null && other.fCurTheme.fSuper != null) {
				other = other.fCurTheme.fSuper;
				continue;
			}
			return other.fCurTheme.fBackground;
		}
	}

	/**
	 * Get lining of style.
	 *
	 * @param theme The theme to look lining up for. If the theme does not exist, the default theme is used.
	 * @return A REDLining object representing the lining of the style.
	 */
	public REDLining getLining(String theme) {
		REDStyle other = this;
		while (true) {
			ThemeEntry e = other.getThemeEntrySafe(theme);
			if (e.fLining == null && e.fSuper != null) {
				other = e.fSuper;
				continue;
			}
			return e.fLining;
		}
	}

	/**
	 * Get lining of style of active theme.
	 *
	 * @return A REDLining object representing the lining of the style.
	 */
	public REDLining getLining() {
		REDStyle other = this;
		while (true) {
			if (other.fCurTheme.fLining == null && other.fCurTheme.fSuper != null) {
				other = other.fCurTheme.fSuper;
				continue;
			}
			return other.fCurTheme.fLining;
		}
	}

	/**
	 * Get font face of style.
	 *
	 * @param theme The theme to look font face up for. If the theme does not exist, the default theme is used.
	 * @return The string representing the font face of the style.
	 */
	public String getFontFace(String theme) {
		REDStyle other = this;
		while (true) {
			ThemeEntry e = other.getThemeEntrySafe(theme);
			if (e.fFontFace == null || e.fFontFace.isEmpty()) {
				other = e.fSuper;
				continue;
			}
			return e.fFontFace;
		}
	}

	/**
	 * Get font face of style for active theme.
	 *
	 * @return The string representing the font face of the style.
	 */
	public String getFontFace() {
		REDStyle other = this;
		while (true) {
			if (other.fCurTheme.fFontFace == null || other.fCurTheme.fFontFace.isEmpty()) {
				other = other.fCurTheme.fSuper;
				continue;
			}
			return other.fCurTheme.fFontFace;
		}
	}

	/**
	 * Get font style of style.
	 *
	 * @param theme The theme to look up font style for. If the theme does not exist, the default theme is used.
	 * @return The font style of the style.
	 */
	public String getFontStyle(String theme) {
		REDStyle other = this;
		while (true) {
			ThemeEntry e = other.getThemeEntrySafe(theme);
			if (e.fFontStyle == null || e.fFontStyle.isEmpty()) {
				other = e.fSuper;
				continue;
			}
			return e.fFontStyle;
		}
	}

	/**
	 * Get font style of style for active style.
	 *
	 * @return The font style of the style.
	 */
	public String getFontStyle() {
		REDStyle other = this;
		while (true) {
			if (other.fCurTheme.fFontStyle == null || other.fCurTheme.fFontStyle.isEmpty()) {
				other = other.fCurTheme.fSuper;
				continue;
			}
			return other.fCurTheme.fFontStyle;
		}
	}

	/**
	 * Get font size of style.
	 *
	 * @param theme The theme to look up font size for. If the theme does not exist, the default theme is used.
	 * @return The font size of the style.
	 */
	public int getFontSize(String theme) {
		REDStyle other = this;
		while (true) {
			ThemeEntry e = other.getThemeEntrySafe(theme);
			if (e.fFontSize == other.INHERITED) {
				other = e.fSuper;
				continue;
			}
			return e.fFontSize;
		}
	}

	/**
	 * Get font size of style for active theme.
	 *
	 * @return The font size of the style.
	 */
	public int getFontSize() {
		REDStyle other = this;
		while (true) {
			if (other.fCurTheme.fFontSize == other.INHERITED) {
				other = other.fCurTheme.fSuper;
				continue;
			}
			return other.fCurTheme.fFontSize;
		}
	}
	
	/** Get font of style. */
	public Font getFont() {
		if (fCurTheme.fFontCache == null) {
			fCurTheme.fFontCache = Font.decode(getFontFace() + '-' + getFontStyle() + '-' + getFontSize());
		}
		return fCurTheme.fFontCache;
	}

	/** Get superstyle. 
	  * @param theme The theme to look up superstyle for. If the theme does not exist, the default theme is used.
	  * @return The superstyle of this style or <Code>null</Code> if this style has no superstyle.
	  */
	REDStyle getSuperStyle(String theme) {
		return getThemeEntrySafe(theme).fSuper;
	}

	/** Get superstyle. 
	  * @return The superstyle of this style or <Code>null</Code> if this style has no superstyle.
	  */
	REDStyle getSuperStyle() {
		return fCurTheme.fSuper;
	}
	
	/** Fixup superstyle. After making a copy of a style hierarchy (REDStyleManager.deepCopy()), super styles must be fixed.
	  * This method will change the super styles of all its themes by looking up the current super style as key in the passed hashmap and 
	  * setting the super style to the found value.
	  * @param map A map containing oldStyle => newStyle mappings.
	  */
	void fixupSuperstyle(HashMap map) {
		Iterator iter = fThemes.values().iterator();
		while (iter.hasNext()) {
			ThemeEntry e = (ThemeEntry) iter.next();
			if (e.fSuper != null) {
				REDStyle newSuper = (REDStyle) map.get(e.fSuper);
				if (newSuper != null) {
					e.fSuper = newSuper;
					e.fFontCache = null;
				}
			}
		}
	}
	
	/** Get superstyle relationship.
	  * @param s The style to check against.
	  * @return <Code>true</Code> if this is equal or a substyle of <Code>s</Code>
	  */
	public boolean isA(REDStyle s) {
		return s == this || fCurTheme.fSuper != null && fCurTheme.fSuper.isA(s);
	}
	
	/** Check for theme entry.
	  * @param theme The theme to check for.
	  * @param return <Code>true</Code>, if this style has the given Theme defined. False otherwise.
	  */
	public boolean hasTheme(String theme) {
		return fThemes.get(theme) != null;
	}
	
	/** Check for definition of foreground.
	  * @param theme The theme to check for. If the theme does not exist, the default theme is used.
	  * @return <Code>true</Code> if the given theme defines the foreground color without using the superstyle.
	  */
	public boolean definesForeground(String theme) {
		return getThemeEntrySafe(theme).fForeground != null;
	}
	
	/** Check for definition of background.
	  * @param theme The theme to check for. If the theme does not exist, the default theme is used.
	  * @return <Code>true</Code> if the given theme defines the background color without using the superstyle.
	  */
	public boolean definesBackground(String theme) {
		return getThemeEntrySafe(theme).fBackground != null;
	}
	
	/** Check for definition of font face.
	  * @param theme The theme to check for. If the theme does not exist, the default theme is used.
	  * @return <Code>true</Code> if the given theme defines the font face without using the superstyle.
	  */
	public boolean definesFontFace(String theme) {
		return getThemeEntrySafe(theme).fFontFace != null;
	}
	
	/** Check for definition of font size.
	  * @param theme The theme to check for. If the theme does not exist, the default theme is used.
	  * @return <Code>true</Code> if the given theme defines the font size without using the superstyle.
	  */
	public boolean definesFontSize(String theme) {
		return getThemeEntrySafe(theme).fFontSize != INHERITED;
	}
	
	/** Check for definition of font style.
	  * @param theme The theme to check for. If the theme does not exist, the default theme is used.
	  * @return <Code>true</Code> if the given theme defines the font style without using the superstyle.
	  */
	public boolean definesFontStyle(String theme) {
		return getThemeEntrySafe(theme).fFontStyle != null;
	}
	
	/** Check for definition of lining.
	  * @param theme The theme to check for. If the theme does not exist, the default theme is used.
	  * @return <Code>true</Code> if the given theme defines the lining without using the superstyle.
	  */
	public boolean definesLining(String theme) {
		return getThemeEntrySafe(theme).fLining != null;
	}
	
	/** Check for definition of superstyle.
	  * @param theme The theme to check for. If the theme does not exist, the default theme is used.
	  * @return <Code>true</Code> if the given theme has a superstyle.
	  */
	public boolean definesSuperStyle(String theme) {
		return getThemeEntrySafe(theme).fSuper != null;
	}
	
	/** Iterate alphabetically over defined theme names.
	  * @return An iterator which will return String objects in ascending order, representing the defined themes of this style.
	  */
	public Iterator themeIterator() {
		return fThemes.keySet().iterator();
	}
	
	/** Put key <-> value mapping into style.
	  * @param key The key of the mapping.
	  * @param value The value of the mapping.
	  */
	void put(Object key, Object value) {
		fMappings.put(key, value);
	}
	
	/** Remove key <-> value mapping from style.
	  * @param key The key of the mapping to remove.
	  */
	void remove(Object key) {
		fMappings.remove(key);
	}
	
	/** Get value from style. 
	  * @param key The key to get mapped value for.
	  * @return The value associated with the given key, or <Code>null</Code> if it has not got a value for the given key.
	  */
	Object get(Object key) {
		return fMappings.get(key);
	}
		
	/** Auxiliary XML writing method. */
	private static void writeContentEntity(REDXMLHandlerWriter handler, String tagName, Object content) throws IOException {
		if (content != null) {
			handler.writeEntity(tagName, null, String.valueOf(content));
		}
	}
	
	/** Auxiliary XML writing method. */
	private static void writeColorEntity(REDXMLHandlerWriter handler, String tagName, Color color) throws IOException {
		if (color != null) {
			handler.writeEntity(tagName, "red=\"" + color.getRed() + "\" green=\"" + color.getGreen() + "\" blue=\"" + color.getBlue() + '"', null);
		}
	}
	
	void writeTheme(String theme, REDXMLHandlerWriter handler) throws IOException {
		ThemeEntry e = getThemeEntry(theme);
		if (theme.equals("Default")) {
			handler.openTag("Style", "id=\"" + getName() + '"');
		}
		else {
			handler.openTag("Style", "id=\"" + getName() + "\" theme=\"" + theme + '"');
		}
				
		writeContentEntity(handler, "FontFace", e.fFontFace);
		if (e.fFontSize != INHERITED) {
			writeContentEntity(handler, "FontSize", String.valueOf(e.fFontSize));
		}
		writeContentEntity(handler, "FontStyle", e.fFontStyle);
		writeContentEntity(handler, "Lining", e.fLining);	
		writeColorEntity(handler, "Foreground", e.fForeground);
		writeColorEntity(handler, "Background", e.fBackground);
		if (e.fSuper != null) {
			writeContentEntity(handler, "Super", e.fSuper.getName());
		}
		
		handler.closeTag();
	}
	
	
	void setManager(REDStyleManagerImpl manager) {
		fManager = manager;
	}
	
	boolean equalsTheme(String theme, REDStyle that) {
		ThemeEntry thisTheme = getThemeEntry(theme);
		ThemeEntry thatTheme = that.getThemeEntry(theme);
		return thisTheme != null && thatTheme != null && thisTheme.equalsEntry(thatTheme);
	}

	static class ThemeEntry {
		private Color fForeground;
		private Color fBackground;
		private REDLining fLining;
		private String fFontFace;
		private String fFontStyle;
		private int fFontSize;
		private Font fFontCache;
		private REDStyle fSuper;
		private File fBackingStore;
		
		boolean equalsEntry(ThemeEntry that) {
			return fFontSize == that.fFontSize && 
				fLining == that.fLining &&
				(fFontStyle == null && that.fFontStyle == null || fFontStyle != null && fFontStyle.equals(that.fFontStyle)) && 
				(fFontFace == null && that.fFontFace == null || fFontFace != null && fFontFace.equals(that.fFontFace)) &&
				(fForeground == null && that.fForeground == null || fForeground != null && fForeground.equals(that.fForeground)) && 
				(fBackground == null && that.fBackground == null || fBackground != null && fBackground.equals(that.fBackground)) && 
				(fSuper == null && that.fSuper == null || fSuper != null && fSuper.getName().equals(that.fSuper.getName()));
		}
		
		ThemeEntry copy() {
			ThemeEntry e = new ThemeEntry();
			e.fForeground = fForeground;
			e.fBackground = fBackground;
			e.fLining = fLining;
			e.fFontFace = fFontFace;
			e.fFontStyle = fFontStyle;
			e.fFontSize = fFontSize;
			e.fFontCache = fFontCache;
			e.fSuper = fSuper;
			e.fBackingStore = fBackingStore;
			return e;
		}
	}		

	private final TreeMap fThemes;
	private ThemeEntry fCurTheme;
	private final HashMap fMappings;
	private String fName, fDisplayName, fDescription;
	private REDStyleManagerImpl fManager;
}
