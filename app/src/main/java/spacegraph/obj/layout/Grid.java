package spacegraph.obj.layout;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.mutable.MutableFloat;
import spacegraph.Surface;

import java.util.Collection;
import java.util.List;

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



    @Override
    public void layout() {

        int n = children.size();
        if (n == 0)
            return;


        float a = aspect.floatValue();
        if ((n < 3) && !((a==0) || (a == Float.POSITIVE_INFINITY)))
            a = 0; //use linear layout for small n

        float margin = 0.05f;

        if (a == 0) {
            layoutLinear(1f/n, 0f, margin);
        } else if (!Float.isFinite(a)) {
            layoutLinear(0f, 1f/n, margin);
        } else {
            //HACK: pretends a = 0.5f;
            int x = (int)Math.max(1, Math.ceil(Math.sqrt(n)));
            int y = (int)Math.max(1, Math.ceil((float)n / x));
            layoutGrid(x, y, margin);
        }
    }

    private void layoutGrid(int nx, int ny, float margin) {
        int i = 0;
        float content = 1f - margin;

        float px, py = margin/2f;
        float dx = 1f/nx;
        float dxc = dx * content;
        float dy = 1f/ny;
        float dyc = dy * content;
        int n = children.size();
        //System.out.println(nx + " " + ny + " x " + dx + " " + dy);

        for (int y = 0; y < ny; y++) {

            px = margin/2f;

            for (int x = 0; x < nx; x++) {
                //System.out.println("\t" + px + " " + py);

                Surface c = children.get(i);

                c.translateLocal.set(px, py, 0);
                c.scaleLocal.set(dxc, dyc);
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
            c.scaleLocal.set(dxc, dyc);
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
