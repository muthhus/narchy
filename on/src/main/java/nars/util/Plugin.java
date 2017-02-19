package nars.util;

import nars.NAR;

import java.io.Serializable;

/**
 * NAR plugin interface
 */
public interface Plugin extends Serializable {

    /** called when plugin is activated (enabled = true) / deactivated (enabled=false) */
    boolean setEnabled(NAR n, boolean enabled);
    
    default CharSequence name() {
        return this.getClass().getSimpleName();
    }
}
