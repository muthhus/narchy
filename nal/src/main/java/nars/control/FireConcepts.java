package nars.control;

import jcog.data.FloatParam;
import jcog.event.On;
import nars.*;
import nars.premise.Derivation;
import org.jetbrains.annotations.NotNull;

import static jcog.bag.Bag.BagCursorAction.Next;
import static jcog.bag.Bag.BagCursorAction.Stop;


/**
 * fires sampled active focus concepts
 */
public class FireConcepts implements Runnable {





    /**
     * in total priority per cycle, however
     * it is actually sum(1 + concept priority)
     * so that 0 priority concepts consume something
     */
    public final @NotNull FloatParam rate = new FloatParam((Param.UnificationTTLMax * 1), 0f, (512));

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


    public FireConcepts(@NotNull Focus source, NAR nar) {

        this.nar = nar;
        this.source = source;
        this.on = nar.onCycle(this);

    }

    @Override
    public void run() {

        float load =
                //0f;
                nar.exe.load();
//            if (load > 0.9f) {
//                logger.error("overload {}", load);
//                return;
//            }

        ConceptBagFocus csrc = (ConceptBagFocus) source;
        if (((ConceptBagFocus) this.source).active.commit().size() == 0)
            return; //no concepts

        final float[] curTTL = { (rate.floatValue() * (1f - load))};
        if (curTTL[0] == 0)
            return; //idle

//        float idealMass = 0.5f /* perfect avg if full */ * csrc.active.capacity();
//        float mass = ((HijackBag) csrc.active).mass;
//        float priDecayFactor =
//                //1f - (((float)csrc.active.size()) / csrc.active.capacity());
//                Util.unitize(
//                        1f - mass / (mass + idealMass)
//                );


        final int[] fired = {0};

        csrc.active.sample(pc -> {

            float priFired = pc.priSafe(0);

            ConceptFire cf = new ConceptFire(pc.get(), priFired);
            fired[0]++;
            nar.input(cf);

            //pc.priMult(priDecayFactor);


            curTTL[0] -= (1f + priFired);
            return curTTL[0] > 0 ? Next : Stop;
        }, false);


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
