package nars.nal;

import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.op.stm.STMTemporalLinkage;
import nars.test.TestNAR;
import org.junit.After;

/**
 * Created by me on 2/10/15.
 */
public abstract class AbstractNALTest extends NARS {


    public final NAR nar;
    public final TestNAR test;

    static {
        Param.ANSWER_REPORTING = false; //HACK
    }

    protected AbstractNALTest() {


        nar = get();
        nar.termVolumeMax.setValue(32);
        //n.nal(level);
        nar.DEFAULT_BELIEF_PRIORITY = 0.5f;
        nar.DEFAULT_GOAL_PRIORITY = 0.5f;
        nar.DEFAULT_QUEST_PRIORITY = 0.25f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.25f;
        //if (level >= 7) {
        new STMTemporalLinkage(this.nar, 1, false);

        Param.DEBUG = true;
        test = new TestNAR(this.nar);
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
    }


}
