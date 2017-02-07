package nars.nal;

import com.google.common.collect.Sets;
import nars.conceptualize.ConceptBuilder;
import nars.derive.Deriver;
import nars.derive.TrieDeriver;
import nars.derive.meta.BoolCondition;
import nars.derive.rule.PremiseRule;
import nars.index.term.PatternTermIndex;
import org.apache.commons.math3.stat.Frequency;
import org.junit.Test;

import java.util.*;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 12/12/15.
 */
public class TrieDeriverTest {

    final static TrieDeriver d = (TrieDeriver) Deriver.get("nal1.nal");

    @Test public void printCompiledRuleTree() {

        d.print(System.out);
    }

//    @Test public void printRuleSet() {
//
////        List<PremiseRule> rr = d.rules.rules;
////        System.out.println(rr.size() + " rules");
////        rr.forEach(r -> {
////            System.out.println(r);
////        });
//
//        d.trie.costAnalyze((t) -> 1, System.out);
//    }



    static final String r0 = "(S --> P), (S <-> P), task(\"?\") |- (S --> P), (Belief:StructuralIntersection, Punctuation:Belief)";

    static final String r1 = "((|,X,A..+) --> M), M, task(\".\") |- (X --> M), (Belief:StructuralDeduction)";
    static final String r1Case = "<(|, puppy, kitten) --> animal>.";

    static final String rN = "(C --> {A..+}), (C --> {B..+}) |- (C --> {A..+,B..+}), (Belief:Union), (C --> intersect({A..+},{B..+})), (Belief:Intersection)";


//    @Test
//    public void testNAL3Rule() {
//
//        NAR x = testRuleInputs(r1, r1Case);
//
//        assertEquals(1, ((TrieDeriver) (((Default) x).premiser.derv)).roots.length);
//
//        x.log().run(4);
//    }

//    @Test
//    public void testTriePreconditions0() {
//        TrieDeriver d = testRule(r0);
//        TrieDeriver e = testRule(r1);
//        TrieDeriver f = testRule(rN);
//
////        assertEquals(1, d.roots.length);
////        assertEquals(2, d.rules.size());
//
////
////        out.println(d.trie);
////
////        d.trie.printSummary();
////        d.derivationLinks.entries().forEach( System.out::println/*("\t" + k + "\t" + v)*/);
////
////        for (Term p : d.roots)
////            out.println(p);
//    }
//
//    @NotNull
//    public Default testRuleInputs(String rule, String... inputs) {
//        return testRuleInputs(new TrieDeriver(rule), inputs);
//    }

//    @NotNull
//    public Default testRuleInputs(@NotNull TrieDeriver d, String... inputs) {
//        return (Default) new Default() {
//            @NotNull
//            @Override
//            protected Deriver newDeriver() {
//                return d;
//            }
//        }.input(inputs);
//    }
//
//    @NotNull
//    public TrieDeriver testRule(String... rules) {
//        TrieDeriver d = new TrieDeriver(rules);
//        new Default() {
//            @NotNull
//            @Override
//            protected Deriver newDeriver() {
//                return d;
//            }
//        };
//        return d;
//    }

//    @Test public void testEllipsisRule() {
//        TrieDeriver d = testRule(
//            "(&&, A..+, X), B |- substituteIfUnifies((&&,A..+),\"#\",X,B), (Belief:AnonymousAnalogy, Goal:Strong, Order:ForAllSame, SequenceIntervals:FromTask)\n"
//        );
//        //test that A..+ survives as an ellipsis
//        assertTrue(d.trie.getSummary().contains("..+"));
//    }
//
//    @Test public void testConditionalAbductionRule() {
//
//        //test that ellipsis survives as an ellipsis after normalization no matter where it occurrs in a premise pattern
//
//        assertTrue(testRule(
//            "((X --> R) ==> Z), ((&&,A..+,(#Y --> B),(#Y --> R)) ==> Z) |- (X --> B), (Belief:Abduction)"
//        ).trie.getSummary().contains("..+"));
//        assertTrue(testRule(
//            "((X --> R) ==> Z), ((&&,A..*,(#Y --> B),(#Y --> R)) ==> Z) |- (X --> B), (Belief:Abduction)"
//        ).trie.getSummary().contains("..*"));
//        assertTrue(testRule(
//            "((X --> R) ==> Z), ((&&,(#Y --> B),(#Y --> R),A..*) ==> Z) |- (X --> B), (Belief:Abduction)"
//        ).trie.getSummary().contains("..*"));
//
//
//    }


//    @Test public void testBackwardsRules() {
//
//        TrieDeriver d = testRule(
//                "(A --> B), (B --> C), neq(A,C) |- (A --> C), (Truth:Deduction, Desire:Strong, Derive:AllowBackward)",
//                "(A --> B), (A --> C), neq(B,C) |- (C --> B), (Truth:Abduction, Desire:Weak, Derive:AllowBackward)",
//                "(A --> C), (B --> C), neq(A,B) |- (B --> A), (Truth:Induction, Desire:Weak, Derive:AllowBackward)",
//                "(A --> B), (B --> C), neq(C,A) |- (C --> A), (Truth:Exemplification, Desire:Weak, Derive:AllowBackward)"
//        );
//        d.trie.printSummary();
//
//        Default n = testRuleInputs(d,
//                "<bird --> swimmer>.", "<?1 --> swimmer>?"
//        );
//        n.log();
//        n.frame(64);
//
//        //TODO write test
//    }


