package nars.concept;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.concept.dynamic.DynTruth;
import nars.concept.dynamic.DynamicBeliefTable;
import nars.concept.dynamic.DynamicConcept;
import nars.nar.NARBuilder;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static nars.$.$;
import static nars.Op.QUESTION;
import static org.junit.Assert.*;

/**
 * Created by me on 10/27/16.
 */
public class DynamicBeliefTableTest {

    @Test
    public void testDynamicConjunction2() throws Narsese.NarseseException {
        NAR n = new NARBuilder().get();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("b:x", 0f, 0.9f);
        n.run(1);
        long now = n.time();
        int dur = n.dur();
        assertEquals($.t(1f,0.81f), n.beliefTruth("(a:x && a:y)", now));
        assertEquals($.t(0f,0.81f), n.beliefTruth("(b:x && a:y)", now));
        assertEquals($.t(0f,0.81f), n.beliefTruth("(a:x && (--,a:y))", now));
        assertEquals($.t(1f,0.81f), n.beliefTruth("((--,b:x) && a:y)", now));
        assertEquals($.t(0f,0.81f), n.beliefTruth("((--,b:x) && (--,a:y))", now));
    }
    @Test
    public void testDynamicIntersection() throws Narsese.NarseseException {
        NAR n = new NARBuilder().get();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:z", 0f, 0.9f);
        n.believe("x:b", 1f, 0.9f);
        n.believe("y:b", 1f, 0.9f);
        n.believe("z:b", 0f, 0.9f);
        n.run(1);
        long now = n.time();
        int dur = n.dur();
        assertTrue(n.conceptualize($("((x|y)-->a)")).beliefs() instanceof DynamicBeliefTable);
        assertEquals($.t(1f,0.81f), n.beliefTruth("((x|y)-->a)", now));
        assertEquals($.t(0f,0.81f), n.beliefTruth("((x|z)-->a)", now));
        assertEquals($.t(1f,0.81f), n.beliefTruth("((x&z)-->a)", now));
        assertEquals($.t(1f,0.81f), n.beliefTruth("(b --> (x|y))", now));
        assertEquals($.t(1f,0.81f), n.beliefTruth("(b --> (x|z))", now));
        assertEquals($.t(0f,0.81f), n.beliefTruth("(b --> (x&z))", now));

        Concept xIntNegY = n.conceptualize($("((x|(--,y))-->a)"));
        assertTrue(xIntNegY instanceof DynamicConcept);
        assertEquals($.t(0f,0.81f), n.beliefTruth(xIntNegY, now));
        assertEquals($.t(1f,0.81f), n.beliefTruth("((x|(--,z))-->a)", now));
    }

    @Test
    public void testDynamicConjunction3() throws Narsese.NarseseException {
        NAR n = new NARBuilder().get();
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
        {
            Concept ccn = n.conceptualize($("(&&, a:x, (--, a:y), a:z)"));
            Truth nown = n.beliefTruth(ccn, n.time());
            assertTrue($.t(0f, 0.73f).equals(nown, 0.01f));
        }

        n.clear();

        //test change after a component's revision:
        n.believe("a:y", 0, 0.95f);
        n.run(1);
        Truth now2 = n.beliefTruth("(&&, a:x, a:y, a:z)", n.time());
        assertTrue(now2.freq() < 0.4f);

    }

    @Test
    public void testDynamicConjunction2Temporal() throws Narsese.NarseseException {
        NAR n = new NARBuilder().get();
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
        {
            Compound template = $("((x) &&+4 (y))");

            DynTruth xt = xtable.truth(0, template, true, n);
            assertNotNull(xt);
            assertTrue(xt.truth().toString(), $.t(1f, 0.83f).equals(xt.truth(), 0.05f));

//            for (int i = -4; i <= n.time() + 4; i++) {
//                System.out.println(i + ": " + xtable.truth(i, template.dt(), true) + " " + xtable.generate(template, i));
//            }
        }

//        long when = 0;
//        for (int i = 0; i <= 8; i++) {
//            Compound template = $("((x) &&+"+ i + " (y))");
//            System.out.println( xtable.truth(when, template.dt(), true) + " " + xtable.generate(template, when));
//        }

        assertEquals(0.79f, xtable.generate($("((x) &&+6 (y))"), 0, 0, n).conf(), 0.05f);
        assertEquals(0.81f, xtable.generate($("((x) &&+4 (y))"), 0, 0, n).conf(), 0.05f); //best match to the input
        assertEquals(0.79f, xtable.generate($("((x) &&+2 (y))"), 0, 0, n).conf(), 0.05f);
        assertEquals(0.77f, xtable.generate($("((x) &&+0 (y))"), 0, 0, n).conf(), 0.05f);
        assertEquals(0.62f, xtable.generate($("((x) &&-32 (y))"), 0, 0, n).conf(), 0.05f);


    }

