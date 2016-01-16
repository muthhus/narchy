package nars.util.graph;

import nars.Global;
import nars.NAR;
import nars.concept.Concept;
import nars.util.data.ConceptMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Abstract graph model of inter-Concept dynamics.
 * Uses a concept meta to hold adjacency data
 * TODO incomplete
 */
public abstract class ConceptGraph2<E> extends ConceptMap implements DirectedGraph<Concept,E>, EdgeFactory<Concept,E> {

    @Override
    public int inDegreeOf(Concept vertex) {
        return 0;
    }

    @Nullable
    @Override
    public Set<E> incomingEdgesOf(Concept vertex) {
        return null;
    }

    @Override
    public int outDegreeOf(Concept vertex) {
        return 0;
    }

    @Nullable
    @Override
    public Set<E> outgoingEdgesOf(Concept vertex) {
        return null;
    }

    static class Vertex<E> {

        public enum EdgeSet {
            Incoming, Outgoing
        }

        public Map<Concept, E> incoming;
        public Map<Concept, E> outgoing;

        protected void ensureIncoming() {  if (incoming == null)  incoming = Global.newHashMap();       }
        protected void ensureOutgoing() { if (outgoing == null) outgoing = Global.newHashMap();        }



        public void addEdge(Concept target, E e, @NotNull EdgeSet set) {
            switch (set) {
                case Incoming:
                    ensureIncoming();
                    incoming.put(target, e);
                    break;
                case Outgoing:
                    ensureOutgoing();
                    outgoing.put(target, e);
                    break;
            }
        }

        @Nullable
        public E removeEdge(Concept target, @NotNull EdgeSet set) {
            switch (set) {
                case Incoming:
                    if (incoming!=null)
                        return incoming.remove(target);
                    break;
                case Outgoing:
                    if (outgoing!=null)
                        return outgoing.remove(target);
                    break;
            }
            return null;
        }
    }

    protected ConceptGraph2(@NotNull NAR nar) {
        super(nar);
    }

    @Override
    public abstract boolean contains(Concept c);

    @Override
    protected boolean onConceptForget(Concept c) {
        //TODO maybe should check whether graph contains it
        return true;
    }

    @Override
    protected boolean onConceptActive(@NotNull Concept c) {
        c.put(this, new Vertex());
        return true;
    }


    @NotNull
    @Override
    public abstract E createEdge(Concept source, Concept target);

    @Nullable
    @Override
    public Set<E> getAllEdges(Concept sourceVertex, Concept targetVertex) {
        return null;
    }

    @Nullable
    @Override
    public E getEdge(Concept sourceVertex, Concept targetVertex) {
        return null;
    }

    @Nullable
    @Override
    public EdgeFactory<Concept, E> getEdgeFactory() {
        return null;
    }

    @NotNull
    @Override
    public E addEdge(@NotNull Concept source, @NotNull Concept target) {
        E e = createEdge(source, target);
        Vertex sv = source.get(this);
        Vertex tv = target.get(this);
        sv.addEdge(target, e, Vertex.EdgeSet.Outgoing);
        tv.addEdge(source, e, Vertex.EdgeSet.Incoming);
        return e;
    }

    @Override
    public boolean addEdge(Concept sourceVertex, Concept targetVertex, E e) {
        return false;
    }

    @Override
    public boolean addVertex(Concept concept) {
        return false;
    }

    @Override
    public boolean containsEdge(Concept sourceVertex, Concept targetVertex) {
        return false;
    }

    @Override
    public boolean containsEdge(E e) {
        return false;
    }

    @Override
    public boolean containsVertex(Concept concept) {
        return false;
    }

    @Nullable
    @Override
    public Set<E> edgeSet() {
        return null;
    }

    @Nullable
    @Override
    public Set<E> edgesOf(Concept vertex) {
        return null;
    }

    @Override
    public boolean removeAllEdges(Collection<? extends E> edges) {
        return false;
    }

    @Nullable
    @Override
    public Set<E> removeAllEdges(Concept sourceVertex, Concept targetVertex) {
        return null;
    }

    @Override
    public boolean removeAllVertices(Collection<? extends Concept> vertices) {
        return false;
    }

    @Nullable
    @Override
    public E removeEdge(@NotNull Concept source, @NotNull Concept target) {
        Vertex<E> sv = source.get(this);
        Vertex<E> tv = target.get(this);
        E x1 = sv.removeEdge(target, Vertex.EdgeSet.Outgoing);
        E x2 = tv.removeEdge(source, Vertex.EdgeSet.Incoming);
        if (x1!=x2)
            throw new RuntimeException("graph fault, different edge instance between " + source + " and " + target);
        return x1;
    }

    @Override
    public boolean removeEdge(E e) {
        return false;
    }

    @Override
    public boolean removeVertex(Concept concept) {
        return false;
    }

    @Nullable
    @Override
    public Set<Concept> vertexSet() {
        return null;
    }

    @Nullable
    @Override
    public Concept getEdgeSource(E e) {
        return null;
    }

    @Nullable
    @Override
    public Concept getEdgeTarget(E e) {
        return null;
    }

    @Override
    public double getEdgeWeight(E e) {
        return 0;
    }

}
