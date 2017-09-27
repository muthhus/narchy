//package nars.analyze;
//
//import com.google.common.base.Joiner;
//import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
//import nars.Global;
//import nars.NAR;
//import nars.bag.BLink;
//import nars.budget.control.Forget;
//import nars.concept.Concept;
//import nars.concept.DefaultConceptProcess;
//import nars.nal.Deriver;
//import nars.nal.meta.PremiseRule;
//import nars.nar.Default;
//import nars.task.Task;
//import nars.term.Term;
//import nars.term.Termed;
//import nars.util.graph.DerivationGraph;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.nustaq.serialization.FSTConfiguration;
//
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.PrintStream;
//import java.io.Serializable;
//import java.util.Collection;
//import java.util.List;
//
///**
// * A NAR for collecting statistics and measuring qualities
// * about reasoner activity
// */
//public class DiagNAR extends Default {
//
//    private static PrintStream out;
//    static {
//        try {
//            out = new PrintStream(new FileOutputStream("/tmp/t.csv"));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            out = null;
//        }
//        Runtime.getRuntime().addShutdownHook(new Thread(()->{
//            out.close();
//        }));
//    }
//
//    public DiagNAR()  {
//        super();
//    }
//
//    public DiagNAR(int i, int i1, int i2, int i3)  {
//        super(i, i1, i2, i3);
//    }
//
//    @Override
//    public DefaultPremiseGenerator newPremiseGenerator() {
//        return new DefaultPremiseGenerator(this, Deriver.getDefaultDeriver(),
//                //Global.newArrayList()
//                Global.newHashSet(64)
//        ) {
//
//            @Override @NotNull
//            protected DefaultConceptProcess newPremise(BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief) {
//                return new DiagConceptProcess(nar, concept, taskLink, termLink, belief, sharedResultBuffer);
//            }
//        };
//    }
//
//    class DiagConceptProcess extends DefaultConceptProcess {
//
//        public DiagConceptProcess(NAR nar, BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief, Collection<Task> sharedResultBuffer) {
//            super(nar, concept, taskLink, termLink, belief, sharedResultBuffer);
//        }
//
//        @Override
//        protected void commit() {
//            onPremiseComplete(this);
//            super.commit();
//        }
//    }
//
////    final MutableTask nullTask = new TaskBuilder($.$("(null)"), '.');
////    static final MutableTask null = new TaskBuilder($.$("(null)"), '.');
//
//    private void onPremiseComplete(DiagConceptProcess p) {
//        Collection<Task> c = p.results;
//        Task task = p.task();
//        Task belief = p.belief();
//
//        Term beliefTerm = p.beliefTerm().term();
//
//        if (belief == null)
//            belief = null;
//
//        if (c.isEmpty()) {
//            derive(p, task, beliefTerm, belief, null);
//        }
//        else {
//            final Task finalBelief = belief;
//            c.forEach(t -> {
//                derive(p, task, beliefTerm, finalBelief, t);
//            });
//        }
//
//        //1. associate the premise with the rules that produced its results
//        //2. analyze any duplicate or redundant tasks, and what rules may be overlapping
//        //3. record budget input vs. generated, confidence input vs. generated, etc
//        //      compute budget inflation and confidence loss/gain
//    }
//
//
//    private void derive(DiagConceptProcess cp, Task task, Term beliefTerm, Task finalBelief, Task t) {
//
//        new Derivation(cp, task, beliefTerm, finalBelief, t);
//
//    }
//
//
//    static final FSTConfiguration serialize = FSTConfiguration.createJsonConfiguration(false, true);
//    static {
//        serialize.setForceSerializable(true);
//        serialize.setCrossPlatform(false);
//        serialize.setForceClzInit(false);
//    }
//
//
//    /** ((t,b) ==> {c}) */
//    public static class Derivation implements Serializable {
//
//        public final String c;
//        public final String cGen;
//        public final String b;
//        public final String bGen;
//        public final String bTerm;
//        public final String t;
//        public final String tGen;
//        public final String rule;
//        public final float dSumm; /* delta summary inflation=negative, reduction=positive  */
//        public final float pConf; /* percent confidence maintained in derivation (of max(task, belief) conf */
//
//        public Derivation(DiagConceptProcess cp, Task task, Term beliefTerm, Task belief, Task c) {
//            this.t = task.toString();
//            this.bTerm = beliefTerm.toString();
//
//            ObjectIntHashMap<Term> h = new ObjectIntHashMap<>();
//
//            this.tGen = DerivationGraph.genericString(task.term(), h);
//            this.bGen = belief!=null ? DerivationGraph.genericString(belief.term(), h) : null;
//
//            this.b = belief!=null ? belief.toString() : null;
//
//            this.c = c!=null ? c.toString() : null;
//            this.cGen = c!=null ? DerivationGraph.genericString(c.term(), h) : null;
//
//
//            PremiseRule r = c != null ? rule(c) : null;
//            this.rule = r != null ? r.toString() : null;
//
//            dSumm = (c!= null ? c.summary() : 0) -
//                            (task.summary() + (belief!=null ? belief.summary() : 0));
//
//            float pConfNum = (c!=null && c.isBeliefOrGoal()) ? c.conf() : 0;
//            float pConfDen =
//                    Math.max((task.isBeliefOrGoal() ? task.conf() : 0),
//                        (belief!=null && belief.isBeliefOrGoal() ? belief.conf() : 0));
//            this.pConf = (pConfDen != 0) ? pConfNum/pConfDen : 0;
//
//
//            //byte[] bytes = serialize.asByteArray(this);
//            //String ss = new String(bytes);
//
//
//            print();
//
//        }
//
//        public void print() {
//
//            String line = Joiner.on("\t").useForNull("null").join(tGen, bGen, cGen, pConf, dSumm, rule);
//            out.println(line);
//            //System.out.println(line);
////            System.out.println(bytes.length + "  " + ss);
////            //Object deser = conf.asObject(bytes);
////
////            out.println(ss);
//
//        }
//
//        public static PremiseRule rule(Task c) {
//            @Nullable List l = c.log();
//            //if (l == null) return null;
//            return (PremiseRule) l.stream().filter(x -> x instanceof PremiseRule).findFirst().orElse(null);
//        }
//    }
//
//
//
//    public static void main(String[] args) throws Exception {
//        Global.DEBUG = true;
//        NAR d = new DiagNAR(768,2, 2, 4);
//        //NAR d = new Default(768, 3, 2, 4);
////        d.input("(a-->(b&&c)).");
////        d.input("(c-->d).");
////        d.input("<<$x --> bird> ==> <$x --> flyer>>.");
////        d.input("<{Tweety} --> [withWings]>."); //en("Tweety has wings.");
////        d.input("<(&&,<$x --> [chirping]>,<$x --> [withWings]>) ==> <$x --> bird>>."); //en("If something can chirp and has wings, then it is a bird.");
//
//        d.logSummaryGT(System.out, 0);
//
//        //t:(--,b). (t:(--,#y) ==> t:(not,#y)).
////        d.input(
////            "t:a.",
////            "t:(--,b).",
////            //"(t:a && (--,t:b)).",
////            //"(t:(--,$1) ==> t:(not,$1))."
////            //"(t:(--,$1) ==> t:(not,$1)).",
////            "(t:($x | (--,$y)) ==> t:(xor, $x, $y))."
////            //,"xor(a,b)?"
////            //"(($x && $y) ==> and({$x, $y}))."
////        );
//        /*d.memory.eventTaskRemoved.on(t -> {
//            System.err.println("rem: " +t);
//        });*/
//        d.input(
//                "believe(x).",
//                "want(x).",
//                "((believe($x) && want($x)) ==> grateful($x))."
//        );
//
//        d.run(16);
//
//    }
//}
