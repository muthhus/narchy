package nars.control;

import jcog.Util;
import jcog.bag.impl.HijackBag;
import jcog.data.FloatParam;
import jcog.event.On;
import jcog.pri.PLink;
import nars.Focus;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.premise.Derivation;
import nars.premise.DerivationBudgeting;
import nars.premise.Premise;
import nars.premise.PremiseBuilder;
import nars.task.UnaryTask;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.term.util.InvalidTermException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.bag.Bag.BagCursorAction.Next;
import static jcog.bag.Bag.BagCursorAction.Stop;


/**
 * fires sampled active focus concepts
 */
public class FireConcepts implements Runnable {


    public final DerivationBudgeting budgeting;
    public final Deriver deriver;


    private final ThreadLocal<FireConcepts.DirectDerivation> derivation;

    final int MISFIRE_COST = 1;
    int premiseCost = Param.BeliefMatchTTL;
    int linkSampleCost = 1;


    /**
     * in TTL per cycle
     */
    public final @NotNull FloatParam rate = new FloatParam((Param.UnificationTTLMax * 1), 0f, (16 * 1024));

    //    public final MutableInteger derivationsInputPerCycle;
//    this.derivationsInputPerCycle = new MutableInteger(Param.TASKS_INPUT_PER_CYCLE_MAX);
    protected final NAR nar;
    private final On on;
    public final Focus source;

//    class PremiseVectorBatch implements Consumer<BLink<Concept>>{
//
//        public PremiseVectorBatch(int batchSize, NAR nar) {
//            nar.focus().sample(batchSize, c -> {
//                if (premiseVector(nar, c.get(), FireConcepts.this)) return true; //continue
//
//                return true;
//            });
//        }
//
//        @Override
//        public void accept(BLink<Concept> conceptBLink) {
//
//        }
//    }

    public class ConceptFire extends UnaryTask<Concept> {

        /** a reducing factor for assigning priority to this concept firing task.
         *  if it uses the priority directly then it competes for execution against
         *  tasks themselves. one reason this is probably necessary is that in terms of
         *  feedback, the firing of a concept can produce many tasks which should
         *  themselves have more chance to survive than another firing which produces tasks
         *  and so on
         */
        final static float ACTIVATION_FACTOR = 1f/10f;

        public ConceptFire(Concept c, float pri) {
            super(c, pri * ACTIVATION_FACTOR);
        }

        public int ttlMax() {
            float pri = pri();
            if (pri!=pri) //deleted
                return -1;
            return Util.lerp(pri/ACTIVATION_FACTOR, Param.UnificationTTLMax, Param.UnificationTTLMin);
        }

        @Override
        public void run(NAR nar) throws Concept.InvalidConceptException, InvalidTermException, InvalidTaskException {


            int ttl = ttlMax();
            if (ttl <= 0)
                return; //??

            Concept c = value;

            c.tasklinks().commit();//.normalize(0.1f);
            c.termlinks().commit();//.normalize(0.1f);
            nar.terms.commit(c);


            @Nullable PLink<Task> tasklink = null;
            @Nullable PLink<Term> termlink = null;
            float taskLinkPri = -1f, termLinkPri = -1f;


            FireConcepts.DirectDerivation d = derivation.get();


            /**
             * this implements a pair of roulette wheel random selectors
             * which have their options weighted according to the normalized
             * termlink and tasklink priorities.  normalization allows the absolute
             * range to be independent which should be ok since it is only affecting
             * the probabilistic selection sequence and doesnt affect derivation
             * budgeting directly.
             */
            d.restartA();
            while (ttl > 0) {
                if (tasklink == null || (d.random.nextFloat() > taskLinkPri)) { //sample a new link inversely probabalistically in proportion to priority
                    tasklink = c.tasklinks().sample();
                    ttl -= linkSampleCost;
                    if (tasklink == null)
                        break;

                    taskLinkPri = c.tasklinks().normalizeMinMax(tasklink.priSafe(0));
                    d.restartB(tasklink.get());
                }


                if (termlink == null || (d.random.nextFloat() > termLinkPri)) { //sample a new link inversely probabalistically in proportion to priority
                    termlink = c.termlinks().sample();
                    ttl -= linkSampleCost;
                    if (termlink == null)
                        break;
                    termLinkPri = c.termlinks().normalizeMinMax(termlink.priSafe(0));
                }

                if (ttl <= premiseCost)
                    break; //not enough remaining to create premise

                Premise p = PremiseBuilder.premise(c, tasklink, termlink, d.time, nar, -1f);
                ttl -= premiseCost; //failure of premise generation still causes cost

                if (p != null) {

                    int start = ttl;


                    int ttlRemain = deriver.run(d, p, ttl);
                    assert (start >= ttlRemain);

                    ttl -= (start - ttlRemain);
                    if (ttl <= 0) break;

//                    int nextDerivedTasks = d.buffer.size();
//                    int numDerived = nextDerivedTasks - derivedTasks;
//                    ttl -= numDerived * derivedTaskCost;
//                    derivedTasks = nextDerivedTasks;

                }
            }


        }

    }


