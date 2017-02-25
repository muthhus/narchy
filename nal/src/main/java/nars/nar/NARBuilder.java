package nars.nar;

import jcog.bag.Bag;
import jcog.data.random.XorShift128PlusRandom;
import jcog.learn.lstm.SimpleLSTM;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.impl.BLinkHijackBag;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.TermIndex;
import nars.index.term.map.CaffeineIndex;
import nars.op.mental.Compressor;
import nars.op.mental.Inperience;
import nars.op.stm.MySTMClustered;
import nars.term.Term;
import nars.time.Time;
import nars.util.exe.Executioner;
import nars.util.exe.InstrumentedExecutor;
import nars.util.exe.MultiThreadExecutor;
import nars.util.exe.SynchronousExecutor;
import org.apache.commons.math3.util.MathArrays;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.BiFunction;

import static jcog.Texts.n2;
import static jcog.Texts.n4;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * Created by me on 12/27/16.
 */
public interface NARBuilder {

    static Default newMultiThreadNAR(int cores, Time time) {
        return newMultiThreadNAR(cores, time, false);
    }

    static Default newMultiThreadNAR(int threads, Time time, boolean sync) {
        Random rng = new XorShift128PlusRandom(1);
        Executioner exe =
                //new SynchronousExecutor();
                new MultiThreadExecutor(threads, 512, sync);

        //exe = new InstrumentedExecutor(exe, 8);

        int conceptsPerCycle = 128 * exe.concurrency();

        final int reprobes = 4;

        //Multi nar = new Multi(3,512,
        DefaultConceptBuilder cb = new DefaultConceptBuilder() {
            @Override
            public <X> X withBags(Term t, BiFunction<Bag<Term, BLink<Term>>, Bag<Task, BLink<Task>>, X> f) {
                Bag<Term, BLink<Term>> termlink = new BLinkHijackBag(reprobes, BudgetMerge.plusBlend, rng);
                Bag<Task, BLink<Task>> tasklink = new BLinkHijackBag(reprobes, BudgetMerge.maxBlend, rng);
                return f.apply(termlink, tasklink);
            }
        };


        Default nar = new Default(8 * 1024,
                conceptsPerCycle, 1, 3, rng,

                //new HijackTermIndex(cb, 1024 * 128, reprobes)
                //new NullTermIndex(cb)
                new CaffeineIndex(cb, -1 /*256 * 1024*/, -1, null /* null = fork join common pool */)
                //new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 300000, 32 * 1024, 3)
                ,
                time,
                exe) {

            final Compressor compressor = new Compressor(this, "_",
                    3, 7,
                    3f, 16, 256);

            @Override
            public Task pre(@NotNull Task t) {
                if (!t.isInput()) {
                    return compressor.encode(t);
                } else {
                    @NotNull Task encoded = compressor.encode(t);
                    if (!encoded.equals(t))
                        process(encoded); //input both forms
                    return t;

                    //return t; //dont affect input
                }
            }

            @NotNull
            @Override
            public Term pre(@NotNull Term t) {
                return compressor.encode(t);
            }

            @NotNull
            @Override
            public Task post(@NotNull Task t) {
                return compressor.decode(t);
            }

            @Override
            @NotNull
            public Term post(@NotNull Term t) {
                return compressor.decode(t);
            }


        };

        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.9f);

        float p = 0.75f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.5f * p;
        nar.DEFAULT_GOAL_PRIORITY = 0.8f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.4f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.4f * p;

        nar.confMin.setValue(0.02f);
        //nar.truthResolution.setValue(0.01f);

        //NARTune tune = new NARTune(nar);

        MySTMClustered stm = new MySTMClustered(nar, 64, BELIEF, 3, true, 6);
        MySTMClustered stmGoal = new MySTMClustered(nar, 32, GOAL, 2, true, 4);

//        Abbreviation abbr = new Abbreviation(nar, "the",
//                4, 16,
//                0.02f, 32);

