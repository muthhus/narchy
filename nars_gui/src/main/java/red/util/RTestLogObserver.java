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

import red.*;
import junit.framework.*;

/** Auxiliary  base class for test cases who need to observe the global log.
  * @author rli@chello.at
  * @tier test
  */
public class RTestLogObserver extends TestCase {
	public RTestLogObserver(String name) {
		super(name);
	}

	public void observeLog(boolean doObserve) {
		observeLog(doObserve,false);
	}

	public void observeLog(boolean doObserve,boolean forward) {
		if (doObserve) {
			fLog = new RTestLogCounter(REDGLog.fgLog,forward);
			REDGLog.fgLog = fLog;
		}
		else if (fLog != null) {
			REDGLog.fgLog = fLog.fSuper;
		}
	}

	protected int getLogCount () {
		REDAssert.ensure(REDGLog.fgLog == fLog);
		return fLog.getCount();
	}

	protected void resetLog() {
		REDAssert.ensure(REDGLog.fgLog == fLog);
		fLog.reset();
	}
	
	protected RTestLogCounter fLog;
}
