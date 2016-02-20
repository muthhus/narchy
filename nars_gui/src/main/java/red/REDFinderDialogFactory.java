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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import red.rexParser.*;

/** Factory for GUI elements  in a find/replace dialog for RED.
  * You can use this class in two ways:
  * <UL>
  * <LI> The get*Panel - methods return complete building blocks for your find/replace GUI. You can customize their internal layout by setting a Layout - object.
  * <LI> The other get - methods allow you to access each radio button, check box, etc., allowing complete control over the layout.
  * </UL>
  * There are also some convenience methods, which allow to easily access the state of radio buttons and check boxes.
  * @author rli@chello.at
  * @tier API
  */
public class REDFinderDialogFactory  {
	
	/** Create factory.
	  * This method may only be called by a REDFinder - object.
	  */
	REDFinderDialogFactory(REDFinder finder) {
		fFinder = finder;
		fFindAction = new FindAction();
		setupCombos();
		setupDirection();
		setupReplaceAll();
		setupGeneralOptions();
		setupButtons();
		setupLog();
		setupTriggers();
	}
	
	private void setupCombos() {
		// setup gridbag - stuff
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints comboConstraints = new GridBagConstraints();
		comboConstraints.weightx = 1.0;
		comboConstraints.fill = GridBagConstraints.HORIZONTAL;
		comboConstraints.gridwidth = GridBagConstraints.REMAINDER;
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.anchor = GridBagConstraints.EAST;

		fComboPanel = new JPanel(gridbag);
		
		// Find
		JLabel findLabel = new JLabel("Find: ");
		gridbag.setConstraints(findLabel, labelConstraints);
		fComboPanel.add(findLabel); 
		fFindCombo = new JComboBox(fFinder.getFindHistory()); fFindCombo.setEditable(true);
		gridbag.setConstraints(fFindCombo, comboConstraints);		
		fComboPanel.add(fFindCombo);
		
		// Replace
		JLabel replaceLabel = new JLabel("Replace: ");
		gridbag.setConstraints(replaceLabel, labelConstraints);
		fComboPanel.add(replaceLabel); 
		fReplaceCombo = new JComboBox(fFinder.getReplaceHistory()); fReplaceCombo.setEditable(true);
		gridbag.setConstraints(fReplaceCombo, comboConstraints);		
		fComboPanel.add(fReplaceCombo);
	}		
	
	private void setupDirection() {
		ButtonGroup bg = new ButtonGroup();
		fDirForward = new JRadioButton("Forward"); fDirForward.setSelected(true); bg.add(fDirForward);
		fDirBackward = new JRadioButton("Backward"); bg.add(fDirBackward);
		fDirectionPanel = new JPanel(new GridLayout(2, 1));
		fDirectionPanel.add(fDirForward);
		fDirectionPanel.add(fDirBackward);
		fDirectionPanel.setBorder(BorderFactory.createTitledBorder("Direction"));
	}
	
	private void setupReplaceAll() {
		ButtonGroup bg = new ButtonGroup();
		fReplaceAllFile = new JRadioButton("File"); fReplaceAllFile.setSelected(true); bg.add(fReplaceAllFile);
		fReplaceAllSelection = new JRadioButton("Selection"); bg.add(fReplaceAllSelection);
		fReplaceAllOptionsPanel = new JPanel(new GridLayout(2, 1));
		fReplaceAllOptionsPanel.add(fReplaceAllFile);
		fReplaceAllOptionsPanel.add(fReplaceAllSelection);
		fReplaceAllOptionsPanel.setBorder(BorderFactory.createTitledBorder("Replace All"));
	}
	
