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

import java.awt.event.*;

/** decorator base class - this class provides a decorator pattern for REDViewController
  * if you want to add/change functionality of a controller, subclass from this decorator class and overwrite the methods you're interested in
  * @author rli@chello.at
  * @tier API
  */
public class REDViewControllerDecorator extends REDViewController {
	/** 
	  * @pre v != null
	  */
	public REDViewControllerDecorator(REDViewController decorated) {
		super();
		fDecorated = decorated;
	}
	
	public void crsLeft(REDView v, KeyEvent e) { fDecorated.crsLeft(v, e); }
	public void crsRight(REDView v, KeyEvent e) { fDecorated.crsRight(v, e); }
	public void crsUp(REDView v, KeyEvent e) { fDecorated.crsUp(v, e); }
	public void crsDown(REDView v, KeyEvent e) { fDecorated.crsDown(v, e); }
	public void crsPageUp(REDView v, KeyEvent e) { fDecorated.crsPageUp(v, e); }
	public void crsPageDown(REDView v, KeyEvent e) { fDecorated.crsPageDown(v, e); }
	public void crsHome(REDView v, KeyEvent e) { fDecorated.crsHome(v, e); }
	public void crsEnd(REDView v, KeyEvent e) { fDecorated.crsEnd(v, e); }
	public void keyBackspace(REDView v, KeyEvent e) { fDecorated.keyBackspace(v, e); }
	public void keyDelete(REDView v, KeyEvent e) { fDecorated.keyDelete(v, e); }
	public void keyInsert(REDView v, KeyEvent e) { fDecorated.keyInsert(v, e); }
	public void keyNewline(REDView v, KeyEvent e) { fDecorated.keyNewline(v, e); }
	public void keyEsc(REDView v, KeyEvent e) { fDecorated.keyEsc(v, e); }
	public void keyTab(REDView v, KeyEvent e) { fDecorated.keyTab(v, e); }
	public void key(REDView v, KeyEvent e) { fDecorated.key(v, e); }
	public void mouseLeft(REDView v, MouseEvent e) { fDecorated.mouseLeft(v, e); }
	public void mouseMiddle(REDView v, MouseEvent e) { fDecorated.mouseMiddle(v, e); }
	public void mouseRight(REDView v, MouseEvent e) { fDecorated.mouseRight(v, e); }
	public void setLinebreak(String lb) { fDecorated.setLinebreak(lb); }
	public String getLinebreak() { return fDecorated.getLinebreak(); }

	public void setDecorated(REDViewController decorated) {
		fDecorated = decorated;
	}
	
	public REDViewController getDecorated() {
		return fDecorated;
	}
	
	REDViewController fDecorated;
}
