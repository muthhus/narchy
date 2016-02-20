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
import red.file.*;
import red.plugins.*;
import red.plugins.brMatcher.*;
import red.plugins.synHi.*;
import red.util.*;

/** interactive testbed for editor classes 
  * @author rli@chello.at
  * @tier test
  */
public class OTestEditor extends REDEventAdapter {
	public OTestEditor(String [] args) {
		String name = "";
		if (args.length > 0) {
			name = args[0];
		}
		else {
			File tf = new File("RTestREDView.1.in");
			if (tf.canRead()) {
				REDFile.copyFile(new REDFile("RTestREDView.1.in", true), new REDFile("TestText.txt"));
				name = "TestText.txt";
			}
		}
		fAutoIndent = null;
		fEditor = new REDEditor(name, false); fEditor.addREDEventListener(this);
		fProtector = new REDTextProtector();
		fEditor.addPlugin(fProtector);
		JComponent v = fEditor.getView();
		v.setBorder(BorderFactory.createMatteBorder(20, 50, 20, 50, Color.gray.darker()));
		fFrame = new JFrame("RED interactive testbed"); 
		fFrame.getContentPane().add(new JTextField("Some bla", 80), BorderLayout.NORTH);
		JScrollPane scroller = new JScrollPane(v);
		scroller.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
		fFrame.getContentPane().add(scroller, BorderLayout.CENTER);
		
		setupFindReplace();
        JMenuBar mb = new JMenuBar();
        mb.add(createFileActions());
        mb.add(createEditActions());
        mb.add(createCaretActions());
        mb.add(createBorderActions());        
        mb.add(createStyleActions());        
        mb.add(createViewModeActions());        
        mb.add(createPluginActions());        
        mb.add(createTabActions());        
        mb.add(createProtectActions());
		mb.add(createIntelliselectActions());
        fFrame.setJMenuBar(mb);

		fStyles = new REDStyle[5];
        fStyles[0] = new REDStyle(new Color(250, 100, 100), new Color(255, 255, 0), REDLining.SINGLEUNDER, "Monospaced", "ITALIC", 24, null);
        fStyles[1] = new REDStyle(new Color(100, 100, 250), new Color(255, 255, 255), REDLining.DOUBLEUNDER, "Helvetica", "PLAIN", 18, null);
        fStyles[2] = new REDStyle(new Color(90, 150, 30), new Color(255, 0, 255), REDLining.NONE, "Times", "BOLDITALIC", 14, null);
        fStyles[3] = new REDStyle(new Color(250, 250, 100), new Color(0, 0, 0), REDLining.DOUBLETHROUGH, "Monospaced", "BOLD", 12, null);
        fStyles[4] = new REDStyle(new Color(0, 100, 100), new Color(150, 50, 255), REDLining.SINGLETHROUGH, "Monospaced", "PLAIN", 10, null);
		fProtStyle = new REDStyle(new Color(150, 150, 150), new Color(255, 255, 255), REDLining.NONE, "Monospaced", "PLAIN", 12, null);

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
		OTestEditor ote = new OTestEditor(args);
		synchronized(ote) { ote.wait(); }
	}	
	
	public JMenu createFileActions() {
		JMenu menu = new JMenu("File");
		AbstractAction action;
		JMenuItem menuItem;

		action = new AbstractAction("Open...") {
			public void actionPerformed(ActionEvent e) {
				FileDialog fd = new FileDialog(fFrame, "Load File", FileDialog.LOAD);
				fd.show();
				long start = System.currentTimeMillis();
				fEditor.loadFile(fd.getDirectory() + fd.getFile(), false);
				long end = System.currentTimeMillis();
				REDTracer.info("red", "OTestEditor", "Loading file took: " + (end - start) + " msec.");
			}
		};
		menuItem = menu.add(action);

		action = new AbstractAction("Save") {
			public void actionPerformed(ActionEvent e) {
				fEditor.saveFile("bak");
			}
		};
		menuItem = menu.add(action);

		action = new AbstractAction("Save as...") {
			public void actionPerformed(ActionEvent e) {
				FileDialog fd = new FileDialog(fFrame, "Save File as", FileDialog.SAVE);
				fd.show();
				fEditor.saveFileAs(fd.getDirectory() + fd.getFile(), false);
			}
		};
		menuItem = menu.add(action);

		action = new AbstractAction("Revert") {
			public void actionPerformed(ActionEvent e) {
				fEditor.revert();
			}
		};
		menuItem = menu.add(action);
			
		menu.addSeparator();
		action = new AbstractAction("Quit") {
			public void actionPerformed(ActionEvent e) {
				fFrame.setVisible(false); System.exit(0);
			}
		};
		menuItem = menu.add(action);

		return menu;		
	}
		
