package spacegraph.obj;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.mutable.MutableFloat;
import spacegraph.Surface;

import java.util.List;

/** TODO parameterize DX/DY to choose between row, column, or grid of arbitrary aspect ratio
       aspect ratio=0: row (x)
    aspect ratio=+inf: col (x)
                 else: grid( %x, %(ratio * x) )
 */
public class GridSurface extends LayoutSurface {

    private final MutableFloat aspect = new MutableFloat(0f);

    public static final float HORIZONTAL = 0f;
    public static final float VERTICAL = Float.POSITIVE_INFINITY;
    public static final float GRID_SQUARE = 0.25f;

    public GridSurface(Surface... children) {
        this(0f, children);
    }

    public GridSurface(List<? extends Surface> children) {
        this(children, 0f);
    }

    public GridSurface(float aspect, Surface... children) {
        super();
        this.aspect.setValue(aspect);
        setChildren(children);
    }

    public GridSurface(List<? extends Surface> children, float aspect) {
        super();
        this.aspect.setValue(aspect);
        setChildren(children);
    }



    @Override
    protected void layout() {

        int n = children.size();
        if (n == 0)
            return;

        float a = aspect.floatValue();

        float margin = 0.1f;

        if (a == 0) {
            layoutLinear(1f/n, 0f, margin);
        } else if (!Float.isFinite(a)) {
            layoutLinear(0f, 1f/n, margin);
        } else {
            //HACK: pretends a = 0.5f;
            int x = (int)Math.ceil(Math.sqrt(n));
            int y = n / x;
            layoutGrid(x, y);
        }
    }

    private void layoutGrid(int x, int y) {
        throw new UnsupportedOperationException();
    }

    protected void layoutLinear(float dx, float dy, float margin) {
        float content = 1f - margin;
        float x = margin/2f;
        float y = margin/2f;
        int n = children.size();
        for (int i = 0; i < n; i++) {
            Surface c = children.get(i);
            c.translateLocal.set(x, y, 0);
            c.scaleLocal.set(dx!=0 ? dx * content : content, dy!=0 ? dy * content : content);
            x += dx;
            y += dy;
        }
    }


}
