package nars.experiment;

import com.google.common.base.Joiner;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.nar.Default;
import nars.op.PrologCore;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;


/**
 * Created by me on 3/17/16.
 */
public class BooleanSATTest {

    private static final Logger logger = LoggerFactory.getLogger(BooleanSATTest.class);
    final Map<Termed, Truth> queries = Global.newHashMap();
    float best;
    long bestAt;

    public final Truth unknown = new DefaultTruth(0.5f, 0);
    private final NAR nar;
    private final Map<String, BooleanConcept> variables = Global.newHashMap();

    public static class BooleanConcept extends MutableBoolean {

        final static Atom TRUE = $.the("TRUE");

        private final NAR nar;
        private final Term term;

        public BooleanConcept(NAR n, String id, boolean v) {
            this(n, id, v,

                //(ii) -> (Compound)$.inh($.$(ii), TRUE) //INHERITANCE

                (ii) -> $.p($.$(ii), TRUE) //PRODUCT pair

            );
        }

        public BooleanConcept(NAR n, String id, boolean initialValue, Function<String,Compound> termer) {
            super(initialValue);

            this.term = termer.apply(id);
            this.nar = n;
            update();
        }

        protected void update() {
            nar.believe(term, isTrue());
        }

        @Override
        public void setValue(boolean value) {
            if (isTrue() ^ value) {
                super.setValue(value);
                update();
            }
        }

        @Override
        public void setValue(Boolean value) {
            throw new UnsupportedOperationException();
        }
    }

    protected void updateScore() {
        float sum = (float)queries.values().stream().mapToDouble(t -> t.conf())
                //.average().orElse(0f);
                .sum();

        if (sum != best) {
            best = sum;
            bestAt = nar.time();
        }
    }

    public BooleanSATTest(NAR n) {
        this.nar = n;

        //n.onFrame(nn -> {
            //System.out.println(nn.time() + ":\t" + Joiner.on("\t").join(queries.entrySet()) );
        //});

    }

    public BooleanSATTest variable(String name, boolean initialValue) {
        BooleanConcept b = new BooleanConcept(nar, name, initialValue);
        variables.put(name, b);

        return this;
    }

    public BooleanSATTest estimate(String s) {
        query(s);
        return this;
    }

    public Task query(String s) {

        final String[] ss = {s};
        variables.forEach((k,v)->{
            ss[0] = ss[0].replace(k, v.term.toString());
        });


        Task t = nar.ask(ss[0]);
        queries.put(t.term(), unknown);
        nar.onFrame(nn -> {
            Truth current = t.concept(nn).beliefs().topEternalTruth(unknown);
            Truth prev = queries.put(t.term(), current);
            if (!Objects.equals(current,prev))
                updateScore();
        });
        return t;
    }

    public BooleanSATTest expect(String s, boolean actual) {

        Task t = query(s);

        nar.onFrame(nn -> {
            Truth tt = t.concept(nn).beliefs().topEternalTruth(unknown);
            if ((tt.isNegative() && actual) || (tt.isPositive() && !actual)) {
                logger.error("Condition violated: " + t + " === " + actual);
                //assert(false);
                //throw new RuntimeException("violated");
            }
        });
        return this;
    }


    public void run(int frames) {
        nar.run(frames);
        logger.info("score={} @ time={}", best, bestAt);
        logger.info("result={}", Joiner.on("\t").join(queries.entrySet()));
    }


    public static void main(String[] args) {


        Default n = new Default();
//        n.setDefaultJudgmentConfidence(1f);
        new PrologCore(n);
//
        //pairANDvsOR(n);
        tripleBinaryVsTrinary(n);

    }

    public static void pairANDvsOR(NAR n) {
        new BooleanSATTest(n)
                .variable("a", true)
                .variable("b", false)
                .expect("(a && b)", false) //AND
                .expect("(a || b)", true) //OR
                .run(100);
    }

    public static void tripleBinaryVsTrinary(NAR n) {

        n.log();
        new BooleanSATTest(n)
                .variable("a", true)
                .variable("b", false)
                .variable("c", false)
                .expect("(&&, a, b, c)", false) //AND
                .expect("((a || b) && c)", false)
                .expect("((a && b) || c)", false)
                .expect("((a && b) || a)", true)
                .expect("(||, a, b, c)", true) //OR
                .run(2000);
    }
}
