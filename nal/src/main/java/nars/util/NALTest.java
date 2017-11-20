package nars.util;

import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.control.MetaGoal;
import nars.test.TestNAR;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(NALTestStats.class)
public abstract class NALTest {

    public final NAR nar;
    public final TestNAR test;
    public final MetaGoal.Report metagoals = new MetaGoal.Report();

    protected NALTest() {
        test = new TestNAR(nar = nar());
    }

    @BeforeEach
    void init() {
        Param.DEBUG = true;
        Param.ANSWER_REPORTING = false;
    }

    protected NAR nar() {
        return NARS.tmp();
    }


    @AfterEach
    public void end(TestInfo testInfo) {

        test.test();

        nar.stop();

        metagoals.add(nar.causes).print(System.out);
    }

}
