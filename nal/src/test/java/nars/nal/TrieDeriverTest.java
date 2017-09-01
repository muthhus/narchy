package nars.nal;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.control.Derivation;
import nars.control.Premise;
import nars.derive.Deriver;
import nars.derive.PrediTerm;
import nars.derive.TrieDeriver;
import nars.derive.rule.PremiseRuleSet;
import nars.index.term.PatternTermIndex;
import nars.task.ITask;
import nars.term.Term;
import nars.term.Termed;
import net.byteseek.utils.collections.IdentityHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static nars.derive.rule.PremiseRuleSet.parse;
import static org.junit.Assert.*;

/**
 * Created by me on 12/12/15.
 */
public class TrieDeriverTest {

//    final static TrieDeriver d =
//            //(TrieDeriver) Deriver.get("nal1.nal");
//            //(TrieDeriver) DefaultDeriver.the;
//            new TrieDeriver(DefaultDeriver.rules);

    @Test
    public void printCompiledRuleTree() {

        TrieDeriver.print(TrieDeriver.the(Deriver.DEFAULT(8), NARS.tmp()), System.out);

    }


    @Test
    public void testConclusionWithXTERNAL() throws Narsese.NarseseException {
        PatternTermIndex idx = new PatternTermIndex() {
            @Override
            public @Nullable Termed get(@NotNull Term x, boolean create) {
                Termed u = super.get(x, create);
                assertNotNull(u);
                if (u != x) {
                    System.out.println(x + " (" + x.getClass() + ")" + " -> " + u + " (" + u.getClass() + ")");
                    if (u.equals(x) && u.getClass().equals(x)) {
                        fail("\t ^ same class, wasteful duplicate");
                    }
                }
                return u;
            }
        };

        System.out.println();

        PrediTerm d = TrieDeriver.the(new PremiseRuleSet(idx,
                "Y, Y |- (?1 &&+0 Y), ()",
                "X, X |- (?1 &&+- X), ()"
        ), NARS.tmp());

        System.out.println();

        d.printRecursive();

        System.out.println(d);

        assertTrue(d.toString().contains("(?2&|%1)"));
        assertTrue(d.toString().contains("(?2 &&+- %1)"));


        //assertTrue("something at least got stored in the index", idx.size() > 16);

        //test that A..+ survives as an ellipsis
        //assertTrue(d.trie.getSummary().contains("..+"));
    }


    public static PrediTerm<Derivation> testCompile(String... rules) {
        return testCompile(NARS.tmp(0), false, rules);
    }

    public static PrediTerm<Derivation> testCompile(NAR n, String... rules) {
        return testCompile(n, false, rules);
    }

    public static PrediTerm<Derivation> testCompile(NAR n, boolean debug, String... rules) {

        @NotNull PatternTermIndex pi = new PatternTermIndex();
        PremiseRuleSet src = new PremiseRuleSet(PremiseRuleSet.parse(Stream.of(rules), pi), pi, false);
        PrediTerm d = TrieDeriver.the(src, n);

        if (debug) d.printRecursive();

        Set<Term> byEquality = new HashSet();
        Set<Term> byIdentity = new IdentityHashSet();
        d.recurseTerms(a -> {
            byEquality.add(a);
            byIdentity.add(a);
        });

//        if (debug) {
//            System.out.println("           volume: " + d.volume());
//            System.out.println("       complexity: " + d.complexity());
//            System.out.println("terms by equality: " + byEquality.size());
//            System.out.println("terms by identity: " + byIdentity.size());
//
//            System.out.println("  values: " + n.causes.size());
//            n.causes.forEach(System.out::println);
//        }
//        assertTrue(n.causes.size() > 0);
//
//        if (debug) {
//            System.out.println();
//            TrieDeriver.print(d, System.out);
//        }

        return d;

        //PrediTerm e = src.compile(NARS.single());
    }

    @Test
    public void testCompile() {
        testCompile(
                "(A --> B), (B --> C), neqRCom(A,C) |- (A --> C), (Belief:Deduction, Goal:Strong)"
        );

    }

    @Test
    public void testCompilePatternOpSwitch() {
        testCompile(
                "(A --> B), C, task(\"?\") |- (A --> C), (Punctuation:Question)",
                "(A ==> B), C, task(\"?\") |- (A ==> C), (Punctuation:Question)"
        );

    }

