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
import java.lang.reflect.*;
import java.util.*;
import red.util.*;

/** Callback descriptor class. Instances of this class are used to call back registered methods.
  * @author rli@chello.at
  * @tier system
  */
class REDXMLCallbackDescriptor {
	/** Create callback descriptor. 
	  * @param invocation The method to call back. This string has the following format: 
	  * <PRE>
	  * MethodInvocation = MethodName "(" ParameterList ")".
	  * MethodName = Name.
	  * ParameterList = Parameter { "," Parameter }.
	  * Parameter = [TypeCast] (Value | Variable).
	  * TypeCast = "(" TypeName ")".	// If you don't specify a type cast, String is assumed.
	  * TypeName = Name.		// TypeName is either a primitive datatype (int, float, a class from the java.lang package or a fully qualified class name)
	  * Value = "'" {<char>} "'".	// To have a single quote within a value, write two single quotes.
	  * Variable = # ["!" | "&" | "$" | AttributeName] [ConstantMapping] [DefaultValue].
	  * AttributeName = Name.	// Attributes can only be passed in start tags. They are empty in end tags.
	  * DefaultValue = "=" Value.	// If the variable is empty or cannot be converted into the given type, this value is tried. 
	  * ConstantMapping = "[" FieldPrefix "]".	// Map to public static field denoted by FieldPrefix plus the variable/attribute content.
	  * FieldPrefix = {<char>}.
	  * Name = <char> {<char>}.
	  * </PRE>
	  * Here's a list of meanings of possible variables:
	  * <UL>
	  * <LI># stands for the trimmed cdata since the last event (start or end tag).
	  * <LI>#! stands for the unmodified cdata since the last event (start or end tag).
	  * <LI>#$ stands for the REDXMLManager object the reading is directed with.
	  * <LI>#& stands for the REDXMLHandlerReader object currently used.
	  * <LI>#<attributeName> stands for the content of the named attribute. This only works in start tags.
	  * </UL>
	  * If the type of a variable is (int) it is possible to pass a list of pipe-separated values, which are added together. This is useful for flags.
	  */
	REDXMLCallbackDescriptor(String invocation, Object receiver, REDXMLHandlerReader handler, REDXMLManager manager) throws REDXMLCallbackError {
		fObj = receiver;
		fHandler = handler;
		fManager = manager;
		fParameters = new ArrayList();
		invocation = invocation.trim();
		int idx = invocation.indexOf('(');
		if (idx != -1) {
			String methodName = invocation.substring(0, idx).trim();
			String argList = invocation.substring(idx+1, invocation.length()-1).trim();
			ParameterListParser parser = new ParameterListParser();
			parser.parse(argList);		
			fMethod = getMethod(methodName);
			if (fMethod == null) {
				throw new REDXMLCallbackError("No such method: " + fObj.getClass().getName() + '.' + methodName + '(' + getParmListString() + ')');
			}
		}
	}

	/** Get default value for classes. */	
	private static String getDefaultValue(Class type) {
		if (fgTypeMap.containsKey(type)) {
			return "0";
		}
		else if (type == char.class) {
			return "\0";
		}
		return null;
	}
	
	/** Parameter list parser. See the documentation on REDXMLCallbackDescriptor constructor for details about the 
	  * grammar of parameter lists. 
	  */
	class ParameterListParser {
		void parse(String argList) throws REDXMLCallbackError {
			String value, defaultValue, variable, constantMapping; 
			Class type;
			fArgList = argList; fIdx = 0; fChar = ' '; 
			
			read(false);
			while (!eos()) {
				type = readType();
				variable = readVariable();
				constantMapping = readConstantMapping();
				defaultValue = readDefaultValue();
				value = readValue();
				if (fChar != ',' && fChar != '\0') throw new REDXMLCallbackError("Syntax Error. ',' or end of parameter list expected at " + fIdx + " in '" + fArgList);
				read(false);
				if (variable != null) {
					if (variable.equals("&")) {
						fParameters.add(new FixedParameter(fHandler));
					}
					else if (variable.equals("$")) {
						fParameters.add(new FixedParameter(fManager));
					}
					else if (variable.equals("!")) {
						fParameters.add(new ContentParameter(false, type, defaultValue, constantMapping));
					}
					else if (variable.isEmpty()) {
						fParameters.add(new ContentParameter(true, type, defaultValue, constantMapping));
					}			
					else {
						fParameters.add(new AttributeParameter(variable, type, defaultValue, constantMapping));
					}
				}
				else {
					if (value == null) {
						throw new REDXMLCallbackError("Must have at least value or variable.");
					}
					if (type == null) {
						type = String.class;
					}
					Object o = createObject(value, type, getDefaultValue(type), null);
					fParameters.add(new FixedParameter(o, type));
				}
			}
		}
		
