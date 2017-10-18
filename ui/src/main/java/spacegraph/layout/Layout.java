package spacegraph.layout;

import com.google.common.collect.Lists;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.NotNull;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by me on 7/20/16.
 */
public class Layout<S extends Surface> extends Surface {

    public List<S> children;

    protected boolean clipTouchBounds = true;

    public Layout(S... children) {
        this(Lists.newArrayList(children));
    }

    public Layout(List<S> children) {
        set(children);
    }

    public List<S> children() {
        return children;
    }

    @Override
    protected void paint(GL2 gl) {

        List<? extends Surface> cc = children;
        for (int i = 0, childrenSize = cc.size(); i < childrenSize; i++) {
            cc.get(i).render(gl);
        }

    }

    public final Layout set(S... s) {
        set(Lists.newArrayList(s));
        return this;
    }

    public Layout<S> set(List<S> next) {
        synchronized (scale) {

            if (!Objects.equals(this.children, next)) {

                List<S> existing = this.children;
                if (existing != null) {
                    for (Surface x : existing)
                        if (!next.contains(x)) {
                            remove(x);
                        }
                }

                this.children = next;


                for (Surface x : next) {
                    if (x != null) {
                        if (x.parent != this)
                            add(x);
                    }
                }

                layout();
            }
        }
        return this;
    }

    private void add(Surface x) {
        x.start(this);
    }

    private static void remove(Surface x) {
        x.stop();
    }

    @Override
    public void stop() {
        synchronized (scale) {
            children.forEach(Surface::stop);
            children = null;
            super.stop();
        }

    }

    @Override
    public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {
        Surface x = super.onTouch(finger, hitPoint, buttons);
        if (x != null)
            return x;

        //2. test children reaction
        List<S> cc = this.children;

        // Draw forward, propagate touch events backwards
        if (hitPoint == null) {
            for (int i = cc.size()-1; i >=0; i--) {
                cc.get(i).onTouch(finger, null, null);
            }
            return null;
        } else {

            for (int i = cc.size()-1; i >=0; i--) {
                Surface c = cc.get(i);

                v2 sc = c.scale;
                float csx = sc.x;
                float csy = sc.y;
                if (/*csx != csx || */csx <= 0 || /*csy != csy ||*/ csy <= 0)
                    continue;

                //project to child's space
                v2 subHit = new v2(hitPoint);
                subHit.sub(c.pos.x, c.pos.y);

                subHit.scale(1f / csx, 1f / csy);

                //subHit.sub(tx, ty);

                float hx = subHit.x, hy = subHit.y;
                if (!clipTouchBounds || (hx >= 0f && hx <= 1f && hy >= 0 && hy <= 1f)) {
                    //subHit.add(c.translateLocal.x*csx, c.translateLocal.y*csy);

                    Surface s = c.onTouch(finger, subHit, buttons);

                    if (s != null)
                        return s; //FIFO
                }
            }
        }

        return this;
    }

    @Override
    public boolean onKey(KeyEvent e, boolean pressed) {
        if (!super.onKey(e, pressed)) {

            for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                if (children.get(i).onKey(e, pressed))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(hitPoint, charCode, pressed)) {

            List<S> cc = this.children;
            for (int i = 0, childrenSize = cc.size(); i < childrenSize; i++) {
                if (cc.get(i).onKey(hitPoint, charCode, pressed))
                    return true;
            }
        }
        return false;
    }

    public void forEach(Consumer<S> o) {
        children.forEach(o);
    }

}
