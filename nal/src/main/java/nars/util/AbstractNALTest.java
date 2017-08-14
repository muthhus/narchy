package nars.util;

import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.test.TestNAR;
import org.junit.After;
import org.junit.jupiter.api.AfterEach;


public abstract class AbstractNALTest  {

    public final NAR nar;
    public final TestNAR test;

    static {
        Param.ANSWER_REPORTING = false;
    }

    protected AbstractNALTest() {
        Param.DEBUG = true;
        test = new TestNAR(nar = nar());
    }

    protected NAR nar() {
        return NARS.tmp();
    }

    @After
    public void end() {

        test.run();

        nar.stop();
        nar.clear();
    }

}