		boolean eos() {
			return fChar == '\0';
		}
		
		char peek() {
			if (fIdx < fArgList.length()) {
				return fArgList.charAt(fIdx);
			}
			else {
				return '\0';
			}
		}
		
		void read(boolean withWhitespace) {
			if (fIdx < fArgList.length()) {
				fChar = fArgList.charAt(fIdx++);
				while (!withWhitespace && fIdx < fArgList.length() && Character.isWhitespace(fChar)) {
					fChar = fArgList.charAt(fIdx++);
				}
			}
			else {
				fIdx = fArgList.length() + 1;
				fChar = '\0';
			}
		}
		
		private Class getClass(String classSpec) throws REDXMLCallbackError {
			Class cl = (Class) fgTypeMap.get(classSpec);
			if (cl == null) {
				try {
					cl = Class.forName(classSpec);
				}
				catch (ClassNotFoundException cnfe) {
					try {
						cl = Class.forName("java.lang." + classSpec);
					}
					catch (ClassNotFoundException cnfe2) {
						throw new REDXMLCallbackError("Cannot find class: " + classSpec);
					}
				}
			}
			return cl;
		}

		Class readType() throws REDXMLCallbackError {
			Class retClass = null;
			if (fChar == '(') {
				int oldIdx = fIdx;
				read(false);
				while (!eos() && fChar != ')') {
					read(false);
				}
				retClass = getClass(fArgList.substring(oldIdx, fIdx - 1).trim());
				read(false);
			}
			return retClass;
		}
		
		String readVariable() {
			String retVar = null;
			if (fChar == '#') {
				int oldIdx = fIdx;
				read(false);
				while (!eos() && (Character.isLetter(fChar) || "!$&".indexOf(fChar) != -1)) {
					read(false);
				}
				retVar = fArgList.substring(oldIdx, fIdx - 1).trim();
			}
			return retVar;
		}
		
		String readDefaultValue() {
			if (fChar != '=') {
				return null;
			}
			else {
				read(false);
				return readValue();
			}
		}
		
		String readConstantMapping() {
			String retVal = null;
			if (fChar == '[') {
				StringBuilder buf = new StringBuilder();
				read(true);
				while (!eos() && fChar != ']') {
					buf.append(fChar);
					read(true);
				}
				retVal = String.valueOf(buf);
				read(false);
			}
			return retVal;
		}
		
		
		String readValue() {
			String retVal = null;
			if (fChar == '\'') {
				StringBuilder buf = new StringBuilder();
				read(true);
				while (!eos() && (fChar != '\'' || peek() == '\'')) {
					buf.append(fChar);
					if (fChar == '\'') {
						read(true);
					}
					read(true);
				}
				retVal = String.valueOf(buf);
				while (!eos() && fChar != ',') {
					read(false);
				}
			}
			return retVal;
		}
		
		String fArgList;
		int fIdx;
		char fChar;
	}
	
	private static Class mapType(Class type) {
		Class newType = (Class) fgTypeMap.get(type);
		if (newType != null) {
			return newType;
		}
		else {
			return type;
		}
	}
	
	private void checkBoolean(Class type, String from) {
		if (type == Boolean.class && !(from.equalsIgnoreCase("true") || from.equalsIgnoreCase("false") || from.equals("0"))) {	// explicetely disallow anything but 'true' and 'false' and '0' (which is the built-in default for boolean)
			REDGLog.warning("XML", "While " + fHandler.getErrorContext() + ": constructing boolean value from '" + from + "' will result in 'false'.");
		}
	}
	
	private Object findObject(String constMapping, String name) throws REDXMLCallbackError {
		Object retVal = null;
		Class cl = null;
		
		int idx = constMapping.lastIndexOf('.');
		if (idx == -1) {
			cl = fObj.getClass();
		}
		else {
			try {
				cl = Class.forName(constMapping.substring(0, idx));
			}
			catch (ClassNotFoundException cnfe) {
				throw new REDXMLCallbackError("Could not find class '" + constMapping.substring(0, idx) + "' for constant mapping '" + constMapping + "'.");
			}
			constMapping = constMapping.substring(idx+1);
		}
		
		String [] namePerms = new String[4];
		namePerms[0] = constMapping + name;
		namePerms[1] = constMapping + name.substring(0, 1).toUpperCase() + name.substring(1);
		namePerms[2] = constMapping + name.toUpperCase();
		namePerms[3] = constMapping + name.toLowerCase();
		
		Field field = null;
		int x = 0;
		while (field == null && x < namePerms.length) {
			try {
				field = cl.getField(namePerms[x]);
			}
			catch (NoSuchFieldException | SecurityException nsfe) { } finally {
				x++;
			}
		}
		
		if (field == null) {
			StringBuilder tried = new StringBuilder();
			for (x = 1; x < namePerms.length; x++) {
				if (tried.length() > 0) {
					tried.append(", ");
				}
				tried.append('\'').append(namePerms[x]).append('\'');
			}
			throw new REDXMLCallbackError("Could not find/access constant field '" + namePerms[0] + "' in class '" + cl +"'. Even tried "  + tried);
		}
		else {
			try {
				retVal = field.get(fObj);
			}
			catch (Exception e) {
				throw new REDXMLCallbackError("Could not get value from constant field '" + namePerms[0] + "': " + e);
			}
		}
		return retVal;		
	}
	