        new Inperience(nar, 0.005f, 16);

//        //causal accelerator
//        nar.onTask(t -> {
//
//            switch (t.op()) {
//                case IMPL:
//                    //decompose with Goal:Induction
//                    if (t.isBelief()) {
//                        Term subj = t.term(0);
//                        Term pred = t.term(1);
//                        if (pred instanceof Compound && (subj.vars() == 0) && (pred.vars() == 0)) {
//                            Concept postconditionConcept = nar.concept(pred);
//
//                            //if (pred.equals(a1.term()) || pred.equals(a2.term())) {
//                            boolean negate = false;
//                            if (subj.op() == NEG) {
//                                subj = subj.unneg();
//                                negate = true;
//                            }
//                            Concept preconditionConcept = nar.concept(subj);
//                            if (preconditionConcept != null) {
//
//                                int dt = t.dt();
//                                if (dt == DTERNAL)
//                                    dt = 0;
//
//                                for (long when : new long[]{t.occurrence(),
//                                        nar.time(), nar.time() + 1, nar.time() + 2 //, nar.time() + 200, nar.time() + 300}
//                                }) {
//
//                                    if (when == ETERNAL)
//                                        continue;
//
//                                    //TODO project, not just eternalize for other times
//                                    Truth tt = when != t.occurrence() ? t.truth().eternalize() : t.truth();
//
//                                    if (!(postconditionConcept instanceof SensorConcept)) {
//                                        {
//                                            Task preconditionBelief = preconditionConcept.beliefs().top(when);
//                                            if (preconditionBelief != null) {
//                                                Truth postcondition = BeliefFunction.Deduction.apply(preconditionBelief.truth().negated(negate), tt, nar, nar.confMin.floatValue());
//                                                if (postcondition != null) {
//                                                    Task m = new GeneratedTask(pred, '.', postcondition.truth())
//                                                            .evidence(Stamp.zip(t, preconditionBelief))
//                                                            .budget(t.budget())
//                                                            .time(nar.time(), when + dt)
//                                                            .log("Causal Accel");
//                                                    nar.inputLater(m);
//                                                }
//                                            }
//                                        }
//                                        {
//                                            Task preconditionGoal = preconditionConcept.goals().top(when);
//                                            if (preconditionGoal != null) {
//                                                Truth postcondition = GoalFunction.Induction.apply(preconditionGoal.truth().negated(negate), tt, nar, nar.confMin.floatValue());
//                                                if (postcondition != null) {
//                                                    Task m = new GeneratedTask(pred, '!', postcondition.truth())
//                                                            .evidence(Stamp.zip(t, preconditionGoal))
//                                                            .budget(t.budget())
//                                                            .time(nar.time(), when + dt)
//                                                            .log("Causal Accel");
//                                                    nar.inputLater(m);
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                            //}
//                        }
//                    }
//                    break;
//            }
//        });


        return nar;
    }

//    static NAR newALANN(@NotNull Time time, int cores, int coreSize, int coreFires, int coreThreads, int auxThreads) {
//
//        Executioner exe = auxThreads == 1 ? new SynchronousExecutor() {
//            @Override public int concurrency() {
//                return auxThreads + coreThreads;
//            }
//        } : new MultiThreadExecutioner(auxThreads, 1024 * auxThreads).sync(false);
//
//        NAR n = new NAR(time,
//                    new CaffeineIndex(new DefaultConceptBuilder(), 128 * 1024, false, exe),
//                        //new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 512 * 1024, 1024 * 32, 3),
//                    new XorShift128PlusRandom(1),
//                    exe
//        );
//
//        new STMTemporalLinkage(n, 2);
//
//        n.setControl(new AlannControl(n, cores, coreSize, coreFires, coreThreads));
//
//        return n;
//    }

    NAR get();

    //Control getControl(NAR n);
    //n.setControl(getControl(n));

    Executioner getExec();

    Time getTime();

    TermIndex getIndex();

    Random getRandom();/* {
        return new XorShift128PlusRandom(1);
    }*/

    class MutableNARBuilder implements NARBuilder {


        private Executioner exec;
        private Time time;
        private TermIndex index;
        private Random rng;

        @Override
        public NAR get() {
            NAR n = new NAR(getTime(), getIndex(), getRandom(), getExec());

            return n;
        }

        public MutableNARBuilder exec(Executioner exec) {
            this.exec = exec;
            return this;
        }

        public MutableNARBuilder time(Time time) {
            this.time = time;
            return this;
        }

        public MutableNARBuilder index(TermIndex index) {
            this.index = index;
            return this;
        }

        public MutableNARBuilder random(Random rng) {
            this.rng = rng;
            return this;
        }

        @Override
        public Executioner getExec() {
            return exec;
        }

        @Override
        public Time getTime() {
            return time;
        }

        @Override
        public TermIndex getIndex() {
            return index;
        }

        @Override
        public Random getRandom() {
            return rng;
        }
    }


    static class NARTune implements Runnable {
        private final NAR nar;
        final static int outputs = 4, inputs = outputs;
        private final SimpleLSTM net;

        double[] prev, next, predict;
        private float alpha = 0.05f;

        public NARTune(NAR nar) {

            this.nar = nar;

            prev = new double[inputs];
            next = new double[outputs];
            predict = new double[outputs];

            this.net = new SimpleLSTM(nar.random, inputs, outputs, /* estimate: */ inputs * outputs * 2);

            nar.onCycle(this);

        }

        @Override
        public void run() {
            double[] current = new double[outputs];
            current[0] = nar.emotion.learning();
            current[1] = (float) nar.emotion.busyVol.getMean() / Param.COMPOUND_VOLUME_MAX;
            current[2] = (float) nar.emotion.busyPri.getMean();
            current[3] = (float) nar.emotion.confident.getMean();

            double error = MathArrays.distance1(predict, current);

            double[] predictNext = net.learn(prev, current, alpha);

            System.out.println(n2(error) + " err\t" + n4(prev) + " -|> " + n4(current) + " ->? " + n4(predictNext));

            System.arraycopy(predictNext, 0, predict, 0, outputs);
            System.arraycopy(current, 0, prev, 0, outputs);
        }
    }

}
