package nars.gui.graph;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.pri.Deleteable;
import jcog.pri.Pri;
import nars.concept.Concept;
import nars.gui.TermIcon;
import nars.term.Term;
import nars.term.Termed;
import spacegraph.SimpleSpatial;
import spacegraph.Surface;
import spacegraph.math.Quat4f;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamics;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.render.Draw;
import spacegraph.render.JoglPhysics;
import spacegraph.space.Cuboid;
import spacegraph.space.EDraw;

public class TermWidget extends Cuboid<Termed> {


    //caches a reference to the current concept
    public Concept concept;
    public float pri;
    protected transient TermSpace space;

    public TermWidget(Termed x, float w, float h) {
        super(x, w, h);

        setFront(
//            /*col(
                //new Label(x.toString())
//                row(new FloatSlider( 0, 0, 4 ), new BeliefTableChart(nar, x))
//                    //new CheckBox("?")
//            )*/
                new TermIcon(x)
        );

    }

    public void commit(ConceptWidget.TermVis vis, TermSpace space) {

        this.space = space;
    }


    @Override
    public void delete(Dynamics dyn) {
        concept = null;
        super.delete(dyn);
        //edges.setCapacity(0);
    }

    @Override
    public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
        Surface s = super.onTouch(body, hitPoint, buttons, space);
        if (s != null) {
        }

        if (buttons.length > 0 && buttons[0] == 1) {
            if (concept != null)
                concept.print();
        }

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

    public static class TermEdge extends EDraw<TermWidget> implements Termed, Deleteable {

        float termlinkPri, tasklinkPri;

        private final int hash;

        public TermEdge(TermWidget target) {
            super(target);
            this.hash = target.key.hashCode();
        }


        protected void decay(float rate) {
            //termlinkPri = tasklinkPri = 0;

            //decay
            termlinkPri *= rate;
            tasklinkPri *= rate;
        }


        public void add(float p, boolean termOrTask) {
            if (termOrTask) {
                termlinkPri += p;
            } else {
                tasklinkPri += p;
            }
        }

        @Override
        public Term term() {
            return id.key.term();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || id.key.equals(o);
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        public void update(ConceptWidget src, float conceptEdgePriSum, float termlinkBoost, float tasklinkBoost) {

            float edgeSum = (termlinkPri + tasklinkPri);


            if (edgeSum >= 0) {

                //float priAvg = priSum/2f;

                float minLineWidth = 0.1f;

                final float MaxEdgeWidth = 4;

                this.width = minLineWidth + Util.sqr(1 + pri * MaxEdgeWidth);

                //z.r = 0.25f + 0.7f * (pri * 1f / ((Term)target.key).volume());
//                float qEst = ff.qua();
//                if (qEst!=qEst)
//                    qEst = 0f;


                this.r = 0.05f + 0.9f * (tasklinkPri / edgeSum);
                this.g = 0.05f + 0.9f * (termlinkPri / edgeSum);
                this.b = 0.5f * (1f - (r + g) / 2f);


                this.a = 0.05f + 0.9f * Util.and(this.r * tasklinkBoost, this.g * termlinkBoost);

                this.attraction = 0.01f * width;// + priSum * 0.75f;// * 0.5f + 0.5f;
                this.attractionDist = 1f + 2 * src.radius() + id.radius(); //target.radius() * 2f;// 0.25f; //1f + 2 * ( (1f - (qEst)));
            } else {
                this.a = -1;
                this.attraction = 0;
            }

        }

        @Override
        public boolean isDeleted() {
            return super.isDeleted() || id.active();
        }
    }
}