	/** Create an object from a final static field. */
	private Object createFromConstant(Class type, String constMapping, String name) throws REDXMLCallbackError {
		Object retVal = null;
		if (name.contains("|") && type == Integer.class) {
			int x = 0;
			StringTokenizer tok = new StringTokenizer(name, "|");
			while (tok.hasMoreTokens()) {
				x += (Integer) findObject(constMapping, tok.nextToken().trim());
			}
			retVal = x;
		}
		else {
			retVal = findObject(constMapping, name);
		}
		return retVal;
	}
	
	/** Create an object from String representation.
	  * @param from The string represenation to create object from.
	  * @param type The class of the object to create. This must either be a primitive datatype class (e.g. int.class, char.class, etc.) 
	  * or a public constructor accepting one string parameter must exist.
	  * @param defaultValue The defaultValue to use in case the object cannot be built from <Code>from</Code>.
	  */
	private Object createObject(String from, Class type, String defaultValue, String constMapping) throws REDXMLCallbackError {
		if (from == null || from.isEmpty()) {
			if (defaultValue != null) {
				from = defaultValue;
			}
			else {
				from = "";
			}
		}

		type = mapType(type);
		
		if (constMapping != null) {
			return createFromConstant(type, constMapping, from);
		}
		
		if (type == String.class) {
			return from;
		}
		else if (type == char.class) {
			if (from.length() > 0) {
				return from.charAt(0);
			}
			else {
				return '\0';
			}
		}
		
		checkBoolean(type, from);
		Class [] argType = { String.class };
		String [] args = { from };
		Constructor cons = null;
		try {
			cons = type.getConstructor(argType);
		}
		catch (NoSuchMethodException nsme) {
			throw new REDXMLCallbackError("Could not find a constructor with one String parameter for " + type + " while " + fHandler.getErrorContext());
		}
		
		try {
			return cons.newInstance(args);
		}
		catch (IllegalAccessException iae) {
			throw new REDXMLCallbackError("Can't access constructor with one String parameter for " + type+ " while " + fHandler.getErrorContext());
		}
		catch (InvocationTargetException ite) {
			if (defaultValue != null) {
				REDGLog.warning("XML", "Could not construct " + type + " from '" + from + "' " + " while " + fHandler.getErrorContext() + ". Trying default value '" + defaultValue + '\'');
				args[0] = defaultValue;
				try {
					checkBoolean(type, from);
					return cons.newInstance(args);
				}
				catch (Exception e) {
					throw new REDXMLCallbackError("Could not construct " + type + " from neither '" + from + "' nor default value '" + defaultValue + "' while " + fHandler.getErrorContext());
				}
			}
			else {
				throw new REDXMLCallbackError("Could not construct " + type + " from '" + from + "' and no default value specified." + " while " + fHandler.getErrorContext());
			}
		}
		catch (InstantiationException ie) {
			throw new REDXMLCallbackError("Instantiation exception while " + fHandler.getErrorContext() + ": " + ie);
		}
	}

	/** Base class for parameter entries. */	
	abstract static class Parameter {
		abstract Object getActualParameter(StringBuffer content, Attributes atts) throws REDXMLCallbackError ;
		abstract Class getParameterClass();
	}
	
	/** Base class for parameters with a type and default value. */	
	abstract class TypedParameter extends Parameter {
		TypedParameter(Class type, String defaultValue, String constantMapping) {
			fType = type;
			if (fType == null) {
				fType = String.class;
			}
			fDefaultValue = defaultValue;
			fConstantMapping = constantMapping;
		}
		
		abstract Object getActualParameter(StringBuffer content, Attributes atts) throws REDXMLCallbackError;
		Class getParameterClass() {
			return fType;
		}
		
		Class fType;
		String fDefaultValue;
		String fConstantMapping;
	}
		
