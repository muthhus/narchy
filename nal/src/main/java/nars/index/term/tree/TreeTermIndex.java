package nars.index.term.tree;

import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.util.ConceptBuilder;
import nars.index.term.TermIndex;
import nars.term.Term;
import nars.term.Termed;
import nars.util.data.map.nbhm.HijacKache;
import nars.util.radixtree.MyConcurrentRadixTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * concurrent radix tree index
 */
public class TreeTermIndex extends TermIndex implements Consumer<NAR> {

    public final TermTree concepts;

    private final ConceptBuilder conceptBuilder;

    long updatePeriodMS = 500;

    int sizeLimit;

    public TreeTermIndex(ConceptBuilder conceptBuilder, int sizeLimit) {

        this.conceptBuilder = conceptBuilder;
        this.concepts = new TermTree() {

            @Override
            public boolean onRemove(Termed r) {
                if (r instanceof Concept) {
                    Concept c = (Concept) r;
                    if (removeable(c)) {
                        onRemoval((Concept) r);
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            }
        };
        this.sizeLimit = sizeLimit;

//        Thread t = new Thread(this, this.toString() + "_Forget");
//        //t.setPriority(Thread.MAX_PRIORITY - 1);
//        t.setUncaughtExceptionHandler((whichThread, e) -> {
//            logger.error("Forget: {}", e);
//            e.printStackTrace();
//            System.exit(1);
//        });
//        t.start();

    }

    @Override
    public void start(NAR nar) {
        super.start(nar);
        nar.onFrame(this);
    }

    //    //1. decide how many items to remove, if any
//    //2. search for items to meet this quota and remove them
//    @Override
//    public void run() {
//
//        while (true) {
//
//            try {
//                Thread.sleep(updatePeriodMS);
//            } catch (InterruptedException e) {
//            }
//
//            if (nar != null)
//                forgetNextLater();
//        }
//
//    }
//
//    private void forgetNextLater() {
//        nar.runLater(this::forgetNext);
//    }

    private void forgetNext() {

        int sizeBefore = sizeEst();

        int overflow = sizeBefore - sizeLimit;

        if (overflow < 0)
            return;

        float maxFractionThatCanBeRemovedAtATime = 0.01f;
        float descentRate = 0.75f;
        int iterationLimit = 16; //TODO tune iterationLimit by the overflow amount

        int maxConceptsThatCanBeRemovedAtATime = (int) Math.max(1, sizeBefore * maxFractionThatCanBeRemovedAtATime);

        if (overflow < maxConceptsThatCanBeRemovedAtATime)
            return;

        concepts.acquireWriteLock();
        try {
            MyConcurrentRadixTree.SearchResult s = null;

            while ((iterationLimit-- > 0) && ((sizeEst() - sizeLimit) > maxConceptsThatCanBeRemovedAtATime)) {

                Random rng = nar.random;

                MyConcurrentRadixTree.Node subRoot = volumeWeightedRoot(rng);

                if (s == null)
                    s = concepts.random(subRoot, descentRate, rng);

                MyConcurrentRadixTree.Node f = s.found;

                if (f != null && f != subRoot) {
                    int subTreeSize = concepts.sizeIfLessThan(f, maxConceptsThatCanBeRemovedAtATime);

                    if (subTreeSize > 0) {
                        //long preBatch = sizeEst();
                        concepts.removeHavingAcquiredWriteLock(s, true);
                        //logger.info("  Forgot Batch {}", preBatch - sizeEst());
                    }

                    s = null; //restart
                }

            }
        } finally {
            concepts.releaseWriteLock();
        }

//        int sizeAfter = sizeEst();
//        logger.info("Forgot {} Concepts", sizeBefore - sizeAfter);

    }

    /**
     * since the terms are sorted by a volume-byte prefix, we can scan for removals in the higher indices of this node
     */
    private MyConcurrentRadixTree.Node volumeWeightedRoot(Random rng) {

        List<MyConcurrentRadixTree.Node> l = concepts.root.getOutgoingEdges();
        int levels = l.size();

        //HEURISTIC: x^8 sharp curve
        float r = rng.nextFloat();
        r = (r * r); //r^2
        r = (r * r); //r^4
        r = (r * r); //r^8
        r = 1 - r;

        return l.get( Math.round((levels - 1) * r) );
    }

    private int sizeEst() {
        return concepts.sizeEst();
    }

//    /** relative capacitance; >0 = over-capacity, <0 = under-capacity */
//    private float capacitance() {
//        int s = sizeEst();
//        //if (s > sizeLimit) {
//        return s - sizeLimit;
//        //}
//        //return 0;
//    }

    private static boolean removeable(Concept c) {
        return !(c instanceof PermanentConcept);
    }


    @Override
    public @Nullable Termed get(@NotNull Term t, boolean createIfMissing) {
        TermKey k = TermTree.key(t);

        if (createIfMissing) {
            return _get(k, t);
        } else {
            return _get(k);
        }
    }

    protected @Nullable Termed _get(@NotNull TermKey k) {
        return concepts.get(k);
    }

    protected @NotNull Termed _get(@NotNull TermKey k, @NotNull Term finalT) {
        return concepts.putIfAbsent(k, () -> conceptBuilder.apply(finalT));
    }

    @NotNull
    public TermKey key(@NotNull Term t) {
        return TermTree.key(t);
    }


    @Override
    public void set(@NotNull Term src, Termed target) {

        concepts.acquireWriteLock();
        try {
            @NotNull TermKey k = key(src);
            Termed existing = concepts.get(k);
            if (!(existing instanceof PermanentConcept)) {
                concepts.put(k, target);
            }
        } finally {
            concepts.releaseWriteLock();
        }
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("yet");
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        concepts.forEach(c);
    }

    @Override
    public int size() {
        return concepts.size(); //WARNING may be slow
    }

    @Override
    public @Nullable ConceptBuilder conceptBuilder() {
        return conceptBuilder;
    }


    @Override
    public @NotNull String summary() {
        //return ((nar.random.nextFloat() < SIZE_UPDATE_PROB) ? (this.lastSize = size()) : ("~" + lastSize)) + " terms";
        return concepts.sizeEst() + " concepts";
    }


    @Override
    public void remove(@NotNull Term entry) {
        TermKey k = key(entry);
        Termed result = concepts.get(k);
        if (result != null) {
            concepts.remove(k);
        }
    }


    protected void onRemoval(@NotNull Concept value) {
        delete(value, nar);
    }

    @Override
    public void accept(NAR eachFrame) {
        forgetNext();
    }


    /**
     * Tree-index with a front-end "L1" non-blocking hashmap cache
     */
    public static class L1TreeIndex extends TreeTermIndex {

        private final HijacKache<Term, Termed> L1;

        public L1TreeIndex(ConceptBuilder conceptBuilder, int sizeLimit, int cacheSize, int reprobes) {
            super(conceptBuilder, sizeLimit);
            this.L1 = new HijacKache<>(cacheSize, reprobes);
        }

        @Override
        public @Nullable Termed get(@NotNull Term t, boolean createIfMissing) {

            Object o = L1.computeIfAbsent2(t,
                    createIfMissing ?
                            ttt -> {
                                return super.get(ttt, true);
                            } :
                            ttt -> {
                                Termed v = super.get(ttt, false);
                                if (v == null)
                                    return L1; //this will result in null at the top level, but the null will not be stored in L1 itself
                                return v;
                            }
            );

            if (o instanceof Termed)
                return (Termed) o;
            else if (createIfMissing) { //HACK try again: this should be handled by computeIfAbsent2, not here
                L1.miss++;
                return super.get(t, true);
            } else {
                return null;
            }
        }

        @Override
        public @NotNull String summary() {
            return super.summary() + "\t, L1:" + L1.summary(true);
        }

        @Override
        protected void onRemoval(Concept r) {
            super.onRemoval(r);
            L1.remove(r);
        }
    }
}
