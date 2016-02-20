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

/** XML persistence interface for reading.
  * @author rli@chello.at
  * @tier system
  */
public interface REDXMLReadable {
	/** Set REDXMLHandlerReader mappings.
	  * This method tells the passed handler how to map tags to method calls, by using the map-methods of REDXMLHandlerReader.
	  * @param handler The handler to set the mappings for.
	  */
	void setMappings(REDXMLHandlerReader handler) throws REDXMLCallbackError;
	
	/** Report inner production.
	  * This method is used to report the production of some "inner" object. 
	  * @param obj The inner object produced
	  * @param inner The inner REDXMLHandlerReader which produced the object
	  * @param outer The outer REDXMLHandlerReader, i.e. the current handler
	  */
	void innerProduction(REDXMLReadable obj, REDXMLHandlerReader inner, REDXMLHandlerReader outer);
}
