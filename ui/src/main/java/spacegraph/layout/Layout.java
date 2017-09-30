package spacegraph.layout;

import com.google.common.collect.Lists;
import com.jogamp.newt.event.KeyEvent;
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

    public List<? extends S> children;

    protected boolean clipTouchBounds = true;

    public Layout(S... children) {
        this(Lists.newArrayList(children));
    }

    public Layout(List<? extends S> children) {
        set(children);
    }


    @Override
    public List<? extends S> children() {
        return children;
    }

    public final void set(S... s) {
        set(Lists.newArrayList(s));
    }

    public Layout<S> set(@NotNull List<? extends S> next) {
        synchronized (scale) {

            if (!Objects.equals(this.children, next)) {

                List<? extends S> existing = this.children;
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
            if (children!=null) {
                children.forEach(Surface::stop);
                children = null;
            }
            super.stop();
        }

    }

    @Override
    public final Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {
        Surface x = super.onTouch(finger, hitPoint, buttons);
        if (x!=null || children == null)
            return x;

        //2. test children reaction
        if (hitPoint == null) {
            for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                children.get(i).onTouch(finger, null, null);
            }
            return null;
        } else {

            for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                Surface c = children.get(i);

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
            if (children != null) {
                for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                    if (children.get(i).onKey(e, pressed))
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onKey(v2 hitPoint, char charCode, boolean pressed) {
        if (!super.onKey(hitPoint, charCode, pressed)) {
            if (children != null) {
                for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                    if (children.get(i).onKey(hitPoint, charCode, pressed))
                        return true;
                }
            }
        }
        return false;
    }

    public void forEach(Consumer<S> o) {
        children.forEach(o::accept);
    }

}
