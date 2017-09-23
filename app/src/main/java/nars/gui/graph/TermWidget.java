package nars.gui.graph;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.pri.Deleteable;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.concept.Concept;
import nars.gui.TermIcon;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.render.Draw;
import spacegraph.render.JoglPhysics;
import spacegraph.space.Cuboid;
import spacegraph.space.EDraw;

import java.util.LinkedHashMap;

import static spacegraph.math.v3.v;

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
            if (concept!=null)
                concept.print();
        }

        return s;
    }


    public void render(@NotNull GL2 gl, @NotNull EDraw e) {


        float width = e.width;
        float thresh = 0.1f;
        if (width <= thresh) {
            gl.glColor4f(e.r, e.g, e.b, e.a * (width / thresh) /* fade opacity */);
            Draw.renderLineEdge(gl, this, e, width);
        } else {
            gl.glColor4f(e.r, e.g, e.b, e.a);
            Draw.renderHalfTriEdge(gl, this, e, width / 9f, e.r * 2f /* hack */);
        }
    }

    public static class TermEdge extends EDraw<Termed, TermWidget> implements Termed, Deleteable {

        float termlinkPri, tasklinkPri;

        private final int hash;

        public TermEdge(@NotNull TermWidget target) {
            super(target);
            this.hash = target.key.hashCode();
        }

        protected void decay(float rate) {
            //termlinkPri = tasklinkPri = 0;

            //decay
            termlinkPri *= rate;
            tasklinkPri *= rate;
        }


        public void add(PriReference b, boolean termOrTask) {
            float p = b.priElseZero();
            if (termOrTask) {
                termlinkPri += p;
            } else {
                tasklinkPri += p;
            }
        }

        @Override
        public Term term() {
            return target.key.term();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || target.key.equals(o);
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        public void update(ConceptWidget src, float conceptEdgePriSum, float termlinkBoost, float tasklinkBoost) {

            float edgeSum = (termlinkPri + tasklinkPri);


            if (edgeSum >= 0) {

                //float priAvg = priSum/2f;

                float minLineWidth = 1f;
                float priToWidth = 3f;

                float widthSqrt = priToWidth * edgeSum;
                final float MaxEdgeWidth = 10;
                this.width = Math.min(MaxEdgeWidth, Util.sqr(minLineWidth + widthSqrt));

                //z.r = 0.25f + 0.7f * (pri * 1f / ((Term)target.key).volume());
//                float qEst = ff.qua();
//                if (qEst!=qEst)
//                    qEst = 0f;


                if (edgeSum > 0) {
                    this.b = 0.1f;
                    this.r = 0.1f + 0.8f * (tasklinkPri / edgeSum);
                    this.g = 0.1f + 0.8f * (termlinkPri / edgeSum);
                } else {
                    this.r = this.g = this.b = 0.1f;
                }

                this.a = Util.and(this.r * tasklinkBoost, this.g * termlinkBoost);

                this.attraction = 0.1f * width;// + priSum * 0.75f;// * 0.5f + 0.5f;
                this.attractionDist = 1f + 2 * src.radius() + target.radius(); //target.radius() * 2f;// 0.25f; //1f + 2 * ( (1f - (qEst)));
            } else {
                this.a = -1;
                this.attraction = 0;
            }

        }

        @Override
        public boolean isDeleted() {
            return !target.active();
        }
    }
}
