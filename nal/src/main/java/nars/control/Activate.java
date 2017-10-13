package nars.control;

import jcog.bag.Bag;
import jcog.map.SaneObjectFloatHashMap;
import jcog.pri.PLink;
import jcog.pri.PLinkUntilDeleted;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.task.UnaryTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.container.TermContainer;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static nars.Op.INT;

/**
 * concept firing, activation, etc
 */
public class Activate extends UnaryTask<Concept>  {

    static final int TASKLINKS_SAMPLED = 2;
    static final int TERMLINKS_SAMPLED = 3;

    public Activate(Concept c, float pri) {
        super(c, pri);
        assert (c.isNormalized()) :
                c + " not normalized";
    }


    public static void activate(Task t, float activationApplied, NAR n) {

        Concept cc = t.concept(n, true);
        if (cc != null) {
            activate(t, activationApplied, n, cc);
        }

    }

    public static void activate(Task t, float activationApplied, NAR n, Concept cc) {

        n.emotion.onActivate(t, activationApplied, cc, n);


        float evalAmp = n.evaluate(t.cause());

        activationApplied *= evalAmp;

        cc.tasklinks().putAsync(
                new PLinkUntilDeleted<>(t, activationApplied)
                //new PLink<>(t, activation)
        );

        BatchActivate.add(cc, activationApplied, n);

        n.eventTask.emit(t);
    }

    public static class BatchActivate {

        final ObjectFloatHashMap<Concept> a = new SaneObjectFloatHashMap<>(64);


        final static ThreadLocal<BatchActivate> batches = ThreadLocal.withInitial(BatchActivate::new);

        final static LongHashSet active = new LongHashSet();

        public static BatchActivate get() {
            assert(active.contains(Thread.currentThread().getId()));
            return batches.get();
        }

        BatchActivate() {

        }

        public void commit(NAR nar) {
            if (!a.isEmpty()) {
                try {
                    a.forEachKeyValue((c, p) -> nar.input(new Activate(c, p)));
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    a.clear();
                }
            }
        }

        public void put(Concept c, float pri) {
            a.addToValue(c, pri);
        }

        /** enable for the current thread */
        public static void enable() {
            synchronized (active) {
                active.add(Thread.currentThread().getId());
            }
        }

        static void add(Concept cc, float v, NAR n) {
            if (active.contains(Thread.currentThread().getId()))
                get().put(cc, v);
            else
                n.input(new Activate(cc, v));
        }

//        public static class BatchActivateCommit extends NativeTask {
//
//            private final Activate[] activations;
//
//            public BatchActivateCommit(Activate[] l) {
//                this.activations = l;
//            }
//
//            @Override
//            public String toString() {
//                return "ActivationBatch x" + activations.length;
//            }
//
//            @Override
//            public @Nullable Iterable<? extends ITask> run(NAR n) {
//                n.input(activations);
//                return null;
//            }
//        }
    }


    @Override
    public List<Premise> run(NAR nar) {
        nar.emotion.conceptFires.increment();


        final Bag<Task, PriReference<Task>> tasklinks = id.tasklinks();
        tasklinks.commit(tasklinks.forget(Param.LINK_FORGET_TEMPERATURE));

        int talSampled = Math.min(tasklinks.size(), TASKLINKS_SAMPLED);
        if (talSampled == 0)
            return null;

        List<PriReference<Task>> taskl = $.newArrayList(talSampled);
        tasklinks.sample(talSampled, ((Consumer<PriReference>) taskl::add));
        if (taskl.isEmpty()) {
            //nar.emotion.count("ConceptFire_run_but_zero_taskslinks_selected");
            return null;
        }


        //concept priority transfers into:
        //      its termlinks to subterms
        //      and their reverse links back to this

        final Bag<Term, PriReference<Term>> termlinks = id.termlinks();
        termlinks.commit(termlinks.forget(Param.LINK_FORGET_TEMPERATURE));


        BatchActivate ba = BatchActivate.get();
        Collection<Concept> localSubConcepts = linkTemplates(nar, ba);


        List<PriReference<Term>> terml;
        int tlSampled = Math.min(termlinks.size(), TERMLINKS_SAMPLED);
        if (tlSampled == 0) {
            return null;
        } else {
            terml = $.newArrayList(tlSampled);
            termlinks.sample(tlSampled, ((Consumer<PriReference>) terml::add));
        }

        int tasklSize = taskl.size();
        if (tasklSize == 0) return null;
        int termlSize = terml.size();
        if (termlSize == 0) return null;

        List<Premise> premises = $.newArrayList(tasklSize * termlSize);

        for (int i = 0; i < tasklSize; i++) {
            PriReference<Task> tasklink = taskl.get(i);

            final Task task = tasklink.get();
            if (task != null) {

                for (int j = 0; j < termlSize; j++) {
                    PriReference<Term> termlink = terml.get(j);


                    final Term term = termlink.get();
                    if (term != null) {

                        float pri = Param.linksToPremise.apply(task.priElseZero(), termlink.priElseZero());
                        Premise p = new Premise(task, term, pri, localSubConcepts);
                        premises.add(p);
                    }
                }
            }
        }


        return premises;
    }


