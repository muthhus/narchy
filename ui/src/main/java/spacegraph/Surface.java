package spacegraph;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.math.v2;
import spacegraph.math.v3;

import java.util.Collections;
import java.util.List;

/**
 * planar subspace.
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
public class Surface {


    protected v2 scaleGlobal;



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

    public final v3 translateLocal;
    public final v2 scaleLocal;

    public Surface parent;

    /**
     * not used unless aspect ratio is set to non-NaN value
     */
    Align align = Align.Center;

    /**
     * height/width target aspect ratio; if aspect is NaN, no adjustment applied
     */
    protected float aspect = Float.NaN;

    public Surface() {
        translateLocal = new v3();
        scaleLocal = new v2(1f, 1f);
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
    @Nullable
    public Surface onTouch(v2 hitPoint, short[] buttons) {
        //System.out.println(this + " " + hitPoint + " " + Arrays.toString(buttons));

        //1. test local reaction
        boolean b = onTouching(hitPoint, buttons);
        if (b)
            return this;

        return null;
    }


    /**
     * may be overridden to trap events on this surface (returning true), otherwise they pass through to any children
     */
    protected boolean onTouching(v2 hitPoint, short[] buttons) {
        return false;
    }


    protected void paint(GL2 gl) {

    }

    public Surface move(float dx, float dy) {
        translateLocal.add(dx, dy,0);
        return this;
    }

    public final void render(GL2 gl, v2 globalScale) {


        v2 s = this.scaleLocal;
        float scaleX = s.x;
        if (scaleX != scaleX || scaleX <= 0)
            return; //invisible


        gl.glPushMatrix();

        transform(gl, globalScale);

        //gl.glNormal3f(0,0,1);

        paint(gl);

        List<? extends Surface> cc = children();
        if (cc != null) {
            v2 childGlobal = new v2(this.scaleGlobal);
            childGlobal.scale(s);
            for (int i = 0, childrenSize = cc.size(); i < childrenSize; i++) {
                Surface ss = cc.get(i);
                if (ss!=null)
                    ss.render(gl, childGlobal);
            }
        }

        gl.glPopMatrix();
    }

    public List<? extends Surface> children() {
        return Collections.emptyList();
    }


    public void transform(GL2 gl, v2 globalScale) {
        final Surface c = this;

        this.scaleGlobal = globalScale;

        v3 translate = c.translateLocal;

        v2 scale = c.scaleLocal;

        float sx, sy;

        if (Float.isFinite(aspect)) {
            float globalAspect = globalScale.y / globalScale.x;
            float targetAspect = aspect/globalAspect;
            if (targetAspect < 1) {
                //wider, shrink y
                sx = scale.x;
                sy = scale.y * targetAspect;
            } else {
                //taller, shrink x
                sx = scale.x / targetAspect;
                sy = scale.y;
            }


        } else {
            //consume entire area, regardless of aspect
            sx = scale.x;
            sy = scale.y;
        }

        float tx = translate.x, ty = translate.y;
        switch (align) {

            //TODO others

            case Center:
                //HACK TODO figure this out
                tx += (1f - (sx/scale.x))/2f;
                ty += (1f - (sy/scale.y))/2f;
                break;

            case None:
            default:
                break;

        }

        //System.out.println(this + " scale=" + sx + "," + sy + " (a=" + aspect + ") @ " + tx + ","+ ty + "," + translate.z);
        gl.glTranslatef(tx, ty, translate.z);
        gl.glScalef(sx, sy, 1f);
    }


    public static boolean leftButton(short[] buttons) {
        return buttons!=null && buttons.length == 1 && buttons[0] == 1;
    }


    public Surface scale(float x, float y) {
        scaleLocal.set(x, y);
        return this;
    }

    public Surface pos(float x, float y) {
        translateLocal.set(x, y);
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
        return Math.max(scaleLocal.x, scaleLocal.y);
    }

    public Surface hide() {
        scale(Float.NaN, Float.NaN);
        return this;
    }

}
