package spacegraph.obj.layout;

import com.google.common.collect.Iterables;
import com.jogamp.opengl.GL2;
import nars.util.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import spacegraph.Surface;

import java.util.Collection;
import java.util.List;

import static nars.util.Util.lerp;

/** TODO parameterize DX/DY to choose between row, column, or grid of arbitrary aspect ratio
       aspect ratio=0: row (x)
    aspect ratio=+inf: col (x)
                 else: grid( %x, %(ratio * x) )
 */
public class Grid extends Layout {

    private final MutableFloat aspect = new MutableFloat(0f);

    public static final float HORIZONTAL = 0f;
    public static final float VERTICAL = Float.POSITIVE_INFINITY;
    public static final float SQUARE = 0.5f;

    float margin = 0.05f;

    public Grid(Surface... children) {
        this(SQUARE, children);
    }

    public Grid(List<Surface> children) {
        this(SQUARE, children);
    }

    public Grid(float aspect, Surface... children) {
        super();
        this.aspect.setValue(aspect);
        setChildren(children);
    }

    public Grid(float aspect, List<Surface> children) {
        super();
        this.aspect.setValue(aspect);
        setChildren(children);
    }

    /** previous scale */
    float lw, lh;
    @Override
    protected void paint(GL2 gl) {
        if (isGrid() && (lw!=scaleLocal.x || lh!=scaleLocal.y) ) {

            layout();

        }
        super.paint(gl);
    }

    public boolean isGrid() {
        float a = aspect.floatValue();
        return a!=0 && a!=Float.POSITIVE_INFINITY;
    }

    @Override
    public void layout() {

        lw = scaleLocal.x;
        lh = scaleLocal.y;

        int n = children.size();
        if (n == 0)
            return;


        float a = aspect.floatValue();
        if ((n < 3) && !((a==0) || (a == Float.POSITIVE_INFINITY)))
            a = 0; //use linear layout for small n


        if (a == 0) {
            layoutLinear(1f/n, 0f, margin);
        } else if (!Float.isFinite(a)) {
            layoutLinear(0f, 1f/n, margin);
        } else {

            //determine the ideal rows and columns of the grid to match the visible aspect ratio
            //in a way that keeps each grid cell as close to 1:1 as possible

            float actualAspect = lh/lw;

            int x;
            int s = (int)Math.sqrt(n);
            if (actualAspect > 1f) {
                x = lerp(1, s, (actualAspect-1f)/s );
            } else if (actualAspect < 1f) {
                x = lerp(s, 1, (actualAspect)/s );
            } else {
                x = s;
            }

            x = Math.max(1, x);
            int y = (int)Math.max(1, Math.ceil((float)n / x));

            layoutGrid(x, y, margin);
        }
    }

    private void layoutGrid(int nx, int ny, float margin) {
        int i = 0;
        float content = 1f - margin;

        float px;
        float py = margin/2;
        float dx = 1f/nx;
        float dxc = dx * content;
        float dy = 1f/ny;
        float dyc = dy * content;
        int n = children.size();
        //System.out.println(nx + " " + ny + " x " + dx + " " + dy);

        for (int y = ny-1; y >=0; y--) {

            px = margin/2f;

            for (int x = 0; x < nx; x++) {
                //System.out.println("\t" + px + " " + py);

                Surface c = children.get(i);

                c.translateLocal.set(px, py, 0);
                c.scale(dxc, dyc);
                c.layout();

                px += dx;

                i++;
                if (i >= n) break;
            }

            if (i >= n) break;
            py += dy;

        }
    }

    protected void layoutLinear(float dx, float dy, float margin) {
        float content = 1f - margin;
        float x = margin/2f;
        float y = margin/2f;
        float dxc = dx != 0 ? dx * content : content;
        float dyc = dy != 0 ? dy * content : content;
        int n = children.size();
        for (int i = 0; i < n; i++) {
            Surface c = children.get(i);
            c.translateLocal.set(x, y, 0);
            c.scale(dxc, dyc);
            x += dx;
            y += dy;
        }
    }


    public static Grid grid(Iterable<Surface> content) {
        return grid( Iterables.toArray(content, Surface.class ) );
    }

    public static Grid grid(Surface... content) {
        return new Grid(content);
    }

    public static Grid row(Collection<Surface> content) {
        return row(array(content));
    }
    public static Grid col(Collection<Surface> content) {
        return col(array(content));
    }

    static Surface[] array(Collection<Surface> content) {
        return content.toArray(new Surface[content.size()]);
    }

    public static Grid row(Surface... content) {
        return new Grid(HORIZONTAL, content);
    }
    public static Grid col(Surface... content) {
        return new Grid(VERTICAL, content);
    }

}
