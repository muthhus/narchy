package nars.nal.meta;

import com.google.common.base.Joiner;
import nars.Narsese;
import nars.Param;
import nars.index.term.PatternTermIndex;
import nars.nal.rule.PremiseRule;
import nars.nal.rule.PremiseRuleSet;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Set;

import static nars.nal.rule.PremiseRuleSet.parse;
import static org.junit.Assert.*;

/**
 * Created by me on 7/7/15.
 */
public class PremiseRuleTest {


    static final Narsese p = Narsese.the();

    /**
     * for printing complex terms as a recursive tree
     */
    public static void printRecursive(@NotNull Term x) {
        Terms.printRecursive(System.out, x, 0);
    }


    @Test
    public void testParser() {


        //NAR p = new NAR(new Default());

        assertNotNull("metaparser can is a superset of narsese", p.term("<A --> b>"));

        //

        assertEquals(0, p.term("#A").complexity());
        assertEquals(1, p.term("#A").volume());
        assertEquals(0, p.term("%A").complexity());
        assertEquals(1, p.term("%A").volume());

        assertEquals(3, p.term("<A --> B>").complexity());
        assertEquals(1, p.term("<%A --> %B>").complexity());

        {
            PremiseRule x = rule("A, A |- A, (Belief:Revision, Goal:Weak)");
            assertNotNull(x);
            //assertEquals("((A,A),(A,((Revision-->Belief),(Weak-->Desire))))", x.toString());
            // assertEquals(12, x.getVolume());
        }


        int vv = 19;
        {
            PremiseRule x = rule("<A --> B>, <B --> A> |- <A <-> B>, (Belief:Revision, Goal:Weak)");
            x = rule(x);
            assertEquals(vv, x.volume());
            //assertEquals("(((%1-->%2),(%2-->%1)),((%1<->%2),((Revision-->Belief),(Weak-->Desire))))", x.toString());

        }
        {
            PremiseRule x = rule("<A --> B>, <B --> A> |- <A <-> nonvar>, (Belief:Revision, Goal:Weak)");
            x = rule(x);
            assertEquals(vv, x.volume()); //same volume as previous block
            //assertEquals("(((%1-->%2),(%2-->%1)),((nonvar<->%1),((Revision-->Belief),(Weak-->Desire))))", x.toString());
        }
        {
            PremiseRule x = rule(" <A --> B>, <B --> A> |- <A <-> B>,  (Belief:Conversion, Punctuation:Judgment)");
            x = rule(x);
            assertEquals(vv, x.volume());
            //assertEquals("(((%1-->%2),(%2-->%1)),((%1<->%2),((Conversion-->Belief),(Judgment-->Punctuation))))", x.toString());
        }


//        {
//            TaskRule x = p.termRaw("<<A --> b> |- (X & y)>");
//            assertEquals("((<A --> b>), ((&, X, y)))", x.toString());
//            assertEquals(9, x.getVolume());
//        }

        //and the first complete rule:
        PremiseRule x = rule("(S --> M), (P --> M) |- (P <-> S), (Belief:Comparison,Goal:Strong)");
        x = rule(x);
        //assertEquals("(((%1-->%2),(%3-->%2)),((%1<->%3),((Comparison-->Belief),(Strong-->Desire))))", x.toString());
        assertEquals(vv, x.volume());

    }

    @NotNull static PremiseRule rule(PremiseRule onlyRule) {
        return new PremiseRuleSet(true, onlyRule).rules.get(0);
    }

    @NotNull static PremiseRule rule(@NotNull String onlyRule) {
        return parse(onlyRule, new PatternTermIndex());
//        PremiseRule r = (PremiseRule) p.term(onlyRule);
//        return rule(
//                r
//        );
    }

