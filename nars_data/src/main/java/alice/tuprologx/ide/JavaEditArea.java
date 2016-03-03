/*
 * tuProlog - Copyright (C) 2001-2004  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprologx.ide;

import javax.swing.*;
import javax.swing.undo.*;
import javax.swing.event.*;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.beans.*;
import java.awt.Color;
import java.awt.Font;

/**
 * An edit area for the Java 2 platform. Makes use of an advanced Swing text area.
 * 
 * @author    <a href="mailto:giulio.piancastelli@studio.unibo.it">Giulio Piancastelli</a>
 * @version    1.0 - 14-nov-02
 */

@SuppressWarnings("serial")
public class JavaEditArea extends JPanel implements TheoryEditArea, FileEditArea {

    /**
	 * The advanced Swing text area used by this edit area.
	 */
    private final RSyntaxTextArea inputTheory;
    /**
	 * The line number corresponding to the caret's current position in the text area.
	 */
    private int caretLine;
    /**
	 * Indicate if the edit area is changed after the last Set Theory operation issued by the editor.
	 */
    private boolean dirty;
    /**
	 * Indicate if the edit area is changed after the last save operation
	 */
    private boolean saved;
    /**
	 * Used for components interested in changes of console's properties.
	 */
    private final PropertyChangeSupport propertyChangeSupport;
    /**
	 * Undo Manager for the Document in the JEditTextArea.
	 */
    private final UndoManager undoManager;

    public JavaEditArea(CompletionProvider completionProvider) {
    	// Create the editor
        inputTheory = new RSyntaxTextArea(20, 60);
        inputTheory.setTabSize(2);
        inputTheory.setClearWhitespaceLinesEnabled(true);
        inputTheory.setAntiAliasingEnabled(true);
        inputTheory.setMarkOccurrences(true);
        
        // Add token definitions for syntax coloring
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory)TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/Prolog", "alice.tuprologx.ide.PrologTokenMaker2");
        inputTheory.setSyntaxEditingStyle("text/Prolog");
        SyntaxScheme scheme = inputTheory.getSyntaxScheme();
        scheme.getStyle(Token.VARIABLE).foreground = Color.BLUE;
        
        // Add text completion
        AutoCompletion ac = new AutoCompletion(completionProvider);
        ac.install(inputTheory);
        ac.setShowDescWindow(true);
        ac.setParameterAssistanceEnabled(true);
        ac.setAutoCompleteSingleChoices(false);
        
        RTextScrollPane inputTheoryScrollPane = new RTextScrollPane(inputTheory);
        
        setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints constraints = new java.awt.GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        //constraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        constraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        constraints.fill = java.awt.GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        //constraints.insets = new java.awt.Insets(0, 0, 10, 0);

        inputTheory.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent event) {
                setCaretLine(inputTheory.getCaretLineNumber() + 1);
            }
        });

        dirty = false;
        saved = true;
        inputTheory.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                changedUpdate(event);
            }
            @Override
            public void removeUpdate(DocumentEvent event) {
                changedUpdate(event);
            }
            @Override
            public void changedUpdate(DocumentEvent event) {
                if (!dirty)
                    setDirty(true);
                  if (saved)
                       setSaved(false);
            }

        });

        undoManager = new UndoManager();
        inputTheory.getDocument().addUndoableEditListener(undoManager);

        add(inputTheoryScrollPane, constraints);
        
        propertyChangeSupport = new PropertyChangeSupport(this);
    }
    
    @Override
    public void setCaretLine(int caretLine) {
        int oldCaretLine = getCaretLine();
        this.caretLine = caretLine;
        propertyChangeSupport.firePropertyChange("caretLine", oldCaretLine, caretLine);
    }

    @Override
    public int getCaretLine() {
        return caretLine;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void setTheory(String theory) {
        inputTheory.setText(theory);

        //to set caret at the begin of the edit area
        inputTheory.setCaretPosition(0);
    }

    @Override
    public String getTheory() {
        return inputTheory.getText();
    }


    @Override
    public void setDirty(boolean flag) {
        dirty = flag;
    }


    @Override
    public boolean isDirty() {
        return dirty;
    }


    @Override
    public void setSaved(boolean flag) {
        if (inputTheory.isEditable())
        {
            boolean oldSaved = isSaved();
            saved = flag;
            propertyChangeSupport.firePropertyChange("saved", oldSaved, saved);
        }
    }


    @Override
    public boolean isSaved() {
        return saved;
    }

    /* Managing Undo/Redo actions. */

    @Override
    public void undoAction() {
        try {
            undoManager.undo();
        } catch (CannotUndoException e) {
            // e.printStackTrace();
        }
    }

    @Override
    public void redoAction() {
        try {
            undoManager.redo();
        } catch (CannotRedoException e) {
            // e.printStackTrace();
        }
    }

    public void setFontDimension(int dimension)
    {
        Font font = new Font(inputTheory.getFont().getName(), inputTheory.getFont().getStyle(), dimension);
        inputTheory.setFont(font);
    }

    public void setEditable(boolean flag)
    {
        inputTheory.setEditable(flag);
    }
}