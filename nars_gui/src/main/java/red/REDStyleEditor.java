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

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import red.xml.*;

/** REDStyle editor is a GUI for editing styles, themes and the style hierarchy
  * @author rli@chello.at
  * @tier API
  * @see REDStyleManager
  * @see REDStyle
  */
public class REDStyleEditor implements TreeSelectionListener, REDStyleEditorSelectionListener  {
	public REDStyleEditor() {
		fStyleManager = REDStyleManager.getInstance().deepCopy();
		setupThemeChooser();
		setupSampleEditor();
		setupHierarchyGUI();
		setupStyleEditGUI();
		fListeners = new HashSet();
		addStyleEditorSelectionListener(this);
		fInUpdate = false;
	}
	
	static class Node extends DefaultMutableTreeNode {
		Node(REDStyle s, String name) {
			super(s);
			fName = name;
		}
		
		public String toString() {
			return fName;
		}
		String fName;
	}
	
	/** Add selection listener.
	  * @param listener The listener to add to this editor.
	  * @return <Code>true</Code> if successful.
	  */
	public boolean addStyleEditorSelectionListener(REDStyleEditorSelectionListener listener) {
		return fListeners.add(listener);
	}
	
	/** Remove selection listener.
	  * @param listener The listener to remove from this editor.
	  * @return <Code>true</Code> if successful.
	  */
	public boolean removeStyleEditorSelectionListener(REDStyleEditorSelectionListener listener) {
		return fListeners.remove(listener);
	}

	/** Get hierarchy GUI. */
	public JComponent getHierarchyGUI() {
		return fHierarchy;
	}

	/** Get tree of hierarchy GUI. */	
	public JTree getHierarchyTree() {
		return fTree;
	}
	
	/** Get root node of hierarchy tree. */
	public TreeNode getHierarchyTreeRoot() {
		return fRoot;
	}
	
	private void setupSampleEditor() {
		fSampleEditor = new REDEditor();
		fSampleEditor.replace("The quick brown fox jumps over the lazy dog.\nABCDEFGHJIKLMNOPQRSTUVWXYZ\nabcdefghijklmnopqrstuvwxyz\n1234567890@\\\"&%/()[]{}=?", 0, 0, null);
		fStyleManager.doAddStyleEventListener((REDView) fSampleEditor.getView());
		fSampleEditor.setViewMode(REDAuxiliary.VIEWMODE_READONLY);
	}
	
	private void setupThemeChooser() {
		fThemes = new JComboBox();
		TreeSet set = new TreeSet();
		Iterator outer = fStyleManager.doIterator();
		while (outer.hasNext()) {
			REDStyle s = (REDStyle) outer.next();
			Iterator inner = s.themeIterator();
			while (inner.hasNext()) {
				String theme = (String) inner.next();
				set.add(theme);
			}
		}
		Iterator iter = set.iterator();
		while (iter.hasNext()) {
			fThemes.addItem(iter.next());
		}
		fThemes.setSelectedItem("Default");
		fThemes.addActionListener(e -> {
            fStyleManager.doSetTheme(String.valueOf(fThemes.getSelectedItem()));
            updateHierarchyGUI();
            updateStyleEditGUI(getSelectedStyle());
        });
	}
	
	private void updateHierarchyGUI() {
		Enumeration e = fRoot.breadthFirstEnumeration();
		while (e.hasMoreElements()) {
			Node node = (Node) e.nextElement();
			Node parent = (Node) node.getParent();
			if (parent != null) {
				REDStyle superStyle = ((REDStyle) node.getUserObject()).getSuperStyle();
				if (parent.getUserObject() != superStyle) {
					Node newParent = (Node) fStyleToNodeMap.get(superStyle);
					parent.remove(node);
					newParent.add(node);
				}
			}
		}
	}
	