    @Test public void testDynamicProductImageExtensional() throws Narsese.NarseseException {
        NAR n = new NARBuilder().get();

        n.believe($("f(x,y)"), (long)0, 1f, 0.9f).run(1);

        CompoundConcept prod = (CompoundConcept) n.concept($("f(x, y)"));
        int dur = n.dur();

        long when1 = n.time();
        Truth t = prod.belief(when1, when1, dur, n);
        System.out.println(t);

        CompoundConcept imgX = (CompoundConcept) n.conceptualize($("(x --> (/,f,_,y))"));
        assertEquals(t, n.beliefTruth(imgX, n.time()));


        CompoundConcept imgY = (CompoundConcept) n.conceptualize($("(y --> (/,f,x,_))"));
        assertEquals(t, n.beliefTruth(imgY, n.time()));



        n.run(16); //by now, structural decomposition should have also had the opportunity to derive the image

        long when = n.time();
        Truth t2 = prod.belief(when, when, dur, n);

        assertEquals(t2, n.beliefTruth(imgX, n.time()));
        assertEquals(t2, n.beliefTruth(imgY, n.time()));

    }

    @Test public void testDynamicProductImageIntensional() throws Narsese.NarseseException {
        NAR nar = new NARBuilder().get();
        int dur = 9;
        nar.time.dur(dur);

        nar.believe($("(f-->(x,y))"), (long)0, 1f, 0.9f).run(1);

        CompoundConcept prod = (CompoundConcept) nar.concept($("(f-->(x, y))"));
        Truth t = prod.belief((long) 0, (long) 0, dur, nar);

        CompoundConcept imgX = (CompoundConcept) nar.conceptualize($("((\\,f,_,y)-->x)"));
        assertNotNull(imgX);
        Truth xb = imgX.belief((long) 0, (long) 0, dur, nar);
        assertNotNull(xb);
        assertEquals(t, xb);

        CompoundConcept imgY = (CompoundConcept) nar.conceptualize($("((\\,f,x,_)-->y)"));
        assertEquals(t, imgY.belief((long) 0, (long) 0, dur, nar));



        nar.run(6); //by now, structural decomposition should have also had the opportunity to derive the image

        Truth t2 = prod.belief((long) 0, (long) 0, dur, nar);

        assertEquals(t2, imgX.belief((long) 0, (long) 0, dur, nar));
        long when1 = nar.time();
        assertNotEquals(t2, imgX.belief(when1, when1, dur, nar));
        assertEquals(t2, imgY.belief((long) 0, (long) 0, dur, nar));
        long when = nar.time();
        assertNotEquals(t2, imgY.belief(when, when, dur, nar));

    }

    @Test public void testAnswerTemplateWithVar() throws Narsese.NarseseException {
        NAR n = new NARBuilder().get();
        String c = "(tetris-->(((0,(1,(1))),(0,(0,(1,(0)))))&((1,(0,(1))),(0,(0,(1,(0)))))))";
        n.believe(c);
        n.run(1);
        @Nullable Task a = n.concept(c).beliefs().match((long) 0, (long) 0, 1, $.task($("(tetris-->#1)"), QUESTION, null).apply(n), null, false, n);
        //System.out.println(a);
        assertTrue(a.toString().endsWith(" (tetris-->(((0,(1,(1))),(0,(0,(1,(0)))))&((1,(0,(1))),(0,(0,(1,(0))))))). %1.0;.90%"));
//        @Nullable Task b = n.concept(c).beliefs().match(10, 0, 1, $.task($("(tetris-->#1)"), QUESTION, null).apply(n), false);
//        System.out.println(b);
    }
}