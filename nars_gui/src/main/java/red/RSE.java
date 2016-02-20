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
import javax.swing.*;
import red.*;
import red.plugins.*;
import red.plugins.brMatcher.*;
import red.plugins.synHi.*;
import red.util.*;

/** Standalone editor.
  * @author rli@chello.at
  * @tier application
  */
public class RSE extends REDEventAdapter {
	public RSE(String [] args) {
		String name = "";
		if (args.length > 0) {
			name = args[0];
		}
		fEditor = new REDEditor(name, false); fEditor.addREDEventListener(this);
		fEditor.addPlugin(new REDAutoIndent());
		JComponent v = fEditor.getView();
		v.setBorder(
			BorderFactory.createCompoundBorder(
				BorderFactory.createRaisedBevelBorder(), 
				BorderFactory.createMatteBorder(2, 2, 2, 2, Color.white)
			)
		);
		fFrame = new JFrame("RED Standalone editor"); 
		JScrollPane scroller = new JScrollPane(v);
		fFrame.getContentPane().add(scroller, BorderLayout.CENTER);
		
		setupFindReplace();
        JMenuBar mb = new JMenuBar();
        mb.add(createFileActions());
        mb.add(createEditActions());
        fFrame.setJMenuBar(mb);
		
		//Finish setting up the fFrame, and show it.
		fFrame.addWindowListener(new MyWindowAdapter());
		fFrame.setSize(800, 600);
//		fFrame.setPreferredSize(new Dimension(600, 400));
		fFrame.setVisible(true);		
		v.requestFocus();
	}
	
	public void setupFindReplace() {
		GridBagLayout gridbag = new GridBagLayout();
		fFinder = REDFinder.getInstance();
		fFinderFactory = fFinder.getREDFinderDialogFactory();
		fFindDialog = new JDialog(fFrame, "Find/Replace");
		fFindDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Container cp = fFindDialog.getContentPane();
		cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
		
		JPanel topPanel = new JPanel(); topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));

		JPanel leftSide = new JPanel(); leftSide.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.Y_AXIS));
		
		// Find & Replace combos
		JPanel comboPanel = fFinderFactory.getComboPanel();
		comboPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));		
		leftSide.add(comboPanel);
		
		// Options
		JPanel optionsPanel = new JPanel(gridbag);
		GridBagConstraints optionConstraints = new GridBagConstraints();
		optionConstraints.anchor = GridBagConstraints.NORTH;
		
		gridbag.setConstraints(fFinderFactory.getDirectionPanel(), optionConstraints);
		optionsPanel.add(fFinderFactory.getDirectionPanel()); 
		
		gridbag.setConstraints(fFinderFactory.getReplaceAllOptionsPanel(), optionConstraints);
		optionsPanel.add(fFinderFactory.getReplaceAllOptionsPanel()); 
		
		gridbag.setConstraints(fFinderFactory.getGeneralOptionsPanel(), optionConstraints);
		optionsPanel.add(fFinderFactory.getGeneralOptionsPanel()); 

		leftSide.add(optionsPanel);
		
		// Buttons
		JPanel buttonPanel = fFinderFactory.getButtonPanel();
		buttonPanel.setLayout(gridbag);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		GridBagConstraints buttonConstraints = new GridBagConstraints();
		buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
		buttonConstraints.gridwidth = GridBagConstraints.REMAINDER;
		buttonConstraints.weighty = 1.0;
		
		JButton findButton = fFinderFactory.getFindButton();
		gridbag.setConstraints(findButton, buttonConstraints);
		
		JButton replaceButton = fFinderFactory.getReplaceButton();
		gridbag.setConstraints(replaceButton, buttonConstraints);
		
		JButton replaceAndFindButton = fFinderFactory.getReplaceAndFindButton();
		gridbag.setConstraints(replaceAndFindButton, buttonConstraints);
		
		JButton replaceAllButton = fFinderFactory.getReplaceAllButton();
		gridbag.setConstraints(replaceAllButton, buttonConstraints);
		
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new CloseButtonAction());
		gridbag.setConstraints(closeButton, buttonConstraints);
		buttonPanel.add(closeButton);

		topPanel.add(leftSide);
		topPanel.add(buttonPanel);
		
		JPanel bottomPanel = fFinderFactory.getLogPanel();
		bottomPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createEmptyBorder(0, 5, 5, 5),
			bottomPanel.getBorder()));

		cp.add(topPanel);
		cp.add(bottomPanel);
		
		fFindDialog.pack();
