package nars.nar;

import jcog.bag.Bag;
import jcog.bag.impl.hijack.DefaultHijackBag;
import jcog.learn.lstm.SimpleLSTM;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.conceptualize.DefaultConceptBuilder;
import nars.conceptualize.state.DefaultConceptState;
import nars.index.term.HijackTermIndex;
import nars.index.term.TermIndex;
import nars.index.term.map.CaffeineIndex;
import nars.op.stm.MySTMClustered;
import nars.term.Term;
import nars.time.Time;
import nars.util.exe.Executioner;
import nars.util.exe.MultiThreadExecutor;
import org.apache.commons.math3.util.MathArrays;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;

import static jcog.Texts.n2;
import static jcog.Texts.n4;
import static nars.Op.BELIEF;

/**
 * Created by me on 12/27/16.
 */
public interface NARBuilder {

    static Default newMultiThreadNAR(int cores, Time time) {
        return newMultiThreadNAR(cores, time, true);
    }

    static Default newMultiThreadNAR(int threads, Time time, boolean sync) {

        if (threads == -1)
            threads = 3;
                    //(int) Math.ceil(Runtime.getRuntime().availableProcessors()-2);

        Executioner exe =
                //new SynchronousExecutor();
                new MultiThreadExecutor(threads, 1024, sync);

        //exe = new InstrumentedExecutor(exe, 8);


        final int reprobes = 4;

        //Multi nar = new Multi(3,512,
        DefaultConceptBuilder cb = new DefaultConceptBuilder(
                new DefaultConceptState("sleep", 16, 16, 3, 24, 16),
                new DefaultConceptState("awake", 32, 32, 4, 32, 24)
        ) {
            @Override
            public <X> X withBags(Term t, BiFunction<Bag<Term, PLink<Term>>, Bag<Task, PLink<Task>>, X> f) {
                Bag<Term, PLink<Term>> termlink = new DefaultHijackBag<>(DefaultConceptBuilder.DEFAULT_BLEND, reprobes);
                Bag<Task, PLink<Task>> tasklink = new DefaultHijackBag<>(DefaultConceptBuilder.DEFAULT_BLEND, reprobes);
                return f.apply(termlink, tasklink);
            }

            @NotNull
            @Deprecated @Override
            public <X> Bag<X, PLink<X>> newBag(@NotNull Map m, PriMerge blend) {
                return new DefaultHijackBag<>(blend, reprobes);
            }
        };


        int maxConcepts = 192 * 1024;

        int activeConcepts = 1024;

        Default nar = new Default(activeConcepts,

                //new HijackTermIndex(cb, maxConcepts, reprobes)

                //new NullTermIndex(cb)

                new CaffeineIndex(cb, /* -1 */ maxConcepts * 3 /* by weight */, -1,
                    exe
                    //null /* null = fork join common pool */
                )

//              new TreeTermIndex(new DefaultConceptBuilder(), 300000, 32 * 1024, 3)
                ,time,
                exe) {

//            @Override
//            public Bag<Concept,PLink<Concept>> newConceptBag(int initialCapacity) {
//                return new PLinkHijackBag(initialCapacity, reprobes);
//            }

//            @Override
//            public Deriver newDeriver() {
//                //return new InstrumentedDeriver((TrieDeriver) (DefaultDeriver.the));
//                return Deriver.get("induction.nal", "nal6.nal");
//            }

//            @Override
//            public PreferSimpleAndPolarized newDerivationBudgeting() {
//                return new PreferSimpleAndPolarized() {
//                    @Override
//                    public Priority budget(@NotNull Derivation d, @NotNull Compound conclusion, @Nullable Truth truth, byte punc, long start, long end) {
//                        Priority p = super.budget(d, conclusion, truth, punc, start, end);
//                        if (start!=ETERNAL && start >= dur() + time()) {
//                            p.priMult(2);
//                        }
//                        return p;
//                    }
//                };
//            }


            //            @Override
//            public MatrixPremiseBuilder newPremiseBuilder() {
//                return new MatrixPremiseBuilder() {
//                    @Override
//                    public @NotNull Premise newPremise(@NotNull Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
//                        return new PreferSimpleAndConfidentPremise(c, task, beliefTerm, belief, pri, qua) {
//                            @Override
//                            protected float priFactor(Compound conclusion, @Nullable Truth truth, byte punc, Task task, Task belief) {
//                                float p = super.priFactor(conclusion, truth, punc, task, belief);
//
//                                if (punc == GOAL)
//                                    return 1f;
//
//                                switch (conclusion.op()) {
//                                    case NEG:
//                                        throw new RuntimeException("shouldnt happen");
//
//                                    case INH:
//                                        if (Op.isOperation(conclusion))
//                                            p *= 1f;
//                                        else
//                                            p *= 0.8f;
//                                        break;
//
//                                    case CONJ:
//                                        if (conclusion.vars() > 0)
//                                            p*=1f;
//                                        else
//                                            p*=0.8f;
//                                        break;
//
//                                    case EQUI:
//                                    case IMPL:
//                                        p *= 1f;
//                                        break;
//                                    default:
//                                        p *= 0.8f;
//                                        break;
//                                }
//                                return p;
//                            }
//                        };
//                    }
//                };
//            }

//            final static int COMPRESS_ABOVE_COMPLEXITY = 16;
//            final Compressor compressor = new Compressor(this, "_",
//                    4, 20,
//                    0.25f, 64, 16);
//
//            @Override
//            public Task pre(@NotNull Task t) {
//                if (!t.isInput() && t.volume() > COMPRESS_ABOVE_COMPLEXITY) {
//                    return compressor.encode(t);
//                } else {
//                    //@NotNull Task encoded = compressor.encode(t);
////                    if (!encoded.equals(t))
////                        process(t); //input both forms
//                    //return encoded;
//
//                    return t; //dont affect input
//                }
//            }
//
//            @NotNull
//            @Override
//            public Term pre(@NotNull Term t) {
//                if (t.volume() > COMPRESS_ABOVE_COMPLEXITY)
//                    return compressor.encode(t);
//                else
//                    return t;
//            }
//
//            @NotNull
//            @Override
//            public Task post(@NotNull Task t) {
//                return compressor.decode(t);
//            }
//
//            @Override
//            @NotNull
//            public Term post(@NotNull Term t) {
//                return compressor.decode(t);
//            }
//
//
        };

        nar.deriver.rate.setValue(0.05f);

        nar.termVolumeMax.setValue(48);

        nar.beliefConfidence(0.75f);
        nar.goalConfidence(0.5f);

        float p = 0.5f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.75f * p;
        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.5f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f * p;

        nar.focus.activationRate.setValue(1f/Math.sqrt(activeConcepts));

        //nar.stmLinkage.capacity.set(0);

        //nar.activationRate.setValue(0.5f);
        nar.confMin.setValue(0.01f);
        nar.truthResolution.setValue(0.01f);


        //NARTune tune = new NARTune(nar);

        MySTMClustered stm = new MySTMClustered(nar, 256, BELIEF, 4, false, 32);
        //MySTMClustered stmGoal = new MySTMClustered(nar, 32, GOAL, 2, false, 16);

//        Abbreviation abbr = new Abbreviation(nar, "the",
//                4, 16,
//                0.02f, 32);

        //new Inperience(nar, 0.25f, 6);

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

//    class MutableNARBuilder implements NARBuilder {
//
//
//        private Executioner exec;
//        private Time time;
//        private TermIndex index;
//        private Random rng;
//
//        @Override
//        public NAR get() {
//            NAR n = new NAR(getTime(), getIndex(), getRandom(), getExec());
//
//            return n;
//        }
//
//        public MutableNARBuilder exec(Executioner exec) {
//            this.exec = exec;
//            return this;
//        }
//
//        public MutableNARBuilder time(Time time) {
//            this.time = time;
//            return this;
//        }
//
//        public MutableNARBuilder index(TermIndex index) {
//            this.index = index;
//            return this;
//        }
//
//        public MutableNARBuilder random(Random rng) {
//            this.rng = rng;
//            return this;
//        }
//
//        @Override
//        public Executioner getExec() {
//            return exec;
//        }
//
//        @Override
//        public Time getTime() {
//            return time;
//        }
//
//        @Override
//        public TermIndex getIndex() {
//            return index;
//        }
//
//        @Override
//        public Random getRandom() {
//            return rng;
//        }
//    }


    class NARTune implements Runnable {
        private final NAR nar;
        final static int outputs = 4, inputs = outputs;
        private final SimpleLSTM net;

        double[] prev, next, predict;
        private final float alpha = 0.05f;

        public NARTune(NAR nar) {

            this.nar = nar;

            prev = new double[inputs];
            next = new double[outputs];
            predict = new double[outputs];

            this.net = new SimpleLSTM(nar.random(), inputs, outputs, /* estimate: */ inputs * outputs * 2);

            nar.onCycle(this);

        }

        @Override
        public void run() {
            double[] current = new double[outputs];
            current[0] = nar.emotion.learningVol();
            current[1] = nar.emotion.busyVol.getMean() / Param.COMPOUND_VOLUME_MAX;
            current[2] = nar.emotion.busyPri.getMean();
            current[3] = nar.emotion.confident.getMean();

            double error = MathArrays.distance1(predict, current);

            double[] predictNext = net.learn(prev, current, alpha);

            System.out.println(n2(error) + " err\t" + n4(prev) + " -|> " + n4(current) + " ->? " + n4(predictNext));

            System.arraycopy(predictNext, 0, predict, 0, outputs);
            System.arraycopy(current, 0, prev, 0, outputs);
        }
    }

}
