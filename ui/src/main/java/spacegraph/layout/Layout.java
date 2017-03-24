package spacegraph.layout;

import com.google.common.collect.Lists;
import com.jogamp.newt.event.KeyEvent;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.math.v3;
import spacegraph.widget.Windo;

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

    public void set(Surface... s) {
        set(Lists.newArrayList(s));
    }

    public void set(List<Surface> children) {
        if (!Objects.equals(this.children, children)) {
            if ((this.children = children)!=null)
                layout();
        }
    }

    @Override @Nullable
    public final Surface onTouch(v2 hitPoint, short[] buttons) {
        Surface x = super.onTouch(hitPoint, buttons);
        if (x!=null || children == null)
            return x;

        //2. test children reaction
        return onChildTouching(hitPoint, buttons);
    }

    protected final Surface onChildTouching(v2 hitPoint, short[] buttons) {
        if (hitPoint == null) {
            for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
                children.get(i).onTouch(null, null);
            }
            return null;
        } else {

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
                    //subHit.add(c.translateLocal.x*csx, c.translateLocal.y*csy);


                    Surface s = c.onTouch(subHit, buttons);
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
}
