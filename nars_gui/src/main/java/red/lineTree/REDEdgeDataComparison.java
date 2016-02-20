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
 
package red.lineTree;

/** Comparison function for edge data.
  * @author rli@chello.at
  * @see REDNode
  * @tier system
  */
public interface REDEdgeDataComparison {
	/** compare (less or equal)
	  * @param d1 The data to compare 
	  * @param d2 The data to compare 
	  * @return true: if d1 < d2; false: otherwise
	  */
	boolean lt(REDEdgeData d1, REDEdgeData d2);

	/** compare (less or equal)
	  * @param d1 The data to compare 
	  * @param d2 The data to compare 
	  * @return true: if d1 <= d2; false: otherwise
	  */
	boolean leq(REDEdgeData d1, REDEdgeData d2);	
}
