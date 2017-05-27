package nars.gui.graph.run;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.data.FloatParam;
import jcog.pri.RawPLink;
import nars.$;
import nars.concept.Concept;
import nars.control.ConceptFire;
import nars.gui.Vis;
import nars.gui.graph.ConceptSpace;
import nars.gui.graph.ConceptWidget;
import nars.gui.graph.MyForceDirected;
import nars.nar.Terminal;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import spacegraph.Ortho;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.layout.Flatten;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.render.Draw;
import spacegraph.render.JoglPhysics;
import spacegraph.widget.Label;

import java.util.Collection;
import java.util.List;

import static nars.gui.Vis.reflect;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.col;

/**
 * display a directed graph by wrapping its elements in NAR concepts (HACK)
 */
public class SimpleGraph1 extends ConceptSpace {

    ConceptWidget touched = null;
    final Surface status = new Label("ready");

    public SimpleGraph1(int maxEdges) {
        super(new Terminal(), maxEdges, maxEdges);


        nodeBuilder = (x) ->
            new ConceptWidget(x) {
                @Override
                public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
                    if (touched!=this) {
                        ((Label)status).set(concept.toString());
                        ((Label)status).color.a(0.8f);
                    }
                    touched = this;

                    return super.onTouch(body, hitPoint, buttons, space);
                }
            };

        vis = new ConceptWidget.ConceptVis() {
            final float minSize = 3f;
            final float maxSize = 8;

            @Override
            public void apply(ConceptWidget cw, Term tt) {
                float p = cw.pri;

                float nodeScale = (float) (minSize + Math.sqrt(p) * maxSize);
                float l = nodeScale * 2;
                float w = nodeScale;
                float h = 1;
                cw.scale(l, w, h);


                float density = 0.25f;
                if (cw.body != null) {
                    cw.body.setMass(l * w * h * density);
                    cw.body.setDamping(0.9f, 0.9f);
                }
                Concept c = cw.concept;
                if (c != null) {
                    Draw.colorHash(c.hashCode(), cw.shapeColor, 0.75f, 0.35f, 0.9f);
                }
            }
        };

    }

    @Override
    public void start(SpaceGraph space) {
        super.start(space);
        space.add( new Ortho( status ).maximize() );
                //Vis.logConsole(nar, 90, 40, new FloatParam(0f)).opacity(0.25f))

    }

    //TODO use AtomicReference
    List<ConceptFire> nodes = $.newArrayList(), next = null;

    protected SimpleGraph1 commit(Graph g) {
        List<ConceptFire> n2 = $.newArrayList(g.nodes().size());

        g.nodes().forEach(x -> {
            //HACK todo use proxyterms in a cache
            Concept c = nar.conceptualize(nodeTerm(x));
            c.termlinks().clear();
            g.successors(x).forEach( y -> c.termlinks().put(new RawPLink(nodeTerm(y),  1f) ));
            n2.add(new ConceptFire(c, 1f));
        });

        this.next = n2;
        return this;
    }

    @NotNull
    private Termed nodeTerm(Object x) {
        return $.quote(System.identityHashCode(x) + " " + x.toString());
    }

    @Override
    protected void get(Collection<ConceptWidget> displayNext) {
        if (next!=null) {
            nodes = next;
            next = null;
        }
        nodes.forEach(c -> displayNext.add(nodeGetOrCreate(c)));
    }

    public synchronized SpaceGraph show(int w, int h) {
        SpaceGraph<Term> s = new SpaceGraph(this.with(new Flatten()) );

        MyForceDirected fd = new MyForceDirected();
        s.dyn.addBroadConstraint(fd);

        s.camPos(0, 0, 90).show(1300, 900);

        window(
            col(
                reflect(fd),
                reflect(vis),
                reflect(s)
            ), w, h);

        return s;
    }

    public static void main(String[] args) {

        MutableGraph g = GraphBuilder.directed().build();
        g.putEdge("x", "y");
        g.putEdge("y", "z");
        g.putEdge("y", "w");

        SimpleGraph1 cs = new SimpleGraph1(10).commit(g);
        cs.show(800, 600);


    }

}
