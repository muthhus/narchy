package nars.gui.graph.run;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jcog.pri.PLink;
import jcog.pri.RawPLink;
import nars.$;
import nars.concept.Concept;
import nars.control.ConceptFire;
import nars.gui.NARSpace;
import nars.gui.graph.ConceptSpace;
import nars.gui.graph.ConceptWidget;
import nars.gui.graph.MyForceDirected;
import nars.nar.Terminal;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.layout.Flatten;

import java.util.Collection;
import java.util.List;

import static nars.gui.Vis.reflect;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.col;

/**
 * display a directed graph by wrapping its elements in NAR concepts (HACK)
 */
public class SimpleGraph1 extends ConceptSpace {


    public SimpleGraph1(int maxEdges) {
        super(new Terminal(), maxEdges, maxEdges);
    }

    List<ConceptFire> nodes = $.newArrayList(), next = null;

    protected void commit(Graph g) {
        List<ConceptFire> n2 = $.newArrayList(g.nodes().size());

        g.nodes().forEach(x -> {
            //HACK todo use proxyterms in a cache
            Concept c = nar.conceptualize(nodeTerm(x));
            c.termlinks().clear();
            g.successors(x).forEach( y -> c.termlinks().put(new RawPLink(nodeTerm(y),  1f) ));
            n2.add(new ConceptFire(c, 1f));
        });

        this.next = n2;
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
                reflect(conceptVis),
                reflect(s)
            ), w, h);

        return s;
    }

    public static void main(String[] args) {


        MutableGraph g = GraphBuilder.directed().build();
        g.putEdge("x", "y");
        g.putEdge("y", "z");
        g.putEdge("y", "w");

        SimpleGraph1 cs = new SimpleGraph1(10);
        cs.show(800, 600);
        cs.commit(g);


    }

}
