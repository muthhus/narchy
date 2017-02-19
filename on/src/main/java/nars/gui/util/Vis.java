package nars.gui.util;

import nars.gui.util.swing.PCanvas;
import processing.core.PGraphics;

/**
 * Something that can be visualized, drawn, or otherwies represented graphically / visually.
 */
public interface Vis {
    
    /** returns true if it should remain visible, false if it is to be removed */
    boolean draw(PGraphics g);

    /** notifies this when visibility has changed */
    default void onVisible(boolean showing) {
        
    }

    default void init(PCanvas p) {
        
    }
    
    
    
}
