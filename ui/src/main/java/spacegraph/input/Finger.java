package spacegraph.input;

import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import org.jetbrains.annotations.Nullable;
import spacegraph.Ortho;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.math.v3;
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
    public final Ortho root;

    //TODO wheel state

    /** widget above which this finger currently hovers */
    public @Nullable Widget touching;

    public v2 nextHit = null;
    public short[] nextButtonDown;

    /** global pointer screen coordinate, set by window the (main) cursor was last active in */
    public final static Point pointer = new Point();


    public Finger(Ortho root) {
        this.root = root;
    }


//    private boolean on(@Nullable Widget touched) {
//
//        if (touched!=touching && touching!=null) {
//            touching.touch(null);
//        }
//
//        touching = touched;
//
//        if (touching!=null) {
//            touching.touch(this);
//            return true;
//        }
//        return false;
//    }


    public boolean off() {
        if (touching!=null) {
            touching.onTouch(null);
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



    public void print() {
        System.out.println(root.surface + " " + hit + " " + touching + " " + Arrays.toString(buttonDown));
    }

    public void zoom(Surface s) {

        System.out.println("zoom: " + s.translateLocal + " " + s.scaleGlobal + " " + s.scaleLocal);

        v3 pos = new v3(s.translateLocal);
        v2 size = new v2(s.scaleLocal);
        //pos.scale(size);

        Surface p = s.parent;
        while (p!=null) {
            pos.scale(p.scaleLocal.x, p.scaleLocal.y, 1f);
            size.scale(p.scaleLocal.x, p.scaleLocal.y);
            pos.add(p.translateLocal.x * p.scaleLocal.x,
                    p.translateLocal.y * p.scaleLocal.y, 0 );

            p = p.parent;
        }

        float speed = 0.01f;
        root.translate.set(
            (speed) * pos.x +
                    (1f - speed) * (root.translate.target.x),
            (speed) * pos.y +
                    (1f - speed) * (root.translate.target.y)
        );
    }
}
