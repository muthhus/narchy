//package nars.op;
//
//import nars.Global;
//import nars.NAR;
//import nars.Symbols;
//import nars.data.Range;
//
//import nars.task.Task;
//import nars.term.Termed;
//import nars.truth.DefaultTruth;
//import nars.util.HaiQ;
//import nars.util.data.random.XorShift128PlusRandom;
//import nars.util.signal.Autoencoder;
//import org.apache.commons.lang3.mutable.MutableFloat;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.function.Consumer;
//import java.util.function.DoubleSupplier;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
///**
// * Q-Learning Autonomic Coprocessor (NAR Operator/Plugin)
// */
//public class NarQ implements Consumer<NAR> {
//
//    //private static final Logger logger = LoggerFactory.getLogger(NarQ.class);
//
//    private final NAR nar;
//    private final StateCompressionRatio stateCompression;
//
//    public @Nullable HaiQ q;
//
//    /** master control of strength factor of output activity */
//    @Range(min=0f, max=1f)
//    public final MutableFloat power = new MutableFloat(1f);
//
//    /**
//     * reward bias
//     * negative value: always hope for your discontent
//     */
//    @Range(min=-1f, max=1f)
//    public final MutableFloat rewardBias = new MutableFloat(-0.1f);
//
//    /** numeric vector percept */
//    public static class Vercept  {
//
//        /**
//         * sensor inputs, each providing at least one float value component of the
//         * input vector
//         */
//        final List<DoubleSupplier> ins = Global.newArrayList();
//
//        public void add(DoubleSupplier d) {
//            ins.add(d);
//        }
//        public void addAll(DoubleSupplier... d) {
//            Collections.addAll(ins, d);
//        }
//        public void addAll(@NotNull Collection<? extends DoubleSupplier> d) {
//            ins.addAll(d);
//        }
//        public void clear() { ins.clear(); }
//
//        @Nullable
//        public float[] get(@Nullable float[] target) {
//            int s = ins.size();
//            if (target == null || target.length!=s)
//                target = new float[s];
//            for (int i = 0; i < s; i++) {
//                target[i] = (float)ins.get(i).getAsDouble();
//            }
//            return target;
//        }
//    }
//
//    @NotNull
//    final public Vercept input;
//
//
//    public interface Action {
//
//        /** activate the procedure with a certain proportional strength from 0..1.0 */
//        void run(float strength);
//
//        /** estimate of current level of activity; not necessarily the exact value supplied previously to run(f) but that is valid way to compute a simple implementation */
//        float ran();
//    }
//
//    @NotNull
//    public static Action NullAction = new Action() {
//        private float last;
//
//        private void clear() {
//            last = 0f;
//        }
//
//        @Override
//        public void run(float strength) {
//            last = strength;
//        }
//
//        @Override
//        public float ran() {
//            float l = last;
//            clear();
//            return l;
//        }
//
//    };
//
//    /** inputs a goal task for a given term in proportion to the supplied strength value */
//    public static class InputTask implements Action {
//        final Termed term;
//        final NAR nar;
//        private final byte punct;
//        private final boolean invert;
//
//        @Deprecated public InputTask(NAR n, Termed term) {
//            this(n, term, Symbols.GOAL, false);
//        }
//
//        @NotNull
//        @Override
//        public String toString() {
//            return ((invert) ? "--" : "") + term.toString() + punct;
//        }
//
//        public InputTask(NAR n, Termed term, byte punct, boolean invert) {
//            this.nar = n;
//            this.term = term;
//            this.punct = punct;
//            this.invert = invert;
//        }
//
//        @Override
//        public void run(float strength) {
//
//            //float strengthExp = strength/2f + 0.5f; //strength -> expectation
//
//
//            float existingExp = NarQ.motivation(nar, term, 0f, false, 0); //range 0..1.0 if >0.5, 0 otherwise
//            float additionalExp = strength - existingExp;
//            if (additionalExp < 0) //already desired at least at this level
//                return;
//
//            float conf = strength;
//
//            int dt = 0;
//            //TODO solve for strength/additional desire so expectation is correct
//            long now = nar.time();
//            final Task t = new TaskBuilder(term, punct, new DefaultTruth(invert ? 0f : 1f, conf))
//                    //.time(Tense.Future, nar.memory)
//                    .time(now, now + dt )
//                    .log("Q Action");
//            //logger.info("q act: {}", t );
//            nar.input(t);
//
//        }
//
//        @Override
//        public float ran() {
//            int dt = 0; //nar.memory.duration()/2; //-1; //duration/2?
//            float e =  NarQ.motivation(nar, term, 0f /* equal pos/neg opportunity */, punct != Symbols.GOAL, dt /* desire in previous time */);
//            //e = (e-0.5f)*2f; //expectation -> strength?
//            if (invert) e = -e;
//            return e;
//        }
//
//
//    }
//
//    /**
//     * terms representing goals that can be activated in the hope of maximizing
//     * future reward
//     */
//    public final List<Action> output = Global.newArrayList();
//
//    /**
//     * reward sensor x strength/factor
//     */
//    public final Map<DoubleSupplier /* TODO make FloatSupplier */, MutableFloat> goal = Global.newHashMap();
//
//
//
//    @FunctionalInterface  public interface StateCompressionRatio {
//        int states(int inputs, int outputs);
//    }
//
//    public NarQ(NAR n) {
//        this(n, (i,o)->(1+i)*(1+o) /*default */);
//    }
//
//    public NarQ(NAR n, StateCompressionRatio s) {
//        this.nar = n;
//        this.input = new Vercept();
//        this.stateCompression = s;
//        nar.onFrame(this);
//    }
//
//    /**
//     * called each frame by the NAR
//     */
//    @Override
//    public void accept(NAR t) {
//        if (power.floatValue() == 0)
//            return; //do nothing
//
//        float[] ii = inputs();
//        if (ii != null) {
//            if (validDimensionality(ii.length)) {
//                act(q.act(reward(), ii));
//            }
//        }
//    }
//
//    protected boolean validDimensionality(int inputs) {
//        final int outputs = output.size();
//
//        if (inputs == 0 || outputs == 0 || goal.isEmpty()) {
//            q = null;
//            return false;
//        }
//
//        if (q == null || q.inputs() != inputs || q.actions() != outputs) {
//            //TODO allow substituting an arbitrary I/O agent interface
//            q = new HaiQImpl(inputs, stateCompression.states(inputs, outputs), outputs);
//        }
//
//        return true;
//    }
//
//    public List<? extends DoubleSupplier> getBeliefMotivations(Termed... terms) {
//        return Stream.of(terms).map(t -> new BeliefMotivation(nar, t)).collect(Collectors.toList());
//    }
//    public List<? extends DoubleSupplier> getBeliefMotivations(String... terms) {
//        return getBeliefMotivations(nar.terms(terms));
//    }
//
//    public List<? extends DoubleSupplier> getBeliefRewards(String... terms) {
//        return Stream.of(nar.terms(terms)).map(t -> new BeliefReward(nar, t)).collect(Collectors.toList());
//    }
//
//    private class HaiQImpl extends HaiQ {
//
//        //Hsom...
//        final static float perceptionAlpha = 0.04f;
//        @NotNull
//        final Autoencoder ae;
//
//        public HaiQImpl(int inputs, int states, int outputs) {
//            super(states, outputs);
//            ae = new Autoencoder(inputs, states, new XorShift128PlusRandom(1));
//        }
//
//        @Override
//        protected int nextAction(int state) {
//            //alpha is applied elsewhere, so here directly choose
//            return choose(state);
//        }
//
//        @Override
//        protected int perceive(float[] input) {
//            ae.train(input, perceptionAlpha,  0.05f, 0.02f, true);
//            int w = ae.max();
//            return w;
//        }
//
//        @Override
//        protected int lastAction() {
//            //evaluate the actual desire values of the action concepts to get a sense of what the NAR actually wants/wanted to do
//            //and what it ideally would have done if it could execute them all in parallel in proportional amounts
//
//            //Termed best = null;
//            int best = -1;
//            float bestE = Float.NEGATIVE_INFINITY;
//
//            int s = output.size();
//
//            final float epsi = Epsilon;
//
//
//            for (int j = 0; j < s; j++) {
//
//                float e = output.get(j).ran();
//
//                //add noise
//                if (epsi != 0) {
//                    e += epsi * (rng.nextFloat() - 0.5f) * 2f;
//                }
//
//                //System.out.println(outs.get(j) + " " + e);
//
//                //System.out.println("last action: " + j + " "  + e);
//                if (e >= bestE) {
//                    //best = c;
//                    best = j;
//                    bestE = e;
//                }
//            }
//
//            return best;
//        }
//
//    }
//
//    @Nullable float[] inputBuffer;
//
//    @Nullable
//    private float[] inputs() {
//        return (this.inputBuffer = input.get(this.inputBuffer));
//    }
//
////    private float expectation(int j, float ifNonExists, boolean beliefOrDesire, int dt) {
////        Termed t = outs.get(j);
////        if (t == null) {
////            return ifNonExists;
////        }
////        //TODO cache the Concept reference to avoid lookup but invalidate it if the Concept is Deleted so that the new one can be retrieved after
////        return expectation(t, ifNonExists, beliefOrDesire, dt);
////    }
//
////    public float motivation(Termed x, float ifNonExists, boolean beliefOrDesire, int dt) {
////        return motivation(nar, x, ifNonExists, beliefOrDesire, dt);
////    }
//
//    public static float motivation(@NotNull NAR nar, Termed x, float ifNonExists, boolean beliefOrDesire, int dt) {
//        //TODO
//        return nar.concept(x).belief(nar.time()).expectation();
//        //return ((OperationConcept)nar.concept(x)).motivation(nar);
//        //return NarQ.motivation(nar, nar.concept(x), ifNonExists, beliefOrDesire, dt);
//    }
//
////    public static float motivation(@NotNull NAR n, @Nullable Concept cx, float ifNonExists, boolean beliefOrDesire, int dt) {
////        //TODO this an be optimized
////
////        float v = ifNonExists;
////        if (cx != null) {
////            long now = n.time();
////            BeliefTable table = beliefOrDesire ? cx.beliefs() : cx.goals();
////            Truth t = table.truth(now + dt, now, n.duration());
////            if (t != null) {
////                v = t.motivation();
////            }
////        }
////        return v;
////    }
//
//    abstract public static class BeliefSensor implements DoubleSupplier {
//
//        public final Termed term;
//
//        public final NAR nar;
//
//        public BeliefSensor(NAR nar, Termed t) {
//            this.term = t;
//            this.nar = nar;
//        }
//
//    }
//
//    /** positive expectation mapped to a -1,+1 range */
//    public static class BeliefReward extends BeliefSensor {
//
//        public BeliefReward(@NotNull NAR nar, @NotNull String term) {
//            this(nar, (Termed) nar.term(term));
//        }
//
//        public BeliefReward(NAR nar, Termed t) {
//            super(nar, t);
//        }
//
//        protected float expectation() {
//            return NarQ.motivation(nar, term, 0f, true, 0);
//        }
//
//        @Override
//        public double getAsDouble() {
//            return (expectation() - 0.5f) * 2f;
//        }
//
//    }
//
//    /** negative motivation mapped to a -1,+1 range */
//    public static class NotBeliefReward extends BeliefReward {
//
//        public NotBeliefReward(@NotNull NAR nar, @NotNull Termed term) {
//            super(nar, term);
//        }
//
//        public NotBeliefReward(@NotNull NAR nar, @NotNull String term) {
//            super(nar, term);
//        }
//
//        @Override
//        public float expectation() {
//            return 1f - super.expectation();
//        }
//
//    }
//
//    /** expectation directly */
//    public static class BeliefMotivation extends BeliefSensor {
//
//        public BeliefMotivation(NAR nar, Termed t) {
//            super(nar, t);
//        }
//
//        @Override
//        public double getAsDouble() {
//            return motivation(nar, term, 0, true, 0);
//        }
//
//    }
//
//    private float reward() {
//        //return sum of expectation truth value of the assigned reward concepts(s)
//
//        final float r[] = new float[]{0};
//
//        goal.forEach((reward, weight) -> {
//            r[0] += ((float) reward.getAsDouble()) * weight.floatValue();
//        });
//
//        return (r[0] / goal.size()) - rewardBias.floatValue();
//    }
//
//    void act(int x) {
//
//        //desire action of the given action concept
//        //logger.info("act: " + x);
////        Concept c = nar.concept(outs[x]);
////        if (c!=null) {
////
////        }
////        float onF = 1f, offF = 0.5f;
////        for (int a = 0; a < outs.length; a++) {
////            final Task t = new TaskBuilder(outs[a]).goal().truth(a == x ? onF : offF, 0.7f).time(Tense.Future, nar.memory).log("Q Action");
////            //logger.info("q act: {}", t );
////            nar.input(t);
////        }
//
//        final float p  = power.floatValue();
//        if (p > 0) {
//            output.get(x).run(p);
//        }
//
//    }
//
//}