	private void setupHierarchyGUI() {
		fStyleToNodeMap = new HashMap();
		TreeMap map = new TreeMap();
		Iterator iter = fStyleManager.doIterator();
		while (iter.hasNext()) {
			REDStyle s = (REDStyle) iter.next();
			Node n = new Node(s, s.getDisplayName());
			map.put(s.getDisplayName(), n);
			fStyleToNodeMap.put(s, n);
		}
		
		iter = map.values().iterator();
		while (iter.hasNext()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) iter.next();
			REDStyle style = (REDStyle) node.getUserObject();
			REDStyle superStyle = style.getSuperStyle();
			if (superStyle != null) {
				DefaultMutableTreeNode parent = (DefaultMutableTreeNode) map.get(superStyle.getDisplayName());
				parent.add(node);
			}
		}
		
		fRoot = (DefaultMutableTreeNode) map.get("Default");
		DefaultTreeModel model = new DefaultTreeModel(fRoot);
		fTree = new JTree(model);
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setOpenIcon(null);	
		renderer.setClosedIcon(null);	
		renderer.setLeafIcon(null);
		fTree.setCellRenderer(renderer);
		fTree.addTreeSelectionListener(this);
		fTree.putClientProperty("JTree.lineStyle", "Angled");
		fTree.setShowsRootHandles(true);
		fTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		fHierarchy = new JScrollPane(fTree);
	}
	
	/** Get style edit GUI. The style edit GUI contains widgets for manipulating the currently selected style. */
	public JComponent getStyleEditGUI() {
		return fStyleEditGUI;
	}

	public boolean showStyle(String styleName) {
		if (!fStyleManager.doHasStyle(styleName)) return false;
		REDStyle s = fStyleManager.doGetStyle(styleName);
		Node n = (Node) fStyleToNodeMap.get(s);
		if (n == null) return false;
		DefaultTreeModel model = (DefaultTreeModel) fTree.getModel();
		TreePath path = new TreePath(model.getPathToRoot(n));
		fTree.expandPath(path);
		return true;
	}	
	
	public boolean selectStyle(String styleName) {
		if (!fStyleManager.doHasStyle(styleName)) return false;
		REDStyle s = fStyleManager.doGetStyle(styleName);
		Node n = (Node) fStyleToNodeMap.get(s);
		if (n == null) return false;
		DefaultTreeModel model = (DefaultTreeModel) fTree.getModel();
		TreePath path = new TreePath(model.getPathToRoot(n));
		fTree.setSelectionPath(path);
		return true;		
	}

	abstract class ColorButton extends JButton {
		ColorButton() {
			super(" ");
		}
		
		public void changeColor(Color bg) {
			REDStyle s = getSelectedStyle();
			if (s != null) {
				setBackground(bg);
				colorChanged(s, bg);
				updateStyleEditGUI(s);
			}
		}
			
		abstract void colorChanged(REDStyle s, Color c);
	}
	
	static class ButtonColorizer implements ActionListener {
		ButtonColorizer(ColorButton button) {
			fButton =  button;
		}
		
		public void actionPerformed(ActionEvent e) {
			Color col = JColorChooser.showDialog(fButton, "Choose color", fButton.getBackground());
			if (col != null) {
				fButton.changeColor(col);
			}
		}		
		ColorButton fButton;
	}
	
	abstract class StyleEditListener implements ActionListener {
		StyleEditListener() {
		}
		
		abstract void stateChanged(REDStyle s);
		
		public void actionPerformed(ActionEvent e) {
			REDStyle s = getSelectedStyle();
			if (s != null && !fInUpdate) {
				stateChanged(s);
				s.installTheme(getTheme());
				updateStyleEditGUI(s);
			}
		}		
	}
	
	private void addStyleEditGUI(GridBagLayout gridbag, GridBagConstraints constraints, JComponent comp, boolean border) {
		gridbag.setConstraints(comp, constraints);
		if (border) {
			comp.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLoweredBevelBorder(), 
				BorderFactory.createEmptyBorder(2, 2, 2, 2)));
		}
		fStyleEditGUI.add(comp);
	}
	
	/** Auxiliary method for setupStyleEditGUI(). */
	private void setupStyleEditGUIWidgets() {
		setupStyleEditGUIWidgetsFontFace();
		setupStyleEditGUIWidgetsFontSize();
		setupStyleEditGUIWidgetsFontStyle();
		setupStyleEditGUIWidgetsLining();
		setupStyleEditGUIWidgetsForeground();
		setupStyleEditGUIWidgetsBackground();
		setupStyleEditGUIWidgetsDisplayName();
		setupStyleEditGUIWidgetsDescription();
	}
	
	private void setupStyleEditGUIWidgetsFontFace() {
		fEditFontFace = new JComboBox(); 
		fEditFontFace.addActionListener(new StyleEditListener() {
			void stateChanged(REDStyle s) {
				if (! ("".equals(fEditFontFace.getSelectedItem()) || s.getFontFace().equals(String.valueOf(fEditFontFace.getSelectedItem())))) {
					s.setFontFace(getTheme(), String.valueOf(fEditFontFace.getSelectedItem()));
				}
			}
		});
		fEditFontFaceInherited = new JCheckBox(); 
		fEditFontFaceInherited.addActionListener(new StyleEditListener() {
			void stateChanged(REDStyle s) {
				if (fEditFontFaceInherited.isSelected()) {
					s.setFontFace(getTheme(), null);
				}
				else {
					s.setFontFace(getTheme(), String.valueOf(fEditFontFace.getSelectedItem()));
				}
			}
		});
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String [] fonts = ge.getAvailableFontFamilyNames();
		for (int x = 0; x < fonts.length; x++) {
			fEditFontFace.addItem(fonts[x]);
		}
	}
		
	private void setupStyleEditGUIWidgetsFontSize() {
		fEditFontSize = new JComboBox(fgFontSizes);  fEditFontSize.setEditable(true);
		fEditFontSize.addActionListener(new StyleEditListener() {
			void stateChanged(REDStyle s) {
				try {
					int x = Integer.parseInt(String.valueOf(fEditFontSize.getEditor().getItem()));
					if (! ("".equals(fEditFontSize.getEditor().getItem()) || s.getFontSize() == x)) {
						s.setFontSize(getTheme(), x);
					}
				}
				catch (NumberFormatException nfe) {
					REDAuxiliary.beep();
				}
			}
		});
		fEditFontSizeInherited = new JCheckBox(); 
		fEditFontSizeInherited.addActionListener(new StyleEditListener() {
			void stateChanged(REDStyle s) {
				if (fEditFontSizeInherited.isSelected()) {
					s.setFontSize(getTheme(), REDStyle.INHERITED);
				}
				else {
					int x = s.getFontSize();
					try {
						x = Integer.parseInt(String.valueOf(fEditFontSize.getEditor().getItem()));
					}
					catch (NumberFormatException nfe) { }
					s.setFontSize(getTheme(), x);
				}
			}
		});
	}

	private void setupStyleEditGUIWidgetsFontStyle() {
		fEditFontStyle = new JComboBox(fgFontStyles); 
		fEditFontStyle.addActionListener(new StyleEditListener() {
			void stateChanged(REDStyle s) {
				if (! ("".equals(fEditFontStyle.getSelectedItem()) || s.getFontStyle().equals(String.valueOf(fEditFontStyle.getSelectedItem())))) {
					s.setFontStyle(getTheme(), String.valueOf(fEditFontStyle.getSelectedItem()));
				}
			}
		});
		fEditFontStyleInherited = new JCheckBox(); 
		fEditFontStyleInherited.addActionListener(new StyleEditListener() {
			void stateChanged(REDStyle s) {
				if (fEditFontStyleInherited.isSelected()) {
					s.setFontStyle(getTheme(), null);
				}
				else {
					s.setFontStyle(getTheme(), String.valueOf(fEditFontStyle.getSelectedItem()));
				}
			}
		});
	}

	private void setupStyleEditGUIWidgetsLining() {
		fEditLining = new JComboBox(REDLining.fgLinings);
		fEditLining.addActionListener(new StyleEditListener() {
			void stateChanged(REDStyle s) {
				if (! (fEditLining.getSelectedItem() == null || s.getLining().equals(fEditLining.getSelectedItem()))) {
					s.setLining(getTheme(), (REDLining) fEditLining.getSelectedItem());
				}
			}
		});
		fEditLiningInherited = new JCheckBox();
		fEditLiningInherited.addActionListener(new StyleEditListener() {
			void stateChanged(REDStyle s) {
				if (fEditLiningInherited.isSelected()) {
					s.setLining(getTheme(), null);
				}
				else {
					s.setLining(getTheme(), (REDLining) fEditLining.getSelectedItem());
				}
			}
		});
	}
		
	private void setupStyleEditGUIWidgetsForeground() {
		fEditForeground = new ColorButton() {
			void colorChanged(REDStyle s, Color col) {
				s.setForeground(getTheme(), col);
			}	
		}; 
		fEditForeground.addActionListener(new ButtonColorizer(fEditForeground));
		fEditForegroundInherited = new JCheckBox();
		fEditForegroundInherited.addActionListener(new StyleEditListener() {
			void stateChanged(REDStyle s) {
				if (fEditForegroundInherited.isSelected()) {
					s.setForeground(getTheme(), null);
				}
				else {
					s.setForeground(getTheme(), fEditForeground.getBackground());
				}
			}
		});
	}
		
	private void setupStyleEditGUIWidgetsBackground() {
		fEditBackground = new ColorButton() {
			void colorChanged(REDStyle s, Color col) {
				s.setBackground(getTheme(), col);
			}	
		}; 
		fEditBackground.addActionListener(new ButtonColorizer(fEditBackground));
		fEditBackgroundInherited = new JCheckBox();
		fEditBackgroundInherited.addActionListener(new StyleEditListener() {
			void stateChanged(REDStyle s) {
				if (fEditBackgroundInherited.isSelected()) {
					s.setBackground(getTheme(), null);
				}
				else {
					s.setBackground(getTheme(), fEditBackground.getBackground());
				}
			}
		});
	}

	private void setupStyleEditGUIWidgetsDisplayName() {
		fEditDisplayName = new JTextField(20);
	}

	private void setupStyleEditGUIWidgetsDescription() {
		fEditDescription = new JTextArea(2, 29); fEditDescription.setLineWrap(true); fEditDescription.setWrapStyleWord(true);
		fEditDescription.setBorder(BorderFactory.createLineBorder(Color.black));
	}
	
	private static GridBagConstraints createConstraints(double weightx, int gridwidth, int anchor, int fill) {
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = weightx;
		c.gridwidth = gridwidth;
		c.anchor = anchor;
		c.fill = fill;
		c.insets = new Insets(2, 2, 2, 2);
		return c;
	}
	
	private void setupStyleEditGUI() {
		// Setup gridbag 
		GridBagLayout gridbag = new GridBagLayout();		
		GridBagConstraints contConstraints = createConstraints(1.0, GridBagConstraints.REMAINDER, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);
		GridBagConstraints labelConstraints = createConstraints(0.0, 1, GridBagConstraints.EAST, GridBagConstraints.NONE);
		GridBagConstraints cbConstraints = createConstraints(0.0, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE);
		GridBagConstraints shorty = createConstraints(0.1, GridBagConstraints.RELATIVE, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL);

		setupStyleEditGUIWidgets();

		fStyleEditGUI = new JPanel(gridbag); 

		// Header
		addStyleEditGUI(gridbag, labelConstraints, new JLabel(""), false);
		addStyleEditGUI(gridbag, labelConstraints, new JLabel("Inherited"), false);
		addStyleEditGUI(gridbag, contConstraints, new JLabel(""), false);
		
		// Font face
		addStyleEditGUI(gridbag, labelConstraints, new JLabel("Face: "), false);
		addStyleEditGUI(gridbag, cbConstraints, fEditFontFaceInherited, false);
		addStyleEditGUI(gridbag, contConstraints, fEditFontFace, false);
		
		// Font size
		addStyleEditGUI(gridbag, labelConstraints, new JLabel("Size: "), false);
		addStyleEditGUI(gridbag, cbConstraints, fEditFontSizeInherited, false);
		addStyleEditGUI(gridbag, shorty, fEditFontSize, false);
		addStyleEditGUI(gridbag, contConstraints, new JLabel(""), false);	// Filler
		
		// Font style
		addStyleEditGUI(gridbag, labelConstraints, new JLabel("Style: "), false);
		addStyleEditGUI(gridbag, cbConstraints, fEditFontStyleInherited, false);
		addStyleEditGUI(gridbag, shorty, fEditFontStyle, false);
		addStyleEditGUI(gridbag, contConstraints, new JLabel(""), false);	// Filler
		
		// Lining
		addStyleEditGUI(gridbag, labelConstraints, new JLabel("Lining: "), false);
		addStyleEditGUI(gridbag, cbConstraints, fEditLiningInherited, false);
		addStyleEditGUI(gridbag, shorty, fEditLining, false);
		addStyleEditGUI(gridbag, contConstraints, new JLabel(""), false);	// Filler
		
		// Foreground
		addStyleEditGUI(gridbag, labelConstraints, new JLabel("Foreground: "), false);
		addStyleEditGUI(gridbag, cbConstraints, fEditForegroundInherited, false);
		addStyleEditGUI(gridbag, shorty, fEditForeground, false);
		addStyleEditGUI(gridbag, contConstraints, new JLabel(""), false);	// Filler
		
		// Background
		addStyleEditGUI(gridbag, labelConstraints, new JLabel("Background: "), false);
		addStyleEditGUI(gridbag, cbConstraints, fEditBackgroundInherited, false);
		addStyleEditGUI(gridbag, shorty, fEditBackground, false);
		addStyleEditGUI(gridbag, contConstraints, new JLabel(""), false);	// Filler		
		
		// Display name		
		addStyleEditGUI(gridbag, labelConstraints, new JLabel("Display Name: "), false);
		addStyleEditGUI(gridbag, contConstraints, fEditDisplayName, true);

		// Description 		
		addStyleEditGUI(gridbag, labelConstraints, new JLabel("Description: "), false);
		addStyleEditGUI(gridbag, contConstraints, fEditDescription, true);
		
		fStyleEditGUI.setMinimumSize(new Dimension(200, 100));		
	}
	
	public void updateStyleEditGUI(REDStyle style) {
		if (style == null) {
			selectStyle("Default");
		}
		else {
			fInUpdate = true;
			fEditDisplayName.setText(style.getDisplayName());
			fEditDescription.setText(style.getDescription());
			String theme = getTheme();
			fEditFontFace.setSelectedItem(style.getFontFace(theme));
			if (!fEditFontFace.getSelectedItem().equals(style.getFontFace(theme))) {	// insert new font face into combobox list
				fEditFontFace.addItem(style.getFontFace(theme));
				fEditFontFace.setSelectedItem(style.getFontFace(theme));
			}
			fEditFontFaceInherited.setSelected(!style.definesFontFace(theme));
			fEditFontSize.getEditor().setItem(String.valueOf(style.getFontSize(theme)));
			fEditFontSizeInherited.setSelected(!style.definesFontSize(theme));
			fEditFontStyle.setSelectedItem(style.getFontStyle(theme).toLowerCase());
			fEditFontStyleInherited.setSelected(!style.definesFontStyle(theme));
			fEditForeground.setBackground(style.getForeground(theme));
			fEditForegroundInherited.setSelected(!style.definesForeground(theme));
			fEditBackground.setBackground(style.getBackground(theme));
			fEditBackgroundInherited.setSelected(!style.definesBackground(theme));
			fEditLining.setSelectedItem(style.getLining(theme));
			fEditLiningInherited.setSelected(!style.definesLining(theme));
	
			fEditFontFaceInherited.setEnabled(style.definesSuperStyle(theme));
			fEditFontSizeInherited.setEnabled(style.definesSuperStyle(theme));
			fEditFontStyleInherited.setEnabled(style.definesSuperStyle(theme));
			fEditForegroundInherited.setEnabled(style.definesSuperStyle(theme));
			fEditBackgroundInherited.setEnabled(style.definesSuperStyle(theme));
			fEditLiningInherited.setEnabled(style.definesSuperStyle(theme));		
			fInUpdate = false;
		}
	}
	
	public void styleSelected(REDStyle style) {
		if (style != null) {
			updateStyleEditGUI(style);
			fSampleEditor.setStyle(0, fSampleEditor.length(), style);
		}
	}
	
	public void valueChanged(TreeSelectionEvent e) {
		REDStyle style = null;
		Node node = (Node) fTree.getLastSelectedPathComponent();
		if (node != null) {
			style = (REDStyle) node.getUserObject();
		}
		Iterator iter = fListeners.iterator();
		while (iter.hasNext()) {
			REDStyleEditorSelectionListener listener = (REDStyleEditorSelectionListener) iter.next();
			listener.styleSelected(style);
		}
	}
	
	REDStyle getSelectedStyle() {
		DefaultMutableTreeNode node = null;
		TreePath path = fTree.getSelectionPath();
		if (path != null) {
			 node = (DefaultMutableTreeNode) path.getLastPathComponent();
		}
		if (node != null) {
			return (REDStyle) node.getUserObject();
		}
		else {
			return null;
		}
	}
	
	REDStyleManagerImpl getStyleManager() {
		return fStyleManager;
	}
	
	public REDEditor getSampleEditor() {
		return fSampleEditor;
	}
	
	public JComponent getThemeChooser() {
		return fThemes;
	}
	
	public String getTheme() {
		return String.valueOf(fThemes.getSelectedItem());
	}
	
	/** Save changed styles.
	  * @param stream The place to write changes to (usually a file).
	  */
	public void saveChangedStyles(OutputStream stream) throws IOException {
		REDXMLHandlerWriter writer = new REDXMLHandlerWriter(stream);
		writer.writeXMLHeader();
		writer.openTag("REDConfig", null);
		REDStyle s1, s2;
		Iterator iter = fStyleManager.doIterator();
		while (iter.hasNext()) {
			s1 = (REDStyle) iter.next();
			s2 = REDStyleManager.getStyle(s1.getName());
			Iterator iter2 = s1.themeIterator();
			while (iter2.hasNext()) {
				String theme = (String) iter2.next();
				if (!s1.equalsTheme(theme, s2)) {
					s1.writeTheme(theme, writer);
				}
			}			
		}
		writer.closeTag();		
	}

	REDStyleManagerImpl fStyleManager;
	JComponent fHierarchy;
	HashMap fStyleToNodeMap;
	JComponent fStyleEditGUI;
	JTree fTree;
	JComboBox fThemes;
	DefaultMutableTreeNode fRoot;	
	HashSet fListeners;
	JTextField fEditDisplayName;
	JComboBox fEditFontFace;
	JCheckBox fEditFontFaceInherited;
	JComboBox fEditFontSize;
	JCheckBox fEditFontSizeInherited;
	JComboBox fEditFontStyle;
	JCheckBox fEditFontStyleInherited;
	JComboBox fEditLining;
	JCheckBox fEditLiningInherited;
	ColorButton fEditForeground;
	JCheckBox fEditForegroundInherited;
	ColorButton fEditBackground;
	JCheckBox fEditBackgroundInherited;
	JTextArea fEditDescription;
	REDEditor fSampleEditor;
	boolean fInUpdate;
		
	static final Object[] fgFontSizes = {"6", "7", "8", "9", "10", "11", "12", "13", "14", "16", "20", "24", "32", "40", "50", "60"};
	static final Object[] fgFontStyles = {"plain", "bold", "italic", "bolditalic"};
	static final ImageIcon fgExpandedIcon =  new ImageIcon(REDEditor.class.getClassLoader().getResource("red/icons/tree_expanded.png"));
	static final ImageIcon fgCollapsedIcon = new ImageIcon(REDEditor.class.getClassLoader().getResource("red/icons/tree_collapsed.png"));
}
