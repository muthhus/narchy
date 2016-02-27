package nars.nlp;

import nars.Memory;
import nars.NAR;
import nars.nar.Default;

import java.io.FileNotFoundException;

/**
 * Created by me on 2/10/16.
 */
public class SATStream {

    public static void main(String[] args) throws FileNotFoundException {
        Default d = new Default(1000, 1, 1, 3);

        d.core.activationRate.setValue(0.2f);
        ((Memory) NAR.this).perfection.setValue(0.5f);
        d.premiser.confMin.setValue(0.1f);

        d.log();
        d.input(
                "$0.1;0.2;0.5$ T:x1. :|:",
                "$0.1;0.2;0.5$ T:x2. :|:",
                "$0.1;0.2;0.5$ T:(--,x3). :|:",
                "$0.1;0.2;0.5$ T:(--,x4). :|:",

                "$1.0;1.0;1.0$ (($c:$x && $c:$y) ==> AND({$x,$y})). %1.00;1.00%"

                //"$1.0;1.0;1.0$ ((T:$x && T:(--,$y)) <-> AND:{$x,$y})! %1.00;1.00%"


                //"$1.0;1.0;1.0$ ((T:$x & T:$y) ==> AND:{$x,$y}). %1.00;1.00%",
                //"$1.0;1.0;1.0$ ((T:{$x} & T:{(--,$y)}) ==> XOR:{$x,$y}). %1.00;1.00%"
                //"$1.0;1.0;1.0$ ((--,(T:$x & T:$y)) ==> XOR:{$x,$y}). %1.00;1.00%"
        );

        d.run(10360);

        d.core.active.printAll();

        //d.dumpConcepts("/tmp/x.txt");

    }
}
