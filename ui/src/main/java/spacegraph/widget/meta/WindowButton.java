package spacegraph.widget.meta;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import spacegraph.SpaceGraph;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.ToggleButton;

import java.util.function.Supplier;

/** toggle button, which when actived, creates a window, and when inactivated destroys it
 *  TODO window width, height parameters
 * */
public class WindowButton extends CheckBox implements ToggleButton.ToggleAction, WindowListener {

    private final Supplier spacer;

    SpaceGraph space;

    public WindowButton(String text, Object o) {
        this(text, ()->o);
    }

    public WindowButton(String text, Supplier spacer) {
        super(text);
        this.spacer = spacer;
        on(this);
    }

    @Override
    public void onChange(ToggleButton t, boolean enabled) {
        synchronized (this) {
            if (enabled) {
                if (space == null) {
                    int width = 800, height = 800;
                    space = SpaceGraph.window(spacer.get(), width, height);
                    space.addWindowListener(this);
                }
            } else {
                if (this.space!=null) {
                    GLWindow win = this.space.window;
                    this.space = null;
                    if (win != null && win.getWindowHandle() != 0)
                        win.destroy();
                }
            }
        }
    }

    @Override
    public void windowResized(WindowEvent e) {

    }

    @Override
    public void windowMoved(WindowEvent e) {

    }

    @Override
    public void windowDestroyNotify(WindowEvent e) {

    }

    @Override
    public void windowDestroyed(WindowEvent e) {
        this.space = null;
        set(false);
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {

    }

    @Override
    public void windowLostFocus(WindowEvent e) {

    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {

    }
}
