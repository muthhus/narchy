package spacegraph.obj;

import com.jogamp.opengl.GL2;
import nars.$;
import nars.gui.ConceptMaterializer;
import nars.gui.ConceptWidget;
import nars.term.Termed;
import nars.util.Util;
import nars.util.data.list.CircularArrayList;
import spacegraph.*;
import spacegraph.phys.collision.shapes.BoxShape;
import spacegraph.phys.collision.shapes.CollisionShape;
import spacegraph.phys.collision.shapes.ConvexInternalShape;
import spacegraph.phys.dynamics.RigidBody;

import javax.vecmath.Vector3f;
import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static javax.vecmath.Vector3f.v;

/**
 * Created by me on 7/9/16.
 */
public class Physiconsole extends ListInput<Object, Spatial<Object>> implements Function<Object, Spatial<Object>> {


    CircularArrayList<Object> buffer = new CircularArrayList(256);

    public boolean needsLayout;

    public Physiconsole() {

    }

//    public static class LineBlock extends Spatial {
//        public LineBlock() {
//
//        }
//    }

    public void println(String line) {
        append(line);
    }

    public void append(Object s) {
        buffer.add(s);
        commit(buffer.toArray(new Object[buffer.size()])); //HACK todo optimize
    }

    @Override
    public void commit(Object[] xx) {
        super.commit(xx);
        needsLayout = true;
    }

    @Override
    public void start(SpaceGraph space) {
        super.start(space);
        space.with(new SpaceTransform() {

            @Override
            public void update(SpaceGraph g, List verts, float dt) {
                if (needsLayout) {
                    layout();
                    needsLayout = false;
                }
            }
        });
    }

    protected void layout() {
        float x = 0, y = 0, z = 0;


        float marginY = 0.5f;

        System.out.println("layout " + visible.size());
        for (Spatial v : visible) {
            RigidBody body = v.body;
            if (body == null)
                continue;

            Vector3f vs = ((ConvexInternalShape) body.shape()).implicitShapeDimensions;
            System.out.println(x + " " + y);
            float r = 2f;
            float width = vs.x * r;
            float height = vs.y * r;
            v.move(width/2f, y, z);
            y += height + marginY;
        }
    }

    public static void main(String[] args) {
        Physiconsole p = new Physiconsole();
        SpaceGraph<?> s = new SpaceGraph(p, p);

        p.println("1");
        p.println("XY");
        p.println("abc");
        p.println("abcd");
        p.println("abcdGv");
        p.println("abcdefz");


//                (List<Surface> vt) -> new SurfaceMount<>(null,
//                        new GridSurface(vt, GridSurface.VERTICAL)),
//
//                newArrayList(
//                        new GridSurface(newArrayList(
//                                new XYPadSurface(),
//                                new XYPadSurface()
//                        ), GridSurface.HORIZONTAL),
//                        new GridSurface(newArrayList(
//                                new SliderSurface(0.75f,  0, 1),
//                                new SliderSurface(0.25f,  0, 1),
//                                new SliderSurface(0.5f,  0, 1)
//                        ), GridSurface.VERTICAL)
//                )
//        );
//
//        s.add(new Facial(new ConsoleSurface(new ConsoleSurface.DummyTerminal(80, 25))).scale(500f, 400f));
//        s.add(new Facial(new CrosshairSurface(s)));


        s.show(800, 800);
    }


    @Override
    protected void updateImpl() {

    }

    @Override
    public float now() {
        return 0;
    }

    @Override
    public Spatial apply(Object x) {
        String s = x.toString();

        float cAspect = 2f;
        float sx = s.length()/cAspect;
        float sy = 1f;

        ConceptWidget w = new ConceptWidget($.the(s), 0) {
            protected CollisionShape newShape() {

                return new BoxShape(v(sx, sy, 0.1f));
            }

            @Override
            protected void colorshape(GL2 gl) {
                gl.glColor3f(0.1f,0.1f,0.1f);
            }
        };


        return w;
    }
}
