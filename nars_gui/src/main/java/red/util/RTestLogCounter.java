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

/** A simple decorator for logs to count the number of messages.
  * @author rli@chello.at
  * @tier test
  */	
public class RTestLogCounter implements REDLog {
	public RTestLogCounter(REDLog superLog,boolean forward) {
		fSuper = superLog;
		fCount = 0;
		fForward=forward;
	}
	
	public void reset() {
		fCount = 0;
	}
	
	public void log(int sev, String group, String message) {
		fCount++;
		if(fForward)
			fSuper.log(sev, group, message);
	}
	
	public int getCount() {
		return fCount;
	}
	
	public REDLog fSuper;
	public int fCount;
	protected boolean fForward;
}

