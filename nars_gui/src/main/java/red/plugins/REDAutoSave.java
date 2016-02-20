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
 
package red.plugins;

import red.*;
import java.awt.event.*;
import javax.swing.*;

/** This plugin makes backup copies of modified texts periodically
  * @author rli@chello.at
  * @tier plugin
  */
public class REDAutoSave extends REDPlugin implements ActionListener {
	final public static String fcDefaultExtension = "auto";
	final public static int fcDefaultInterval = 2;	// minutes
	public REDAutoSave() {
		super();
	}
	
	public void actionPerformed(ActionEvent e) {
		if (fEditor.isModified()) {
			fEditor.saveEmergency(fExtension);
		}
	}

	
	public void setEditor(REDEditor editor) {
		super.setEditor(editor);
		if (editor != null) {
			fTimer.start();
		}
		else {
			fTimer.stop();
		}
	}
	
	/** set file extension for backup copy 
	  * @param extension a non-null, non-empty string to be appended to the file's name, without a leading dot 
	  * @pre extension != null && !extension.equals("")
	  */
	public void setExtension(String extension) {
		fExtension = extension;
	}
	
	/** get file extension for backup copy 
	  * @return a non-null, non-empty string to be appended to the file's name, without a leading dot 
	  */
	public String getExtension() {
		return fExtension;
	}
	
	/** set auto save interval 
	  * @param interval The auto save interval in minutes
	  */
	public void setInterval(int interval) {
		fInterval = interval;
		fTimer.setDelay(fInterval * 60 * 1000);
		fTimer.restart();
	}
	
	/** get auto save interval 
	  * @return interval The auto save interval in minutes
	  */
	public int getInterval() {
		return fInterval;
	}
	
	private int fInterval;	
	private String fExtension;
	private final Timer fTimer;
	{
		fExtension = fcDefaultExtension;
		fInterval = fcDefaultInterval;
		fTimer = new Timer(fInterval * 60 * 1000, this);
	}
}