    @Test
    public void testRuleStatistics() {
        List<PremiseRule> R = d.rules.rules;
        int registeredRules = R.size();


        Frequency f = new Frequency();
        R.forEach(f::addValue);
        Iterator<Map.Entry<Comparable<?>, Long>> ii = f.entrySetIterator();
        while (ii.hasNext()) {
            Map.Entry<Comparable<?>, Long> e = ii.next();
            if (e.getValue() > 1) {
                System.err.println("duplicate: " + e);
            }
        }
        out.println("total: " + f.getSumFreq() + ", unique=" + f.getUniqueCount());

        HashSet<PremiseRule> setRules = Sets.newHashSet(R);

        assertEquals("no duplicates", registeredRules, setRules.size());

        Set<BoolCondition> preconds = new HashSet();
        int totalPrecond = 0;

        out.println("total precondtions = " + totalPrecond + ", unique=" + preconds.size());

        //preconds.forEach(p -> System.out.println(p));


        //Set<TaskBeliefPair> ks = d.ruleIndex.keySet();
//        System.out.println("Patterns: keys=" + ks.size() + ", values=" + d.ruleIndex.size());
//        for (TaskBeliefPair pp : ks) {
//            System.out.println(pp + " x " + d.ruleIndex.get(pp).size());
//
//        }


    }

//    @Test public void testPostconditionSingletons() {
////        System.out.println(PostCondition.postconditions.size() + " unique postconditions " + PostCondition.totalPostconditionsRequested);
////        for (PostCondition p : PostCondition.postconditions.values()) {
////            System.out.println(p);
////        }
//
//    }

    @Test public void testPatternIndexContainsNoConcepts() {
        PatternTermIndex p = d.rules.patterns;
        assertTrue(ConceptBuilder.Null == p.conceptBuilder());
//        //out.println(p.data);
//        //out.println(p.atoms);
//        p.forEach(t -> {
//
//            if (!(t instanceof TransformConcept))
//                assertFalse( t instanceof Concept);
//
//            //test all subterms are in the pattern index too
//            t.term().recurseTerms((s, parent)->{
//                if (s instanceof Variable)
//                    return;
//
//
//                Termed sub = p.concept(s, false);
//                if (sub == null) {
//                    System.out.println("subterm " + s + " of " + parent + " not in PatternIndex");
//                }
//                assertNotNull(sub);
//            });
//
//            //out.println(t);
//        });
//        //System.out.println("compounds: "+ p.internings + " internings, " + p.size() + " unique");

    }




//
//        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

//
//        List<PremiseRule> R = Deriver.getDefaultDeriver().rules.rules;
//        for (PremiseRule r : R) {
//            byte barray[] = conf.asByteArray(r);
//            System.out.println(r + " "+ barray.length + " bytes");
//
//            Object decoded = conf.asObject(barray);
//            System.out.println("\t " + decoded);
//        }
//    }

//    @Test
//    public void testDerivationComparator() {
//
//        NARComparator c = new NARComparator(
//                new Default(),
//                new Default()
//        ) {
//
//
//        };
//        c.input("<x --> y>.\n<y --> z>.\n");
//
//
//
//        int cycles = 64;
//        for (int i = 0; i < cycles; i++) {
//            if (!c.areEqual()) {
//
//                /*System.out.println("\ncycle: " + c.time());
//                c.printTasks("Original:", c.a);
//                c.printTasks("Rules:", c.b);*/
//
////                System.out.println(c.getAMinusB());
////                System.out.println(c.getBMinusA());
//            }
//            c.frame(1);
//        }
//
//        System.out.println("\nDifference: " + c.time());
//        System.out.println("Original - Rules:\n" + c.getAMinusB());
//        System.out.println("Rules - Original:\n" + c.getBMinusA());
//
//    }

}