	public JMenu createCaretActions() {
		JMenu menu = new JMenu("Caret");
		AbstractAction action;
		JMenuItem menuItem;

		action = new AbstractAction("Normal blinking") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setCaretBlink(400);
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Fast blinking") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setCaretBlink(100);
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Slow blinking") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setCaretBlink(1000);
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Stop blinking") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setCaretBlink(0);
			}
		};
		menuItem = menu.add(action);

		return menu;
	}

	public void addPluginMenuEntry(JMenu menu, String label, String className) {
		JCheckBoxMenuItem cbmi;

		cbmi = new JCheckBoxMenuItem(label);
		try {
			cbmi.addItemListener(new PluginHandler(Class.forName(className)));
		}
		catch (ClassNotFoundException cnfe) {
			throw new Error(cnfe.toString());
		}
		menu.add(cbmi);
	}
	
	public void addPluginMenuEntry(JMenu menu, String label, REDPlugin plugin) {
		JCheckBoxMenuItem cbmi;

		cbmi = new JCheckBoxMenuItem(label);
		cbmi.addItemListener(new PluginHandler(plugin));
		menu.add(cbmi);
	}

	
	public JMenu createPluginActions() {
		JMenu menu = new JMenu("Plugins");
		addPluginMenuEntry(menu, "Auto-Indentation", "red.plugins.REDAutoIndent");
		addPluginMenuEntry(menu, "Emergency Auto-Save", "red.plugins.REDAutoSave");
		REDSyntaxHighlighter highlighter = REDSyntaxHighlighterManager.createHighlighter("Java");
		addPluginMenuEntry(menu, "Syntax Highlighter", highlighter);
		REDBracketMatcher brMatcher = REDBracketMatcherManager.createMatcher("Java");
		addPluginMenuEntry(menu, "Bracket Matcher", brMatcher);
		
		return menu;
	}
	
	class PluginHandler implements ItemListener {
		public PluginHandler(Class cl) {
			try {
				fPlugin = (REDPlugin) cl.newInstance();
			}
			catch (Exception e) {
				throw new Error(String.valueOf(e));
			}
		}
		
		public PluginHandler(REDPlugin plugin) {
			fPlugin = plugin;
		}

		
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				fEditor.addPlugin(fPlugin);
			}
			else {
				fEditor.removePlugin(fPlugin);
			}
		}
		
		REDPlugin fPlugin;
	}

	public JMenu createBorderActions() {
		JMenu menu = new JMenu("Border");
		AbstractAction action;
		JMenuItem menuItem;

		action = new AbstractAction("Thick gray") {
			public void actionPerformed(ActionEvent e) {
				fEditor.getView().setBorder(BorderFactory.createMatteBorder(20, 50, 20, 50, Color.gray.darker()));				
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Nice") {
			public void actionPerformed(ActionEvent e) {
				fEditor.getView().setBorder(
					BorderFactory.createCompoundBorder(
						BorderFactory.createRaisedBevelBorder(), 
						BorderFactory.createLoweredBevelBorder()
					)
				);
			}
		};
		menuItem = menu.add(action);
		
		return menu;
	}
	
	public JMenu createViewModeActions() {
		JMenu menu = new JMenu("Mode");
		AbstractAction action;
		JMenuItem menuItem;

		action = new AbstractAction("Insert") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setViewMode(REDAuxiliary.VIEWMODE_INSERT);				
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Overwrite") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setViewMode(REDAuxiliary.VIEWMODE_OVERWRITE);				
			}
		};
		menuItem = menu.add(action);

		action = new AbstractAction("Readonly") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setViewMode(REDAuxiliary.VIEWMODE_READONLY);				
			}
		};
		menuItem = menu.add(action);
		
		menu.addSeparator();
		action = new AbstractAction("Toggle Whitespace vis") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setVisualizeWhitespace(!fEditor.getVisualizeWhitespace());				
			}
		};
		menuItem = menu.add(action);
				
		return menu;
	}
	
	public void applyStyle(int style) {
		REDStyle s;
		if (style >= 1 && style <= 5) {
			s = fStyles[style-1];
		}
		else {
			s = REDStyleManager.getDefaultStyle();
		}
		if (fEditor.hasSelection()) {
			fEditor.setStyle(fEditor.getSelectionStart(), fEditor.getSelectionEnd(), s);
		}
		else {
			fFrame.getToolkit().beep();
		}
	}
	
	public JMenu createStyleActions() {
		JMenu menu = new JMenu("Style");
		AbstractAction action;
		JMenuItem menuItem;

		action = new AbstractAction("Apply style 1 to selection") {
			public void actionPerformed(ActionEvent e) {
				applyStyle(1);
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Apply style 2 to selection") {
			public void actionPerformed(ActionEvent e) {
				applyStyle(2);
			}
		};
		
		menuItem = menu.add(action);
		action = new AbstractAction("Apply style 3 to selection") {
			public void actionPerformed(ActionEvent e) {
				applyStyle(3);
			}
		};
		
		menuItem = menu.add(action);
		action = new AbstractAction("Apply style 4 to selection") {
			public void actionPerformed(ActionEvent e) {
				applyStyle(4);
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Apply style 5 to selection") {
			public void actionPerformed(ActionEvent e) {
				applyStyle(5);
			}
		};
		menuItem = menu.add(action);

		action = new AbstractAction("Apply default style to selection") {
			public void actionPerformed(ActionEvent e) {
				applyStyle(0);
			}
		};
		menuItem = menu.add(action);
		
		return menu;
	}

	public JMenu createTabActions() {
		JMenu menu = new JMenu("Tab");
		AbstractAction action;
		JMenuItem menuItem;

		action = new AbstractAction("Tab width 2") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setTabWidth(2);
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Tab width 4") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setTabWidth(4);
			}
		};
		menuItem = menu.add(action);

		action = new AbstractAction("Tab width 8") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setTabWidth(8);
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Tab width 16") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setTabWidth(16);
			}
		};
		menuItem = menu.add(action);
		
		menu.addSeparator();
		
		action = new AbstractAction("Indent width 2") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setIndentWidth(2);
			}
		};
		menuItem = menu.add(action);

		action = new AbstractAction("Indent width 4") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setIndentWidth(4);
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Indent width 5") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setIndentWidth(5);
			}
		};
		menuItem = menu.add(action);


		action = new AbstractAction("Indent width 8") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setIndentWidth(8);
			}
		};
		menuItem = menu.add(action);

		action = new AbstractAction("Indent width 16") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setIndentWidth(16);
			}
		};
		menuItem = menu.add(action);
		
		menu.addSeparator();
		
		action = new AbstractAction("Indent as is") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setIndentMode(REDIndentMode.ASIS);
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Indent with spaces") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setIndentMode(REDIndentMode.SPC);
			}
		};
		menuItem = menu.add(action);
		
		action = new AbstractAction("Indent with tabs") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setIndentMode(REDIndentMode.TAB);
			}
		};
		menuItem = menu.add(action);
		
		return menu;
	}

	public JMenu createEditActions() {
		JMenu menu = new JMenu("Edit");
		AbstractAction actionUndo, actionRedo, actionCut, actionCopy, actionPaste, actionFindReplace;
		JMenuItem menuItem;

		actionUndo = new AbstractAction("Undo") {
			public void actionPerformed(ActionEvent e) {
				fEditor.undo();
			}
		};
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK);
		menuItem = menu.add(actionUndo);
		menuItem.setAccelerator(ks);
		
		actionRedo = new AbstractAction("Redo") {
			public void actionPerformed(ActionEvent e) {
				fEditor.redo();
			}
		};
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK);
		menuItem = menu.add(actionRedo);
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
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK);
		menuItem = menu.add(actionCut);
		menuItem.setAccelerator(ks);

		actionCopy = new AbstractAction("Copy") {
			public void actionPerformed(ActionEvent e) {
				if (!fEditor.clipboardCopy()) {
					fFrame.getToolkit().beep();
				}
			}
		};
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK);
		menuItem = menu.add(actionCopy);
		menuItem.setAccelerator(ks);

		actionPaste = new AbstractAction("Paste") {
			public void actionPerformed(ActionEvent e) {
				if (!fEditor.clipboardPaste()) {
					fFrame.getToolkit().beep();
				}
			}
		};
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK);
		menuItem = menu.add(actionPaste);
		menuItem.setAccelerator(ks);

		menu.addSeparator();
		
		actionFindReplace = new AbstractAction("Find...") {
			public void actionPerformed(ActionEvent e) {
				fFinder.setEditor(fEditor);
				fFindDialog.show();
			}
		};
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK);
		menuItem = menu.add(actionFindReplace);
		menuItem.setAccelerator(ks);

		return menu;
	}

	public JMenu createProtectActions() {
		JMenu menu = new JMenu("Protection");

		AbstractAction action = new AbstractAction("Protect selection") {
			public void actionPerformed(ActionEvent e) {
				fProtector.protect(fEditor.getSelectionStart(), fEditor.getSelectionEnd());
				fEditor.setStyle(fEditor.getSelectionStart(), fEditor.getSelectionEnd(), fProtStyle);
			}
		};
		menu.add(action);
		
		action = new AbstractAction("Protect line(s)") {
			public void actionPerformed(ActionEvent e) {
				int start = fEditor.getLineForPosition(fEditor.getSelectionStart());
				int end = fEditor.getLineForPosition(fEditor.getSelectionEnd());
				REDTracer.info("red", "OTestEditor", "Protecting lines: " + start + " to " + end);
				fProtector.protectLines(start, end);
				fEditor.setStyle(fEditor.getLineStart(start), fEditor.getLineEnd(end), fProtStyle);
			}
		};
		menu.add(action);
		
		action = new AbstractAction("Dump protected areas") {
			public void actionPerformed(ActionEvent e) {
				fProtector.dumpProtection();
			}
		};
		menu.add(action);
	
		
		return menu;
	}
	
	public JMenu createIntelliselectActions() {
		JMenu menu = new JMenu("IntelliSelect");

		AbstractAction action = new AbstractAction("Line 100, 30/30") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setSelection(fEditor.getLineStart(100), fEditor.getLineStart(101), 30, 30);
			}
		};
		menu.add(action);
		
		action = new AbstractAction("Line 100, 50/50") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setSelection(fEditor.getLineStart(100), fEditor.getLineStart(101), 50, 50);
			}
		};
		menu.add(action);

		menu.addSeparator();

		action = new AbstractAction("Highlight line 13") {
			public void actionPerformed(ActionEvent e) {
				fEditor.setHighlightLine(13);
			}
		};
		menu.add(action);
		
		
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
	
	private void setTitle() {
		fFrame.setTitle(fEditor.getFilename() + '(' + getEditorState() + ')');
	}
	
	public void afterCmdProcessorChange(int op) {
		setTitle();
	}
	
	public void afterModeChange(int oldMode, int newMode) {
		setTitle();
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


	
	REDEditor fEditor;
	JFrame fFrame;
	REDStyle fStyles[];
	REDAutoIndent fAutoIndent;
	REDTextProtector fProtector;
	REDStyle fProtStyle;
	REDFinder fFinder;
	REDFinderDialogFactory fFinderFactory;
	JDialog fFindDialog;
}
