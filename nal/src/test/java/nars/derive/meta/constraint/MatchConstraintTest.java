package nars.derive.meta.constraint;

import nars.Narsese;
import nars.derive.Deriver;
import nars.derive.TrieDeriver;
import nars.nal.AbstractNALTest;
import nars.nar.Default;
import nars.test.TestNAR;
import org.junit.Ignore;
import org.junit.Test;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 5/3/17.
 */
public class MatchConstraintTest {

    final static int CYCLES = 1024;

    @Test public void testNeqComRecursiveConstraintAllRules() {

        /*
        SHOULD NOT HAPPEN:
        $.02;.09$ ((o-(i-happy))-->happy). 497⋈527 %.55;.18% {497⋈527: æ0IáËÑþKn;æ0IáËÑþKM;æ0IáËÑþKÄ;æ0IáËÑþKÉ;æ0IáËÑþKÌ} (((%1-->%2),(%1-->%3),neqCom(%2,%3)),((%3-->%2),((Abduction-->Belief),(Weak-->Goal),(Backward-->Permute))))
            $.04;.75$ happy(L). 497⋈512 %.55;.75% {497⋈512: æ0IáËÑþKÄ}
            $.05;.53$ ((L)-->(o-(i-happy))). 527 %.54;.53% {527: æ0IáËÑþKn;æ0IáËÑþKM;æ0IáËÑþKÉ;æ0IáËÑþKÌ} Dynamic
        */
        new TestNAR(AbstractNALTest.nars(4).iterator().next().get())
                .believe("happy(L)", 1f, 0.9f)
                .believe("((L)-->(o-(i-happy)))", 1f, 0.9f)
                .mustNotOutput(CYCLES, "((o-(i-happy))-->happy)", BELIEF, ETERNAL);
    }

    @Test public void testNeqComRecursiveConstraintOneRule() {

        /*
        SHOULD NOT HAPPEN:
        $.02;.09$ ((o-(i-happy))-->happy). 497⋈527 %.55;.18% {497⋈527: æ0IáËÑþKn;æ0IáËÑþKM;æ0IáËÑþKÄ;æ0IáËÑþKÉ;æ0IáËÑþKÌ} (((%1-->%2),(%1-->%3),neqCom(%2,%3)),((%3-->%2),((Abduction-->Belief),(Weak-->Goal),(Backward-->Permute))))
            $.04;.75$ happy(L). 497⋈512 %.55;.75% {497⋈512: æ0IáËÑþKÄ}
            $.05;.53$ ((L)-->(o-(i-happy))). 527 %.54;.53% {527: æ0IáËÑþKn;æ0IáËÑþKM;æ0IáËÑþKÉ;æ0IáËÑþKÌ} Dynamic
        */
        new TestNAR(new Default() {
//            @Override
//            public Deriver newDeriver() {
//                try {
//                    return TrieDeriver.get("(M --> P), (M --> S), task(\".\"), notSet(S), notSet(P), neqRCom(S,P) |- (M --> (P | S)), (Belief:Union)");
//                } catch (Narsese.NarseseException e) {
//                    throw new RuntimeException(e);
//                }
//            }
        })
                .log()
                .believe("happy(L)", 1f, 0.9f)
                .believe("((L)-->(o-(i-happy)))", 1f, 0.9f)
                .mustNotOutput(CYCLES, "((o-(i-happy))-->happy)", BELIEF, ETERNAL);
    }

    @Ignore
    @Test public void testNeqComConstraint() {
        /*
        SHOULD NOT HAPPEN:
        $.05;.07$ ((((L)~(i|(L)))|(L))-->happy). 1866⋈1876 %.10;.16% {1866⋈1876: êbaîCóòmh;êbaîCóòoÁ;êbaîCóòoÃ;êbaîCóòrj;êbaîCóòrm;êbaîCóòrÏ} (((%1-->%2),(%3-->%2),notSet(%3),notSet(%1),neqCom(%3,%1)),(((%1|%3)-->%2),((Intersection-->Belief),(Intersection-->Goal))))
            $.08;.75$ happy(L). 1866⋈1876 %1.0;.75% {1866⋈1876: êbaîCóòrj}
            $.04;.43$ ((((L)~(i|(L)))|(L))-->happy). 1876 %.10;.21% {1876: êbaîCóòmh;êbaîCóòoÁ;êbaîCóòoÃ;êbaîCóòrm;êbaîCóòrÏ} Dynamic
        */
        new TestNAR(new Default() {
//            @Override
//            public Deriver newDeriver() {
//                return null;
//                //return TrieDeriver.get("what is it");
//            }
        })
                .believe("happy(L)", 1f, 0.9f)
                .believe("(((i)|(L))-->happy)", 1f, 0.9f)
                .mustNotOutput(CYCLES, "(((i)|(L))-->happy)", BELIEF, 1f, 1f, 0.81f, 0.81f, ETERNAL);
    }


}