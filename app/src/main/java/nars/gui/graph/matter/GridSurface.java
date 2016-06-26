package nars.gui.graph.matter;

import nars.gui.graph.Surface;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.List;

/** TODO parameterize DX/DY to choose between row, column, or grid of arbitrary aspect ratio
       aspect ratio=0: row (x)
    aspect ratio=+inf: col (x)
                 else: grid( %x, %(ratio * x) )
 */
public class GridSurface extends Surface {

    private final MutableFloat aspect;

    public static final float HORIZONTAL = 0f;
    public static final float VERTICAL = Float.POSITIVE_INFINITY;
    public static final float GRID_SQUARE = 0.25f;

    public GridSurface(List<? extends Surface> children) {
        this(children, 0f);
    }

    public GridSurface(List<? extends Surface> children, float aspect) {
        super();
        this.aspect = new MutableFloat(aspect);
        setChildren(children);
    }


    public int length() {
        return children.size();
    }

    protected void layout() {
        int n = children.size();
        float a = aspect.floatValue();
        if (a == 0) {
            layoutLinear(1f/n, 0f);
        } else if (!Float.isFinite(a)) {
            layoutLinear(0f, 1f/n);
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

    protected void layoutLinear(float dx, float dy) {
        float x = 0;
        float y = 0;
        int n = children.size();
        for (int i = 0; i < n; i++) {
            Surface c = children.get(i);
            c.translateLocal.set(x, y, 0);
            c.scaleLocal.set(dx!=0 ? dx : 1, dy!=0 ? dy : 1);
            x += dx;
            y += dy;
        }
    }


}
