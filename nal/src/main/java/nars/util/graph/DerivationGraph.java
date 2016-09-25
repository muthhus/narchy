//package nars.util.graph;
//
//import nars.$;
//import nars.nal.Deriver;
//import nars.nal.meta.PremiseEval;
//import nars.nal.rule.PremiseRule;
//import nars.nal.rule.PremiseRuleSet;
//import nars.nar.Terminal;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.transform.VariableNormalization;
//import nars.term.var.Variable;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.jgrapht.graph.DirectedPseudograph;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.TreeSet;
//import java.util.stream.Collectors;
//
///**
// * table for holding derivation results for online & offline
// * testing and analysis
// *
// * associates an input condition to different instances of output results
// *
// * input:
// *      concept
// *      tasklink
// *      termlink (optional)
// *
// * output(s):
// *      list of new tasks derived
// *
// * terms are stored with their names normalized to generic values, ex;
// * <(*,<A --> B>,(--,C)) --> D>
// *
// * TODO add all removed tasks and their reasons as extra metadata
// */
//public class DerivationGraph extends DirectedPseudograph<Term, Integer> {
//
//    int edgeID;
//
//    public DerivationGraph(@NotNull PremiseRuleSet rules) {
//        super(Integer.class);
//
//        List<Compound> patterns = rules.rules.stream().map(PremiseRule::reified).distinct().collect(Collectors.toList());
//
//        TreeSet<Term> taskSet = new TreeSet();
//        patterns.forEach(p -> {
//            addVertex(p);
//            Compound subject = (Compound)p.term(0);
//            Compound pred = (Compound)p.term(1);
//
//
//
//            Term conc = normalize(pred.term(0));
//            if (taskSet.add(conc)) { //conclusion
//                addVertex(conc);
//            }
//            addEdge(p, conc, edgeID++);
//
//            //TODO create edges for the additional possibility of a conclusion containing an ellipsis to collapse to a singular term by transofrming it that way and adding that as a secondary conclusion path
//
//
//            Term tp = normalize(subject.term(0));
//            if (taskSet.add(tp)) { //task
//                addVertex(tp);
//            }
//            addEdge(tp,p, edgeID++);
//
//
//            Term bp = normalize(subject.term(1));
//            if (taskSet.add(bp)) { //task
//                addVertex(bp);
//            }
//            addEdge(bp,p, edgeID++);
//
//
//        });
//        List<Term> tasks = new ArrayList(taskSet);
//
//
//        patterns.forEach(p -> {
//            Compound subject = (Compound)p.term(0);
//            Term tp = normalize(subject.term(0));
//            Term bp = normalize(subject.term(1));
//            tasks.forEach(t -> {
//                if (unifies(tp, t)) //the pattern goes in the first argument
//                    addEdge(t, tp, edgeID++);
//
//                if (unifies(bp, t)) //the pattern goes in the first argument
//                    addEdge(t, bp, edgeID++);
//            });
//        });
//
//
//
//    }
//
//    public
//    @Nullable
//    Term normalize(Term x) {
//        if (x instanceof Compound)
//            return $.terms.transform((Compound)x, new VariableNormalization(0));
//        else if (x instanceof Variable)
//            return $.v(x.op(), 1);
//        else
//            return x;
//    }
//
//
//
//    private boolean unifies(@NotNull Term a, @NotNull Term b) {
//        //HACK
//        final int[] matches = {0};
//
//        PremiseEval p = new PremiseEval(new Terminal()) {
//
//            @Override
//            public boolean onMatch() {
//                matches[0]++;
//                return false; //only need the first one
//            }
//        };
//
//        p.unifyAll(a, b);
//        p.clear();
//
//
//        //System.out.println(a + " " + b + " " + (matches > 0));
//        boolean match = matches[0] > 0;
//        if (match && !a.equals(b))
//            System.out.println(a + " " + b + ' ' + (match));
//        return match;
//
//    }
//
//    public static void main(String[] args) {
//        DerivationGraph d = new DerivationGraph(Deriver.getDefaultDeriver().rules);
//        System.out.println(d.vertexSet().size() + " vertices, " + d.edgeSet().size() + " edges");
//    }
//}
//
////    @NotNull
////    public static String tenseRelative(long then, long now) {
////        long dt = then - now;
////        return (dt < 0 ? "[" : "[+") + dt + ']';
////    }
////
////    public static class DerivationPattern extends Keyed  {
////
////
////        //  premise key
////        public final PremiseKey key;
////
////
////        //  example premises (incl optional NL labels (NLP training) per task)
////        //public final Set<Premise> examplePremises = new HashSet();
////        //Pair<Premise,Task..
////
////        //  results
////        public final Set<TaskResult> actual = new HashSet();
////
////        //  expected results
////        //public final Set<Task> expected = new HashSet();
////
////        public DerivationPattern(PremiseKey key) {
////            this.key = key;
////        }
////
////        @Override
////        public String name() {
////            return key.toString();
////        }
////
////        @Override
////        public int hashCode() {
////            return key.hashCode();
////        }
////
////        @Override
////        public boolean equals(Object o) {
////            return key.equals(o);
////        }
////
////
////
////
////    }
////
////    @NotNull
////    public final Map<PremiseKey, DerivationPattern> premiseResult;
//////    Map<Object,Double> edgeWeights = Global.newHashMap();
////
////    private final boolean includeDerivedBudget;
////    private final boolean includeDerivedTruth;
//////    private final boolean includeDerivedParents = true;
////
////
////    static final Terminal np = new Terminal(1024);
////    static final Map<String, String> parsedTerm = new HashMap(1024);
////
////    public DerivationPattern add(@NotNull ConceptProcess n, Task... derived) {
////        return add(n.taskLink.get(),
////                n.termLink.get().term(),
////                n.belief(), n.nar().time(), derived);
////    }
////
//////    public DerivationPattern addExpected(ConceptProcess n, Task... expected) {
//////        DerivationPattern p = add(n);
//////        Collections.addAll(p.expected, expected);
//////        return p;
//////    }
////
////    public DerivationPattern add(@NotNull Task tasklink, @Nullable Term termlink, @Nullable Task belief, long now, @NotNull Task... result) {
////
////        ObjectIntHashMap<Term> unique = new ObjectIntHashMap();
////
////        PremiseKey premise = newPremise(tasklink.task(),
////                termlink, belief, unique, now);
////
////        addVertex(premise);
////
//////        TermPattern conceptTerm = addTermPattern(c.getTerm(), unique);
//////        newEdge(conceptTerm, premise);
////
////        TermPattern taskLinkTerm = addTermPattern(tasklink.term(), unique);
////        //TaskPattern taskLinkTask = addSentencePattern(tasklink.getTask(), unique, now);
////
////        /*addEdge(taskLinkTerm, taskLinkTask);
////        addEdge(taskLinkTask, premise);*/
////
////        addEdge(taskLinkTerm, premise);
////
////        if (termlink!=null) {
////            TermPattern termLinkTerm = addTermPattern(termlink, unique);
////            addEdge(termLinkTerm, premise);
////        }
////
////        if (belief!=null) {
////            TermPattern beliefTerm = addTermPattern(belief.term(), unique);
////            addEdge(beliefTerm, premise);
////        }
////
////        Set<TaskResult> resultSet = new TreeSet(); //order
////
////        for (Task t : result) {
////
////            TaskResult tr = new TaskResult(t, unique, now, includeDerivedTruth, includeDerivedBudget);
////            TermPattern tp = addTermPattern(t.term(), unique);
////
////            /*addVertex(tr);
////            addEdge(premise, tr);
////            addEdge(tr, tp);*/
////
////            addEdge(premise, tp);
////
////            /*newEdge(tr, sp);
////            newEdge(sp, tp);*/
////
////            resultSet.add(tr);
////
////        }
////
////        DerivationPattern pattern = premiseResult.computeIfAbsent(premise, (p) -> new DerivationPattern(premise));
////        pattern.actual.addAll(resultSet);
////
////        addVertex(pattern);
////        addEdge(premise, pattern);
////        addEdge(pattern, premise);
////
////        return pattern;
////
////    }
////
////
////    /** this normalizes any commutative terms which are out of order, and caches them in a list to avoid reparsing */
////    public static String parseTerm(@NotNull String i) {
////        String s = parsedTerm.get(i);
////        if (s == null) {
////            s = np.term(i).term().toString();
////
////            parsedTerm.put(i, s);
////        }
////        return s;
////    }
////
////    public DerivationGraph(boolean includeDerivedBudget, boolean includeDerivedTruth) {
////        super(Tuples::twin);
////
////        premiseResult =
////                new TreeMap();
////                //Global.newHashMap();
////
////
////        this.includeDerivedBudget = includeDerivedBudget;
////        this.includeDerivedTruth = includeDerivedTruth;
////    }
////
////    public int size() {
////        return premiseResult.size();
////    }
////
//////    public void print(String filePath) throws FileNotFoundException {
//////        print(new PrintStream(new FileOutputStream(new File(filePath))));
//////    }
////
////    public abstract static class Keyed implements Comparable<Keyed> {
////
////        public abstract String name();
////
////        @Override
////        public String toString() {
////            return name();
////        }
////
////        @Override
////        public int hashCode() {
////            return name().hashCode();
////        }
////
////        @Override
////        public boolean equals(Object obj) {
////            if (obj instanceof Keyed) {
////                return name().equals(((Keyed) obj).name());
////            }
////            return false;
////        }
////
////        @Override
////        public int compareTo(@NotNull Keyed o) {
////            return name().compareTo(o.name());
////        }
////
////    }
////
////    public static class PremiseKey extends Keyed  {
////
////        //private final String conceptKey;
////        @NotNull
////        private final String taskLinkKey;
////        @NotNull
////        private final String termLinkKey;
////        @NotNull
////        public final String key;
////        @NotNull
////        private final String beliefKey;
////        private final int beliefVolume;
////        private final int taskVolume;
////        private final int termVolume;
////
////
////        public PremiseKey(@NotNull Task tasklink, @Nullable Term termlink, @Nullable Task belief, ObjectIntHashMap<Term> unique, long now, boolean truth, boolean budget) {
////            //this.conceptKey = genericString(concept.getTerm(), unique);
////            taskLinkKey = genericString(tasklink, unique, now, truth, budget, false);
////            termLinkKey = termlink == null ? "_" : genericString(termlink, unique);
////            beliefKey = belief == null ? "_" : genericString(belief, unique, now, truth, budget, false);
////
////            taskVolume = tasklink.term().volume();
////            termVolume = termlink.term().volume();
////            beliefVolume = (belief!=null) ? belief.term().volume() : 0;
////
////            key = (taskLinkKey + ':' +
////                    termLinkKey + ':' +
////                    beliefKey).trim();
////        }
////
////        @NotNull
////        @Override
////        public String name() { return key; }
////
////        @Override
////        public boolean equals(Object obj) {
////            if (this == obj) return true;
////            if (obj instanceof PremiseKey) {
////                return name().equals(((PremiseKey)obj).name());
////            }
////            return false;
////        }
////
////        @Override
////        public int compareTo(Keyed o) {
////            if (o instanceof PremiseKey) {
////                PremiseKey pk = (PremiseKey)o;
////                int i = Integer.compare(taskVolume, pk.taskVolume);
////                if (i != 0) return i;
////                int j = Integer.compare(termVolume, pk.termVolume);
////                if (j != 0) return j;
////                int k = Integer.compare(beliefVolume, pk.beliefVolume);
////                if (k != 0) return k;
////                return super.compareTo(o);
////            }
////            return super.compareTo(o);
////        }
////    }
////
////
////    public static class TaskResult extends Keyed {
////
////        @NotNull
////        public final String key;
////
////        public TaskResult(@NotNull Task t, ObjectIntHashMap<Term> unique, long now, boolean includeDerivedTruth, boolean includeDerivedBudget) {
////            key = genericString(t, unique, now, includeDerivedTruth, includeDerivedBudget, false);
////        }
////
////        @NotNull
////        @Override
////        public String name() {
////            return key;
////        }
////
////    }
////
////
////    public static class TermPattern extends Keyed {
////
////        public final String key;
////
////        public TermPattern(@NotNull Term t, ObjectIntHashMap<Term> unique) {
////            key = genericString(t, unique);
////        }
////
////        @Override
////        public String name() {
////            return key;
////        }
////
////    }
////
////    public static class TaskPattern extends Keyed {
////
////        public final String key;
////
////        public TaskPattern(@NotNull Task s, ObjectIntHashMap<Term> unique, long now, boolean includeDerivedTruth) {
////            key = genericString(s, unique, now, includeDerivedTruth);
////        }
////
////        @Override
////        public String name() {
////            return key;
////        }
////
////    }
////
////    @NotNull
////    public PremiseKey newPremise(@NotNull Task tasklink, Term termlink, Task belief, ObjectIntHashMap<Term> unique, long now) {
////        return new PremiseKey(tasklink, termlink, belief, unique, now, includeDerivedTruth, includeDerivedBudget);
////    }
////
//////    public NARReaction record(NAR n) {
//////        return new NARReaction(n, /* Events.TermLinkTransformed.class,
//////                Events.ConceptProcessed.class, Events.TermLinkSelected.class,*/ Events.TaskDerive.class) {
//////
//////            @Override
//////            public void event(Class event, Object[] args) {
//////
////////                if (event == Events.TermLinkTransformed.class) {
////////                    TaskLink tl = (TaskLink)args[0];
////////                    Concept c = (Concept)args[1];
////////                    NAL n = (NAL)args[2];
////////                    result(c, tl, null, null /*n.getNewTasks()*/, n.time()); //TODO see if derivations can be noticed another way
////////                }
////////
////////                else if (event == Events.TermLinkSelected.class) {
////////                    TermLink termlink = (TermLink)args[0];
////////                    ConceptProcess n = (ConceptProcess)args[1];
////////                    TaskLink tasklink = n.getTaskLink();
////////                    Concept c = n.getConcept();
////////                    int taskStart = (int) args[2];
////////                    int taskEnd = (int) args[3];
////////
////////                    result(c, tasklink, termlink, getTasks(n, taskStart, taskEnd), n.time());
////////                }
////////
//////                if (event == Events.TaskDerive.class) {
//////                    //System.out.println(args[0]);
//////                    Task derived = (Task)args[0];
//////                    if (args[1] instanceof ConceptProcess) {
//////                        ConceptProcess n = (ConceptProcess) args[1];
//////                        result(n, Lists.newArrayList(derived));
//////                    }
//////                    else {
//////                        //revision, etc.
//////                    }
//////
//////
//////                }
//////            }
//////        };
//////    }
////
////    @NotNull
////    static Iterable<Task> getTasks(Premise n, int taskStart, int taskEnd) {
////        if (taskStart == taskEnd)
////            return Collections.emptyList();
////        return Iterables.limit(Iterables.skip(null /*n.getNewTasks()*/, taskStart), taskEnd - taskStart); //TODO see if derivations can be noticed another way
////    }
////
////
////
////
////    public static String genericString(@NotNull Task t, ObjectIntHashMap<Term> unique, long now, boolean includeDerivedTruth, boolean includeDerivedBudget, boolean includeDerivedParents) {
////        StringBuilder tempTaskString = new StringBuilder(128);
////
////        String s = genericString(t, unique, now, includeDerivedTruth);
////
////        if (includeDerivedBudget)
////            tempTaskString.append(t.budget().toBudgetString());
////
////        tempTaskString.append(s);
////
////        if (includeDerivedParents)
////            tempTaskString.append(t);
////
////        return tempTaskString.toString();
////    }
////
////
////
//////    @Override
//////    public void setEdgeWeight(Object o, double weight) {
//////        edgeWeights.put(o, weight);
//////    }
//////
//////    @Override
//////    public double getEdgeWeight(Object o) {
//////        return edgeWeights.get(o);
//////    }
////
//////    public final Object newEdge(Keyed a, Keyed b) {
//////        Object edge = a.name() + "||" + b.name();
//////
//////        addEdge(a, b);
//////
////////        if (containsEdge(edge)) {
////////            //setEdgeWeight(edge, getEdgeWeight(edge)+1);
////////        }
////////        else {
////////            addEdge(a, b, edge);
////////            //setEdgeWeight(edge, 1);
////////        }
//////        return edge;
//////    }
////
////
////
////    @NotNull
////    TaskPattern addSentencePattern(@NotNull Task sentence, ObjectIntHashMap<Term> unique, long now) {
////        TaskPattern s = new TaskPattern(sentence, unique, now, includeDerivedTruth);
////        addVertex(s);
////        return s;
////    }
////    @NotNull
////    TermPattern addTermPattern(@NotNull Term term, ObjectIntHashMap<Term> unique) {
////        TermPattern s = new TermPattern(term, unique);
////        addVertex(s);
////        return s;
////    }
////
////
////    public static String genericLiteral(@NotNull Term c, @NotNull ObjectIntHashMap<Term> unique) {
////        c.recurseTerms((t, superterm) -> {
////            if ((t instanceof Atomic) && (!(t instanceof Variable))) {
////                unique.getIfAbsentPut(t, unique.size());
////            }
////        });
////
////        //TODO use a better generation method, replacement might cause error if term names contain common subsequences
////        //maybe use a sorted Map so that the longest terms to be replaced are iterated first, so that a shorter subterm will not interfere with subsequent longer replacement
////
////        String[] s = new String[1];
////        s[0] = c.toString();
////
////        //HACK convert to lowercase so it doesnt interfere with the reassignments
////        s[0] = s[0].toLowerCase();
////
////        unique.forEachKeyValue( (tn, i) -> {
////            if (i > 25) throw new RuntimeException("TODO support > 26 different unique atomic terms");
////            String cc = String.valueOf((char) ('A' + i));
////
////            s[0] = s[0].replace(tn.toString(), cc); //this is replaceAll but without regex
////        });
////
////        s[0] = parseTerm(s[0]);
////
////        return s[0];
////
////    }
////
////    public static String genericString(@NotNull Task s, long now, boolean includeTruth) {
////        return genericString(s, new ObjectIntHashMap<>(), now, includeTruth);
////    }
////
////    public static String genericString(@NotNull Task s, ObjectIntHashMap<Term> unique, long now, boolean includeTruth) {
////        String t = genericString(s.term(), unique);
////
////        t += s.punc();
////
////
////        if (includeTruth) {
////            t += " %";
////            t += s.truth() != null ? Texts.n2(s.freq()) + ";" + Texts.n2(s.conf()) : "?;?";
////            t += "%";
////        }
////
////
////        if (!s.isEternal()) {
////            t += ' ' + tenseRelative(s.occurrence(), now);
////        }
////
////        return t;
////    }
////
////    public static String genericString(@NotNull Term t, @Nullable ObjectIntHashMap<Term> _unique) {
////        ObjectIntHashMap<Term> unique = _unique == null ? new ObjectIntHashMap() : _unique;
////
////        if (t.getClass() == Atom.class) {
////            //atomic term
////            return genericLiteral(t, unique);
////        }
////        if (t instanceof AbstractOperator) {
////            return t.toString();
////        }
////        if (t instanceof Variable || t instanceof Compound) {
////            //return t.toString();
////            return genericLiteral(t, unique);
////        }
////
////        throw new RuntimeException("genericString Unhandled term: " + t);
////    }
////
////    @Override
////    public String toString() {
////        return premiseResult.toString();
////    }
////
//////    public void print(@NotNull Writer out) {
//////
////////        for (PremiseKey premise : premiseResult.keySet()) {
////////            resultGroups = premiseResult.get(premise);
////////            //int g = 0;
////////
////////
////////            if (resultGroups.isEmpty()) {
////////                p.println(premise + ";\t DERIVE; " +  "; null");
////////            }
////////
////////            for (Set<TaskResult> result : resultGroups) {
////////
////////                if (result.isEmpty()) {
////////                    p.println(premise + ";\t DERIVE; " +  "; null");
////////                }
////////                else {
////////                    for (TaskResult task : result) {
////////                        p.println(premise + ";\t DERIVE; " +  "; " + task);
////////                    }
////////                }
////////                //g++;
////////            }
////////        }
////////
////////        p.println(vertexSet().size() + " " + edgeSet().size());
////////
////////
////////        SummaryStatistics s = new SummaryStatistics();
////////        for (Double d : edgeWeights.values())
////////            s.addValue(d);
////////        //System.out.println("weights: " + s);
//////
//////        GmlExporter gme = new GmlExporter(new IntegerNameProvider(), new StringNameProvider() {
//////            @Override
//////            public String getVertexName(@NotNull Object vertex) {
//////                return super.getVertexName(vertex);
//////            }
//////        }, new IntegerEdgeNameProvider(), new StringEdgeNameProvider() {
//////            @NotNull
//////            @Override
//////            public String getEdgeName(@NotNull Object edge) {
//////                return super.getEdgeName(edge) + "\"\n\tweight \"" + getEdgeWeight(edge) ;
//////            }
//////        });
//////        gme.setPrintLabels(GmlExporter.PRINT_EDGE_VERTEX_LABELS);
//////        try {
//////            gme.export(out, this);
//////
//////            //ex: filter:
//////                    //weightAtleast(0.5 * (s.getMean() + s.getGeometricMean())));
//////        } catch (Exception e) {
//////            e.printStackTrace();
//////        }
//////    }
////
//////    @NotNull
//////    private DirectedMaskSubgraph weightAtleast(double v) {
//////        MaskFunctor e = new MaskFunctor() {
//////            @Override
//////            public boolean isEdgeMasked(@NotNull Object edge) {
//////                return getEdgeWeight(edge) < v;
//////            }
//////
//////            @Override
//////            public boolean isVertexMasked(Object vertex) {
//////                return false;
//////            }
//////        };
//////        return new DirectedMaskSubgraph(this, e);
//////    }
