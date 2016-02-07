package nars.analyze;

import com.gs.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import nars.Global;
import nars.NAR;
import nars.bag.BLink;
import nars.concept.Concept;
import nars.concept.DefaultConceptProcess;
import nars.nal.Deriver;
import nars.nal.meta.PremiseRule;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.util.meter.DerivationGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nustaq.serialization.FSTConfiguration;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * A NAR for collecting statistics and measuring qualities
 * about reasoner activity
 */
public class DiagNAR extends Default {

    private static PrintStream out;

    public DiagNAR() throws FileNotFoundException {
        super();
        out = new PrintStream(new FileOutputStream("/tmp/t.csv"));
    }

    public DiagNAR(int i, int i1, int i2, int i3) throws FileNotFoundException {
        super(i, i1, i2, i3);
        out = new PrintStream(new FileOutputStream("/tmp/t.csv"));
    }

    @Override
    public DefaultPremiseGenerator newPremiseGenerator() {
        return new DefaultPremiseGenerator(this, Deriver.getDefaultDeriver(), Global.newArrayList()) {

            @Override @NotNull
            protected DefaultConceptProcess newPremise(BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief) {
                return new DiagConceptProcess(nar, concept, taskLink, termLink, belief, sharedResultBuffer);
            }
        };
    }

    class DiagConceptProcess extends DefaultConceptProcess {

        public DiagConceptProcess(NAR nar, BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief, Collection<Task> sharedResultBuffer) {
            super(nar, concept, taskLink, termLink, belief, sharedResultBuffer);
        }

        @Override
        protected void commit() {
            onPremiseComplete(this);
            super.commit();
        }
    }

//    final MutableTask nullTask = new MutableTask($.$("(null)"), '.');
//    static final MutableTask null = new MutableTask($.$("(null)"), '.');

    private void onPremiseComplete(DiagConceptProcess p) {
        Collection<Task> c = p.results;
        Task task = p.task();
        Task belief = p.belief();

        Term beliefTerm = p.beliefTerm().term();

        if (belief == null)
            belief = null;

        if (c.isEmpty()) {
            derive(p, task, beliefTerm, belief, null);
        }
        else {
            final Task finalBelief = belief;
            c.forEach(t -> {
                derive(p, task, beliefTerm, finalBelief, t);
            });
        }

        //1. associate the premise with the rules that produced its results
        //2. analyze any duplicate or redundant tasks, and what rules may be overlapping
        //3. record budget input vs. generated, confidence input vs. generated, etc
        //      compute budget inflation and confidence loss/gain
    }


    private void derive(DiagConceptProcess cp, Task task, Term beliefTerm, Task finalBelief, Task t) {

        new Derivation(cp, task, beliefTerm, finalBelief, t);

    }


    static final FSTConfiguration serialize = FSTConfiguration.createJsonConfiguration();
    static {
        serialize.setForceSerializable(true);

    }


    /** ((t,b) ==> {c}) */
    public static class Derivation implements Serializable {

        public final String c;
        public final String cGen;
        public final String b;
        public final String bGen;
        public final String bTerm;
        public final String t;
        public final String tGen;
        public final String rule;
        public final float dSumm; /* summary */
        public final float dConf;

        public Derivation(DiagConceptProcess cp, Task task, Term beliefTerm, Task belief, Task c) {
            this.t = task.toString();
            this.bTerm = beliefTerm.toString();

            ObjectIntHashMap<Term> h = new ObjectIntHashMap<>();

            this.tGen = DerivationGraph.genericString(task.term(), h);
            this.bGen = belief!=null ? DerivationGraph.genericString(belief.term(), h) : null;

            this.b = belief!=null ? belief.toString() : null;

            this.c = c!=null ? c.toString() : null;
            this.cGen = c!=null ? DerivationGraph.genericString(c.term(), h) : null;


            this.rule = c!=null ? rule(c).toString() : null;

            dSumm = (c!= null ? c.summary() : 0) -
                            (task.summary() + (belief!=null ? belief.summary() : 0));

            float deltaConfidence = (c!=null && c.isJudgmentOrGoal()) ? c.conf() : 0;
            deltaConfidence -= (task.isJudgmentOrGoal() ? task.conf() : 0);
            deltaConfidence -= (belief!=null && belief.isJudgmentOrGoal() ? belief.conf() : 0);
            this.dConf = deltaConfidence;


            byte[] bytes = serialize.asByteArray(this);
            String ss = new String(bytes);

            System.out.println(bytes.length + "  " + ss);
            //Object deser = conf.asObject(bytes);

            out.println(ss);

        }


        public static PremiseRule rule(Task c) {
            @Nullable List l = c.log();
            //if (l == null) return null;
            return (PremiseRule) l.stream().filter(x -> x instanceof PremiseRule).findFirst().get();
        }
    }



    public static void main(String[] args) throws Exception {
        Global.DEBUG = true;
        DiagNAR d = new DiagNAR(1024, 1, 2, 3);
        d.input("(a-->(b&&c)).");
        d.input("(c-->d).");
        d.input("<<$x --> bird> ==> <$x --> flyer>>.");
        d.input("<{Tweety} --> [withWings]>."); //en("Tweety has wings.");
        d.input("<(&&,<$x --> [chirping]>,<$x --> [withWings]>) ==> <$x --> bird>>."); //en("If something can chirp and has wings, then it is a bird.");

        d.run(1000);

    }
}
