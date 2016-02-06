package nars.util.signal;

import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.concept.Concept;
import nars.concept.util.BeliefTable;
import nars.op.meta.HaiQ;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Termed;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Q-Learning Coprocessor (NAR Operator/Plugin)
 */
public class NarQ implements Consumer<NAR> {

    private static final Logger logger = LoggerFactory.getLogger(NarQ.class);

    private final NAR nar;

    public HaiQ q = null;
    
    /** master control of strength factor of output activity */
    public final MutableFloat power = new MutableFloat(1f);
    
    
    /** numeric vector percept */
    public static class Vercept  {
        
        /**
         * sensor inputs, each providing at least one float value component of the
         * input vector
         */
        final List<DoubleSupplier> ins = Global.newArrayList();

        public void add(DoubleSupplier d) {
            ins.add(d);
        }
        public void addAll(DoubleSupplier... d) {
            Collections.addAll(ins, d);           
        }
        public void addAll(Collection<? extends DoubleSupplier> d) {
            ins.addAll(d);
        }
        public void clear() { ins.clear(); }
        
        public float[] get(float[] target) {
            int s = ins.size();
            if (target == null || target.length!=s)
                target = new float[s];
            for (int i = 0; i < s; i++) {
                target[i] = (float)ins.get(i).getAsDouble();
            }
            return target;
        }
    }

    final public Vercept input;
    
    
    public static interface Action {
        
        /** activate the procedure with a certain proportional strength from 0..1.0 */
        void run(float strength);
        
        /** estimate of current level of activity; not necessarily the exact value supplied previously to run(f) but that is valid way to compute a simple implementation */
        float ran(); 
    }

    public static Action NullAction = new Action() {
        private float last;
        
        private void clear() {
            last = 0f;
        }
        
        @Override
        public void run(float strength) {
            last = strength;
        }

        @Override
        public float ran() {
            float l = last;
            clear();
            return l;
        }
        
    };
    
    /** inputs a goal task for a given term in proportion to the supplied strength value */
    public static class InputTask implements Action {
        final Termed term;
        final NAR nar;
        private final char punct;
        private final boolean invert;

        @Deprecated public InputTask(NAR n, Termed term) {
            this(n, term, Symbols.GOAL, false);
        }

        public InputTask(NAR n, Termed term, char punct, boolean invert) {
            this.nar = n;
            this.term = term;
            this.punct = punct;
            this.invert = invert;
        }

        @Override
        public void run(float strength) {
            
            //float existingDesire = Math.max(0.5f, NarQ.expectation(nar, term, 0, false, 0) - 0.5f) * 2f; //range 0..1.0 if >0.5, 0 otherwise
            //float additionalDesire = 0.5f + 0.5f * Math.max(0.01f, (1f - existingDesire)); //some minimal amount if already totally maxed out
            
            float existingExp = NarQ.expectation(nar, term, 0.5f, false, 0); //range 0..1.0 if >0.5, 0 otherwise
            float additionalExp = Math.max(0.1f, strength - existingExp);
            
            
            //TODO solve for strength/additional desire so expectation is correct
            final Task t = new MutableTask(term, punct).truth(invert ? 0f : 1f, additionalExp)
                    //.time(Tense.Future, nar.memory)                   
                    .time(nar.time(), nar.time() + 1)
                    .log("Q Action");
            //logger.info("q act: {}", t );
            nar.input(t);

        }       

        @Override
        public float ran() {
            int dt = 0; //nar.memory.duration()/2; //-1; //duration/2?
            float e =  NarQ.expectation(nar, term, 0.5f /* equal pos/neg opportunity */, punct == Symbols.GOAL ? false : true, dt /* desire in previous time */);
            if (invert) e = 1f- e;
            return e;
        }     
        
        
    }
    
    /**
     * terms representing goals that can be activated in the hope of maximizing
     * future reward
     */
    public final List<Action> outs = Global.newArrayList();

    /**
     * reward sensor x strength/factor
     */
    public final Map<DoubleSupplier /* TODO make FloatSupplier */, MutableFloat> reward = Global.newHashMap();

    /**
     * reward bias
     * negative value: always hope for your discontent
     */
    float rewardBias = -0.5f;

    public NarQ(NAR n, Vercept input) {
        this.nar = n;
        this.input = input;
        nar.onFrame(this);
    }

    /**
     * called each frame by the NAR
     */
    @Override
    public void accept(NAR t) {
        float[] ii = inputs();
        if (ii != null) {
            if (validDimensionality(ii.length)) {
                act(q.act(ii, reward()));
            }
        }
    }

    protected boolean validDimensionality(int inputs) {
        final int outputs = outs.size();

        if (inputs == 0 || outputs == 0 || reward.isEmpty()) {
            q = null;
            return false;
        }

        if (q == null || q.inputs() != inputs || q.actions() != outputs) {
            //TODO allow substituting an arbitrary I/O agent interface
            q = new HaiQImpl(inputs, inputs*3, outputs);
        }

        return true;
    }

    public List<? extends DoubleSupplier> getBeliefExpectations(String... terms) {
        return Stream.of(nar.terms(terms)).map(t -> new BeliefExpectation(nar, t)).collect(Collectors.toList());
    }