    @Test
    public void testConclusionFold() throws Narsese.NarseseException {

        String[] rules = {
                "(A --> B), C, task(\"?\") |- (A --> C), (Punctuation:Question)",
                "(A --> B), C, task(\"?\") |- (A ==> C), (Punctuation:Question)"
        };

        Set<Task> t1 = testDerivation(rules, "(a-->b)?", "b", 64, false);
        assertEquals(2, t1.size());
        Set<Task> t2 = testDerivation(rules, "(a<->b)?", "b", 64);
        assertEquals(0, t2.size());

    }
   @Test
    public void testConversionWierdness() throws Narsese.NarseseException {

        String[] rules = {
                "(P --> S), (S --> P), task(\"?\") |- (P --> S),   (Punctuation:Quest)"
        };

        Set<Task> t1 = testDerivation(rules, "(a-->b)?", "(b-->a)", 64, true);
        assertEquals(1, t1.size());

    }
    public static Set<Task> testDerivation(String[] rules, String task, String belief, int ttlMax) throws Narsese.NarseseException {
        return testDerivation(rules, task, belief, ttlMax, false);
    }

    public static Set<Task> testDerivation(String[] rules, String task, String belief, int ttlMax, boolean debug) throws Narsese.NarseseException {
        NAR n = NARS.tmp();

        PrediTerm<Derivation> d = testCompile(n, debug, rules);
                //.transform(DebugDerivationPredicate::new);

        Set<Task> tasks = new LinkedHashSet();
        n.onTask(tasks::add);
        n.log();

        Task t = Narsese.parse().task(task, n);
        assertNotNull(t);
        Term b = n.term(belief);
        assertNotNull(b);

        Iterable<? extends ITask> derived = new Premise(t, b, 0.5f) {
            @Override protected Derivation derivation(@NotNull NAR n) {
                return n.derivation(d);
            }
        }.run(n);
        if (derived!=null) {
            derived.forEach(x ->
                    tasks.add((Task) x)
            );
        }

        return tasks;
    }

    @Test
    public void testDeductionRecrusveWeirdness() {

        String s = "B, (A ==> C), neq(A,B), notImpl(B) |- subIfUnifiesAny(C,A,B,strict), (Belief:DeductionRecursive)";

        NAR n = NARS.tmp();
        PrediTerm<Derivation> d = testCompile(n, false, s);
        TrieDeriver.print(d, System.out);

    }

    @Test
    public void testContrapositionWierdness() {

        String s = "( (--,%S) ==> P), ( (--,%S) ==> P) |- ( (--,%P) ==>+- S),       (Belief:Contraposition)";

        NAR n = NARS.tmp();
        PrediTerm<Derivation> d = testCompile(n, false, s);
        TrieDeriver.print(d, System.out);

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


//    @Test
//    public void testRuleStatistics() {
//        List<PremiseRule> R = d.rules.rules;
//        int registeredRules = R.size();
//
//
//        Frequency f = new Frequency();
//        R.forEach(f::addValue);
//        Iterator<Map.Entry<Comparable<?>, Long>> ii = f.entrySetIterator();
//        while (ii.hasNext()) {
//            Map.Entry<Comparable<?>, Long> e = ii.next();
//            if (e.getValue() > 1) {
//                System.err.println("duplicate: " + e);
//            }
//        }
//        out.println("total: " + f.getSumFreq() + ", unique=" + f.getUniqueCount());
//
//        HashSet<PremiseRule> setRules = Sets.newHashSet(R);
//
//        assertEquals("no duplicates", registeredRules, setRules.size());
//
//        Set<BoolPredicate> preconds = new HashSet();
//        int totalPrecond = 0;
//
//        out.println("total precondtions = " + totalPrecond + ", unique=" + preconds.size());
//
//        //preconds.forEach(p -> System.out.println(p));
//
//
//        //Set<TaskBeliefPair> ks = d.ruleIndex.keySet();
////        System.out.println("Patterns: keys=" + ks.size() + ", values=" + d.ruleIndex.size());
////        for (TaskBeliefPair pp : ks) {
////            System.out.println(pp + " x " + d.ruleIndex.get(pp).size());
////
////        }
//
//
//    }
//
////    @Test public void testPostconditionSingletons() {
//////        System.out.println(PostCondition.postconditions.size() + " unique postconditions " + PostCondition.totalPostconditionsRequested);
//////        for (PostCondition p : PostCondition.postconditions.values()) {
//////            System.out.println(p);
//////        }
////
////    }
//
//    @Test public void testPatternIndexContainsNoConcepts() {
//        PatternTermIndex p = d.rules.patterns;
//        assertTrue(ConceptBuilder.Null == p.conceptBuilder());
////        //out.println(p.data);
////        //out.println(p.atoms);
////        p.forEach(t -> {
////
////            if (!(t instanceof TransformConcept))
////                assertFalse( t instanceof Concept);
////
////            //test all subterms are in the pattern index too
////            t.term().recurseTerms((s, parent)->{
////                if (s instanceof Variable)
////                    return;
////
////
////                Termed sub = p.concept(s, false);
////                if (sub == null) {
////                    System.out.println("subterm " + s + " of " + parent + " not in PatternIndex");
////                }
////                assertNotNull(sub);
////            });
////
////            //out.println(t);
////        });
////        //System.out.println("compounds: "+ p.internings + " internings, " + p.size() + " unique");
//
//    }
//
//
//

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