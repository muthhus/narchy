package nars.task;

import nars.Global;
import nars.NAR;
import nars.nar.AbstractNAR;
import nars.nar.Default;
import nars.task.flow.TaskQueue;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by me on 8/31/15.
 */
public class UniqueInputSerialTest {

    @Test
    public void inputTwoUniqueTasksDef() {
        inputTwoUniqueTasks(new Default());
    }
    /*@Test public void inputTwoUniqueTasksSolid() {
        inputTwoUniqueTasks(new Solid(4, 1, 1, 1, 1, 1));
    }*/
    /*@Test public void inputTwoUniqueTasksEq() {
        inputTwoUniqueTasks(new Equalized(4, 1, 1));
    }
    @Test public void inputTwoUniqueTasksNewDef() {
        inputTwoUniqueTasks(new Default());
    }*/

    public void inputTwoUniqueTasks(NAR n) {

        Global.DEBUG = true;

        Task x = n.inputTask("<a --> b>.");
        assertArrayEquals(new long[]{1}, x.evidence());
        n.step();

        Task y = n.inputTask("<b --> c>.");
        assertArrayEquals(new long[]{2}, y.evidence());
        n.step();

        n.reset();

        TaskQueue z = n.inputs("<e --> f>.  <g --> h>. "); //test when they are input on the same parse

        n.run(10);

        Task q = n.inputTask("<c --> d>.");
        assertArrayEquals(new long[]{5}, q.evidence());

    }


    @Test
    public void testDoublePremiseMultiEvidence() {

        AbstractNAR d = new Default(100,1,1,3);
        d.nal(2);
        d.input("<a --> b>.", "<b --> c>.");

        long[] ev = {1, 2};
        d.memory.eventTaskProcess.on(t -> {
            if (t.isDouble())
                assertArrayEquals("all double-premise derived terms have this evidence: "
                        + t + ": " + Arrays.toString(ev) + "!=" + Arrays.toString(t.evidence()), ev, t.evidence());

            //System.out.println(t);
        });

        d.run(64);


    }
}
