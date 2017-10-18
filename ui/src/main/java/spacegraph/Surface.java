package spacegraph;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.math.v3;

import java.util.List;

/**
 * planar subspace.
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
abstract public class Surface {


    public enum Align {


        None,

        /**
         * 1:1, centered
         */
        Center,

        /**
         * 1:1, x=left, y=center
         */
        LeftCenter,

        /**
         * 1:1, x=right, y=center
         */
        RightCenter

        //TODO etc...
    }

    public v3 pos;
    public v2 scale;

    public Surface parent;

    /**
     * not used unless aspect ratio is set to non-NaN value
     */
    protected Align align = Align.Center;

    /**
     * height/width target aspect ratio; if aspect is NaN, no adjustment applied
     */
    protected float aspect = Float.NaN;

    public Surface() {
        pos = new v3();
        scale = new v2(1f, 1f);
    }

    public SurfaceRoot root() {
        Surface parent = this.parent;
        if (parent == null)
            return null;
        return parent.root();
    }

    public Surface align(Align align) {
        this.align = align;
        return this;
    }

    public Surface align(Align align, float aspect) {
        this.aspect = aspect;
        return align(align);
    }

    public Surface align(Align align, float width, float height) {
        return align(align, height / width);
    }

    /** null parent means it is the root surface */
    public void start(@Nullable Surface parent) {
        this.parent = parent;
    }

    public void layout() {
        //nothing by default
    }

    public void stop() {
        parent = null;
    }

    /**
     * returns non-null if the event has been absorbed by a speciifc sub-surface
     * or null if nothing absorbed the gesture
     */
    public Surface onTouch(Finger finger, @Deprecated v2 hitPoint, @Deprecated short[] buttons) {
        //System.out.println(this + " " + hitPoint + " " + Arrays.toString(buttons));

        //1. test local reaction
        boolean b = onTouching(finger, hitPoint, buttons);
        if (b)
            return this;

        return null;
    }


    /**
     * may be overridden to trap events on this surface (returning true), otherwise they pass through to any children
     */
    @Deprecated protected boolean onTouching(Finger finger, v2 hitPoint, short[] buttons) {
        return false;
    }


    abstract protected void paint(GL2 gl);

    public Surface move(float dx, float dy) {
        pos.add(dx, dy,0);
        return this;
    }

    public final void render(GL2 gl) {


        v2 s = this.scale;
        float scaleX = s.x;
        if (scaleX != scaleX || scaleX <= 0)
            return; //invisible

        v2 scale = this.scale;

        float sx, sy;

        float aspect = this.aspect;
        if (aspect==aspect /* not NaN */) {

            if (scale.y/scale.x > aspect) {
                //wider, shrink y
                sx = scale.x;
                sy = scale.y * aspect;
            } else {
                //taller, shrink x
                sx = scale.x / aspect;
                sy = scale.y;
            }


        } else {
            //consume entire area, regardless of aspect
            sx = scale.x;
            sy = scale.y;
        }

        float tx = pos.x, ty = pos.y;
        switch (align) {

            //TODO others

            case Center:
                //HACK TODO figure this out
//                tx += (1f - (sx/scale.x))/2f;
//                ty += (1f - (sy/scale.y))/2f;
                break;

            case None:
            default:
                break;

        }


        gl.glPushMatrix();

        gl.glTranslatef(tx, ty, pos.z);
        gl.glScalef(sx, sy, 1f);

        //gl.glNormal3f(0,0,1);

        paint(gl);

        gl.glPopMatrix();
    }



    public static boolean leftButton(short[] buttons) {
        return buttons!=null && buttons.length == 1 && buttons[0] == 1;
    }


    public Surface scale(float x, float y) {
        scale.set(x, y);
        return this;
    }

    public Surface pos(float x, float y) {
        pos.set(x, y);
        return this;
    }

    public boolean onKey(KeyEvent e, boolean pressed) {
        return false;
    }

    /**
     * returns true if the event has been absorbed, false if it should continue propagating
     */
    @Deprecated
    public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
        return false;
    }

    public float radius() {
        return Math.max(scale.x, scale.y);
    }

    public Surface hide() {
        scale(Float.NaN, Float.NaN);
        return this;
    }

}
