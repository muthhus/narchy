package nars.util;

import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.control.MetaGoal;
import nars.test.TestNAR;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;


@ExtendWith(NALTestStats.class)
public abstract class NALTest {

    public final NAR nar;
    public final TestNAR test;

    static {
        Param.ANSWER_REPORTING = false;
    }

    public MetaGoal.Report metagoals;

    protected NALTest() {
        Param.DEBUG = true;
        test = new TestNAR(nar = nar());
    }

    protected NAR nar() {
        return NARS.tmp();
    }





    @AfterEach
    public void end(TestInfo testInfo) {

        test.test();

        nar.stop();
        nar.clear();


        this.metagoals = new MetaGoal.Report().add(nar.causes);
        System.out.println(
            metagoals.table().toString()
        );
    }

}
