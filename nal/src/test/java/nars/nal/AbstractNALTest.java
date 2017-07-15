package nars.nal;

import nars.NAR;
import nars.nar.NARS;
import nars.op.stm.STMTemporalLinkage;
import nars.test.TestNAR;
import org.jetbrains.annotations.NotNull;
import org.junit.After;

/**
 * Created by me on 2/10/15.
 */
public abstract class AbstractNALTest {


    protected TestNAR test;

    static {
        //Param.DEBUG = true;
    }


    public AbstractNALTest() {

        NAR n = new NARS().get();
        n.termVolumeMax.setValue(28);
        //n.nal(level);
//                    n.DEFAULT_BELIEF_PRIORITY = 0.5f;
//                    n.DEFAULT_GOAL_PRIORITY = 0.5f;
        n.DEFAULT_QUEST_PRIORITY = 0.5f;
        n.DEFAULT_QUESTION_PRIORITY = 0.5f;
        //if (level >= 7) {
        new STMTemporalLinkage(n, 1, false);
        //}

        test = new TestNAR(n);
    }

    @NotNull
    public TestNAR test() {

        return test;
    }

//    public TestNAR test(NAR n) {
//        return new TestNAR(n);
//    }


//    public NAR nar() {
//        //return nar;
//        return nar.get();
//    }


//    @Before
//    public void start() {
//        //System.out.println(tester);
//        //assert(tester==null);
//        //tester = test(nar.get());
//
//    }

    @After
    public void end() {
        test.run();
        //tester = null;
    }


}
