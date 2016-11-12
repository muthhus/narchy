package spacegraph;

import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.math.v2;
import spacegraph.math.v3;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * planar subspace.
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
public class Surface {


    public v3 translateLocal;
    public v2 scaleLocal;

    public Surface parent;
    public List<Surface> children;

    public Surface() {
        translateLocal = new v3();
        scaleLocal = new v2(1f,1f);
    }

    public void setParent(Surface s) {
        parent = s;
    }

    protected void layout() {
        //nothing by default
    }

    public void setChildren(Surface... s) {
        setChildren(Lists.newArrayList(s));
    }

    public void setChildren(List<Surface> children) {
        if (!Objects.equals(this.children, children)) {
            this.children = children;
            layout();
        }
    }

    /** returns non-null if the event has been absorbed by a speciifc sub-surface
     * or null if nothing absorbed the gesture
     */
    @Nullable
    public final Surface onTouch(v2 hitPoint, short[] buttons) {
        //System.out.println(this + " " + hitPoint + " " + Arrays.toString(buttons));

        //1. test local reaction
        boolean b = onTouching(hitPoint, buttons);
        if (b)
            return this;

        //2. test children reaction
        if (children!=null)
            return onChildTouching(hitPoint, buttons);
        else
            return null;
    }

    protected final Surface onChildTouching(v2 hitPoint, short[] buttons) {
        v2 subHit = new v2();

        for (Surface c : children) {
            //project to child's space
            subHit.set(hitPoint);

            float csx = c.scaleLocal.x;
            float csy = c.scaleLocal.y;
            subHit.sub(c.translateLocal.x, c.translateLocal.y);
            subHit.scale(1f / csx, 1f / csy);

            float hx = subHit.x, hy = subHit.y;
            if (hx >= 0f && hx <= 1f && hy >= 0 && hy <= 1f) {
                Surface s = c.onTouch(subHit, buttons);
                if (s!=null)
                    return s; //FIFO
            }
        }
        return this;
    }


    /** may be overridden to trap events on this surface (returning true), otherwise they pass through to any children */
    protected boolean onTouching(v2 hitPoint, short[] buttons) {
        return false;
    }



    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
        if (children!=null) {
            for (Surface c : children) {
                if (c.onKey(hitPoint, charCode, pressed))
                    return true;
            }
        }
        return false;
    }

    protected void paint(GL2 gl) {

    }

    public final void render(GL2 gl) {
        gl.glPushMatrix();

        transform(gl);

        gl.glNormal3f(0,0,1);

        paint(gl);

        List<? extends Surface> cc = this.children;
        if (cc != null) {
            for (int i = 0, childrenSize = cc.size(); i < childrenSize; i++)
                cc.get(i).render(gl);
        }

        gl.glPopMatrix();
    }


    public void transform(GL2 gl) {
        final Surface c = this;

        v3 translate = c.translateLocal;
        if (translate!=null)
            gl.glTranslatef(translate.x, translate.y, translate.z);

        v2 scale = c.scaleLocal;
        if (scale!=null)
            gl.glScalef(scale.x, scale.y, 1f);
    }


    public static boolean leftButton(@NotNull short[] buttons) {
        return buttons.length == 1 && buttons[0]==1;
    }


    public Surface scale(float x, float y) {
        scaleLocal.set(x, y);
        return this;
    }
    public Surface pos(float x, float y) {
        translateLocal.set(x, y);
        return this;
    }
}
