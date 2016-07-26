package spacegraph.obj;

import spacegraph.ListSpace;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.layout.Flatten;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.constraint.DistanceConstraint;
import spacegraph.phys.constraint.Point2PointConstraint;
import spacegraph.phys.shape.BoxShape;
import spacegraph.phys.shape.CollisionShape;

import java.util.List;

import static spacegraph.math.v3.v;

/**
 * Created by me on 7/23/16.
 */
public class TestP2PNode extends RectWidget {

    public TestP2PNode(float w, float h) {
        super(new GridSurface(new ConsoleSurface(new ConsoleSurface.DummyTerminal(10, 4))), w, h);
    }

    public static class TestP2PLink extends RectWidget {

        private final SimpleSpatial a;
        private final SimpleSpatial b;
        private DistanceConstraint p2p;

        public TestP2PLink(SimpleSpatial<?> a, SimpleSpatial<?> b) {
            super(new XYSlider(), 10, 1);
            this.a = a;
            this.b = b;

        }

        @Override
        public boolean collidable() {
            return false;
        }

        @Override
        protected void next(Dynamics world) {
            v3 c = new v3(a.center);
            c.add(b.center);
            c.scale(0.5f);
            move(c);

            float dist = v3.dist(a.center, b.center);
            scale(dist, 1, 1);

            a.body.activate();

            b.body.activate();
        }


        @Override
        protected List<Collidable> enter(Dynamics world) {
            List l = super.enter(world);

            float w = ((BoxShape) body.shape()).x();

            p2p = new DistanceConstraint(
                    a.body, b.body, 5f, 0.75f, 0.1f, 0.75f
            );
            world.addConstraint(p2p, true);

//            world.addConstraint(new Point2PointConstraint(
//                        body, a.body, v(+w / 2, 0, 0), v(-a.radius / 2f, 0, 0)
//                ), true);
//                world.addConstraint(new Point2PointConstraint(
//                        body, b.body, v(-w / 2, 0, 0), v(+a.radius / 2f, 0, 0)
//                ), true);


            //            float w = ((BoxShape)body.shape()).x();
//            v3 axis = v(0, 0, 1);
//            s.dyn.addConstraint(
//                new HingeConstraint(body, a.body, v(+w/2,0,0), v(-a.radius/2f,0,0), axis, axis
//            ), true);
//            s.dyn.addConstraint(
//                new HingeConstraint(body, b.body, v(-w/2,0,0), v(+b.radius/2f,0,0), axis, axis
//            ), true);


            return l;
        }


    }

    public static void main(String[] args) {
        SpaceGraph s = new SpaceGraph<>();

        TestP2PNode a, b;
        a = new TestP2PNode(5, 3);
        a.move(2, 0, 0);
        b = new TestP2PNode(3, 4);

        TestP2PLink ab = new TestP2PLink(a, b);

        s.add(a, b, ab);
        s.add(new ListSpace().with(new Flatten())); //HACK TODO make a Transform-only space input

        s.show(1000, 800)
                .setWindowDestroyNotifyAction(() -> System.exit(1));

    }

}
