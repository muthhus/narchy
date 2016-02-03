package nars.util.signal;

import com.gs.collections.api.set.ImmutableSet;
import com.gs.collections.impl.factory.Sets;
import nars.NAR;
import nars.concept.Concept;
import nars.op.meta.HaiQ;
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
    private HaiQ q = null;
    private Termed[] ins;
    private Termed[] outs;
    private Termed[] reward;

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
        ;
        int states = this.ins.length;
        this.q = new HaiQ(states, states, this.outs.length);
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
            ib[i] = (tt!=null) ? eval(tt.term(), 0) : 0;           
        }

        logger.info("input: {}", inputBuffer);

        return inputBuffer;
    }

    private float eval(Term x, float ifNonExists) {
        return eval(nar.concept(x), ifNonExists);
    }

    private float eval(Concept cx, float ifNonExists) {
        long now = nar.time();

        float v;
        if ((cx != null) && (cx.hasBeliefs())) {
            Task t = cx.beliefs().topTemporal(now, now);
            //System.out.println(t);
            v = t.expectation();
        } else {
            v = ifNonExists;
        }
        return v;
    }

    private float reward() {
        //return sum of expectation truth value of the assigned reward concepts(s)

        float r = 0;
        Termed[] rr = this.reward;
        for (int i = 0, l = rr.length; i < l; i++) {
            r += eval(rr[i].term(), 0);
        }

        return r;
    }

    void act(int x) {
        //desire action of the given action concept
        //logger.info("act: " + x);
    }


}
