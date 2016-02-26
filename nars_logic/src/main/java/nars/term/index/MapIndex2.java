package nars.term.index;

import com.gs.collections.impl.map.mutable.primitive.IntObjectHashMap;
import nars.$;
import nars.Op;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.term.*;
import nars.term.atom.AtomicString;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 1/2/16.
 */
public class MapIndex2 extends AbstractMapIndex {


    public static class SubtermNode extends IntObjectHashMap<Termed> {
        public final TermContainer vector;

        public SubtermNode(TermContainer normalized) {
            this.vector = normalized;
        }

        /*
            g.compact();
        */

    }

    public final Map<TermContainer, SubtermNode> data;
    int count;

    public MapIndex2(Map<TermContainer, SubtermNode> data, Function<Term, Concept> conceptBuilder) {
        this(data, Terms.terms, conceptBuilder);
    }

    public MapIndex2(Map<TermContainer, SubtermNode> data, TermBuilder termBuilder, Function<Term, Concept> conceptBuilder) {

        super(termBuilder, conceptBuilder);

        this.data = data;

    }


    //TODO this needs to go somewhere:
    /*@Override
    public
    @Nullable
    Termed the(Op op, int relation, TermContainer subsBefore, int dt) {
        SubtermNode node = getOrAddNode(subsBefore);
        int oprel = Terms.opRel(op, relation);
        Termed interned = node.get(oprel);
        if (interned == null) {
            interned = internCompound(node.vector, op, relation, dt);
            if (interned == null)
                return null;
            node.put(oprel, interned);
        }
        return interned;
    }*/

    @Override
    public final TermBuilder builder() {
        return builder;
    }

    @NotNull
    static TermVector vector(@NotNull Term t) {
        return (TermVector)((Compound)t).subterms();
    }


    /** returns previous value */
    private Object putItem(TermVector vv, int index, Termed value) {
        SubtermNode g = getOrAddNode(vv);
        return value != null ? g.put(index, value) : null;
    }


    final Function<TermContainer, SubtermNode> termContainerSubtermNodeFunction =
            k -> new SubtermNode(normalize(k));


//    @Nullable
//    @Override
//    public Termed get(@NotNull Termed t) {
//        Term u = t.term();
//        return u instanceof Atom ?
//                    atoms.resolve((Atom)u) :
//                    get(vector(u), t.opRel());
//
//
//    }




    @Nullable
    @Override protected Termed theCompound(@NotNull Compound t) {

        TermContainer subsBefore = t.subterms();
        int oprel = t.opRel();

        SubtermNode node = getOrAddNode(subsBefore);

        Termed interned = node.get(oprel);
        if (interned == null) {

            TermContainer subsAfter = node.vector;
            if (subsAfter!=subsBefore) { //rebuild if necessary
                if ((interned = internCompound(subsAfter, t.op(), t.relation(), t.dt())) == null)
                    return null;
            } else {
                interned = t; //use input directly; for more isolation, this could be replaced with a clone creator
            }

            node.put(oprel, interned);
        }

        return interned;
    }

    @NotNull
    private Termed internCompound(TermContainer subsAfter, Op op, int rel, int dt) {
        Termed interned;
        interned = builder.make(op, rel, subsAfter, dt);
        assert(interned!=null); //should not fail unless the input was invalid to begin with
        return interned;
    }


    @NotNull
    @Override public TermContainer theSubterms(TermContainer s) {
        return getOrAddNode(s).vector;
    }

    @NotNull public SubtermNode getOrAddNode(TermContainer s) {
        return data.computeIfAbsent(s, termContainerSubtermNodeFunction);
    }

    @Nullable
    @Override
    protected TermContainer get(TermContainer subterms) {
        SubtermNode g = data.get(subterms);
        return g != null ? g.vector : null;
    }


    @Nullable
    public Termed get(TermContainer vv, int index) {
        SubtermNode n = data.get(vv);
        return n != null ? n.get(index) : null;
    }

    @Override
    @Deprecated public void put(@NotNull Termed t) {
        Term u = t.term();
        if (u instanceof AtomicString) {
            atoms.putIfAbsent(t.toString(), ()->(AtomConcept)t);
        } else {
            Object replaced = putItem(vector(u), t.opRel(), t);
            if (replaced == null)
                count++;
        }
    }




    @Override
    public void clear() {
        count = 0;
        data.clear();
        //TODO atoms.clear();
    }

    @Override
    public int subtermsCount() {
        return data.size();
    }

    @Override
    public int size() {
        /** WARNING: not accurate */
        return count;
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        data.values().forEach(v->v.forEach(c));

        //throw new UnsupportedOperationException();
        //atoms.getKeyValuePairsForKeysStartingWith()
    }
}
