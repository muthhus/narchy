package nars.gui.graph;

import com.jogamp.opengl.GL2;
import jcog.pri.Pri;
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
import spacegraph.space.Cuboid;
import spacegraph.space.EDraw;

import java.util.function.Consumer;

abstract public class TermWidget<T extends Termed> extends Cuboid<T> {


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
        return id.op().atomic ? new SphereShape(1) : super.newShape() /* cube */;
    }

    abstract public Iterable<? extends EDraw<?>> edges();

    public void commit(TermWidget.TermVis vis, TermSpace<T> space) {
        vis.accept(this);
    }


    @Override
    public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
        Surface s = super.onTouch(body, hitPoint, buttons, space);
        if (s != null) {
        }

//        if (buttons.length > 0 && buttons[0] == 1) {
//            window(Vis.reflect(id), 800, 600);
//        }

        return s;
    }


    public static void render(GL2 gl, SimpleSpatial src, Iterable<? extends EDraw> ee) {

        Quat4f tmpQ = new Quat4f();
        ee.forEach(e -> {
            if (e.a < Pri.EPSILON)
                return;

            float width = e.width;
            float thresh = 0.1f;
            if (width <= thresh) {
                gl.glColor4f(e.r, e.g, e.b, e.a * (width / thresh) /* fade opacity */);
                Draw.renderLineEdge(gl, src, e, width);
            } else {
                Draw.renderHalfTriEdge(gl, src, e, width / 9f, e.r * 2f /* hack */, tmpQ);
            }
        });
    }

    @Override
    public void renderAbsolute(GL2 gl) {
        render(gl, this, edges());
    }

    public interface TermVis<X extends TermWidget> extends Consumer<X> {

    }

}
