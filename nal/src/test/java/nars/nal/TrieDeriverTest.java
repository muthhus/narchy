package nars.nal;

import nars.NAR;
import nars.nal.meta.TrieDeriver;
import nars.nar.Default;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 12/12/15.
 */
public class TrieDeriverTest {

    final String r0 = "(S --> P), (S <-> P), task(\"?\") |- (S --> P), (Belief:StructuralIntersection, Punctuation:Judgment)";

    final String r1 = "((|,X,A..+) --> M), M, task(\".\") |- (X --> M), (Belief:StructuralDeduction)";
    final String r1Case = "<(|, puppy, kitten) --> animal>.";

    final String rN = "(C --> {A..+}), (C --> {B..+}) |- (C --> {A..+,B..+}), (Belief:Union), (C --> intersect({A..+},{B..+})), (Belief:Intersection)";


    @Test
    public void testNAL3Rule() {

        NAR x = testRuleInputs(r1, r1Case);

        assertEquals(1, ((TrieDeriver) (((Default) x).core.der)).roots.length);

        x.log().run(4);
    }

    @Test
    public void testTriePreconditions0() {
        TrieDeriver d = testRule(r0);
        TrieDeriver e = testRule(r1);
        TrieDeriver f = testRule(rN);

//        assertEquals(1, d.roots.length);
//        assertEquals(2, d.rules.size());

//
//        out.println(d.trie);
//
//        d.trie.printSummary();
//        d.derivationLinks.entries().forEach( System.out::println/*("\t" + k + "\t" + v)*/);
//
//        for (Term p : d.roots)
//            out.println(p);
    }

    @NotNull
    public Default testRuleInputs(String rule, String... inputs) {
        return testRuleInputs(new TrieDeriver(rule), inputs);
    }

    @NotNull
    public Default testRuleInputs(@NotNull TrieDeriver d, String... inputs) {
        return (Default) new Default() {
            @Override
            protected Deriver newDeriver() {
                return d;
            }
        }.input(inputs);
    }

    @NotNull
    public TrieDeriver testRule(String... rules) {
        TrieDeriver d = new TrieDeriver(rules);
        new Default() {
            @Override
            protected Deriver newDeriver() {
                return d;
            }
        };
        return d;
    }

    @Test public void testEllipsisRule() {
        TrieDeriver d = testRule(
            "(&&, A..+, X), B |- substituteIfUnifies((&&,A..+),\"#\",X,B), (Belief:AnonymousAnalogy, Desire:Strong, Order:ForAllSame, SequenceIntervals:FromTask)\n"
        );
        //test that A..+ survives as an ellipsis
        assertTrue(d.trie.getSummary().contains("..+"));
    }

    @Test public void testConditionalAbductionRule() {

        //test that ellipsis survives as an ellipsis after normalization no matter where it occurrs in a premise pattern

        assertTrue(testRule(
            "((X --> R) ==> Z), ((&&,A..+,(#Y --> B),(#Y --> R)) ==> Z) |- (X --> B), (Belief:Abduction)"
        ).trie.getSummary().contains("..+"));
        assertTrue(testRule(
            "((X --> R) ==> Z), ((&&,A..*,(#Y --> B),(#Y --> R)) ==> Z) |- (X --> B), (Belief:Abduction)"
        ).trie.getSummary().contains("..*"));
        assertTrue(testRule(
            "((X --> R) ==> Z), ((&&,(#Y --> B),(#Y --> R),A..*) ==> Z) |- (X --> B), (Belief:Abduction)"
        ).trie.getSummary().contains("..*"));


    }


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
}