package nars.experiment;

import nars.NAR;
import nars.NSchool;

/**
 * executes a unix shell and perceives the output as a grid of symbols
 * which can be interactively tagged by human, and optionally edited by NARS
 */
public class ConsoleSchool extends NSchool {

    public ConsoleSchool(NAR nar) {
        super(nar);
    }
}
