package nars.task;

import jcog.pri.Priority;
import nars.NAR;
import nars.concept.Concept;
import nars.task.util.InvalidTaskException;
import nars.term.util.InvalidTermException;

/**
 * generic abstract task used for commands and other processes
 */
public interface ITask extends Priority {

    default byte punc() {
        return 0;
    }

    void run(NAR n) throws Concept.InvalidConceptException, InvalidTermException, InvalidTaskException;

}
