package spacegraph.layout;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Created by me on 7/20/16.
 */
public class Layout extends Surface {

    final AtomicBoolean mustLayout = new AtomicBoolean(true);

    public final CopyOnWriteArrayList<Surface> children = new Children();

    protected boolean clipTouchBounds = true;

    public Layout(Surface... children) {
        super();
        set(children);
    }

    public Layout(List<Surface> children) {
        set(children);
    }


    public final void layout() {
        mustLayout.set(true);
    }

    public void doLayout() {
        children.forEach(Surface::layout);
    }

    @Override
    public void print(PrintStream out, int indent) {
        super.print(out, indent);

        children.forEach(c -> {
            out.print(Texts.repeat("  ", indent + 1));
            c.print(out, indent + 1);
        });
    }

    @Override
    protected void paint(GL2 gl) {

        //TODO maybe in a separate update thread
        if (mustLayout.compareAndSet(true, false)) {
            doLayout();
        }

        List<? extends Surface> cc = children;
        for (int i = 0, childrenSize = cc.size(); i < childrenSize; i++) {
            cc.get(i).render(gl);
        }

    }


    public final Layout set(Surface... next) {
        if (!equals(this.children, next)) {
            synchronized (mustLayout) {
                children.clear();
                for (Surface c : next) {
                    if (c != null)
                        children.add(c);
                }
            }
        }
        return this;
    }

    public Layout set(List<Surface> next) {
        if (!equals(this.children, next)) {
            synchronized (mustLayout) {
                children.clear();
                children.addAll(next);
            }
        }
        return this;
    }


    @Override
    public void stop() {
        children.clear();
        super.stop();
    }


    @Override
    public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {
        Surface x = super.onTouch(finger, hitPoint, buttons);
        if (x != null)
            return x;

        //2. test children reaction
        List<Surface> cc = this.children;

        // Draw forward, propagate touch events backwards
        if (hitPoint == null) {
            for (int i = cc.size() - 1; i >= 0; i--) {
                cc.get(i).onTouch(finger, null, null);
            }
            return null;
        } else {

            for (int i = cc.size() - 1; i >= 0; i--) {
                Surface c = cc.get(i);

                //TODO factor in the scale if different from 1
                float csx = c.w();
                float csy = c.h();
                if (/*csx != csx || */csx <= 0 || /*csy != csy ||*/ csy <= 0)
                    continue;

                //project to child's space
                v2 relativeHit = new v2(finger.hit);
                relativeHit.sub(c.x(), c.y());
                relativeHit.scale(1f / csx, 1f / csy);

                //subHit.sub(tx, ty);

                float hx = relativeHit.x, hy = relativeHit.y;
                if (!clipTouchBounds || (hx >= 0f && hx <= 1f && hy >= 0 && hy <= 1f)) {
                    //subHit.add(c.translateLocal.x*csx, c.translateLocal.y*csy);

                    Surface s = c.onTouch(finger, relativeHit, buttons);

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

            List<Surface> cc = this.children;
            for (int i = 0, childrenSize = cc.size(); i < childrenSize; i++) {
                if (cc.get(i).onKey(hitPoint, charCode, pressed))
                    return true;
            }
        }
        return false;
    }

    public void forEach(Consumer<Surface> o) {
        children.forEach(o);
    }

    /**
     * identity compare
     */
    static boolean equals(List x, Object[] y) {
        int s = x.size();
        if (s != y.length) return false;
        for (int i = 0; i < s; i++) {
            if (x.get(i) != y[i])
                return false;
        }
        return true;
    }

    /**
     * identity compare
     */
    static boolean equals(List x, List y) {
        int s = x.size();
        if (s != y.size()) return false;
        for (int i = 0; i < s; i++) {
            if (x.get(i) != y.get(i))
                return false;
        }
        return true;
    }

    private class Children extends CopyOnWriteArrayList<Surface> {
        @Override
        public boolean add(Surface surface) {
            synchronized (mustLayout) {
                if (!super.add(surface)) {
                    return false;
                }
                if (surface != null) {
                    surface.start(Layout.this);
                    layout();
                }
            }
            return true;
        }

        @Override
        public Surface set(int index, Surface neww) {
            Surface old;
            synchronized (mustLayout) {
                while (size() <= index) {
                    add(null);
                }
                old = super.set(index, neww);
                if (old == neww)
                    return neww;
                else {
                    if (old != null) {
                        old.stop();
                    }
                    if (neww != null) {
                        neww.start(Layout.this);
                    }
                }
            }
            layout();
            return old;
        }

        @Override
        public boolean addAll(Collection<? extends Surface> c) {
            synchronized (mustLayout) {
                for (Surface s : c)
                    add(s);
            }
            layout();
            return true;
        }

        @Override
        public Surface remove(int index) {
            Surface x;
            synchronized (mustLayout) {
                x = super.remove(index);
                if (x == null)
                    return null;
                x.stop();
            }
            layout();
            return x;
        }

        @Override
        public boolean remove(Object o) {
            synchronized (mustLayout) {
                if (!super.remove(o))
                    return false;
                ((Surface) o).stop();
            }
            layout();
            return true;
        }


        @Override
        public void add(int index, Surface element) {
            synchronized (mustLayout) {
                super.add(index, element);
                element.start(Layout.this);
            }
            layout();
        }

        @Override
        public void clear() {
            synchronized (mustLayout) {
                this.removeIf(x -> {
                    x.stop();
                    return true;
                });
            }
            layout();
        }
    }
}
