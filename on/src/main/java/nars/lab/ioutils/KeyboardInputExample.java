/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.lab.ioutils;

import nars.NAR;
import nars.gui.NARSwing;
import nars.gui.input.KeyboardInputPanel;
import nars.gui.util.swing.NWindow;

/**
 *
 * @author me
 */
public class KeyboardInputExample {
    
    public static void main(String[] args) {
        //NAR n = NAR.build(new Neuromorphic().realTime());
        //NAR n = NAR.build(new Default().realTime());
        //n.param.duration.set(100);
        
        NARSwing.themeInvert();
        
        NAR n = new NAR();
        
        
                
        new NARSwing(n).themeInvert();

        new NWindow("Direct Keyboard Input", new KeyboardInputPanel(n)).show(300, 100, false);
        
        n.start(100, 5);
        
        
    }
}
