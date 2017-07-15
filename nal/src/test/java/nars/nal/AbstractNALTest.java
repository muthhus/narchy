package nars.nal;

import nars.NAR;
import nars.Param;
import nars.nar.NARS;
import nars.op.stm.STMTemporalLinkage;
import nars.test.TestNAR;
import org.jetbrains.annotations.NotNull;
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


        this.nar = get();
        this.nar.termVolumeMax.setValue(28);
        //n.nal(level);
//                    n.DEFAULT_BELIEF_PRIORITY = 0.5f;
//                    n.DEFAULT_GOAL_PRIORITY = 0.5f;
        this.nar.DEFAULT_QUEST_PRIORITY = 0.5f;
        this.nar.DEFAULT_QUESTION_PRIORITY = 0.5f;
        //if (level >= 7) {
        new STMTemporalLinkage(this.nar, 1, false);

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
