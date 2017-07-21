package nars;

import junit.ParallelTestRunner;
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
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * high-level integration tests
 */
@Ignore //dont run except manually
@RunWith(ParallelTestRunner.class)
@Suite.SuiteClasses({
        NAL1Test.class,
        NAL2Test.class,
        NAL3Test.class,
        NAL4MultistepTest.class,
        NAL5Test.class,
        NAL6Test.class,
        NAL6MultistepTest.class,
        NAL7Test.class,
        NAL8Test.class,
        NAL8TestExt.class
})
public class AllNAL {
}
