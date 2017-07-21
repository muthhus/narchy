package nars;

import junit.ParallelTestRunner;
import nars.io.NarseseBaseTest;
import nars.io.NarseseExtendedTest;
import nars.io.NarseseTest;
import nars.nal.nal1.NAL1Test;
import nars.nal.nal2.NAL2Test;
import nars.nal.nal3.NAL3Test;
import nars.nal.nal4.NAL4MultistepTest;
import nars.nal.nal5.NAL5Test;
import nars.nal.nal6.NAL6MultistepTest;
import nars.nal.nal6.NAL6Test;
import nars.nal.nal7.NAL7Test;
import nars.nal.nal8.NAL8Test;
import nars.nal.nal8.NAL8TestExt;
import nars.task.TermIOTest;
import nars.term.*;
import nars.term.transform.EllipsisTest;
import nars.term.transform.TermutatorTest;
import nars.term.transform.UnifyTest;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/** low-level Term tests */
@Ignore //dont run except manually
@RunWith(ParallelTestRunner.class)
@Suite.SuiteClasses({
        TermTest.class,
        TermReductionsTest.class,
        TermIOTest.class,
        TermContainerTest.class,
        NarseseBaseTest.class,
        NarseseExtendedTest.class,
        TermutatorTest.class,
        UnifyTest.class,
        EllipsisTest.class,
        TermHashTest.class,
        TermIDTest.class,
        TermNormalizationTest.class,
        ProxyTermTest.class

        //TODO add others
})
public class AllTerm {
}
