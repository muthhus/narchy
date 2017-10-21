package nars.gui.graph;

import com.jogamp.opengl.GL2;
import jcog.pri.Pri;
import nars.gui.DynamicListSpace;
import nars.gui.TermIcon;
import nars.term.Termed;
import spacegraph.SimpleSpatial;
import spacegraph.Surface;
import spacegraph.math.Quat4f;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.SphereShape;
import spacegraph.render.Draw;
import spacegraph.render.JoglPhysics;
import spacegraph.render.JoglSpace;
import spacegraph.space.Cuboid;
import spacegraph.space.EDraw;

import java.util.List;
import java.util.function.Consumer;

abstract public class TermWidget<T extends Termed> extends Cuboid<T> {

    boolean touched = false;

    public TermWidget(T x) {
        super(x, 1, 1);

        setFront(
//            /*col(
                //new Label(x.toString())
//                row(new FloatSlider( 0, 0, 4 ), new BeliefTableChart(nar, x))
//                    //new CheckBox("?")
//            )*/
                new TermIcon(x)
        );

    }

    @Override
    protected CollisionShape newShape() {
        return id.op().atomic ? new SphereShape() : super.newShape() /* cube */;
    }

    abstract public Iterable<? extends EDraw<?>> edges();

    public void commit(TermWidget.TermVis vis, DynamicListSpace<T, TermWidget<T>> space) {
        vis.accept(this);
    }

    @Override
    public void onUntouch(JoglSpace space) {
        touched = false;
    }

    @Override
    public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
        Surface s = super.onTouch(body, hitPoint, buttons, space);
        if (s != null) {
        }

        touched = true;

//        if (buttons.length > 0 && buttons[0] == 1) {
//            window(Vis.reflect(id), 800, 600);
//        }

        return s;
    }


    public static void render(GL2 gl, SimpleSpatial src, float twist, Iterable<? extends EDraw> ee) {

        Quat4f tmpQ = new Quat4f();
        ee.forEach(e -> {
            if (e.a < Pri.EPSILON)
                return;

            float width = e.width;
            float thresh = 0.1f;
            if (width <= thresh) {
                gl.glColor4f(e.r, e.g, e.b, e.a * (width / thresh) /* fade opacity */);
                Draw.renderLineEdge(gl, src, e.tgt(), width);
            } else {
                Draw.renderHalfTriEdge(gl, src, e, width, twist, tmpQ);
            }
        });
    }

    @Override
    public void renderAbsolute(GL2 gl, long timeMS) {
        render(gl, this, timeMS / 100f, edges());

        if (touched) {
            gl.glPushMatrix();
            gl.glTranslatef(x(), y(), z());
            float r = radius() * 2f;
            gl.glScalef(r, r, r);
            Draw.drawCoordSystem(gl);
            gl.glPopMatrix();
        }
    }

    public interface TermVis<X extends TermWidget> extends Consumer<X> {

        /**
         * called after all nodes have been sent through
         * @param pending
         */
        default public void update(List<ConceptWidget> pending) {

        }
    }

}
