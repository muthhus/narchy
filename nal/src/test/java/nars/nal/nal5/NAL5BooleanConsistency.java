package nars.nal.nal5;

import nars.$;
import nars.Narsese;
import nars.Param;
import nars.Task;
import nars.derive.Deriver;
import nars.nar.Default;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * TODO
 */
public class NAL5BooleanConsistency {

    @Test
    public void testSAT2() throws Narsese.NarseseException {

        Param.DEBUG = true;

        float confthresh = 0.1f;

        final Deriver e = Deriver.get(
                "induction.nal", "nal6.nal"
        );

        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++) {
                Default d = new Default(1024) {
                    @Override
                    public Deriver newDeriver() {
                        return e;
                    }
                };

                //d.log();

                String[] outcomes = {
                        "(x-->(0,0))",
                        "(x-->(0,1))",
                        "(x-->(1,0))",
                        "(x-->(1,1))"};
                String expected = "(x --> (" + i + "," + j + "))";

                d.believe( "( (--(x-->i) && --(x-->j)) ==> " + outcomes[0] + ")");
                d.believe( "( (--(x-->i) && (x-->j)) ==> " + outcomes[1] + ")");
                d.believe( "( ((x-->i) && --(x-->j)) ==> " + outcomes[2] + ")");
                d.believe( "( ((x-->i) && (x-->j)) ==> " + outcomes[3] + ")");

                Compound I = $.negIf($.$("(x-->i)"), i == 0);
                Compound J = $.negIf($.$("(x-->j)"), j == 0);

//                d.believe(I);
//                d.believe(J);
                d.believe($.conj(I,J));

//                for (String s : outcomes) {
//                    d.ask(s);
//                }

                d.run(512);

                System.out.println(i + " " + j);
                for (int k = 0, outcomesLength = outcomes.length; k < outcomesLength; k++) {
                    String s = outcomes[k];
                    @Nullable Task t = d.concept(s).beliefs().match(d.time(), d.dur());
                    Truth b = t!=null ? t.truth() : null;

                    System.out.println("\t" + s + "\t" + b);

                    int ex = -1, ey = -1;
                    switch (k) {
                        case 0: ex = 0; ey = 0; break;
                        case 1: ex = 0; ey = 1; break;
                        case 2: ex = 1; ey = 0; break;
                        case 3: ex = 1; ey = 1; break;
                    }
                    boolean thisone = ((ex == i) && (ey == j));
                    if (thisone && b == null)
                        assertTrue("unrecognized true case", false);


                    if (thisone && b!=null && b.isNegative() && b.conf() > confthresh)
                        assertTrue("wrong true case:\n" + t.proof(), false);

                    if (!thisone && b!=null && b.isPositive() && b.conf() > confthresh)
                        assertTrue("wrong false case:\n" + t.proof(), false);

                }

                System.out.println();

                //return;
            }
    }


}
