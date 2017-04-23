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
                Default d = new Default(1024, 3) {
                    @Override
                    public Deriver newDeriver() {
                        return e;
                    }
                };

                d.log();

                String[] outcomes = {
                        "(x --> (0,0))",
                        "(x --> (1,0))",
                        "(x --> (0,1))",
                        "(x --> (1,1))"};
                String expected = "(x --> (" + i + "," + j + "))";

                d.believe("(((x-->i) && (x-->j)) <=> " + outcomes[3] + ")");
                d.believe("((--(x-->i) && --(x-->j)) <=> " + outcomes[0] + ")");
                d.believe("(((x-->i) && --(x-->j)) <=> " + outcomes[1] + ")");
                d.believe("((--(x-->i) && (x-->j)) <=> " + outcomes[2] + ")");

                d.believe($.negIf($.$("(x-->i)"), i==0));
                d.believe($.negIf($.$("(x-->j)"), j==0));

//                for (String s : outcomes) {
//                    d.ask(s);
//                }

                d.run(16);

                System.out.println(i + " " + j);

                for (String s : outcomes) {
                    System.out.println("\t" + s + "\t" + d.concept(s).belief(d.time(), d.dur()));
                }

                System.out.println();

                //return;
            }
    }


}
