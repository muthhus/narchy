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

/** controller for read-only mode. Will prevent all input from changing the text
  * @author rli@chello.at
  * @tier system
  */
class REDViewReadonlyController extends REDViewControllerDecorator {
	REDViewReadonlyController(REDViewController decorated) {
		super(decorated);
	}
	public void keyBackspace(REDView v, KeyEvent e) { key(v, e); }
	public void keyDelete(REDView v, KeyEvent e) { key(v, e); }
	public void keyInsert(REDView v, KeyEvent e) { key(v, e); }
	public void keyNewline(REDView v, KeyEvent e) { key(v, e); }
	public void keyEsc(REDView v, KeyEvent e) { key(v, e); }
	public void keyTab(REDView v, KeyEvent e) { key(v, e); }
	public void key(REDView v, KeyEvent e) {
		REDAuxiliary.beep();
	}	
}