//		fFindDialog.setResizable(false);
	}

	private static class MyWindowAdapter extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
                System.exit(0);
        }
	}

	class CloseButtonAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			fFindDialog.dispose();
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		RSE ote = new RSE(args);
		synchronized(ote) { ote.wait(); }
	}	
	
	public JMenu createFileActions() {
		JMenu menu = new JMenu("File"); menu.setMnemonic('F');
		AbstractAction action;
		JMenuItem menuItem;
		KeyStroke ks = null; 
		
		action = new AbstractAction("New") {
			public void actionPerformed(ActionEvent e) {
				if (checkModified()) {
					fEditor.loadFile("", false);
				}
			}
		};
		menuItem = menu.add(action); menuItem.setMnemonic('N');
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK);
		menuItem.setAccelerator(ks);		

		action = new AbstractAction("Open...") {
			public void actionPerformed(ActionEvent e) {
				int retVal = fFileChooser.showOpenDialog(fFrame);
				if (retVal == JFileChooser.APPROVE_OPTION) {
					if (checkModified()) {
						long start = System.currentTimeMillis();
						fEditor.loadFile(fFileChooser.getSelectedFile().getAbsolutePath(), false);
						long end = System.currentTimeMillis();
						REDTracer.info("red", "RSE", "Loading file took: " + (end - start) + " msec.");
					}
				}
			}
		};
		menuItem = menu.add(action); menuItem.setMnemonic('O');
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK);
		menuItem.setAccelerator(ks);		

		action = new AbstractAction("Save") {
			public void actionPerformed(ActionEvent e) {
				doSave();
			}
		};
		menuItem = menu.add(action); menuItem.setMnemonic('S');
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK);
		menuItem.setAccelerator(ks);

		action = new AbstractAction("Save as...") {
			public void actionPerformed(ActionEvent e) {
				doSaveAs();
			}
		};
		menuItem = menu.add(action); menuItem.setMnemonic('a');
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK);
		menuItem.setAccelerator(ks);

		action = new AbstractAction("Revert") {
			public void actionPerformed(ActionEvent e) {
				fEditor.revert();
			}
		};
		menuItem = menu.add(action); menuItem.setMnemonic('R');
			
		menu.addSeparator();
		action = new AbstractAction("Quit") {
			public void actionPerformed(ActionEvent e) {
				if (checkModified()) {
					fFrame.setVisible(false); System.exit(0);
				}
			}
		};
		menuItem = menu.add(action); menuItem.setMnemonic('Q');
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK);
		menuItem.setAccelerator(ks);

		return menu;		
	}
		
	public JMenu createEditActions() {
		JMenu menu = new JMenu("Edit"); menu.setMnemonic('E');
		AbstractAction actionUndo, actionRedo, actionCut, actionCopy, actionPaste, actionFindReplace;
		JMenuItem menuItem;

		actionUndo = new AbstractAction("Undo") {
			public void actionPerformed(ActionEvent e) {
				fEditor.undo();
			}
		};
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK);
		menuItem = menu.add(actionUndo); menuItem.setMnemonic('U');
		menuItem.setAccelerator(ks);
		
		actionRedo = new AbstractAction("Redo") {
			public void actionPerformed(ActionEvent e) {
				fEditor.redo();
			}
		};
		menuItem = menu.add(actionRedo); menuItem.setMnemonic('R');
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK);
		menuItem.setAccelerator(ks);
		
		actionUndo.setEnabled(fEditor.canUndo());
		actionRedo.setEnabled(fEditor.canRedo());
	
		fEditor.addREDEventListener(new UndoRedoUpdater(fEditor, actionUndo, actionRedo));
		
		menu.add(new JSeparator());
		
		actionCut = new AbstractAction("Cut") {
			public void actionPerformed(ActionEvent e) {
				if (!fEditor.clipboardCut()) {
					fFrame.getToolkit().beep();
				}
			}
		};
		menuItem = menu.add(actionCut); menuItem.setMnemonic('C');
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK);
		menuItem.setAccelerator(ks);

		actionCopy = new AbstractAction("Copy") {
			public void actionPerformed(ActionEvent e) {
				if (!fEditor.clipboardCopy()) {
					fFrame.getToolkit().beep();
				}
			}
		};
		menuItem = menu.add(actionCopy); menuItem.setMnemonic('o');
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK);
		menuItem.setAccelerator(ks);

		actionPaste = new AbstractAction("Paste") {
			public void actionPerformed(ActionEvent e) {
				if (!fEditor.clipboardPaste()) {
					fFrame.getToolkit().beep();
				}
			}
		};
		menuItem = menu.add(actionPaste); menuItem.setMnemonic('P');
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK);
		menuItem.setAccelerator(ks);

		menu.addSeparator();
		
		actionFindReplace = new AbstractAction("Find...") {
			public void actionPerformed(ActionEvent e) {
				fFinder.setEditor(fEditor);
				fFindDialog.show();
			}
		};
		menuItem = menu.add(actionFindReplace); menuItem.setMnemonic('F');
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK);
		menuItem.setAccelerator(ks);

		return menu;
	}
	
	private String getEditorState() {
		String state;
		if (fEditor.isModified()) {
			state = "modified";
		}
		else if (fEditor.getViewMode() == REDAuxiliary.VIEWMODE_READONLY) {
			state = "read-only";
		}
		else {
			state = "writable";
		}
		return state;
	}
	
	private void updateTitle() {
		fFrame.setTitle(getDisplayName() + " (" + getEditorState() + ')');
	}
	
	public void afterCmdProcessorChange(int op) {
		updateTitle();
	}
	
	public void afterModeChange(int oldMode, int newMode) {
		updateTitle();
	}
	
	public void afterFileLoad(String filename) { 
		updateLanguage();
		updateTitle();
	}
	
	public void afterFileSave(String filename) {
		updateLanguage();
		updateTitle();
	}

	
	static class UndoRedoUpdater extends REDEventAdapter {
		public UndoRedoUpdater(REDEditor editor, Action actionUndo, Action actionRedo) {
			fEditor = editor;
			fUndo = actionUndo;
			fRedo = actionRedo;
		}
		
		public void afterCmdProcessorChange(int operation) {
			fUndo.setEnabled(fEditor.canUndo());
			fRedo.setEnabled(fEditor.canRedo());
		}
		REDEditor fEditor;
		Action fUndo, fRedo;
	}
	
	private String determineLanguage() {
		if (fEditor.getFilename().endsWith(".C") || fEditor.getFilename().endsWith(".c") || fEditor.getFilename().endsWith(".cc")
			|| fEditor.getFilename().endsWith(".h")) {
			return "C++";
		}
		else if (fEditor.getFilename().endsWith(".java")) {
			return "Java";
		}
		return "";
	}
	
	private void updateLanguage() {
		String language = determineLanguage();

		// Syntax highlighter
		if (fSynHi != null) {
			fEditor.removePlugin(fSynHi);
			fSynHi = null;
		}
		fSynHi = REDSyntaxHighlighterManager.createHighlighter(language);
		if (fSynHi != null) {
			fEditor.addPlugin(fSynHi);
		}
		
		// Bracket matcher
		if (fBracketMatcher != null) {
			fEditor.removePlugin(fBracketMatcher);
			fBracketMatcher = null;
		}
		fBracketMatcher = REDBracketMatcherManager.createMatcher(language);
		if (fBracketMatcher != null) {
			fEditor.addPlugin(fBracketMatcher);
		}
	}
	
	private String getDisplayName() {
		String retVal = fEditor.getFilename();
		if (retVal.isEmpty()) {
			retVal = "Unnamed file";
		}
		return retVal;
	}
	
	private boolean checkModified() {
		if (fEditor.isModified()) {
            String [] options = { "Yes", "No", "Cancel" } ;
			int retVal = JOptionPane.showOptionDialog(null,
				"<HTML><Strong>" + getDisplayName() + "</Strong> is modified. Do you want to save changes?" , 
				"File modified",
				JOptionPane.YES_NO_CANCEL_OPTION,  
				JOptionPane.QUESTION_MESSAGE,
					null,
				options,
					null);
			if (retVal == JOptionPane.YES_OPTION) {
				return doSave();
			}
			return retVal != JOptionPane.CANCEL_OPTION && retVal != JOptionPane.CLOSED_OPTION;
		}
		return true;
	}
	
	private boolean doSave() {
		if (fEditor.getFilename() != "") {
			long start = System.currentTimeMillis();
			boolean retVal = fEditor.saveFile("bak");
			long end = System.currentTimeMillis();
			REDTracer.info("red", "RSE", "Save took: " + (end - start) + " msec.");
			return retVal;
		}
		else {
			return doSaveAs();
		}
	}
	
	private boolean doSaveAs() {
		if (fFileChooser.showSaveDialog(fFrame) == JFileChooser.APPROVE_OPTION) {
			long start = System.currentTimeMillis();
			boolean retVal = fEditor.saveFileAs(fFileChooser.getSelectedFile().getAbsolutePath(), false);
			long end = System.currentTimeMillis();
			REDTracer.info("red", "RSE", "Save as took: " + (end - start) + " msec.");
			return retVal;
		}
		return false;
	}

	
	REDEditor fEditor;
	JFrame fFrame;
	REDFinder fFinder;
	REDFinderDialogFactory fFinderFactory;
	JDialog fFindDialog;
	REDSyntaxHighlighter fSynHi;
	REDBracketMatcher fBracketMatcher;
	JFileChooser fFileChooser = new JFileChooser();
}
