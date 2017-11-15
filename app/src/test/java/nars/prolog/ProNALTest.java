package nars.prolog;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Theory;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Op;
import nars.op.prolog.PrologToNAL;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class ProNALTest {

    @Test public void test1() throws InvalidTheoryException, Narsese.NarseseException, IOException, URISyntaxException {
//        Theory t = Theory.string(
//            "add(0,X,X).",
//            "add(s(X),Y,s(Z)):-add(X,Y,Z).\n",
//            "goal(R):-add(s(s(0)),s(s(0)),R)."
//        );
        Theory t = Theory.resource(
            "../../../resources/prolog/furniture.pl"
        );
        NAR n = NARS.tmp(6);
        n.log();
        for (nars.term.Term x : PrologToNAL.N(t)) {
            Term xx = x.normalize();
            if (xx.hasAny(Op.VAR_QUERY))
                n.question(xx);
            else
                n.believe(xx);
        }
        n.run(100);
        n.concepts().forEach(c -> {
           c.print();
        });
        /*
        [0] *** ANSWER=goal(s(s(s(s(0)))))
        TOTAL ANSWERS=1
        */

    }
}
