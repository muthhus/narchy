package nars.index;

import nars.NAR;
import nars.Op;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.util.data.map.nbhm.HijaCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * concurrent radix tree index
 */
public class TreeIndex extends TermIndex {

    public final TermTree terms = new TermTree();

    private final Concept.ConceptBuilder conceptBuilder;
    private NAR nar;
    private static float SIZE_UPDATE_PROB = 0.05f;
    private int lastSize = 0;

    public TreeIndex(Concept.ConceptBuilder conceptBuilder) {
        this.conceptBuilder = conceptBuilder;
    }

    @Override
    public void start(NAR nar) {
        this.nar = nar;
    }


    @Override
    public @Nullable Termed get(@NotNull Termed tt, boolean createIfMissing) {
        Term t = tt.term();

        if (t instanceof Compound)
            t = conceptualize((Compound)t);

        TermKey k = new TermKey(t);

        if (createIfMissing) {
            return _get(k, t);
        } else {
            return _get(k);
        }
    }

    protected Termed _get(TermKey k) {
        return terms.get(k);
    }

    protected Termed _get(TermKey k, Term finalT) {
        return terms.putIfAbsent(k, ()->conceptBuilder.apply(finalT));
    }

    public TermKey key(@NotNull Term t) {

        if (t instanceof Compound)
            t = conceptualize((Compound)t);

        return new TermKey(t);
    }


    @Override
    public void set(@NotNull Termed src, Termed target) {
        terms.put(key(src.term()), target);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("yet");
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        terms.forEach(c);
    }

    @Override
    public int size() {
        return terms.size(); //WARNING may be slow
    }

    @Override
    public @Nullable Concept.ConceptBuilder conceptBuilder() {
        return conceptBuilder;
    }

    @Override
    public int subtermsCount() {
        return -1;
    }

    @Override
    public @NotNull String summary() {
        return ((nar.random.nextFloat() < SIZE_UPDATE_PROB) ? (this.lastSize = size()) : ("~" + lastSize)) + " terms";
    }

    @Override
    public void remove(@NotNull Termed entry) {

        TermKey k = key(entry.term());
        Termed result = terms.get(k);
        if (result!=null) {
            if (!terms.remove(k))
                return; //alredy removed since previous lookup or something

            onRemoval(k, result);
        }

    }

    @Override
    public @NotNull Term newCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms) {
        return new GenericCompound(op, dt, subterms);
    }


    protected void onRemoval(TermKey key, Termed value) {
        if (value instanceof Concept) {
            onRemoval((Concept) value);
        }
    }

    protected void onRemoval(Concept value) {
        value.delete(nar);
    }

    @Override
    protected boolean transformImmediates() {
        return true;
    }

    /** Tree-index with a front-end "L1" non-blocking hashmap cache */
    public static class L1TreeIndex extends TreeIndex {

        private final HijaCache<Term, Termed> L1;

        public L1TreeIndex(Concept.ConceptBuilder conceptBuilder, int cacheSize, int reprobes) {
            super(conceptBuilder);
            this.L1 = new HijaCache<Term,Termed>(cacheSize, reprobes);
        }

        @Override
        public @Nullable Termed get(@NotNull Termed tt, boolean createIfMissing) {
            Term t = tt.term();
            t = tt instanceof Compound ? conceptualize(((Compound) t)) : t;
            return L1.computeIfAbsent2(t,
                    createIfMissing ?
                            ttt -> super.get(ttt, true) :
                            ttt -> {
                                Termed v = super.get(ttt, false);
                                if (v == null)
                                    return L1; //this will result in null at the top level, but the null will not be stored in L1 itself
                                return v;
                            }
            );

        }

        @Override
        public @NotNull String summary() {
            return super.summary() + "\tL1:" + L1.summary();
        }

        @Override
        protected void onRemoval(Concept r) {
            super.onRemoval(r);
            L1.remove(r);
        }
    }
}
