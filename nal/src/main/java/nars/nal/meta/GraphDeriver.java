package nars.nal.meta;

import com.google.common.collect.Iterators;
import com.gs.collections.impl.map.mutable.UnifiedMap;
import nars.$;
import nars.Global;
import nars.nal.Deriver;
import nars.nal.meta.op.SubTermOp;
import nars.nal.meta.op.SubTermStructure;
import nars.nal.meta.op.TaskPunctuation;
import nars.nal.meta.op.events;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jgrapht.ext.*;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Integer.compare;
import static java.lang.System.out;

/**
 * Created by me on 5/21/16.
 */
public class GraphDeriver extends Deriver {

    @Nullable
    static final PremiseRuleSet rules;
    static {
        PremiseRuleSet r;
        try {
            r = new PremiseRuleSet();
        } catch (Exception e) {
            r = null;
            System.exit(1);
        }
        rules = r;
    }

//    public static final class StateEdge extends DefaultWeightedEdge {
//
//        private final String state;
//
//        public StateEdge(Object state) {
//            this(state.toString());
//        }
//
//        public StateEdge(String state) {
//            super();
//            this.state = state;
//        }
//
//        @Override
//        public String toString() {
//            return super.toString() + "|" + state;
//        }
//    }

    final SimpleDirectedWeightedGraph<Term,DefaultWeightedEdge> g = new SimpleDirectedWeightedGraph(DefaultWeightedEdge.class);

    public GraphDeriver() {
        this(rules);
    }

    public GraphDeriver(@NotNull PremiseRuleSet ruleset) {
        Set<Term> concs = Global.newHashSet(256);

        ruleset.rules.forEach(rule -> {

            for (PostCondition result : rule.postconditions) {
                List<Term> x = rule.conditions(result);



                Term ruleConc = x.get(x.size()-1);
                if (concs.add(ruleConc)) {
                    g.addVertex(ruleConc);
                }

                Compound ruleVertRaw = $.sete(x.subList(0, x.size() - 1));

                //the ruleVert represents the conjunctive AND-ing of all the conditions that constitute it
                Term ruleVert = ruleName(ruleVertRaw);
                if (g.addVertex(ruleVert)) {

                    for (Term cond : ruleVertRaw.terms()) {
                        //System.out.println(cc.getClass().getSimpleName() + ": " + cc);

                        Term determiner = $.the(proc(cond) + "?");

                        g.addVertex(determiner);
                        if (!determiner.equals(cond)) {
                            g.addVertex(cond);
                            g.addEdge(determiner, cond);
                        }
                        g.addEdge(cond, ruleVert);
                    }
                }
                g.addEdge(ruleVert, ruleConc);

            }

        });


        out.println(ruleID.size() + " unique rules, leading to " + concs.size() + " unique conclusions");
        out.println(g.vertexSet().size() + " total vertices, " + g.edgeSet().size() + " total edges");
        //g.edgeSet().forEach(e -> System.out.println(e));

//        TopologicalOrderIterator top = new TopologicalOrderIterator(g);
//        while (top.hasNext()) {
//            System.out.println("\t" + top.next());
//        }


        Comparator<Term> rank = (a, b) ->
                compare(
                    //g.outDegreeOf(b), g.outDegreeOf(a)
                    reach(b), reach(a)
                );

        List<Term> sortedRoots = g.vertexSet().stream().filter(v -> g.inDegreeOf(v)==0).sorted(rank).collect(Collectors.toList());
        sortedRoots.forEach(r -> {

            out.println(r + " " +
                    //g.outDegreeOf(r) + " " + g.outgoingEdgesOf(r)
                    "reach=" + reach(r) + " fanOut=" + g.outDegreeOf(r) //+ "\t" + cs
            );
        });

        exportGraph(g);
    }

    public int reach(Term start) {
        return Iterators.size(new BreadthFirstIterator(g, start));
    }

    private final Map<Compound,Integer> ruleID = new HashMap();

    private Atom ruleName(Compound r) {
        return $.the(ruleID.computeIfAbsent(r, k->ruleID.size()+1));
    }

    private String proc(@NotNull Term cc) {
        switch (cc.getClass().getSimpleName()) {
            case "MatchOneSubterm":
            case "SolvePuncOverride":
            case "SolvePuncFromTask":
                return cc.toString();

            case "SubTermOp":
                return ("SubTermOp" + ((SubTermOp)cc).subterm);

            case "SubTermStructure": //either 0 or 1
                return ("SubTermStructure" + ((SubTermStructure)cc).subterm );

            case "SubTermsStructure": //pluaral
                return ("SubTermsStructure");

            case "ComponentCondition":
                return ("ComponentCondition");

            case "TaskBeliefEqualCondition":
                return ("TaskBeliefEqualCondition");

            default:

                //custom handling

                if ((cc == TaskPunctuation.Belief) || (cc == TaskPunctuation.Goal) || (cc == TaskPunctuation.Question) || (cc == TaskPunctuation.NotQuestion)) {
                    return ("taskPunc");
                }
//                if ((cc == TaskPositive.the) || (cc == TaskNegative.the)) {
//                    return ("taskFreq");
//                }
//                if ((cc == BeliefPositive.the) || (cc == BeliefNegative.the)) {
//                    return ("beliefFreq");
//                }
                if ((cc == events.after) || (cc == events.afterOrEternal) || (cc == events.ifTermLinkIsBefore)) {
                    return ("event");
                }


                System.err.println("not handled: " + cc.getClass() + " " + cc.getClass().getSimpleName() + " " + cc);
                //return cc.toString();
                throw new UnsupportedOperationException();
        }

    }

    static public void exportGraph(@NotNull SimpleDirectedWeightedGraph<Term,DefaultWeightedEdge> g) {
        IntegerNameProvider<Term> p1=new IntegerNameProvider<Term>();
        StringNameProvider<Term> p2=new StringNameProvider<Term>() {
            @Override
            public String getVertexName(@NotNull Term vertex) {
                return StringEscapeUtils.ESCAPE_JSON.translate( vertex.toString() );
            }
        };
        EdgeNameProvider<DefaultWeightedEdge> e1=new IntegerEdgeNameProvider<DefaultWeightedEdge>();


        ComponentAttributeProvider<DefaultWeightedEdge> p4 =
                new ComponentAttributeProvider<DefaultWeightedEdge>() {
                    @NotNull
                    @Override
                    public Map<String, String> getComponentAttributes(@NotNull DefaultWeightedEdge e) {
                        Map<String, String> map =new UnifiedMap<>(1);
                        map.put("weight", Double.toString(g.getEdgeWeight(e)));
                        return map;
                    }
                };
        GraphMLExporter export = new GraphMLExporter(p1, p2, e1, null);
        //DOTExporter export=new DOTExporter(p1, p2, null, null, p4);
        try {
            export.export(new FileWriter("/tmp/graph.graphml"), g);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run(PremiseEval matcher) {

    }

    public static void main(String[] args) {
        new GraphDeriver();
    }
}
