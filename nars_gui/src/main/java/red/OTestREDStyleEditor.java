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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/** interactive testbed for editor classes 
  * @author rli@chello.at
  * @tier test
  */
public class OTestREDStyleEditor implements REDStyleEditorSelectionListener {
	public OTestREDStyleEditor(String [] args) {
		fStyleEditor = new REDStyleEditor(); 
		fFrame = new JFrame("RED Style editor"); 
		setupStatusBar();
		setupSaveButton();
		
		JPanel left = new JPanel(new BorderLayout(5, 5));
		left.add(fStyleEditor.getThemeChooser(), BorderLayout.NORTH);
		left.add(fStyleEditor.getHierarchyGUI(), BorderLayout.CENTER);		

		JPanel right = new JPanel(new BorderLayout(5, 5));
		right.add(fStyleEditor.getStyleEditGUI(), BorderLayout.NORTH);
		right.add(fStyleEditor.getSampleEditor().getView(), BorderLayout.CENTER);
		right.add(fSaveButton, BorderLayout.SOUTH);
		
		
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
		
		fFrame.getContentPane().add(pane, BorderLayout.CENTER);
		fFrame.getContentPane().add(fStatusBar, BorderLayout.SOUTH);
		pane.setDividerLocation(250);
		fStyleEditor.getHierarchyTree().setSelectionRow(0);
		
//        JMenuBar mb = new JMenuBar();
//        mb.add(createFileActions());

		//Finish setting up the fFrame, and show it.
		fFrame.addWindowListener(new MyWindowAdapter());
		fFrame.setSize(800, 600);
//		fFrame.setPreferredSize(new Dimension(600, 400));
		fFrame.setVisible(true);		
	}
	
	private void setupStatusBar() {
		fStatusBar = new JLabel(" ");
		fStyleEditor.addStyleEditorSelectionListener(this);
	}		

	private void setupSaveButton() {	
		fSaveButton = new JButton("Save ...");
		fSaveButton.addActionListener(e -> {
            try {
                fStyleEditor.saveChangedStyles(new FileOutputStream("OTestREDStyleEditor.1.xml"));
            }
            catch (Exception exception) {
                throw new Error(String.valueOf(exception));
            }
        });
	}
	
	
	// REDStyleEditorSelectionListener impl start
	public void styleSelected(REDStyle style) {
		if (style == null) {
			fStatusBar.setText("");
		}
		else {
			fStatusBar.setText(style.getDescription());
		}
	}
	// REDStyleEditorSelectionListener impl end
	
	
	public static void main(String[] args) throws InterruptedException {
		OTestREDStyleEditor ote = new OTestREDStyleEditor(args);
		synchronized(ote) { ote.wait(); }
	}	
		
	REDStyleEditor fStyleEditor;
	JFrame fFrame;
	JLabel fStatusBar;
	JButton fSaveButton;

	private static class MyWindowAdapter extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
                System.exit(0);
        }
	}
}
