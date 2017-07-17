package nars.concept;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.concept.dynamic.DynTruth;
import nars.concept.dynamic.DynamicBeliefTable;
import nars.concept.dynamic.DynamicConcept;
import nars.NARS;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static nars.$.$;
import static nars.Op.QUESTION;
import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.*;

/**
 * Created by me on 10/27/16.
 */
public class DynamicBeliefTableTest {

    @Test
    public void testDynamicConjunction2() throws Narsese.NarseseException {
        NAR n = new NARS().get();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("b:x", 0f, 0.9f);
        n.run(1);
        long now = n.time();
        assertEquals($.t(1f,0.81f), n.beliefTruth(n.conceptualize($("(a:x && a:y)")), now));
        assertEquals($.t(0f,0.81f), n.beliefTruth(n.conceptualize($("(b:x && a:y)")), now));
        assertEquals($.t(0f,0.81f), n.beliefTruth(n.conceptualize($("(a:x && (--,a:y))")), now));
        assertEquals($.t(1f,0.81f), n.beliefTruth(n.conceptualize($("((--,b:x) && a:y)")), now));
        assertEquals($.t(0f,0.81f), n.beliefTruth(n.conceptualize($("((--,b:x) && (--,a:y))")), now));
    }
    @Test
    public void testDynamicIntersection() throws Narsese.NarseseException {
        NAR n = new NARS().get();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:z", 0f, 0.9f);
        n.believe("x:b", 1f, 0.9f);
        n.believe("y:b", 1f, 0.9f);
        n.believe("z:b", 0f, 0.9f);
        n.run(2);
        for (long now : new long[] { 0, n.time() /* 2 */, ETERNAL }) {
            assertTrue(n.conceptualize($("((x|y)-->a)")).beliefs() instanceof DynamicBeliefTable);
            assertEquals($.t(1f, 0.81f), n.beliefTruth("((x|y)-->a)", now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("((x|z)-->a)")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x&z)-->a)")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("(b --> (x|y))")), now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("(b --> (x|z))")), now));
            assertEquals($.t(0f, 0.81f), n.beliefTruth(n.conceptualize($("(b --> (x&z))")), now));

            Concept xIntNegY = n.conceptualize($("((x|(--,y))-->a)"));
            assertTrue(xIntNegY instanceof DynamicConcept);
            assertEquals($.t(0f, 0.81f), n.beliefTruth(xIntNegY, now));
            assertEquals($.t(1f, 0.81f), n.beliefTruth(n.conceptualize($("((x|(--,z))-->a)")), now));
        }
    }

    @Test
    public void testDynamicConjunction3() throws Narsese.NarseseException {
        NAR n = new NARS().get();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:z", 1f, 0.9f);
        n.run(1);

        Concept cc = n.conceptualize($("(&&, a:x, a:y, a:z)"));
        Truth now = n.beliefTruth(cc, n.time());
        assertTrue($.t(1f, 0.73f).equals(now, 0.01f));
        //the truth values were provided despite the belief tables being empty:
        assertTrue(cc.beliefs().isEmpty());

        //test unknown:
        {
            TaskConcept ccn = (TaskConcept) n.conceptualize($("(&&, a:x, a:w)"));
            Truth nown = n.beliefTruth(ccn, n.time());
            assertNull(nown);
        }

        //test negation:
        Concept ccn = n.conceptualize($("(&&, a:x, (--, a:y), a:z)"));
        Truth nown = n.beliefTruth(ccn, n.time());
        assertTrue($.t(0f, 0.73f).equals(nown, 0.01f));

        n.clear();

        //test change after a component's revision:
        n.believe("a:y", 0, 0.95f);
        n.run(1);
        Truth now2 = n.beliefTruth(n.conceptualize($("(&&, a:x, a:y, a:z)")), n.time());
        assertTrue(now2.freq() < 0.4f);

    }

    @Test
    public void testDynamicConjunction2Temporal() throws Narsese.NarseseException {
        NAR n = new NARS().get();
        n.believe($("(x)"), (long)0, 1f, 0.9f);
        n.believe($("(y)"), (long)4, 1f, 0.9f);
        n.run(2);
        n.time.dur(8);
        CompoundConcept cc = (CompoundConcept) n.conceptualize($("((x) && (y))"));
//        cc.print();
//
//
//        System.out.println("Instantaneous");
//        for (int i = -4; i <= n.time()+4; i++) {
//            System.out.println( i + ": " + cc.belief(i) );
//        }

        DynamicBeliefTable xtable = (DynamicBeliefTable) ((cc).beliefs());
        Compound template = $("((x) &&+4 (y))");

        DynTruth xt = xtable.truth(0, template, true, n);
        assertNotNull(xt);
        assertTrue(xt.truth().toString(), $.t(1f, 0.83f).equals(xt.truth(), 0.05f));

//            for (int i = -4; i <= n.time() + 4; i++) {
//                System.out.println(i + ": " + xtable.truth(i, template.dt(), true) + " " + xtable.generate(template, i));
//            }

//        long when = 0;
//        for (int i = 0; i <= 8; i++) {
//            Compound template = $("((x) &&+"+ i + " (y))");
//            System.out.println( xtable.truth(when, template.dt(), true) + " " + xtable.generate(template, when));
//        }

        assertEquals(0.79f, xtable.generate($("((x) &&+6 (y))"), 0, n).conf(), 0.05f);
        assertEquals(0.81f, xtable.generate($("((x) &&+4 (y))"), 0,  n).conf(), 0.05f); //best match to the input
        assertEquals(0.79f, xtable.generate($("((x) &&+2 (y))"),  0, n).conf(), 0.05f);
        assertEquals(0.77f, xtable.generate($("((x) &&+0 (y))"),  0, n).conf(), 0.05f);
        assertEquals(0.55f, xtable.generate($("((x) &&-32 (y))"), 0,  n).conf(), 0.05f);


    }



    @Test public void testAnswerTemplateWithVar() throws Narsese.NarseseException {
        NAR n = new NARS().get();
        String c = "(tetris-->(((0,(1,(1))),(0,(0,(1,(0)))))&((1,(0,(1))),(0,(0,(1,(0)))))))";
        n.believe(c);
        n.run(1);
        @Nullable Task a = n.conceptualize(c).beliefs().match((long) 0, $.task($("(tetris-->#1)"), QUESTION, null).apply(n), null, false, n);
        //System.out.println(a);
        assertTrue(a.toString().endsWith(" (tetris-->(((0,(1,(1))),(0,(0,(1,(0)))))&((1,(0,(1))),(0,(0,(1,(0))))))). %1.0;.90%"));
//        @Nullable Task b = n.concept(c).beliefs().match(10, 0, 1, $.task($("(tetris-->#1)"), QUESTION, null).apply(n), false);
//        System.out.println(b);
    }
}