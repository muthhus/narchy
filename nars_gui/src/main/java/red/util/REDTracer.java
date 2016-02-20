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
 
package red.util;

import red.xml.*;
import java.util.*;
import java.io.*;

/** XML configured trace facility. A Tracer is used for debugging a program.
  * The enclosing tag is &lt;Tracer level="levelString"&gt; ... &lt;/Tracer&gt;, where levelString may be one of "Off", "Error", "Warning", "Info". 
  * (Cf. the corresponding constants).
  * Configuration entries for the tracer are delimted by &lt;TracePackage&gt; ... &lt;/TracePackage&gt; and contain one tag entry 
  * &lt;On&gt;Rules&lt;/On&gt;, where Rules is defined by the following EBNF syntax:
  * <PRE>
  * Rules = [Rule] {"|" Rule}.
  * Rule = Allow | Deny | Wildcard.
  * Allow = Groupname.
  * Deny = "!" Groupname.
  * Wildcard = "*".
  * </PRE>
  * <UL>
  * <LI>Allow rules specify that traces with the given groupname are displayed.</LI>
  * <LI>Deny rules specify that traces with the given groupname are not displayed.</LI>
  * <LI>Wildcard specifies that all groups of this package are displayed.</LI>
  * </UL>
  * Rules are checked left to right. The first rule applying is taken. Thus <CODE>&lt;On&gt;!XXX|*&lt;/On&gt;</CODE> will highlight all but the 
  * group "XXX" in this package.
  * @author rli@chello.at
  * @tier system
  * @tbd Maybe we should implement a cache for speedup.
  */
final public class REDTracer implements REDXMLReadable {
	/** Do not print anything not specified in Tracer config */
	final public static int fcOff = 0;
	/** Print errors even if not specified in Tracer config */
	final public static int fcError = 1;
	/** Print errors and warnings even if not specified in Tracer config */
	final public static int fcWarning = 2;
	/** Print everything even if not specified in Tracer config */
	final public static int fcInfo = 3;
	
	/** Default constructor. Needed for REDXMLHandlerReader management. Do not use. Use one of the static methods instead. */
	public REDTracer() {
		super();
		fPackage = "";
		fgLevel = fcOff;
	}
	
	public void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError {
		handler.mapStart("#", "setGeneralLevel(#level)");
		handler.mapStart("TracePackage", "setId(#id)");
		handler.mapEnd("on", "parseGroupDetails(#)");
	}
	
	public void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer) { }
	
	
	static private void printMsg(String level, String pack, String group, String message) {
		System.out.println(level + ": " + pack + '[' + group +"]: " + message);
	}
	
	/** Display information message 
	  * @param pack The package this message belongs to.
	  * @param group The group this message belongs to.
	  * @param messsage The message to be displayed.
	  */
	static public void info(String pack, String group, String message) {
		if (fgLevel >= fcInfo || wantTrace(pack, group)) {
			printMsg("INFO", pack, group, message);
		}
	}
	
	/** Display warning message 
	  * @param pack The package this message belongs to.
	  * @param group The group this message belongs to.
	  * @param messsage The message to be displayed.
	  */
	static public void warning(String pack, String group, String message) {
		if (fgLevel >= fcWarning || wantTrace(pack, group)) {
			printMsg("WARN", pack, group, message);
		}
	}
	
	/** Display error message 
	  * @param pack The package this message belongs to.
	  * @param group The group this message belongs to.
	  * @param messsage The message to be displayed.
	  */
	static public void error(String pack, String group, String message) {
		if (fgLevel >= fcError || wantTrace(pack, group)) {
			printMsg("ERR.", pack, group, message);
		}
	}

	static boolean wantTrace(String pack, String group) {
		ArrayList rules = (ArrayList) fgPackages.get(pack);
		if (rules == null) {
			if (!fgDefault && fgWantLevel == 0 && !fgWarnedPacks.contains(pack)) {
				fgWantLevel++;
				REDTracer.warning("red.util", "Tracer", "No rules specified for " + pack + ". Will not display messages of that group.");
				fgWarnedPacks.add(pack);
				fgWantLevel--;
			}
			return fgDefault;
		}
		else {
			Iterator iter = rules.iterator();
			int result = fcUndecided;
			while (iter.hasNext() && result == fcUndecided) {
				result = ((Rule) iter.next()).wantTrace(group);
			}
			return result == fcYes;
		}
	}
	
	/** REDXMLHandlerReader callback routine. */
	public void setId(String id) {
		fPackage = id;
	}
	
	public void setGeneralLevel(String level) {
		try {
			fgLevel = getClass().getField("fc" + level).getInt(this);
		}
		catch (Exception e) {
			System.err.println("Unknown general level specified: '" + level + "'.");
		}
	}
	
	/** REDXMLHandlerReader callback routine. */
	public void parseGroupDetails(String details) {
		ArrayList rules = new ArrayList();
		StringTokenizer tok = new StringTokenizer(details, "|");
		while (tok.hasMoreTokens()) {
			String ruleStr = tok.nextToken();
			REDTracer.info("red.util", "Tracer", "Making rule from " + ruleStr);
			if (ruleStr.equals("*")) {
				rules.add(new Wildcard());
			}
			else if (ruleStr.startsWith("!")) {
				rules.add(new Deny(ruleStr.substring(1)));
			}
			else {
				rules.add(new Allow(ruleStr));
			}
		}
		fgPackages.put(fPackage, rules);
	}

	static final private int fcYes = 0;	// we have allow
	static final private int fcNo = 1;	// we have deny
	static final private int fcUndecided = 2; // we have neither allow nor deny, so keep checking
	
	/** internal rule base class */
	abstract static class Rule {
		abstract int wantTrace(String group);
	}
	
	/** internal rule base class */
	class Wildcard extends Rule {
		int wantTrace(String group) {
			return fcYes;
		}
	}
	
	/** internal rule base class */
	class Allow extends Rule {
		Allow(String group) {
			fGroup = group;
		}
		int wantTrace(String group) {
			if (group.equalsIgnoreCase(fGroup)) {
				return fcYes;
			}
			return fcUndecided;
		}
		String fGroup;
	}
	
	/** internal rule base class */
	class Deny extends Rule {
		Deny(String group) {
			fGroup = group;
		}
		int wantTrace(String group) {
			if (group.equalsIgnoreCase(fGroup)) {
				return fcNo;
			}
			return fcUndecided;
		}
		String fGroup;
	}
	
	static void readConfig(InputStream is, String location) {
		REDXMLManager manager = new REDXMLManager();
		manager.registerHandler("Tracer", REDTracer.class);
		InputStreamReader reader = new InputStreamReader(is); 
		try {
			manager.parse(reader, location);
		}
		catch (Exception e) {
			REDTracer.error("red.util", "Tracer", "Exception while reading tracer config file '" + location + "': " + e);
		}
		try { reader.close(); } catch (IOException ioe) { }				
	}
	
	private static void readConfigs() {
		REDResourceInputStreamIterator iter = REDResourceManager.getInputStreams("config/tracer", ".xml");
		while (iter.hasNext()) {
			InputStream is = (InputStream) iter.next();
			readConfig(is, String.valueOf(iter.curName()));
		}
	}
	
	private String fPackage;	
	static int fgLevel;
	private static final HashMap fgPackages = new HashMap();	// map of package name to ArrayList of Rules
	private static final HashSet fgWarnedPacks = new HashSet();
	private static final boolean fgDefault = true;
	private static int fgWantLevel;	// auxiliary variable to prevent endless recursion

	static {	//  must be last static initializer
		readConfigs();
	}
}
