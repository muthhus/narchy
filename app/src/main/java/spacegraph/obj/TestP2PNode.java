package spacegraph.obj;

import com.jogamp.nativewindow.WindowClosingProtocol;
import spacegraph.ListInput;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.layout.Flatten;
import spacegraph.math.v3;
import spacegraph.phys.constraint.HingeConstraint;
import spacegraph.phys.constraint.Point2PointConstraint;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.shape.BoxShape;

import static spacegraph.math.v3.v;

/**
 * Created by me on 7/23/16.
 */
public class TestP2PNode extends RectWidget {

    public TestP2PNode(float w, float h) {
        super(new GridSurface(new ConsoleSurface(new ConsoleSurface.DummyTerminal(10,4))), w, h);
    }

    public static class TestP2PLink extends RectWidget {

        private final Spatial a;
        private final Spatial b;

        public TestP2PLink(Spatial<?> a, Spatial<?> b) {
            super(new XYSlider(), 10, 1);
            this.a = a;
            this.b = b;

        }

        @Override
        public void updateStart(SpaceGraph s) {
            super.updateStart(s);
//            s.dyn.addConstraint(new Point2PointConstraint(
//                a.body, b.body, v(), v()
//            ));

            float w = ((BoxShape)body.shape()).x();

            v3 axis = v(0, 0, 1);

            s.dyn.addConstraint(
                new HingeConstraint(body, a.body, v(+w/2,0,0), v(-a.radius/2f,0,0), axis, axis
            ), true);
            s.dyn.addConstraint(
                new HingeConstraint(body, b.body, v(-w/2,0,0), v(+b.radius/2f,0,0), axis, axis
            ), true);
        }

    }

    public static void main(String[] args) {
        SpaceGraph s = new SpaceGraph<>();

        TestP2PNode a, b;
        a = new TestP2PNode(5,3);
        b = new TestP2PNode(3,4);

        TestP2PLink ab = new TestP2PLink(a, b);

        s.addAll(a, b, ab);
        s.add(new ListInput().with(new Flatten())); //HACK TODO make a Transform-only space input

        s.show(1000, 800)
                .setWindowDestroyNotifyAction(()->System.exit(1));

    }

}
