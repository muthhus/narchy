package nars.gui.graph;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.NotNull;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.util.Arrays;
import java.util.List;

/**
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
public class Surface {


    public Vector3f translateLocal;
    public Vector2f scaleLocal;

    public Surface parent;
    public List<? extends Surface> children;

    public Surface() {
        translateLocal = new Vector3f();
        scaleLocal = new Vector2f(1f,1f);
    }

    public void setParent(Surface s) {
        parent = s;
    }

    protected void layout() {
        //nothing by default
    }

    public void setChildren(List<? extends Surface> children) {
        if (this.children == null || !this.children.equals(children)) {
            this.children = children;
            layout();
        }
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public final boolean onTouch(Vector2f hitPoint, short[] buttons) {
        return onTouching(hitPoint, buttons) || (children!=null && onChildTouching(hitPoint, buttons));
    }

    protected final boolean onChildTouching(Vector2f hitPoint, short[] buttons) {
        Vector2f subHit = new Vector2f();

        for (Surface c : children) {
            //project to child's space
            subHit.set(hitPoint);

            float csx = c.scaleLocal.x;
            float csy = c.scaleLocal.y;
            subHit.sub(c.translateLocal.x, c.translateLocal.y);
            subHit.scale(1f / csx, 1f / csy);

            float hx = subHit.x, hy = subHit.y;
            if (hx >= 0f && hx <= 1f && hy >= 0 && hy <= 1f) {
                if (c.onTouch(subHit, buttons)) {
                    return true;
                }
            }
        }
        return false;
    }


    /** may be overridden to trap events on this surface (returning true), otherwise they pass through to any children */
    protected boolean onTouching(Vector2f hitPoint, short[] buttons) {
        return false;
    }



    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onKey(Vector2f hitPoint, char charCode) {
        return false;
    }

    protected void paint(GL2 gl) {

    }

    public final void render(GL2 gl) {
        gl.glPushMatrix();

        transform(gl);

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

        Vector3f translate = c.translateLocal;
        if (translate!=null)
            gl.glTranslatef(translate.x, translate.y, translate.z);

        Vector2f scale = c.scaleLocal;
        if (scale!=null)
            gl.glScalef(scale.x, scale.y, 1f);
    }


    public static boolean leftButton(@NotNull short[] buttons) {
        return buttons.length == 1 && buttons[0]==1;
    }

}