    public List<? extends DoubleSupplier> getBeliefRewards(String... terms) {
        return Stream.of(nar.terms(terms)).map(t -> new BeliefReward(nar, t)).collect(Collectors.toList());
    }

    private class HaiQImpl extends HaiQ {

        public HaiQImpl(int inputs, int states, int outputs) {
            super(inputs, states, outputs);
        }

        @Override
        protected int nextAction(int state) {
            //alpha is applied elsewhere, so here directly choose
            return choose(state);
        }

        @Override
        protected int lastAction() {
            //evaluate the actual desire values of the action concepts to get a sense of what the NAR actually wants/wanted to do
            //and what it ideally would have done if it could execute them all in parallel in proportional amounts

            //Termed best = null;
            int best = -1;
            float bestE = Float.NEGATIVE_INFINITY;

            int s = outs.size();

            final float a = q.Alpha;


            for (int j = 0; j < s; j++) {

                float e = outs.get(j).ran();

                //add noise
                if (a != 0) {
                    e += 2f * (q.rng.nextFloat() - 0.5f) * (float) q.Alpha;
                }

                //System.out.println("last action: " + j + " "  + e);
                if (e > bestE) {
                    //best = c; 
                    best = j;
                    bestE = e;
                }
            }

            return best;
        }

    }

    float[] inputBuffer;

    private float[] inputs() {
        return (this.inputBuffer = input.get(this.inputBuffer));
    }

//    private float expectation(int j, float ifNonExists, boolean beliefOrDesire, int dt) {
//        Termed t = outs.get(j);
//        if (t == null) {
//            return ifNonExists;
//        }
//        //TODO cache the Concept reference to avoid lookup but invalidate it if the Concept is deleted so that the new one can be retrieved after
//        return expectation(t, ifNonExists, beliefOrDesire, dt);
//    }

    public float expectation(Termed x, float ifNonExists, boolean beliefOrDesire, int dt) {
        return expectation(nar, x, ifNonExists, beliefOrDesire, dt);
    }

    public static float expectation(NAR nar, Termed x, float ifNonExists, boolean beliefOrDesire, int dt) {
        return NarQ.expectation(nar, nar.concept(x), ifNonExists, beliefOrDesire, dt);
    }

    public static float expectation(NAR n, Concept cx, float ifNonExists, boolean beliefOrDesire, int dt) {
        //TODO this an be optimized
        long now = n.time();

        float v = ifNonExists;
        if (cx != null) {
            BeliefTable table = beliefOrDesire ? cx.beliefs() : cx.goals();
            Task t = table.top(now + dt, now);
            if (t != null) {
                v = t.expectation();
            }
        }
        return v;
    }

    abstract public static class BeliefSensor implements DoubleSupplier {

        public final Termed term;

        public final NAR nar;

        public BeliefSensor(NAR nar, Termed t) {
            this.term = t;
            this.nar = nar;
        }

    }

    /** positive expectation mapped to a -1,+1 range */
    public static class BeliefReward extends BeliefSensor {

        public BeliefReward(NAR nar, String term) {
            this(nar, (Termed) nar.term(term));
        }

        public BeliefReward(NAR nar, Termed t) {
            super(nar, t);
        }

        protected float expectation() {
            return NarQ.expectation(nar, (Termed)term, 0f, true, 0);
        }

        @Override
        public double getAsDouble() {
            return (expectation() - 0.5f) * 2f;
        }

    }

    /** negative expectation mapped to a -1,+1 range */
    public static class NotBeliefReward extends BeliefReward {

        public NotBeliefReward(NAR nar, String term) {
            super(nar, term);
        }

        @Override
        public float expectation() {
            return 1f - super.expectation();
        }

    }

    /** expectation directly */
    public static class BeliefExpectation extends BeliefSensor {

        public BeliefExpectation(NAR nar, Termed t) {
            super(nar, t);
        }

        @Override
        public double getAsDouble() {
            return expectation(nar, term, 0, true, 0);
        }

    }

    private float reward() {
        //return sum of expectation truth value of the assigned reward concepts(s)

        final float r[] = new float[]{0};

        reward.forEach((sensor, weight) -> {
            float v = ((float) sensor.getAsDouble());
            v *= weight.floatValue();
            r[0] += v;
        });

        return (r[0] / reward.size()) - rewardBias;
    }

    void act(int x) {

        //desire action of the given action concept
        //logger.info("act: " + x);
//        Concept c = nar.concept(outs[x]);
//        if (c!=null) {
//            
//        }
//        float onF = 1f, offF = 0.5f;
//        for (int a = 0; a < outs.length; a++) {
//            final Task t = new MutableTask(outs[a]).goal().truth(a == x ? onF : offF, 0.7f).time(Tense.Future, nar.memory).log("Q Action");
//            //logger.info("q act: {}", t );
//            nar.input(t);
//        }

        final float p  = power.floatValue();        
        if (p > 0) {
            final List<Action> o = outs;               
            Action a = o.get(x);               
            if (a!=null)
                a.run(p);
        }
        
    }

}
