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

/** plugin base class
  * @author rli@chello.at
  * @tier API
  */
public class REDPlugin extends REDEventAdapter {
	public REDPlugin() {
		fEditor = null;
	}
	
	/** Attach plugin to editor.
	  * Note: do not call this method directly. Use REDEditor.addPlugin(...) instead
	  */
	public void setEditor(REDEditor editor) {
		if (fEditor != null) {
			fEditor.removeREDEventListener(this);
		}
		fEditor = editor;
		if (fEditor != null) {
			fEditor.addREDEventListener(this);
		}
	}
	
	/** Get the editor, the plugin is attached to.
	  * @return the editor the plugin is currently attached to. may be null, if plugin is not attached to any editor at the moment
	  */
	public REDEditor getEditor() {
		return fEditor;
	}
	
	/** Instead of using getEditor() subclasses may access this variable directly for a small performance gain */
	protected REDEditor fEditor;
}