    @Test
    public void testNotSingleVariableRule1() {
        //tests an exceptional case that should now be fixed

        PatternTermIndex i = new PatternTermIndex();

        String l = "((B,P) --> ?X) ,(B --> A), task(\"?\") |- ((B,P) --> (A,P)), (Belief:BeliefStructuralDeduction, Punctuation:Judgment)";
        Compound x = parse(l, i).normalizeRule(i);
        assertNotNull(x);
        assertNotNull(x.toString());
        assertTrue(!x.toString().contains("%B"));
    }

    @Test
    public void testPatternVarNormalization() {

        //Narsese p = Narsese.the();

        //TODO test combination of lowercase and uppercase pattern terms
//        TaskRule x = p.term("<<A --> b> |- (X & y)>");
//
//        assertEquals("((<%A --> b>), ((&, %X, y)))", x.toString());




        Compound y = rule("(S --> P), (--,%S) |- (P --> S), (Belief:Conversion)");
        assertNotNull(y);

        PatternTermIndex i = new PatternTermIndex();
        y = ((PremiseRule) y).normalizeRule(i);
        assertNotNull(y);
        printRecursive(y);

        //assertEquals("(((%1-->%2),(--,%1)),((%2-->%1),((Conversion-->Belief))))", y.toString());
        assertEquals(10, y.complexity());
        assertEquals(15, y.volume());
    }


    @Test
    public void printTermRecursive() {
        Compound y = rule("(S --> P), --%S |- (P --> S), (Belief:Conversion, Info:SeldomUseful)");
        printRecursive(y);
    }


//    @Test
//    public void testReifyPatternVariables() {
//        Default n = new Default(1024, 2, 3, 3);
//        //n.core.activationRate.setValue(0.75f);
//
//
//        Deriver.getDefaultDeriver().rules.reifyTo(n);
//        n.run(2);
//        n.forEachConcept(c -> {
//            assertEquals(0, c.term().varPattern());
//            c.term().recurseTerms((s, x) -> {
//                assertFalse(s.op() == Op.VAR_PATTERN);
//                //System.out.println(c + " " + s + " " + s.volume() + "," + s.getClass());
//            });
//            //System.out.println(c);
//        });
//
//    }

    @Test
    public void testBackwardPermutations() {
        if (Param.BACKWARD_QUESTION_RULES) {
            Set<PremiseRule> s = PremiseRuleSet.permute(
                    rule("(A --> B), (B --> C), neq(A,C) |- (A --> C), (Belief:Deduction, Goal:Strong, Permute:Backward, Permute:Swap)")
            );
            assertNotNull(s);
            //System.out.println(Joiner.on('\n').join(s));

            //total variations from the one input:
            assertEquals(4 /* negations */, s.size());



            //TODO
            //String x = s.toString();
//            assertTrue(x.contains("(((%1-->%2),(%3-->%1),neq(%3,%2)),((%3-->%2),((DeductionX-->Belief),(StrongX-->Desire),(AllowBackward-->Derive))))"));
//            assertTrue(x.contains("(((%1-->%2),(%2-->%3),neq(%1,%3)),((%1-->%3),"));
//            //assertTrue(x.contains("(((%1-->%2),(%1-->%3),neq(%1,%2),task(\"?\")),((%3-->%2),"));
//            assertTrue(x.contains("(((%1-->%2),(%1-->%3),neq(%1,%3),task(\"?\")),((%2-->%3),"));
//            //assertTrue(x.contains("(((%1-->%2),(%3-->%2),neq(%3,%2),task(\"?\")),((%3-->%1),"));
//            assertTrue(x.contains("(((%1-->%2),(%3-->%2),neq(%1,%2),task(\"?\")),((%1-->%3),"));

        }
    }

    @Test public void testSubstIfUnifies() {
        PremiseRule r = rule("(Y --> L), ((Y --> S) ==> R), neq(L,S) |- substitute(((&&,(#X --> L),(#X --> S)) ==> R),Y,#X), (Belief:Induction, Goal:Induction)");
        System.out.println(r);
        System.out.println(r.source);
        Set<PremiseRule> s = PremiseRuleSet.permute(r);
        System.out.println(Joiner.on('\n').join(s));
    }
}