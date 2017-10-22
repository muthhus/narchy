package spacegraph.layout;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.list.FasterList;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by me on 7/20/16.
 */
public class Layout extends Surface {

    public List<Surface> children;

    protected boolean clipTouchBounds = true;

    public Layout(Surface... children) {
        super();
        set(children);
    }

    public Layout(List<Surface> children) {
        set(children);
    }

    public List<Surface> children() {
        return children;
    }

    @Override
    public void layout() {
        children.forEach(Surface::layout);
    }

    @Override
    public void print(PrintStream out, int indent) {
        super.print(out, indent);

        children.forEach(c -> {
            out.print(Texts.repeat("  ", indent+1));
            c.print(out, indent + 1);
        });
    }

    @Override
    protected void paint(GL2 gl) {

        List<? extends Surface> cc = children;
        for (int i = 0, childrenSize = cc.size(); i < childrenSize; i++) {
            cc.get(i).render(gl);
        }

    }

    public final Layout set(Surface... s) {
        set(new FasterList(s));
        return this;
    }

    public Layout set(List<Surface> next) {
        synchronized (scale) {

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

                if (parent!=null)
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
    public Surface scale(float x, float y) {
        return super.scale(x, y);
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
            for (int i = cc.size()-1; i >=0; i--) {
                cc.get(i).onTouch(finger, null, null);
            }
            return null;
        } else {

            for (int i = cc.size()-1; i >=0; i--) {
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

}
