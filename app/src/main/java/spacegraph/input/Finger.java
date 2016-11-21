package spacegraph.input;

import com.jogamp.newt.event.MouseEvent;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.obj.widget.Widget;

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

    private final Surface root;

    public final v2 hit = new v2();
    public final boolean[] buttonDown = new boolean[5];
    public final boolean[] prevButtonDown = new boolean[5];

    //TODO wheel state

    /** widget above which this finger currently hovers */
    public @Nullable Widget touching = null;

    public Finger(Surface root) {
        this.root = root;
    }

    public Surface on(v2 nextHit, short[] nextButtonDown) {
        this.hit.set(nextHit);

        arraycopy(this.buttonDown, 0, prevButtonDown, 0, buttonDown.length);

        fill(this.buttonDown, false);
        for (short s : nextButtonDown) {
            if (s > 0) //ignore -1 values
                this.buttonDown[ s - 1 /* start at zero=left button */] = true;
        }

        Surface s = root.onTouch(nextHit, nextButtonDown);
        if (s instanceof Widget) {
            on((Widget)s);
            return s;
        } else {
            on(null);
            return null;
        }
    }

    private boolean on(@Nullable Widget touched) {

        if (touching!=null && touched!=touching) {
            touching.touch(null);
        }

        touching = touched;

        if (touching!=null) {
            touching.touch(this);
            return true;
        }
        return false;
    }


    public void off() {
        if (touching!=null) {
            touching.touch(null);
            touching = null;
        }
    }

//    public void update(@Nullable MouseEvent e, GLWindow window) {
//
//        short[] buttonsDown = e!=null ? e.getButtonsDown() : null;
//        update(e, buttonsDown, window);
//    }

    public boolean update(@Nullable MouseEvent e, float x, float y, short[] buttonsDown) {
        if (e == null) {
            off();
            return false;
        } else {
            if (on(v(x, y), buttonsDown)!=null) {
                e.setConsumed(true);
                return true;
            }
            return false;
        }

    }
    public void print() {
        System.out.println(root + " " + hit + " " + touching + " " + Arrays.toString(buttonDown));
    }

}
