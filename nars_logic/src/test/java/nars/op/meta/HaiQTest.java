package nars.op.meta;

import com.gs.collections.api.set.ImmutableSet;
import com.gs.collections.impl.factory.Sets;
import nars.NAR;
import nars.concept.Concept;
import nars.java.Naljects;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Term;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by me on 2/2/16.
 */
public class HaiQTest {

    public static class hai  {

        private static final Logger logger = LoggerFactory.getLogger(hai.class);

        private final NAR nar;
        private HaiQ q = null;
        private Term[] ins;
        private Term[] outs;
        private Term[] reward;

        public hai(NAR n) {
            this.nar = n;
            nar.onFrame(nn->{
                if (q == null) return;

                int a = q.act(inputs(), reward());
                act(a);
            });
        }

        public void set(Iterable<Term> inputs, Iterable<Term> reward, Iterable<Term> outputs) {
            reset(Sets.immutable.ofAll(inputs),
                    Sets.immutable.ofAll(reward),
                    Sets.immutable.ofAll(outputs));
        }

        private void reset(ImmutableSet<Term> ins, ImmutableSet<Term> reward, ImmutableSet<Term> outs) {

            logger.info("reset: inputs={}, outputs={}, reward=" + reward, ins, outs);

            this.ins = ins.toArray(new Term[ins.size()]);
            this.outs = outs.toArray(new Term[outs.size()]);
            this.reward = reward.toArray(new Term[reward.size()]);;
            int states = this.ins.length;
            this.q = new HaiQ(states, states, this.outs.length);
        }


        float[] inputBuffer;

        private float[] inputs() {
            //return expectation truth values of the assigned input concepts
            if (inputBuffer == null || inputBuffer.length != ins.length)
                inputBuffer = new float[ins.length];

            for (int i = 0, l = ins.length; i < l; i++) {
                inputBuffer[i] = eval(ins[i], 0);
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
            if ((cx!=null) && (cx.hasBeliefs())) {
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
            for (int i = 0, l = reward.length; i < l; i++) {
                r += eval(reward[i], 0);
            }

            return r;
        }

        void act(int x) {
            //desire action of the given action concept
            //logger.info("act: " + x);
        }


    }

    @Test
    public void testSimple() throws Exception {

        NAR n = new Default(200, 4, 3, 2);
        //n.log();

        new Naljects(n).the("q", hai.class, n);
        n.input("hai(set, q, ({x:0,x:1}, {reward:xy}, {y:0, y:1}), #z);");

        n.input("x:0. :\\: %0.25%");
        n.input("(x:0 ==>+5 (--,x:1)). :|: %0.99%");
        n.run(1);
        n.input("x:0. :/: %0.5%");
        n.run(1);
        n.input("x:1. :|: %0.5%");
        n.run(1);
        n.input("reward:xy. :/: %0.75%");
        n.run(10);
        //n.input("x:1. :|: %0%");
        n.run(50);

        //n.concept("(1-->x)").print();
    }
}