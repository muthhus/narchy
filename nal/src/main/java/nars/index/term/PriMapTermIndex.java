package nars.index.term;

import jcog.bag.Bag;
import jcog.pri.PriMap;
import jcog.pri.PriReference;
import jcog.sort.TopN;
import jcog.util.LimitedCachedFloatFunction;
import nars.Task;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.index.term.map.MaplikeTermIndex;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;
import nars.truth.Truthed;
import org.HdrHistogram.IntCountsHistogram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import static jcog.pri.PriMap.Hold.SOFT;
import static jcog.pri.PriMap.Hold.STRONG;

public class PriMapTermIndex extends MaplikeTermIndex {

    final IntCountsHistogram conceptScores = new IntCountsHistogram(1000, 2);
    private final PriMap<Term, Termed> concepts;
    /**
     * current update index
     */
    private int visit;

    /**
     * how many items to visit during update
     */
    private final int updateMS = 150;
    private final float initial = 0.5f;
    private final float getBoost = 0.02f;
    private final float forget = 0.05f;
    //private long now;
    //private int dur;

    final int activeVictims = 32;

    final LimitedCachedFloatFunction<Concept> victimScore = new LimitedCachedFloatFunction<>((x) -> -value(x), 512);
    final TopN<Concept> victims = new TopN<>(new Concept[activeVictims], victimScore);

    public PriMapTermIndex() {
        super();
        this.concepts = new PriMap<>() {

            {
                cleaner.setPeriodMS(updateMS);
            }

            @Override
            protected TLink<Term, Termed> link(Object x) {
                TLink<Term, Termed> y = super.link(x);
                if (y != null) {
                    return allowPotentialVictim(y) ? y : null;
                }
                return y;
            }


            @Override
            public void evict(float strength) {

                if (strength > 0) {

                    if (strength > 0.99f) {
                        System.gc();
                    }

                    int nv = victims.size();
                    if (nv > 0) {
                        int kill = (int) Math.ceil(strength * nv);
                        if (kill > 0) {
                            System.err.println("evict : " + strength + ", concepts=" + size());


                            Concept[] vv;
                            synchronized (victims) {
                                if (victims.size() > 0)
                                    vv = victims.drain(new Concept[activeVictims]);
                                else
                                    vv = null;
                            }

                            if (vv != null) {
                                for (int i = 0; i < kill; i++) {
                                    Concept x = vv[i];
                                    if (x != null)
                                        removeGenocidally(x);
                                    else
                                        break; //end of list
                                }
                            }
                        }
                    }
                }
            }

            @Override
            protected Hold mode(Term term) {
                return term.complexity() > 8 ? SOFT : STRONG;
            }

            @Override
            protected void onRemove(Term term, Termed termed) {
                //System.err.println("remove: " + term);
                PriMapTermIndex.this.onRemove(termed);
            }

            /** spider this victim for other victims by looking through its tasklinks/termlinks */
            private void removeGenocidally(Concept concept) {

                Set<Termed> neighbors = new HashSet();

                Consumer<PriReference> victimCollector = (k) -> {
                    Termed t = (Termed) ((PriReference) k).get();
                    if (t != null) {
                        neighbors.add(t);
                    }
                };

                concept.tasklinks().forEach(victimCollector);
                concept.termlinks().forEach(victimCollector);

                remove(concept.term());

                neighbors.forEach(t -> {
                    Termed c = get(t.term());
                    if (c!=null) {
                        Concept cc = (Concept) c;
                        victimize(cc);
                    }
                });

            }

        };

    }

    @Override
    public int size() {
        return concepts.size();
    }

    @Override
    public Stream<Termed> stream() {
        return concepts.values().stream();
    }



    @Nullable
    @Override
    public Termed get(Term x, boolean createIfMissing) {

        //assert(!(x instanceof Variable)): "variables should not be stored in index";
        if (x instanceof Variable)
            return x;

        if (createIfMissing) {
            return concepts.compute(x, conceptBuilder);
        } else {
            return concepts.get(x);
        }
    }

    private boolean allowPotentialVictim(PriMap.TLink<Term, Termed> x) {
        Termed yy = x.get();
        if (yy == null)
            return false;

        Concept c = (Concept) yy;
        victimize(c);
        return true;


//        int score = (int) (score(c) * 1000f);
//
//        float cutoff = 0.25f;
//        if (conceptScores.getTotalCount() > updateBatchSize / 4) {
//            float percentile = (float) conceptScores.getPercentileAtOrBelowValue(score) / 100f;
//            if (percentile < cutoff)
//                forget(x, c, cutoff * (1 - percentile));
//        }
//
//        conceptScores.recordValue(score);


    }

    private void victimize(Concept c) {
        if (victimizable(c)) {
            synchronized (victims) {
                victims.accept(c);
            }
        }
    }

    @Override
    public void set(@NotNull Term src, @NotNull Termed target) {
        concepts.merge(src, target, setOrReplaceNonPermanent);
    }

    /**
     * victim pre-filter
     */
    private boolean victimizable(Concept x) {
        if (x instanceof PermanentConcept)
            return false;

        switch (concepts.state) {
            case CRITICAL:
                return true;
            case ALERT:
                return x.complexity() > 6;
            case FREE:
                return false;
        }

        return true;
    }


    static final ToDoubleFunction<Task> taskScore =
            //t -> t.evi(now, dur);
            Truthed::conf;

    protected float value(Concept c) {


        float beliefConf = ((float) c.beliefs().stream().mapToDouble(taskScore).average().orElse(0));
        float goalConf = ((float) c.goals().stream().mapToDouble(taskScore).average().orElse(0));
        Bag<Task, PriReference<Task>> ta = c.tasklinks();
        float talCap = ta.size() / (1f + ta.capacity());
        Bag<Term, PriReference<Term>> te = c.termlinks();
        float telCap = te.size() / (1f + te.capacity());
        return (((talCap + telCap) / 2f) + (beliefConf + goalConf) / 2f)
                /
                (1f + (c.complexity() + c.volume() / 2f));
    }

    protected void forget(PriReference<Termed> x, Concept c, float amount) {
        //shrink link bag capacity in proportion to the forget amount
        c.tasklinks().setCapacity(Math.round(c.tasklinks().capacity() * (1f - amount)));
        c.termlinks().setCapacity(Math.round(c.termlinks().capacity() * (1f - amount)));

        x.priMult(1f - amount);
    }

}
