package nars.util.graph;

import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DirectedPseudograph;

import java.io.PrintStream;
import java.util.Set;

/**
 * Generates a graph of a set of Concept's TermLinks. Each TermLink is an edge,
 * and the set of unique Concepts and Terms linked are the vertices.
 */
public class TermLinkGraph extends DirectedPseudograph<Termed, String> {

    public TermLinkGraph() {
        super(String.class);
    }


    public TermLinkGraph(NAR n) {
        this();
        add(n, true);
    }

    public TermLinkGraph(Concept... c) {
        this();
        for (Concept x : c)
            add(x, true);
    }

    @Override
    public String toString() {
        return '[' + vertexSet().toString() + ", " + edgeSet() + ']';
    }

    public void print(PrintStream out) {

        Set<Termed> vs = vertexSet();

        out.println(getClass().getSimpleName() + " numTerms=" + vs.size() + ", numTermLinks=" + edgeSet().size() );
        out.print("\t");
        out.println(this);

        for (Termed t : vs) {
            out.print(t + ":  ");
            outgoingEdgesOf(t).forEach(e ->
                out.print("  " + e)
            );
            out.println();
        }
        out.println();

    }

    public static class TermLinkTemplateGraph extends TermLinkGraph {

        public TermLinkTemplateGraph(NAR n) {
            super(n);
        }

        /** add the termlink templates instead of termlinks */
        @Override protected void addTermLinks(Concept c) {


            for (Termed targetTerm : c.getTermLinkTemplates()) {
                if (!containsVertex(targetTerm)) {
                    addVertex(targetTerm);
                }

                addEdge(c, targetTerm,
                        edge(c, targetTerm) );
            }
        }
    }

    public TermLinkGraph add(Concept source, boolean includeTermLinks/*, boolean includeTaskLinks, boolean includeOtherReferencedConcepts*/) {

        if (!containsVertex(source)) {
            addVertex(source);
        }

        if (includeTermLinks) {
            addTermLinks(source);
        }

                /*
                if (includeTaskLinks) {
                    for (TaskLink t : c.taskLinks.values()) {
                        Task target = t.targetTask;
                        if (!containsVertex(target)) {
                            addVertex(target);
                        }
                        addEdge(source, target, t);
                    }
                }
                */


        return this;
    }

    protected void addTermLinks(Concept c) {
        if (c == null)
            throw new RuntimeException("null concept");

        //Term cterm = c.get();

        Bag<Termed> tl = c.getTermLinks();
        if (tl == null) return;

        c.getTermLinks().forEachKey(target -> {
            if (!containsVertex(target)) {
                addVertex(target);
            }

            addEdge(c, target, edge(c,target));
        });
    }

    static String edge(Termed source, Termed target) {
        return '(' + source.toString()
                + ',' + target.toString() + ')';
    }

    public TermLinkGraph add(NAR n, boolean includeTermLinks/*, boolean includeTaskLinks, boolean includeOtherReferencedConcepts*/) {

        n.forEachConcept(c -> add(c, includeTermLinks));

        return this;
    }

    public boolean isConnected() {
        ConnectivityInspector<Term, String> ci = new ConnectivityInspector(this);
        return ci.isGraphConnected();
    }
    public boolean isStronglyConnected() {
        StrongConnectivityInspector<Term, String> ci = new StrongConnectivityInspector(this);
        return ci.isStronglyConnected();
    }

//    public void add(Memory memory) {
//        add(memory.getCycleProcess(), true);
//    }

    /*public boolean includeLevel(int l) {
        return true;
    }*/

}
