package spacegraph.obj.layout;

import com.google.common.collect.Iterables;
import com.jogamp.opengl.GL2;
import spacegraph.Surface;
import spacegraph.math.v2;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static nars.util.Util.lerp;

/** TODO parameterize DX/DY to choose between row, column, or grid of arbitrary aspect ratio
       aspect ratio=0: row (x)
    aspect ratio=+inf: col (x)
                 else: grid( %x, %(ratio * x) )
 */
public class Grid extends Layout {


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
        this.aspect = (aspect);
        setChildren(children);
    }

    public Grid(float aspect, List<Surface> children) {
        super();
        this.aspect = (aspect);
        setChildren(children);
    }

    @Override
    public void transform(GL2 gl, v2 globalScale) {
        super.transform(gl, globalScale);

        if (!children.isEmpty() && isGrid())  {
            float xx = scaleLocal.x * globalScale.x;
            float yy = scaleLocal.y * globalScale.y;
            if ((lw != xx) || (lh != yy)) {
                layout();
                lw = xx;
                lh = yy;
            }
        }
    }

    /** previous scale */
    float lw, lh;
    @Override
    protected void paint(GL2 gl) {
        super.paint(gl);
    }

    public boolean isGrid() {
        float a = aspect;
        return a!=0 && a!=Float.POSITIVE_INFINITY;
    }

    @Override
    public void layout() {

        //lw = lh = Float.NaN; //invalidate to trigger next transform update

        int n = children.size();
        if (n == 0)
            return;

        float a = aspect;
        if ((n < 3) && !((a==0) || (a == Float.POSITIVE_INFINITY)))
            a = 0; //use linear layout for small n


        if (a == 0) {
            //horizontal
            layoutLinear(1f/n, 0f, margin, 0, n);
        } else if (!Float.isFinite(a)) {
            //vertical
            layoutLinear(0f, 1f/n, margin, n-1, -1);
        } else {

            //determine the ideal rows and columns of the grid to match the visible aspect ratio
            //in a way that keeps each grid cell as close to 1:1 as possible

            float actualAspect = lh/lw;

            int x;
            int s = (int)Math.sqrt(n);
            if (actualAspect > 1f) {
                x = Math.round(lerp(1f, s, (actualAspect)/n ));
            } else if (actualAspect < 1f) {
                //TODO fix
                x = Math.round(lerp((float)s, n, 1f-(1f/actualAspect)/n ));
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

        float dx = 1f/nx;
        float dxc = dx * content;
        float dy = 1f/ny;
        float dyc = dy * content;
        float py = ((ny-1) * dy) + margin/2;
        int n = children.size();
        //System.out.println(nx + " " + ny + " x " + dx + " " + dy);

        for (int y = 0; y < ny; y++) {

            float px = margin / 2f;

            for (int x = 0; x < nx; x++) {
                //System.out.println("\t" + px + " " + py);

                Surface c = children.get(i);

                c.pos(px, py);
                c.scale(dxc, dyc);
                c.layout();

                px += dx;

                i++;
                if (i >= n) break;
            }

            if (i >= n) break;
            py -= dy;

        }
    }

    protected void layoutLinear(float dx, float dy, float margin, int start, int end) {
        float content = 1f - margin;
        float x = margin/2f;
        float y = margin/2f;
        float dxc = dx != 0 ? dx * content : content;
        float dyc = dy != 0 ? dy * content : content;

        int inc = start > end ? -1 : +1;
        for (int i = start; i != end; i+=inc) {
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
    public static <S> Grid grid(Collection<S> c, Function<S,Surface> builder) {
        Surface ss [] = new Surface[c.size()];
        int i = 0;
        for (S x : c) {
            ss[i++] = builder.apply(x);
        }
        return grid(ss);
    }

    public static Grid row(Collection<? extends Surface> content) {
        return row(array(content));
    }
    public static Grid col(Collection<? extends Surface> content) {
        return col(array(content));
    }

    static Surface[] array(Collection<? extends Surface> content) {
        return content.toArray(new Surface[content.size()]);
    }

    public static Grid row(Surface... content) {
        return new Grid(HORIZONTAL, content);
    }
    public static Grid col(Surface... content) {
        return new Grid(VERTICAL, content);
    }

}
