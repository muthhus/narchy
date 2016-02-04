package nars.util.signal;

import com.gs.collections.api.set.ImmutableSet;
import com.gs.collections.impl.factory.Sets;
import java.util.stream.Stream;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.util.BeliefTable;
import nars.nal.Tense;
import nars.op.meta.HaiQ;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by me on 2/3/16.
 */
public class hai {

    private static final Logger logger = LoggerFactory.getLogger(hai.class);

    private final NAR nar;
    public HaiQ q = null;
    private Termed[] ins;
    private Termed[] outs;
    private Termed[] reward;
    
    /** always hope for your discontent */
    float rewardBias = -0.1f; 

    public hai(NAR n) {
        this.nar = n;
        nar.onFrame(nn -> {
            if (q == null) return;

            float[] ii = inputs();
            if (ii==null)
                return;
            
            int a = q.act(ii, reward());
            act(a);
        });
    }

    public void set(Iterable<? extends Termed> inputs, Iterable<? extends Termed> reward, Iterable<? extends Termed> outputs) {
        reset(Sets.immutable.ofAll(inputs),
                Sets.immutable.ofAll(reward),
                Sets.immutable.ofAll(outputs));
    }

    private void reset(ImmutableSet<Termed> ins, ImmutableSet<Termed> reward, ImmutableSet<Termed> outs) {

        logger.info("reset: inputs={}, outputs={}, reward=" + reward, ins, outs);

        this.ins = ins.toArray(new Term[ins.size()]);
        this.outs = outs.toArray(new Term[outs.size()]);
        this.reward = reward.toArray(new Term[reward.size()]);
        
        logger.info("Q: " + ins + " x " + reward + " -> " + outs);
        
        int states = this.ins.length;
        this.q = new HaiQ(states, states, this.outs.length) {
            
            @Override
            protected int nextAction(int state) {
                //alpha is instead applied in interpreting the compared desire expectations
                return choose(state);
            }
            
            
            @Override
            protected int lastAction() {
                //evaluate the actual desire values of the action concepts to get a sense of what the NAR actually wants/wanted to do
                //and what it ideally would have done if it could execute them all in parallel in proportional amounts
                
                //Termed best = null;
                int best = -1;
                float bestE = Float.NEGATIVE_INFINITY;
                
                for (int j = 0; j < hai.this.outs.length; j++) {
                    
                    Termed t = hai.this.outs[j];                 
                    
                    float e = eval(t.term(), -1, false, -1 /* desire in previous time */);
                    
                    e += 2f * (q.rng.nextFloat()-0.5f) * (float)q.Alpha;
                    
                    //System.out.println("last action: " + t + " "  + e);
                    if (e > 0.5 && e > bestE) {
                        //best = c; 
                        best = j;
                        bestE = e;
                    }
                }
                
                return best;
            }
         
            
        };
    }
    
    


    float[] inputBuffer;

    private float[] inputs() {
        if (this.ins == null) return null;
        
        //return expectation truth values of the assigned input concepts
        if (inputBuffer == null || inputBuffer.length != ins.length)
            inputBuffer = new float[ins.length];

        Termed[] ii = this.ins;
        float[] ib = this.inputBuffer;
        for (int i = 0, l = ii.length; i < l; i++) {
            Termed tt = ii[i];          
            ib[i] = (tt!=null) ? eval(tt.term(), 0, true, 0) : 0;           
        }

        //logger.info("input: {}", inputBuffer);

        return inputBuffer;
    }

    private float eval(Term x, float ifNonExists, boolean beliefOrDesire, int dt) {
        return eval(nar.concept(x), ifNonExists, beliefOrDesire, dt);
    }

    private float eval(Concept cx, float ifNonExists, boolean beliefOrDesire, int dt) {
        long now = nar.time();

        float v = ifNonExists;
        if (cx!=null) {
            BeliefTable table = beliefOrDesire ? cx.beliefs() : cx.goals();
            Task t = table.top(now+dt, now);
            if (t!=null)  {
                v = t.expectation();
            }            
        }
        return v;
    }

    private float reward() {
        //return sum of expectation truth value of the assigned reward concepts(s)

        float r = 0;
        Termed[] rr = this.reward;
        final int rewards = rr.length;
        if (rewards == 0) return 0f;
        
        for (int i = 0, l = rewards; i < l; i++) {
            r += (eval(rr[i].term(), 0, true, 0)-0.5f)*2f;
        }
        r/=rewards;

        return r - rewardBias;
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

            float existingDesire = Math.max(0.5f, eval(outs[x].term(), 0, false, 0) - 0.5f)*2f; //range 0..1.0 if >0.5, 0 otherwise
            float additionalDesire = 0.5f + 0.5f * Math.max(0.01f, (1f - existingDesire)); //some minimal amount if already totally maxed out
            final Task t = new MutableTask(outs[x]).goal().truth(additionalDesire, 1f/outs.length)
                    //.time(Tense.Future, nar.memory)                   
                    .time(nar.time(), nar.time()+1)
                    .log("Q Action");
            //logger.info("q act: {}", t );
            nar.input(t);
    }


}
