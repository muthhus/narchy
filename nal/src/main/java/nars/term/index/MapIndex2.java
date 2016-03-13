package nars.term.index;

import com.gs.collections.impl.map.mutable.primitive.IntObjectHashMap;
import nars.Op;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.term.*;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
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
            super(4);
            this.vector = normalized;
        }

//        @Override
//        public Termed put(int key, Termed value) {
//            return super.put(key, value);
//        }

        /*
            g.compact();
        */

    }

    /** uses an array for fast lookup and write access to basic term types where relation isnt used */
    public static class SubtermNodeWithArray extends SubtermNode {
        public final TermContainer vector;

        final static int NUM_FAST = 16;
        private final Termed[] fast;

        public SubtermNodeWithArray(TermContainer normalized) {
            super(normalized);
            this.vector = normalized;
            this.fast = new Termed[NUM_FAST];
        }

        @Override
        public Termed get(int key) {
            if (Terms.relComponent(key) == 0xffff) {
                int o = Terms.opComponent(key);
                if (o < NUM_FAST) {
                    return fast[o];
                }
            }
            return super.get(key);
        }

        @Override
        public Termed put(int key, Termed value) {
            if (Terms.relComponent(key) == 0xffff) {
                int o = Terms.opComponent(key);
                if (o < NUM_FAST) {
                    fast[o] = value;
                }
            }
            return super.put(key, value);
        }

        //        @Override
//        public Termed put(int key, Termed value) {
//            return super.put(key, value);
//        }

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


//    @NotNull
//    static TermContainer vector(@NotNull Term t) {
//        return ((Compound)t).subterms();
//    }
//
//
//    /** returns previous value */
//    private Object putItem(TermContainer vv, int index, Termed value) {
//        SubtermNode g = getOrAddNode(vv);
//        return value != null ? g.put(index, value) : null;
//    }


    final Function<TermContainer, SubtermNode> termContainerSubtermNodeFunction =
            k ->
                new SubtermNode(normalize(k));
                //new SubtermNodeWithArray(normalize(k));


    @Nullable
    @Override protected Termed theCompound(@NotNull Compound t, boolean create) {

        TermContainer subsBefore = t.subterms();

        SubtermNode node = create ?
                getOrAddNode(subsBefore) :
                getNode(subsBefore);

        return node!=null ? get(t, node, subsBefore, create) : null;
    }

    @Nullable private Termed get(@NotNull Compound t, @NotNull SubtermNode node, @NotNull TermContainer subsBefore, boolean create) {

        int oprel = t.opRel();

        Termed interned = node.get(oprel);
        if (create && (interned == null)) {

            TermContainer subsAfter = node.vector;
            if (subsAfter!=subsBefore) { //rebuild if necessary
                if ((interned = internCompound(subsAfter, t.op(), t.relation(), t.dt())) == null)
                    throw new InvalidTerm(t);
                    //return null;
            } else {
                interned = t; //use original parameter itself; for more isolation, this could be replaced with a clone creator
            }

            interned = conceptBuilder.apply(interned.term());
            if (interned == null)
                throw new InvalidTerm(t);

            Termed preExisting = node.put(oprel, interned);
            assert(preExisting == null);
        }

        return interned;
    }


    @Nullable
    private Termed internCompound(@NotNull TermContainer subs, @NotNull Op op, int rel, int dt) {
        Termed interned;
        interned = builder.make(op, rel, subs, dt);
        assert(interned!=null); //should not fail unless the input was invalid to begin with

        return interned;
    }


    @NotNull
    @Override public TermContainer theSubterms(TermContainer s) {
        return getOrAddNode(s).vector;
    }

    @NotNull public SubtermNode getOrAddNode(TermContainer s) {
        //return data.computeIfAbsent(s, termContainerSubtermNodeFunction);
        SubtermNode d = data.get(s);
        if (d == null) {
            data.put(s, d = termContainerSubtermNodeFunction.apply(s));
        }
        return d;
    }
    @Nullable public SubtermNode getNode(TermContainer s) {
        return data.get(s);
    }

//    @Nullable
//    @Override
//    protected TermContainer get(TermContainer subterms) {
//        SubtermNode g = data.get(subterms);
//        return g != null ? g.vector : null;
//    }
//    @Nullable
//    public Termed get(TermContainer vv, int index) {
//        SubtermNode n = data.get(vv);
//        return n != null ? n.get(index) : null;
//    }
//    @Override
//    @Deprecated public void put(@NotNull Termed t) {
//        Term u = t.term();
//        if (u instanceof Atomic) {
//            atoms.putIfAbsent(t.toString(), ()->(AtomConcept)t);
//        } else {
//            Object replaced = putItem(vector(u), t.opRel(), t);
//            if (replaced == null)
//                count++;
//        }
//    }




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
        return data.size();// + atoms.size();
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        data.values().forEach(v->v.forEach(c));

        //throw new UnsupportedOperationException();
        //atoms.getKeyValuePairsForKeysStartingWith()
    }
}
