package nars.control;

import com.google.common.collect.Collections2;
import jcog.bag.Bag;
import jcog.list.FasterList;
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
import nars.term.atom.Int;
import nars.term.container.TermContainer;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static nars.Op.INT;

/**
 * concept firing, activation, etc
 */
public class Activate extends UnaryTask<Concept> {

    /**
     * per batch, on empty
     */
    static final int TASKLINKS_SAMPLED = 2;
    /**
     * per batch, on empty
     */
    static final int TERMLINKS_SAMPLED = 2;

    final Deque<Premise> pending = new ArrayDeque(TASKLINKS_SAMPLED * TERMLINKS_SAMPLED); //may need to be concurrent

    private List<Termed> targets;
    private Collection<Concept> targetConcepts;
    int nextTarget = 0;

    public Activate(Concept c, float pri) {
        super(c, pri);
        assert (c.isNormalized()) :
                c + " not normalized";
    }


//    public static void activate(Task t, float activationApplied, NAR n) {
//
//        Concept cc = t.concept(n, true);
//        if (cc != null) {
//            activate(t, activationApplied, n, cc);
//        }
//
//    }

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
            assert (active.contains(Thread.currentThread().getId()));
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

        /**
         * enable for the current thread
         */
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
    public Iterable<Premise> run(NAR nar) {

        nar.emotion.conceptFires.increment();

        if (targets == null) {
            this.targets = linkTemplates(nar);
            this.targetConcepts = Collections2.filter((List)targets, (x)->x instanceof Concept);
        }

        activateTemplates(nar);

        return hypothesize();
    }


    @Nullable
    Iterable<Premise> hypothesize() {
        if (pending.isEmpty()) {

            final Bag<Task, PriReference<Task>> tasklinks = id.tasklinks();
            int ntasklinks = tasklinks.size();
            if (ntasklinks == 0)
                return null;

            final Bag<Term, PriReference<Term>> termlinks = id.termlinks();
            int ntermlinks = termlinks.size();
            if (ntermlinks == 0)
                return null;

            tasklinks.commit(tasklinks.forget(Param.LINK_FORGET_TEMPERATURE));
            termlinks.commit(termlinks.forget(Param.LINK_FORGET_TEMPERATURE));

            int tlSampled = Math.min(ntermlinks, TERMLINKS_SAMPLED);
            FasterList<PriReference> terml = new FasterList(tlSampled);
            termlinks.sample(tlSampled, ((Consumer<PriReference>) terml::add));
            int termlSize = terml.size();
            if (termlSize <= 0)
                return null;

            tasklinks.sample(Math.min(ntasklinks, TASKLINKS_SAMPLED), (tasklink) -> {
                final Task task = tasklink.get();
                if (task != null) {

                    for (int j = 0; j < termlSize; j++) {
                        PriReference<Term> termlink = terml.get(j);

                        final Term term = termlink.get();
                        if (term != null) {

                            float pri = Param.linksToPremise.apply(task.priElseZero(), termlink.priElseZero());
                            Premise p = new Premise(task, term, pri, targetConcepts);
                            pending.add(p);
                        }
                    }
                }
            });

            if (pending.isEmpty())
                return null;
        }

        return Collections.singleton(pending.removeFirst());
    }


    /** send some activation */
    private void activateTemplates(NAR nar) {

        int n = targets.size();
        if (n == 0)
            return;

        float freed = 1f - nar.momentum.floatValue();
        int toFire = (int) Math.ceil(n * freed);

        float budgeted = priElseZero() * freed;
        float budgetedToEach = budgeted / n;
        if (budgetedToEach < Pri.EPSILON)
            return;

        priSub(budgeted);

        MutableFloat refund = new MutableFloat(0);
        BatchActivate ba = BatchActivate.get();

        int numTargets = targets.size();
        for (int i = 0; i < toFire; i++) {
            Termed t = targets.get(nextTarget++);
            if (nextTarget == numTargets)
                nextTarget = 0;
            linkTemplate(t, budgetedToEach/2f, budgetedToEach/2f, ba, nar, refund);
        }

        float r = refund.floatValue();
        if (r > Pri.EPSILON) {
            priAdd(r);
        }

    }

    private void linkTemplate(Termed target, float priForward, float priReverse, BatchActivate a, NAR nar, MutableFloat refund) {
        float decayRate = 1f - nar.momentum.floatValue();
        float budgeted = priElseZero() * decayRate;
        if (budgeted < Pri.EPSILON)
            return;

        priSub(budgeted); //for balanced budgeting: important

        if (target instanceof Concept) {
            Concept c = (Concept) target;
            c.termlinks().put(
                    new PLink(id.term(), priReverse), refund
            );
            a.put(c, priForward);
        } else {
            refund.add(priForward);
        }

        id.termlinks().put(
                new PLink(target.term(), priForward), refund
        );


    }


    @Nullable
    public List<Termed> linkTemplates(NAR nar) {


        Collection<Termed> localTemplates = id.templates();
        int n = localTemplates.size();
        if (n <= 0)
            return null;

        Term thisTerm = id.term();

        List<Termed> localSubConcepts =
                //new HashSet<>(); //temporary for this function call only, so as not to retain refs to Concepts
                //new UnifiedSet(); //allows concurrent read
                $.newArrayList(n); //maybe contain duplicates but its ok, this is fast to construct

        //Random rng = nar.random();
        //float balance = Param.TERMLINK_BALANCE;

        float spent = 0;
        for (Termed localSub : localTemplates) {

            //localSub = mutateTermlink(localSub.term(), rng); //for special Termed instances, ex: RotatedInt etc
//                if (localSub instanceof Bool)
//                    continue; //unlucky mutation


            Termed target = localSub.term(); //if mutated then localSubTerm would change so do it here

            float d;

            if (target.op().conceptualizable && !target.equals(thisTerm)) {

                Concept targetConcept = nar.conceptualize(localSub);
                if (targetConcept != null) {
                    target = (targetConcept);

                }
            }

            localSubConcepts.add(target);
        }

        return !localSubConcepts.isEmpty() ? localSubConcepts : null;

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

    static void taskActivate(Task task, Collection<Concept> subs, float cPri) {
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