    @Nullable
    public Collection<Concept> linkTemplates(NAR nar, BatchActivate ba) {
        float budgeted = priElseZero();
        float decayRate = 1f - nar.momentum.floatValue();
        float decayed = budgeted * decayRate;

        priSub(decayed); //for balanced budgeting: important

        Collection<Termed> localTemplates = id.templates();
        int n = localTemplates.size();
        if (n > 0) {
            float subDecay = budgeted / n;
            Term thisTerm = id.term();

            Collection<Concept> localSubConcepts =
                    //new HashSet<>(); //temporary for this function call only, so as not to retain refs to Concepts
                    //new UnifiedSet(); //allows concurrent read
                    $.newArrayList(n); //maybe contain duplicates but its ok, this is fast to construct

            Random rng = nar.random();
            //float balance = Param.TERMLINK_BALANCE;
            float subDecayReverse = subDecay;// * (1f - balance);
            float subDecayForward = subDecay;// * balance;
            Bag<Term, PriReference<Term>> termlinks = id.termlinks();

            float spent = 0;
            for (Termed localSub : localTemplates) {

                localSub = mutateTermlink(localSub.term(), rng); //for special Termed instances, ex: RotatedInt etc
                if (localSub instanceof Bool)
                    continue; //unlucky mutation

                Term localSubTerm = localSub.term(); //if mutated then localSubTerm would change so do it here

                float d;
                boolean reverseLinked = false;
                if (localSubTerm.op().conceptualizable && !localSubTerm.equals(thisTerm)) {

                    Concept localSubConcept = nar.conceptualize(localSub);
                    if (localSubConcept != null) {

                        localSubConcept.termlinks().putAsync(
                                new PLink(thisTerm, subDecayReverse)
                        );
                        reverseLinked = true;

                        ba.put(localSubConcept, subDecay);

                        localSubConcepts.add(localSubConcept);

                        spent += subDecay;
                    }

                }

                termlinks.putAsync(
                        new PLink(localSubTerm,
                                subDecayForward)
                        //reverseLinked ? subDecayForward : subDecay /* full */)
                );

            }
            float refund = budgeted - spent;
            if (refund > Pri.EPSILON) {
                priAdd(refund);
            }

            if (!localSubConcepts.isEmpty())
                return localSubConcepts;
        }

        return null;
    }

    /**
     * preprocess termlink
     */
    private static Termed mutateTermlink(Term t, Random rng) {

        if (Param.MUTATE_INT_CONTAINING_TERMS_RATE > 0) {
            if (t.hasAny(INT)) {
                TermContainer ts = t.subterms();
                if (ts.OR(xx -> xx instanceof Int) && rng.nextFloat() <= Param.MUTATE_INT_CONTAINING_TERMS_RATE) {

                    Term[] xx = ts.toArray();
                    boolean changed = false;
                    for (int i = 0; i < xx.length; i++) {
                        Term y = xx[i];
                        if (y instanceof Int) {
                            int shift =
                                    rng.nextInt(3) - 1;
                            //nar.random().nextInt(5) - 2;
                            if (shift != 0) {
                                int yy = ((Int) y).id;
                                int j =
                                        Math.max(0 /* avoid negs for now */, yy + shift);
                                if (yy != j) {
                                    xx[i] = Int.the(j);
                                    changed = true;
                                }
                            }
                        }
                    }
                    if (changed)
                        return t.op().the(t.dt(), xx);

                }
            }

        }

        return t;
    }

    static void activateSubterms(Task task, Collection<Concept> subs, float cPri) {
        int numSubs = subs.size();
        if (numSubs == 0)
            return;

        float tfa = cPri * task.priElseZero();
        float tfaEach = tfa / numSubs;


        for (Concept localSubConcept : subs) {

            localSubConcept.tasklinks().putAsync(
                    new PLinkUntilDeleted(task, tfaEach)
            );
//                localSubConcept.termlinks().putAsync(
//                        new PLink(task.term(), tfaEach)
//                );


        }
    }

//    public void activateTaskExperiment1(NAR nar, float pri, Term thisTerm, BaseConcept cc) {
//        Termed[] taskTemplates = templates(cc, nar);
//
//        //if (templateConceptsCount > 0) {
//
//        //float momentum = 0.5f;
//        float taskTemplateActivation = pri / taskTemplates.length;
//        for (Termed ct : taskTemplates) {
//
//            Concept c = nar.conceptualize(ct);
//            //this concept activates task templates and termlinks to them
//            if (c instanceof Concept) {
//                c.termlinks().putAsync(
//                        new PLink(thisTerm, taskTemplateActivation)
//                );
//                nar.input(new Activate(c, taskTemplateActivation));
//
////                        //reverse termlink from task template to this concept
////                        //maybe this should be allowed for non-concept subterms
////                        id.termlinks().putAsync(new PLink(c, taskTemplateActivation / 2)
////                                //(concept ? (1f - momentum) : 1))
////                        );
//
//            }
//
//
//        }
//    }


    @Override
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    //    protected int premise(Derivation d, Premise p, Consumer<DerivedTask> x, int ttlPerPremise) {
//        int ttl = p.run(d, ttlPerPremise);
//        //TODO record ttl usage
//        return ttl;
//    }


    @Override
    public boolean persist() {
        return true;
    }
}
