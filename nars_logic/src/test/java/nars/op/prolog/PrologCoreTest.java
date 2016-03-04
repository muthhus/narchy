package nars.op.prolog;

import alice.tuprolog.Agent;
import nars.NAR;

/**
 * Created by me on 3/3/16.
 */
public class PrologCoreTest {

    public static class PrologCore extends Agent {

        private final NAR nar;

        public PrologCore(NAR n) {
            super("");
            this.nar = n;
        }
    }
}
