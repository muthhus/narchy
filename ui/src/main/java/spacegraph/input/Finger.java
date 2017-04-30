package spacegraph.input;

import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import org.jetbrains.annotations.Nullable;
import spacegraph.Ortho;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.widget.Widget;

import java.util.Arrays;

import static java.lang.System.arraycopy;
import static java.util.Arrays.fill;
import static spacegraph.math.v3.v;

/**
 * gestural generalization of mouse cursor's (or touchpad's, etc)
 * possible intersection with a surface and/or its sub-surfaces.
 *
 * tracks state changes and signals widgets of these
 */
public class Finger {


    public final v2 hit = new v2();
    public final boolean[] buttonDown = new boolean[5];
    public final boolean[] prevButtonDown = new boolean[5];
    private final Ortho root;

    //TODO wheel state

    /** widget above which this finger currently hovers */
    public @Nullable Widget touching;

    public Finger(Ortho root) {
        this.root = root;
    }

    public Surface on(v2 nextHit, short[] nextButtonDown) {
        this.hit.set(nextHit);

        arraycopy(this.buttonDown, 0, prevButtonDown, 0, buttonDown.length);

        fill(this.buttonDown, false);
        if (nextButtonDown!=null) {
            for (short s : nextButtonDown) {
                if (s > 0) //ignore -1 values
                    this.buttonDown[s - 1 /* start at zero=left button */] = true;
            }
        }

        Surface s = root.surface.onTouch(nextHit, nextButtonDown);
        if (s instanceof Widget) {
            if (!on((Widget)s))
                s = null;
        } else {
            s = null;
        }

        if (s!=null)
            on((Widget)s);
        return s;
    }

    private boolean on(@Nullable Widget touched) {

        if (touched!=touching && touching!=null) {
            touching.touch(null);
        }

        touching = touched;

        if (touching!=null) {
            touching.touch(this);
            return true;
        }
        return false;
    }


    public boolean off() {
        if (touching!=null) {
            touching.touch(null);
            touching = null;
            return true;
        }
        return false;
    }

//    public void update(@Nullable MouseEvent e, GLWindow window) {
//
//        short[] buttonsDown = e!=null ? e.getButtonsDown() : null;
//        update(e, buttonsDown, window);
//    }

    /** global pointer screen coordinate, set by window the (main) cursor was last active in */
    public final static Point pointer = new Point();

    public Surface update(@Nullable MouseEvent e, float x, float y, short[] buttonsDown) {

        if (e!=null) {
            SpaceGraph rw;
            Ortho r = root;
            if (r != null) {
                if (r.window != null) {
                    rw = r.window;
                    if (rw != null) {
                        GLWindow rww = rw.window;
                        if (rww != null) {
                            Point p = rww.getLocationOnScreen(new Point());
                            pointer.set(p.getX() + e.getX() , p.getY() + e.getY() );
                        }
                    }
                }
            }
        }

        /*if (e == null) {
            off();
        } else {*/
            Surface s;
            if ((s = on(v(x, y), buttonsDown))!=null) {
                if (e!=null)
                    e.setConsumed(true);
                return s;
            }

        //}

        return null;
    }

    public void print() {
        System.out.println(root.surface + " " + hit + " " + touching + " " + Arrays.toString(buttonDown));
    }

}
