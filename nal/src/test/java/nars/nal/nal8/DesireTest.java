package nars.nal.nal8;

import nars.Global;
import nars.NAR;
import nars.nar.Default;
import nars.task.Task;
import org.junit.Test;

/**
 * Created by me on 1/30/16.
 */
public class DesireTest {

    @Test
    public void testIndirectDesireEnvelope1() {
        int t1 = 20;
        testDesireEnvelope(
                t1,
                "(a:b ==>+" + t1 + " c:d). :|:",
                "a:b! :|:");
        //n.input("c:d! :|:");
    }
    @Test
    public void testIndirectDesireEnvelope2() {
        int t1 = 20;
        testDesireEnvelope(
                t1,
                "(a:b &&+0 c:d). :|:",
                "a:b! :|:");
        //n.input("c:d! :|:");
    }
    public void testDesireEnvelope(int t1, String... inputs) {
        Global.DEBUG = true;

        NAR n = new Default();

        //n.log();

        for (String s : inputs)
            n.input(s);

        for (int i = 0; i < t1 * 2; i++) {
            n.step();
            long now = n.time();
            //System.out.println(n.concept("a:b").goals().topTemporal(now,now));
            print(n, "c:d", now);
            print(n, "a:b", now);
        }

        //n.concept("c:d").print();
    }

    private float print(NAR n, String concept, long now) {
        Task tt = n.concept(concept).goals().top(now);
        System.out.println(now + ": " + "\t" + tt);
            /*if (tt!=null)
                System.out.println(tt.getExplanation());*/
        if (tt!=null)
            return tt.expectation();
        return -1f;
    }
}