    public FireConcepts(@NotNull Focus source, Deriver dderiver, DerivationBudgeting bbudgeting, NAR nar) {

        this.deriver = dderiver;
        this.budgeting = bbudgeting;

        this.nar = nar;
        this.source = source;

        this.derivation =
                ThreadLocal.withInitial(() ->
                        new FireConcepts.DirectDerivation(budgeting, nar)
                );

        this.on = nar.onCycle(this);
    }

    @Override
    public void run() {

        float load =
                0f;
                //nar.exe.load();
//            if (load > 0.9f) {
//                logger.error("overload {}", load);
//                return;
//            }

        ConceptBagFocus csrc = (ConceptBagFocus) source;
        if (((ConceptBagFocus) this.source).active.commit(null).size() == 0)
            return; //no concepts

        final int[] curTTL = {(int) Math.ceil(rate.floatValue() * (1f - load))};
        if (curTTL[0] == 0)
            return; //idle

        float idealMass = 0.5f /* perfect avg if full */ * csrc.active.capacity();
        float mass = ((HijackBag) csrc.active).mass;
        float priDecayFactor =
                //1f - (((float)csrc.active.size()) / csrc.active.capacity());
                Util.unitize(
                        1f - mass / (mass + idealMass)
                );


        final int[] fired = {0};
        csrc.active.sample(pc -> {

            ConceptFire cf = new ConceptFire(pc.get(), pc.priSafe(0));
            fired[0]++;
            nar.input(cf);

            pc.priMult(priDecayFactor);
            int ttlMax = cf.ttlMax();
            if (ttlMax < 0)
                ttlMax = MISFIRE_COST;

            curTTL[0] -= ttlMax;
            return curTTL[0] > 0 ? Next : Stop;
        }, true);


    }


    private static class DirectDerivation extends Derivation {


        public DirectDerivation(DerivationBudgeting b, NAR nar) {
            super(nar, b);
        }

        @Override
        public void derive(Task x) {
            nar.input(x);
        }

    }


}

//    /**
//     * returns # of derivations processed
//     */
//    int premiseVector0(PLink<Concept> pc, Derivation d, MutableIntRange taskLinksFiredPerConcept, MutableIntRange termLinksFiredPerConcept) {
//
//        Concept c = pc.get();
//        float cPri = pc.priSafe(0);
//
//        List<PLink<Task>> tasklinks = c.tasklinks().commit().sampleToList(taskLinksFiredPerConcept.lerp(cPri));
//        if (tasklinks.isEmpty())
//            return 0;
//
//        List<PLink<Term>> termlinks = c.termlinks().commit().sampleToList(termLinksFiredPerConcept.lerp(cPri));
//        if (termlinks.isEmpty())
//            return 0;
//
//        int count = 0;
//
//        long now = nar.time();
//        for (int i = 0, tasklinksSize = tasklinks.size(); i < tasklinksSize; i++) {
//            PLink<Task> tasklink = tasklinks.get(i);
//
//            float tlPri = tasklink.pri();
//
//            for (int j = 0, termlinksSize = termlinks.size(); j < termlinksSize; j++) {
//                PLink<Term> termlink = termlinks.get(j);
//
//                Premise p = PremiseBuilder.premise(c, tasklink, termlink, now, nar, -1f);
//                if (p != null) {
//
//                    float invest = Util.or(tlPri, termlink.pri());
//                    int ttl = Util.lerp(invest, Param.UnificationTTLMax, Param.UnificationTTLMin);
//
//                    if (deriver.test(d.restart(p, ttl)))
//                        count++;
//                }
//            }
//        }
//
//        return count;
//
//    }


//    class PremiseMatrixBatch implements Consumer<NAR> {
//        private final int _tasklinks;
//        private final int batchSize;
//        private final MutableIntRange _termlinks;
//
//        public PremiseMatrixBatch(int batchSize, int _tasklinks, MutableIntRange _termlinks) {
//            this.batchSize = batchSize;
//            this._tasklinks = _tasklinks;
//            this._termlinks = _termlinks;
//        }
//
//        @Override
//        public void accept(NAR nar) {
//            source.sample(batchSize, c -> {
//                premiser.newPremiseMatrix(c.get(),
//                        _tasklinks, _termlinks,
//                        FireConcepts.this, //input them within the current thread here
//                        nar
//                );
//                return true;
//            });
//        }
//
//    }
