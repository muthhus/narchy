package nars.task;

import nars.$;
import nars.Global;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by me on 5/8/16.
 */
public class TruthPolationTest {

    @Test
    public void testRevisionEquivalence() throws Exception {
        TruthPolation polation = new TruthPolation(2, 0);

        Task a = t(1f, 0.5f, 0);
        assertEquals( Revision.revision(a, a), polation.truth(0, a, a) );

    }

    public static Task t(float freq, float conf, long occ) {
        return new MutableTask("a:b", '.', $.t(freq, conf)).time(0, occ);
    }

    public static void main(String[] args) {
        TruthPolation p = new TruthPolation(4,
                0f);
        //0.1f);

        List<Task> l = Global.newArrayList();

        //NAR n = new Default();
        l.add( new MutableTask("a:b", '.', new DefaultTruth(0f, 0.5f) ).occurr(0).setCreationTime(0) );
        l.add( new MutableTask("a:b", '.', new DefaultTruth(1f, 0.5f) ).occurr(5).setCreationTime(0) );
        l.add( new MutableTask("a:b", '.', new DefaultTruth(0f, 0.75f) ).occurr(10).setCreationTime(0) );
        print(p, l, -5, 15);


    }

    public static void print(TruthPolation p, List<Task> l, int start, int end) {
        //interpolation (revision) and extrapolation (projection)
        System.out.println("INPUT");
        for (Task t : l) {
            System.out.println(t);
        }

        System.out.println();

        System.out.println("TRUTHPOLATION");
        for (long d = start; d < end; d++) {
            Truth a1 = p.truth(d, l, null);
            System.out.println(d + ": " + a1);
        }
    }

}