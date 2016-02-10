package nars.nlp;

import nars.NAR;
import nars.concept.Concept;
import nars.nar.Default;

import java.io.FileNotFoundException;

/**
 * Created by me on 2/10/16.
 */
public class SATStream {

    public static void main(String[] args) throws FileNotFoundException {
        Default d = new Default(1000, 1, 3, 3);

        d.core.activationRate.setValue(0.1f);
        d.memory.perfection.setValue(0.75f);

        d.log();
        d.input(
                "$0.1;0.2;0.8$ T:x1. :|:",
                "$0.1;0.2;0.8$ T:x2. :|:",
                "$0.1;0.2;0.8$ T:(--,x3). :|:",
                "$1.0;1.0;1.0$ ((T:$x & T:$y) ==> AND:{$x,$y}). %1.00;1.00%"
                //"$1.0;1.0;1.0$ ((T:$x & T:$y) ==> AND:{$x,$y}). %1.00;1.00%",
                //"$1.0;1.0;1.0$ ((T:{$x} & T:{(--,$y)}) ==> XOR:{$x,$y}). %1.00;1.00%"
                //"$1.0;1.0;1.0$ ((--,(T:$x & T:$y)) ==> XOR:{$x,$y}). %1.00;1.00%"
        );

        d.run(10360);

        d.core.active.printAll();

        //d.dumpConcepts("/tmp/x.txt");

    }
}
