package nars.nal.nal5;

import nars.*;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * NAL5 Boolean / Boolean Satisfiability Sanity
 */
public class NAL5SAT {


    @Test
    public void testSAT2Individual00() throws Narsese.NarseseException {
        testSAT2Individual(0, 0);
    }

    @Test
    public void testSAT2Individual01() throws Narsese.NarseseException {
        testSAT2Individual(0, 1);
    }

    @Test
    public void testSAT2Individual10() throws Narsese.NarseseException {
        testSAT2Individual(1, 0);
    }

    @Test
    public void testSAT2Individual11() throws Narsese.NarseseException {
        testSAT2Individual(1, 1);
    }

    void testSAT2Individual(int i, int j) throws Narsese.NarseseException {

        final float confThresh = 0.7f;
        Param.DEBUG = true;

//        for (int i = 0; i < 2; i++) {
//            for (int j = 0; j < 2; j++) {
        NAR d = new NARS().get();
        d.nal(6);
        d.termVolumeMax.setValue(24);

        String[] outcomes = {
                "(x-->(0,0))",
                "(x-->(0,1))",
                "(x-->(1,0))",
                "(x-->(1,1))"};
        //String expected = "(x --> (" + i + "," + j + "))";

        d.believe("( (--(x-->i) && --(x-->j)) ==> " + outcomes[0] + ")");
        d.believe("( (--(x-->i) && (x-->j)) ==> " + outcomes[1] + ")");
        d.believe("( ((x-->i) && --(x-->j)) ==> " + outcomes[2] + ")");
        d.believe("( ((x-->i) && (x-->j)) ==> " + outcomes[3] + ")");

        Term I = $.$("(x-->i)").negIf(i == 0);
        Term J = $.$("(x-->j)").negIf(j == 0);

//                d.believe(I);
//                d.believe(J);
        d.believe($.conj(I, J));

//                for (String s : outcomes) {
//                    d.ask(s);
//                }

        d.run(1024);

        System.out.println(i + " " + j);
        for (int k = 0, outcomesLength = outcomes.length; k < outcomesLength; k++) {
            String s = outcomes[k];
            Concept dc = d.conceptualize(s);
            assertNotNull(dc);
            @Nullable Task t = d.belief(dc.term(), d.time());
            Truth b = t != null ? t.truth() : null;

            System.out.println("\t" + s + "\t" + b);


            int ex = -1, ey = -1;
            switch (k) {
                case 0:
                    ex = 0;
                    ey = 0;
                    break;
                case 1:
                    ex = 0;
                    ey = 1;
                    break;
                case 2:
                    ex = 1;
                    ey = 0;
                    break;
                case 3:
                    ex = 1;
                    ey = 1;
                    break;
            }
            boolean thisone = ((ex == i) && (ey == j));
            if (thisone && b == null)
                assertTrue("unrecognized true case", false);


            if (thisone && b != null && b.isNegative() && b.conf() > 0)
                assertTrue("wrong true case:\n" + t.proof(), false);

            if (!thisone && b != null && b.isPositive() && b.conf() > confThresh)
                assertTrue("wrong false case:\n" + t.proof(), false);

        }

//                System.out.println();

        //return;
//            }
//        }
    }

//    @Test
//    public void testSAT2Combined() throws Narsese.NarseseException {
//
//        final float confThresh = 0.7f;
//        Param.DEBUG = true;
//
//        NAR d = new NARS().get();
//        d.nal(6);
//        d.termVolumeMax.setValue(32);
//
//        String[] outcomes = {
//                "(x-->(0,0))",
//                "(x-->(0,1))",
//                "(x-->(1,0))",
//                "(x-->(1,1))"};
//
//        for (int i = 0; i < 2; i++) {
//            for (int j = 0; j < 2; j++) {
//
//                //String expected = "(x --> (" + i + "," + j + "))";
//
//                d.believe("( (--(x-->i) && --(x-->j)) ==> " + outcomes[0] + ")");
//                d.believe("( (--(x-->i) && (x-->j)) ==> " + outcomes[1] + ")");
//                d.believe("( ((x-->i) && --(x-->j)) ==> " + outcomes[2] + ")");
//                d.believe("( ((x-->i) && (x-->j)) ==> " + outcomes[3] + ")");
//
//                Compound I = $.negIf($.$("(x-->i)"), i == 0);
//                Compound J = $.negIf($.$("(x-->j)"), j == 0);
//
////                d.believe(I);
////                d.believe(J);
//                d.believe($.conj(I, J));
//
////                for (String s : outcomes) {
////                    d.ask(s);
////                }
//
//
//                //return;
//            }
//        }
//
//        d.run(1024);
//
//        validate(confThresh, d, outcomes);
//
//        System.out.println();
//
//    }

//    public static void validate(float confThresh, NAR d, String[] outcomes) throws Narsese.NarseseException {
//        for (int k = 0, outcomesLength = outcomes.length; k < outcomesLength; k++) {
//            String s = outcomes[k];
//            Concept dc = d.conceptualize(s);
//            assertNotNull(dc);
//            @Nullable Task t = d.belief(dc.term(), d.time());
//            Truth b = t != null ? t.truth() : null;
//
//            System.out.println("\t" + s + "\t" + b);
//
//
//            int ex = -1, ey = -1;
//            switch (k) {
//                case 0:
//                    ex = 0;
//                    ey = 0;
//                    break;
//                case 1:
//                    ex = 0;
//                    ey = 1;
//                    break;
//                case 2:
//                    ex = 1;
//                    ey = 0;
//                    break;
//                case 3:
//                    ex = 1;
//                    ey = 1;
//                    break;
//            }
//            boolean thisone = true; //((ex == i) && (ey == j));
//            if (thisone && b == null)
//                assertTrue("unrecognized true case", false);
//
//
//            if (thisone && b != null && b.isNegative() && b.conf() > 0)
//                assertTrue("wrong true case:\n" + t.proof(), false);
//
//            if (!thisone && b != null && b.isPositive() && b.conf() > confThresh)
//                assertTrue("wrong false case:\n" + t.proof(), false);
//
//        }
//    }


}
