package spacegraph.input;

import com.jogamp.nativewindow.util.Point;
import org.jetbrains.annotations.Nullable;
import spacegraph.Ortho;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.widget.Widget;

import java.util.Arrays;

import static java.lang.System.arraycopy;
import static java.util.Arrays.fill;

/**
 * gestural generalization of mouse cursor's (or touchpad's, etc)
 * possible intersection with a surface and/or its sub-surfaces.
 * <p>
 * tracks state changes and signals widgets of these
 */
public class Finger {

    /**
     * global pointer screen coordinate, set by window the (main) cursor was last active in
     */
    public final static Point pointer = new Point();

    public final v2 hit = new v2(), hitGlobal = new v2();
    public final v2[] hitOnDown = new v2[5], hitOnDownGlobal = new v2[5];
    public final boolean[] buttonDown = new boolean[5];
    public final boolean[] prevButtonDown = new boolean[5];
    private final Ortho root;


    //TODO wheel state

    /**
     * widget above which this finger currently hovers
     */
    public @Nullable Widget touching;

    /**
     * TODO scale this to pixel coordinates, this spatial coordinate is tricky and resolution dependent anyway
     */
    final static float DRAG_THRESHOLD = 0.0004f;

    public Finger(Ortho root) {
        this.root = root;
    }


    public Surface on(float sx, float sy, float lx, float ly, short[] nextButtonDown) {
        this.hit.set(lx, ly);
        this.hitGlobal.set(sx, sy);

        arraycopy(this.buttonDown, 0, prevButtonDown, 0, buttonDown.length);

        fill(this.buttonDown, false);
        if (nextButtonDown != null) {
            for (short s : nextButtonDown) {
                if (s > 0) //ignore -1 values
                    this.buttonDown[s - 1 /* start at zero=left button */] = true;
            }

            for (int j = 0, jj = hitOnDown.length; j < jj; j++) {
                if (!prevButtonDown[j] && buttonDown[j]) {
                    hitOnDown[j] = new v2(hit);
                    hitOnDownGlobal[j] = new v2(hitGlobal);
                }
            }

        } else {
            Arrays.fill(hitOnDown, null);
        }


        //START DESCENT:

        Surface s = root.surface.onTouch(this, hit, nextButtonDown);
        if (s instanceof Widget) {
            if (!on((Widget) s))
                s = null;
        } else {
            s = null;
        }

        for (int j = 0, jj = hitOnDown.length; j < jj; j++) {
            if (!buttonDown[j] && hitOnDown[j] != null) {
                hitOnDown[j] = null; //release
            }
        }

//        if (s != null)
//            on((Widget) s);


        return s;
    }

    public boolean dragging(int button) {
        return (hitOnDownGlobal[button] != null && hitOnDownGlobal[button].distanceSq(hitGlobal) >= DRAG_THRESHOLD * DRAG_THRESHOLD);
    }

    private boolean on(@Nullable Widget touched) {

        if (touched != touching && touching != null) {
            touching.touch(null);
        }

        touching = touched;

        if (touching != null) {
            touching.touch(this);
            return true;
        }
        return false;
    }


    public boolean off() {
        if (touching != null) {
            touching.touch(null);
            touching = null;
            return true;
        }
        return false;
    }


    public void print() {
        System.out.println(root.surface + " " + hit + " " + touching + " " + Arrays.toString(buttonDown));
    }

    public boolean released(int button) {
        return prevButtonDown[button] && !buttonDown[button];
    }

    public boolean pressed(int button) {
        return !prevButtonDown[button] && buttonDown[button];
    }

    public boolean clickReleased(int button) {
        return released(button) && !dragging(button);
    }
}
