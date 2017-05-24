package spacegraph.layout;

import com.google.common.collect.Iterables;
import com.jogamp.opengl.GL2;
import spacegraph.Surface;
import spacegraph.math.v2;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static jcog.Util.lerp;

/** TODO parameterize DX/DY to choose between row, column, or grid of arbitrary aspect ratio
       aspect ratio=0: row (x)
    aspect ratio=+inf: col (x)
                 else: grid( %x, %(ratio * x) )
 */
public class Grid<S extends Surface> extends Layout<S> {


    public static final float HORIZONTAL = 0f;
    public static final float VERTICAL = Float.POSITIVE_INFINITY;
    public static final float SQUARE = 0.5f;

    float margin = 0.05f;
    float gridAspect = Float.NaN;

    public Grid(S... children) {
        this(SQUARE, children);
    }

    public Grid(List<? extends S> children) {
        this(SQUARE, children);
    }

    public Grid(float aspect, S... children) {
        super();
        this.gridAspect = (aspect);
        set(children);
    }

    public Grid(float aspect, List<? extends S> children) {
        super();
        this.gridAspect = (aspect);
        set(children);
    }


    /** previous scale */
    float lw, lh;


    public boolean isGrid() {
        float a = gridAspect;
        return a!=0 && a!=Float.POSITIVE_INFINITY;
    }

    @Override
    public void transform(GL2 gl, v2 globalScale) {
        super.transform(gl, globalScale);

        if (!children.isEmpty() && isGrid())  {
            float xx = scaleLocal.x * globalScale.x;
            float yy = scaleLocal.y * globalScale.y;
            //if ((lw != xx) || (lh != yy)) {
                layout();
                lw = xx;
                lh = yy;
            //}
        }
    }

    @Override
    public void layout() {
        super.layout();

        int n = children.size();
        if (n == 0)
            return;

        float a = gridAspect;
//        if ((n < 3) && !((a==0) || (a == Float.POSITIVE_INFINITY)))
//            a = 0; //use linear layout for small n


        float aa = a;

        if (a!=0 && Float.isFinite(a)) {

            //determine the ideal rows and columns of the grid to match the visible aspect ratio
            //in a way that keeps each grid cell as close to 1:1 as possible

            //TODO use the 'a' value to adjust the x/y balance, currently it is not

            float actualAspect = lh/lw;

            int x;
            int s = (int)Math.sqrt(n);
            if (actualAspect > 1f) {
                x = Math.round(lerp((actualAspect)/n, 1f, s));
            } else if (actualAspect < 1f) {
                //TODO fix
                x = Math.round(lerp(1f-(1f/actualAspect)/n, (float)s, n));
            } else {
                x = s;
            }

            x = Math.max(1, x);
            int y = (int)Math.max(1, Math.ceil((float)n / x));

            if (y==1) {
                aa = Float.POSITIVE_INFINITY; //column
            } else if (x == 1) {
                aa = 0; //row
            } else {
                layoutGrid(x, y, margin);
                return;
            }
        }


        if (aa == 0) {
            //horizontal
            layoutGrid(n, 1, margin);
        } else /*if (!Float.isFinite(aa))*/ {
            //vertical
            layoutGrid(1, n, margin);
        }

    }

    private void layoutGrid(int nx, int ny, float margin) {
        int i = 0;

        float hm = margin/2f;

        float mx = (1 + 1 + nx/2f) * hm;
        float my = (1 + 1 + ny/2f) * hm;

        float dx = nx > 0 ? (1f-hm)/nx : 0;
        float dxc = (1f - mx)/nx;
        float dy = ny > 0 ? (1f-hm)/ny : 0;
        float dyc = (1f - my)/ny;

        int n = children.size();

        float py = ((ny-1) * dy) + hm;

        for (int y = 0; y < ny; y++) {

            float px = hm;//margin / 2f;

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

    public static Grid grid(Iterable<? extends Surface> content) {
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
