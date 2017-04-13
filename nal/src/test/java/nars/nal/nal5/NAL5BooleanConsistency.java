package nars.nal.nal5;

import nars.$;
import nars.Narsese;
import nars.Param;
import nars.derive.Deriver;
import nars.nar.Default;
import org.junit.Test;

/**
 * TODO
 */
public class NAL5BooleanConsistency {

    @Test
    public void testSAT2() throws Narsese.NarseseException {

        Param.DEBUG = true;
        //Param.HORIZON = 10;

        final Deriver e = Deriver.get("induction.nal", "nal6.nal", "misc.nal");

        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++) {
                Default d = new Default(1024, 1, 3) {
                    @Override
                    public Deriver newDeriver() {
                        return e;
                    }
                };

                //d.log();

                String[] outcomes = {
                        "(x --> both_false)",
                        "(x --> i_not_j)",
                        "(x --> j_not_i)",
                        "(x --> both_true)"};

                d.believe("(((i) && (j)) ==> " + outcomes[3] + ")");
                d.believe("((--(i) && --(j)) ==> " + outcomes[0] + ")");
                d.believe("(((i) && --(j)) ==> " + outcomes[1] + ")");
                d.believe("((--(i) && (j)) ==> " + outcomes[2] + ")");

                d.believe($.negIf($.$("(i)"), i==0));
                d.believe($.negIf($.$("(j)"), j==0));

//                for (String s : outcomes) {
//                    d.ask(s);
//                }

                d.run(256);

                System.out.println(i + " " + j);

                for (String s : outcomes) {
                    System.out.println("\t" + s + "\t" + d.concept(s).belief(d.time(), d.dur()));
                }

                System.out.println();

                //return;
            }
    }


}
