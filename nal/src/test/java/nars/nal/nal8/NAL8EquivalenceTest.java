package nars.nal.nal8;

import nars.util.AbstractNALTest;
import org.junit.Ignore;
import org.junit.Test;

import static nars.Op.GOAL;

@Ignore
public class NAL8EquivalenceTest extends AbstractNALTest {

    static final int cycles = NAL8Test.cycles;

    @Test
    public void testPosGoalEquivalenceSpreading() {

        test
                .input("(R)!")
                .input("((G) <=> (R)).")
                .mustDesire(cycles, "(G)", 1.0f, 0.81f);
    }

    @Test
    public void testNegatedGoalEquivalenceSpreading() {

        test
                .input("--(R)!")
                .input("((G) <=> (R)).")
                .mustDesire(cycles, "(G)", 0.0f, 0.81f);
    }

    @Test
    public void testGoalEquivComponent() {

        test
                .input("(happy)!")
                .input("((happy) <=>+0 ((--,(x)) &| (--,(out)))).")
                .mustDesire(cycles, "((--,(x)) &| (--,(out)))", 1f, 0.81f);
    }

    @Test
    public void testGoalEquivComponentNeg() {

        test
                .log()
                .input("(happy)!")
                .input("(--(happy) <=>+0 ((--,(x))&&(--,(out)))).")
                .mustDesire(cycles, "((--,(x))&&(--,(out)))", 0f, 0.81f);
    }

    @Test
    public void testPredictiveEquivalenceTemporalTemporal() {

        test
                .inputAt(0, "((out) <=>-3 (happy)). :|:")
                .inputAt(13, "(happy)! :|:")
                .mustDesire(cycles, "(out)", 1f, 0.81f, 16)
                .mustNotOutput(cycles, "(out)", GOAL, 3);
    }
}