	/** Parameter type for content variables (# and #!) */
	class ContentParameter extends TypedParameter {
		ContentParameter(boolean trimmed, Class type, String defaultValue, String constantMapping) {
			super(type, defaultValue, constantMapping);
			fTrimmed = trimmed;
		}

		Object getActualParameter(StringBuffer content, Attributes atts) throws REDXMLCallbackError {
			String from = String.valueOf(content);
			if (fTrimmed) {
				from = from.trim();
			}
			return createObject(from, fType, fDefaultValue, fConstantMapping);
		}
		
		boolean fTrimmed;
	}
	
	/** Parameter type for attribute variables (#attrName) */
	class AttributeParameter extends TypedParameter {
		AttributeParameter(String attributeName, Class type, String defaultValue, String constantMapping) {
			super(type, defaultValue, constantMapping);
			fAttributeName = attributeName;
		}

		Object getActualParameter(StringBuffer content, Attributes atts) throws REDXMLCallbackError {
			String value;
			if (atts != null) {
				value = atts.getValue(fAttributeName);
			}
			else {
				value = null;
			}
			return createObject(value, fType, fDefaultValue, fConstantMapping);
		}
		
		String fAttributeName;
	}
	
	
	/** Parameter type for handler, manager and fixed values. */
	class FixedParameter extends Parameter {
		FixedParameter(Object obj) {
			fAct = obj;
			fType = fAct.getClass();
		}
		
		FixedParameter(Object obj, Class type) {
			fAct = obj;
			fType = type;
		}

		Object getActualParameter(StringBuffer content, Attributes atts) throws REDXMLCallbackError {
			return fAct;
		}

		Class getParameterClass() {
			return fType;
		}
		
		Object fAct;
		Class fType;
	}		
	
	/** Create string representation of parameter list. */
	private String getParmListString() {
		StringBuilder buf = new StringBuilder();
		Iterator iter = fParameters.iterator();
		while (iter.hasNext()) {
			if (buf.length() > 0) {
				buf.append(", ");
			}
			buf.append( ((Parameter) iter.next()).getParameterClass());				
		}
		return String.valueOf(buf);
	}
	
	/** Get method object for descriptor. */	
	private Method getMethod(String methodName) throws REDXMLCallbackError {
		Class [] argTypes = new Class[fParameters.size()];
		int idx = 0;
		Iterator iter = fParameters.iterator();
		while (iter.hasNext()) {
			argTypes[idx++] = ( (Parameter) iter.next()).getParameterClass();
		}

		try {
			return fObj.getClass().getMethod(methodName, argTypes);
		}
		catch (Exception e) {
			throw new REDXMLCallbackError("Can't get method " + fObj.getClass().getName() + '.' + methodName + '(' + getParmListString() + "): " + e);
		}
	}

	/** Make callback. 
	  * This method will try to actually call the method of the descriptor.
	  */
	void invoke(StringBuffer content, Attributes atts) throws SAXException {
		try {
			Object [] actParm = null;
			int x = 0;
			if (fParameters.size() > 0) {
				actParm = new Object[fParameters.size()];
				Iterator iter = fParameters.iterator();
				while (iter.hasNext()) {
					actParm[x++] = ((Parameter) iter.next()).getActualParameter(content, atts);
					REDTracer.info("red.xml", "REDXMLCallbackDescriptor", "Setting actParm[" + x + "] to a " + actParm[x-1].getClass());
				}
			}		
			fMethod.invoke(fObj, actParm);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new SAXException("Unable to call '" + fMethod + "': " + e);
		}
	}
		
	Method fMethod;
	ArrayList fParameters;
	Object fObj;
	REDXMLHandlerReader fHandler;
	REDXMLManager fManager;
	static HashMap fgTypeMap = new HashMap();	// map primitive datatype classes to object classes and strings to primitive datatypes
	static {
		fgTypeMap.put(short.class, Short.class);
		fgTypeMap.put(int.class, Integer.class);
		fgTypeMap.put(long.class, Long.class);
		fgTypeMap.put(float.class, Float.class);
		fgTypeMap.put(double.class, Double.class);
		fgTypeMap.put(boolean.class, Boolean.class);
		fgTypeMap.put(byte.class, Byte.class);
		fgTypeMap.put("int", int.class);
		fgTypeMap.put("boolean", boolean.class);
		fgTypeMap.put("float", float.class);
		fgTypeMap.put("short", short.class);
		fgTypeMap.put("long", long.class);
		fgTypeMap.put("double", double.class);
		fgTypeMap.put("char", char.class);
		fgTypeMap.put("byte", byte.class);
	}				
}