	private void setupGeneralOptions() {
		fMatchCase = new JCheckBox("Match Case"); fMatchCase.setSelected(true);
		fWholeWord = new JCheckBox("Match Whole Word"); 
		fUseRegExp = new JCheckBox("Regular Expressions"); fUseRegExp.setSelected(true);
		fGeneralOptionsPanel = new JPanel(new GridLayout(3, 1));
		fGeneralOptionsPanel.add(fMatchCase);
		fGeneralOptionsPanel.add(fWholeWord);
		fGeneralOptionsPanel.add(fUseRegExp);
		fGeneralOptionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));		
	}
	
	private void setupButtons() {
		fButtonPanel = new JPanel();
		fFindButton = new JButton("Find");
		fFindButton.addActionListener(new FindButtonAction());
		fButtonPanel.add(fFindButton);
		
		fReplaceButton = new JButton("Replace");
		fReplaceButton.addActionListener(new ReplaceButtonAction(false));
		fButtonPanel.add(fReplaceButton);
		
		fReplaceAndFindButton = new JButton("Replace & Find");
		fReplaceAndFindButton.addActionListener(new ReplaceButtonAction(true));
		fButtonPanel.add(fReplaceAndFindButton);
		
		fReplaceAllButton = new JButton("Replace All");
		fReplaceAllButton.addActionListener(new ReplaceAllButtonAction());
		fButtonPanel.add(fReplaceAllButton);
	}		
	
	private void setupLog() {
		fLogLine = new REDFinderLabelLog();
		setLog(fLogLine);
		fLogPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		fLogPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		fLogPanel.add(fLogLine);
	}
	
	private void setupTriggers() {
		fFindCombo.getEditor().addActionListener(new ButtonTrigger(fFindButton, fFindCombo));
		fReplaceCombo.getEditor().addActionListener(new ButtonTrigger(fFindButton, fReplaceCombo));
	}		
	
	static class ButtonTrigger implements ActionListener {
		ButtonTrigger(JButton button, JComboBox combo) {
			fButton = button;
			fCombo = combo;
		}
		
		public void actionPerformed(ActionEvent e) {
			fCombo.hidePopup();
			fButton.doClick();
		}
		
		JButton fButton;
		JComboBox fCombo;
	}

	// --- Find & Replace combos
	/** Get complete find & replace combo boxes GUI building block.
	  * @return A panel containing the combo boxes together with labels..
	  */
	public JPanel getComboPanel() {
		return fComboPanel;
	}
	
	/** Get find combo box */
	public JComboBox getFindCombo() {
		return fFindCombo;
	}
	
	/** Get replace combo box */
	public JComboBox getReplaceCombo() {
		return fReplaceCombo;
	}
	
	// --- Direction stuff	
	/** Get complete direction selection GUI building block.
	  * @return A panel containing all of the direction radio buttons.
	  */
	public JPanel getDirectionPanel() {
		return fDirectionPanel;
	}
	
	/** Get forward direction radio button */
	public JRadioButton getDirectionForwardButton() {
		return fDirForward;
	}
	
	/** Get backward direction radio button */
	public JRadioButton getDirectionBackwardButton() {
		return fDirBackward;
	}

	/** Get selection direction as indicated by the radio buttons in getDirectionSelector() */
	public REDFinderDirection getDirection() {
		if (fDirBackward.isSelected()) {
			return REDFinderDirection.BACKWARD;
		}
		return REDFinderDirection.FORWARD;
	}

	// --- Replace all stuff
	/** Get complete "replace all options" GUI building block.
	  */
	public JPanel getReplaceAllOptionsPanel() {
		return fReplaceAllOptionsPanel;
	}
	
	/** Get "Replace All -> In Selection" radio button. */
	public JRadioButton getReplaceAllInSelection() {
		return fReplaceAllSelection;
	}
	
	/** Get "Replace All -> In File" radio button. */
	public JRadioButton getReplaceAllInFile() {
		return fReplaceAllFile;
	}

	/** Get status of replace all options.
	  * This is a convenience method for getReplaceAllInSelection().isSelected
	  */
	public boolean isReplaceAllInSelection() {
		return fReplaceAllSelection.isSelected();
	}
	
	// --- general options stuff
	/** Get complete general options GUI building block. */
	public JPanel getGeneralOptionsPanel() {
		return fGeneralOptionsPanel;
	}
	
	/** Get "match case" checkbox. */
	public JCheckBox getMatchCaseCheckBox() {
		return fMatchCase;
	}
	
	/** Get "whole word" checkbox. */
	public JCheckBox getWholeWordCheckBox() {
		return fWholeWord;
	}
	
	/** Get "use regular expressions" checkbox. */
	public JCheckBox getUseRegExpCheckBox() {
		return fUseRegExp;
	}
	
	
	/** Get status of "match case" check box. */
	public boolean isMatchCase() {
		return fMatchCase.isSelected();
	}

	/** Get status of "whole word" check box. */
	public boolean isMatchWord() {
		return fWholeWord.isSelected();
	}

	/** Get status of "use regular expressions" check box. */
	public boolean isUseRegExp() {
		return fUseRegExp.isSelected();
	}
	
	// --- Button stuff
	/** Get complete buttons GUI building block */
	public JPanel getButtonPanel() {
		return fButtonPanel;
	}
	
	/** Get find button. */
	public JButton getFindButton() {
		return fFindButton;
	}

	/** Get replace button. */
	public JButton getReplaceButton() {
		return fReplaceButton;
	}

	/** Get replace and find button. */
	public JButton getReplaceAndFindButton() {
		return fReplaceAndFindButton;
	}

	/** Get replace all button. */
	public JButton getReplaceAllButton() {
		return fReplaceAllButton;
	}
	
	// --- Log stuff
	/** Get complete log GUI building block */
	public JPanel getLogPanel() {
		return fLogPanel;
	}
	
	/** Get label based log line.
	  * This method returns the label - based log managed by REDFinderDialogFactory.
	  * It is defaultedly installed as log 
	  */
	public REDFinderLabelLog getLogLine() {
		return fLogLine;
	}
	
	/** Set log.
	  * Various result and error messages are printed on the log. 
	  * By default, the log is set to getLogLine()
	  * @param The new log to be used. If set to null, info messages will no longer be displayed and a beep will sounded upon error messages.
	  */
	public void setLog(REDFinderLog log) {
		fLog = log;
	}
	
	private static String firstLine(String str) {
		int x = Math.max(str.indexOf('\n'), str.indexOf('\r'));
		if (x == -1) {
			return str;
		}
		else {
			return str.substring(0, x);
		}
	}
		
	private static String quote(String str) {
		StringBuilder buf = new StringBuilder();
		for (int x = 0; x < str.length(); x++) {
			switch (str.charAt(x)) {
				case '\n': buf.append("\\n"); break;
				case '\r': buf.append("\\r"); break;
				case '\t': buf.append("\\t"); break;
				case '\\': case '.': case '*': case '+': case '?': case '[': case ']': case '^': case '$': 
					buf.append('\\');
					// fallthrough
				default: buf.append(str.charAt(x)); break;
			}
		}
		return String.valueOf(buf);
	}
	
	/** Set find string.
	  * This method will <UL>
	  * <LI>Quote \r, \n, \t and other characters if regular expressions are on</LI>
	  * <LI>Use the first line of the passed string</LI>
	  * </UL>
	  * @param findString The string to set in the find combobox.
	  */
	public void setFindString(String findString) {
		String findContent;
		if (isUseRegExp()) {
			findContent = quote(findString);
		}
		else {
			findContent = firstLine(findString);
		}
		fFinder.updateHistory(fFinder.getFindHistory(), findContent);
		getFindCombo().getEditor().selectAll();
	}
	
	/** Set replace string.
	  * This method will set the replace string. In contrast to <Code>setFindString</Code> it will not quote anything.
	  * @param replaceString The string to set in the replace combobox.
	  */
	public void setReplaceString(String replaceString) {
		fFinder.updateHistory(fFinder.getReplaceHistory(), replaceString);
		getReplaceCombo().getEditor().selectAll();
	}

	// --- Not implemented
	private REDFinderDialogFactory() {
	}
	
	// --- auxiliary
	private void find(String pattern, int from) throws REDRexMalformedPatternException {
		fFindAction.resetCounter();
		fFinder.find(pattern, 
			from, 
			getDirection(),
			isMatchCase(), 
			isMatchWord(),
			isUseRegExp(),
			true,
			false,
			fFindAction
		);
	}
	
	private int getFindFrom(boolean inverse) {
		if (getDirection() == REDFinderDirection.FORWARD ^ inverse) {
			return fFinder.getEditor().getSelectionEnd(); 
		}
		else {
			return fFinder.getEditor().getSelectionStart();
		}
	}

	private void log(int severity, String message) {
		if (fLog != null) {
			fLog.log(severity, message);
		}
		else if (severity == REDFinderLog.SEV_ERROR) {
			REDAuxiliary.beep();
		}
	}
	
	private void handleREDRexMalformedPatternException(REDRexMalformedPatternException mpe) {
		log(REDFinderLog.SEV_ERROR, "Error in regular expression. " + mpe.getMessage());
	}	
	
	private String getFindPattern() {
		Object o = fFinder.getREDFinderDialogFactory().getFindCombo().getEditor().getItem();
		if (o != null) {
			return String.valueOf(o);
		}
		return "";
	}

	private String getReplacePattern() {
		Object o = fFinder.getREDFinderDialogFactory().getReplaceCombo().getEditor().getItem();
		if (o != null) {
			return String.valueOf(o);
		}
		return "";
	}
	
	/** Repeat last find without gui */		
	public void findAgain() {
		try {
			find(getFindPattern(), getFindFrom(false));
		}
		catch (REDRexMalformedPatternException mpe) {
		}		
		if (fFindAction.getCounter() == 0) {
			REDAuxiliary.beep();
		}		
	}
	
	class FindButtonAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			try {
				find(getFindPattern(), getFindFrom(false));
				if (fFindAction.getCounter() == 0) {
					log(REDFinderLog.SEV_INFO, "No match.");
				}
				else {
					log(REDFinderLog.SEV_INFO, " ");
				}
			}
			catch (REDRexMalformedPatternException mpe) {
				handleREDRexMalformedPatternException(mpe);
			}
		}
	}
	
	class ReplaceButtonAction extends AbstractAction {
		ReplaceButtonAction(boolean findNext) {
			fFindNext = findNext;
		}
		
		public void actionPerformed(ActionEvent e) {
			String findPattern = getFindPattern();
			String replacePattern = getReplacePattern();
			fFinder.setEditor(fFinder.getEditor());
			
			try {
				REDEditor editor = fFinder.getEditor();
				int oldFrom = editor.getSelectionStart();
				int oldTo = editor.getSelectionEnd();

				log(REDFinderLog.SEV_INFO, " ");
				find(findPattern, getFindFrom(true));
				if (fFindAction.getCounter() == 0) {
					log(REDFinderLog.SEV_INFO, "No match.");
				}
				else {
					if (editor.getSelectionStart() == oldFrom && editor.getSelectionEnd() == oldTo) {
						fFinder.replace(findPattern, replacePattern, 
							getFindFrom(true), 
							getDirection(),
							isMatchCase(), 
							isMatchWord(),
							isUseRegExp(),
							true,
							REDFinderReplaceAllDirective.OFF);
						if (fFindNext) {
							find(findPattern, getFindFrom(false)); 
							if (fFindAction.getCounter() == 0) {
								log(REDFinderLog.SEV_INFO, "No more matches.");
							}
						}
					}
				}
			}
			catch (REDRexMalformedPatternException mpe) {
				handleREDRexMalformedPatternException(mpe);
			}			
		}		
		boolean fFindNext;
	}

	class ReplaceAllButtonAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			String findPattern = getFindPattern();
			String replacePattern = getReplacePattern();
			log(REDFinderLog.SEV_INFO, " ");
			try {
				REDFinderReplaceAllDirective rDir = REDFinderReplaceAllDirective.FILE;
				int start = 0;
				if (isReplaceAllInSelection()) {
					rDir = REDFinderReplaceAllDirective.SELECTION;
					start = fFinder.getEditor().getSelectionStart();
				}
				int nrReplacements = fFinder.replace(findPattern, replacePattern, 
					start, 
					REDFinderDirection.FORWARD,
					isMatchCase(), 
					isMatchWord(),
					isUseRegExp(),
					true,
					rDir);
				if (nrReplacements == 0) {
					log(REDFinderLog.SEV_INFO, "No match.");
				}
				else if (nrReplacements == 1) {
					log(REDFinderLog.SEV_INFO, "1 occurence replaced.");
				}
				else {
					log(REDFinderLog.SEV_INFO, nrReplacements + " occurences replaced.");
				}
			}
			catch (REDRexMalformedPatternException mpe) {
				handleREDRexMalformedPatternException(mpe);
			}			
		}		
		boolean fFindNext;
	}
	
	static class FindAction extends REDFinderAction {
		public void match(REDEditor editor, int from, int to) {
			fCounter++;
			editor.setSelection(from, to, 30, 30);
		}
		
		public void resetCounter() {
			fCounter = 0;
		}
		
		public int getCounter() {
			return fCounter;
		}
		
		private int fCounter;
	}

	private REDFinder fFinder;	
	private JPanel fComboPanel, fDirectionPanel, fReplaceAllOptionsPanel, fGeneralOptionsPanel, fButtonPanel, fLogPanel;
	private JComboBox fFindCombo, fReplaceCombo;
	private JRadioButton fDirForward, fDirBackward;
	private JRadioButton fReplaceAllFile, fReplaceAllSelection;
	private JCheckBox fMatchCase, fWholeWord, fUseRegExp;
	private JButton fFindButton, fReplaceButton, fReplaceAndFindButton, fReplaceAllButton;
	private REDFinderLabelLog fLogLine;
	private REDFinderLog fLog;
	private FindAction fFindAction;
}

