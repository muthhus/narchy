package spacegraph.layout;

import com.google.common.collect.Lists;
import com.jogamp.newt.event.KeyEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.math.v3;
import spacegraph.widget.Windo;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Created by me on 7/20/16.
 */
 public class Layout extends Surface {

    public List<Surface> children;

    protected boolean clipTouchBounds = true;

    public Layout(Surface... children) {
        this(Lists.newArrayList(children));
    }

    public Layout(List<Surface> children) {
        set(children);
    }


    @Override
    public List<Surface> children() {
        return children;
    }

    public final void set(Surface... s) {
        set(Lists.newArrayList(s));
    }

    public void set(@NotNull List<Surface> next) {
        synchronized (scaleLocal) {

            if (!Objects.equals(this.children, next)) {

                List<Surface> existing = this.children;
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
    }

    private void add(Surface x) {
        x.start(this);
    }

    private void remove(Surface x) {
        x.stop();
    }

    @Override
    public void stop() {
        if (children!=null) {
            //synchronized (scaleLocal) {
                children.forEach(Surface::stop);
                children = null;
                super.stop();
            //}
        }
    }

    @Override @Nullable
    public final Surface onTouch(Finger f) {

        Surface x = super.onTouch(f);
        if (x!=null || children == null)
            return x;

        //2. test children reaction
        return onChildTouching(f);
    }

    protected final Surface onChildTouching(Finger f) {
        if ((f == null) || (f.nextHit == null)) {
            for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                children.get(i).onTouch(null);
            }
            return null;
        } else {
            v2 hitPoint = f.nextHit;

            for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                Surface c = children.get(i);

                v2 sc = c.scaleLocal;
                float csx = sc.x;
                float csy = sc.y;
                if (/*csx != csx || */csx <= 0 || /*csy != csy ||*/ csy <= 0)
                    continue;

                v3 tc = c.translateLocal;

                //project to child's space
                v2 subHit = new v2(hitPoint);
                subHit.sub(tc.x, tc.y);

                subHit.scale(1f / csx, 1f / csy);

                //subHit.sub(tx, ty);

                float hx = subHit.x, hy = subHit.y;
                if (!clipTouchBounds || (hx >= 0f && hx <= 1f && hy >= 0 && hy <= 1f)) {

                    f.nextHit = subHit; //HACK save

                    Surface s = c.onTouch(f);

                    f.nextHit = hitPoint; //HACK restore

                } else {
                    c.onTouch(null);
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
}
