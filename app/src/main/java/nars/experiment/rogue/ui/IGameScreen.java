package nars.experiment.rogue.ui;

import java.awt.event.KeyEvent;

/**
 * All classes, that must react on key events and output something,
 * must implement this interface. I mean primarily game dialogs and
 * game itself.
 *
 * @author Digal
 */

public interface IGameScreen {
    /**
     * Method, that repaints the component.
     *
     * @param c Console to draw at.
     */
    void paint(Console c);

    /**
     * Method, called, when the key event happens
     *
     * @param ke - key event.
     * @return true if calling object can interrupt i/o. E.g. dialog is finished.
     */

    boolean getKeyEvent(KeyEvent ke);
}
