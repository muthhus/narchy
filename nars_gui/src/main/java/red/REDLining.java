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

import red.util.*;
import java.awt.*;

/** Enumeration of linings for text. Also contains the painting logic.
  * @author rli@chello.at
  * @tier system
  */
public class REDLining {
	/** No lining. */
	final public static REDLining NONE = new REDLining("none");

	/** Single underline. */
	final public static REDLining SINGLEUNDER = new REDLining("singleunder") {
		void paint(Graphics g, int x, int y, int lineHeight, int width) {
			g.fillRect(x, y + lineHeight - 2, x + width, 1);
		}
	};

	/** Double underline. */
	final public static REDLining DOUBLEUNDER = new REDLining("doubleunder") {
		void paint(Graphics g, int x, int y, int lineHeight, int width) {
			g.fillRect(x, y + lineHeight - 3, x + width, 1);
			g.fillRect(x, y + lineHeight - 1, x + width, 1);
		}
	};

	/** Single line through (aka strikeout). */
	final public static REDLining SINGLETHROUGH = new REDLining("singlethrough") {
		void paint(Graphics g, int x, int y, int lineHeight, int width) {
			g.fillRect(x, y + lineHeight / 2, x + width, 1);
		}
	};

	/** Double line through (aka strikeout). */
	final public static REDLining DOUBLETHROUGH = new REDLining("doublethrough") {
		void paint(Graphics g, int x, int y, int lineHeight, int width) {
			g.fillRect(x, y + lineHeight / 2 - 1, x + width, 1);
			g.fillRect(x, y + lineHeight / 2 + 1, x + width, 1);
		}
	};

	/** Get lining object from string representation.
	  * @param stringRepresentation The case-insensitive name of one of the final public static REDLining objects (NONE, SINGLEUNDER, etc.).
	  * @return A REDLining object, or null, if the given representation can not be mapped to a REDLining object.
	  */
	public static REDLining get(String stringRepresentation) {
		try {
			return (REDLining) REDLining.class.getField(stringRepresentation.toUpperCase()).get(null);
		}
		catch (Exception e) {
			REDGLog.error("RED", "Attempt to get REDLining for '" + stringRepresentation + "' failed.");
			return null;
		}
	}

	private REDLining(String stringRepresentation) { 
		fString = stringRepresentation;
	}
	
	public String toString() {
		return fString;
	}
	
	void paint(Graphics g, int x, int y, int lineHeight,  int width) {
	}
	
	String fString;
	public static final REDLining[] fgLinings = { NONE, SINGLEUNDER, DOUBLEUNDER, SINGLETHROUGH, DOUBLETHROUGH };
}
