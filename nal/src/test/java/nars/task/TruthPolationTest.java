package nars.task;

import nars.$;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 5/8/16.
 */
public class TruthPolationTest {
    @NotNull TruthPolation polation = new TruthPolation(8 /* cap */);

    @Test
    public void testRevisionEquivalence()  {
        MutableTask a = t(1f, 0.5f, 0); //c~=0.67
        a.evidence(new long[] { 0 } );
        MutableTask b = t(1f, 0.5f, 0);
        b.evidence(new long[] { 1 } ); //cause different hash

        //assertEquals(a.truth(), polation.truth(0, a, a)); //same item

        //System.out.println( polation.truth(0, a, b) );
        assertEquals(Revision.revision(a, b), polation.truth(0, a, b));

        polation.print();
    }

    @Test
    public void testRevisionEquivalence2() {
        Task a = t(1f, 0.5f, -4);
        Task b = t(0f, 0.5f, 4);

        Truth pt = polation.truth(0, a, b);
        @Nullable Truth rt = Revision.revision(a, b);

        assertTrue(rt.toString() + " vs. " + pt.toString(), rt.equals(pt, 0.03f));
    }


    @Test
    public void testRevisionEquivalence2Instant() {
        Task a = t(1f, 0.5f, 0);
        Task b = t(0f, 0.5f, 0);
        assertEquals( Revision.revision(a, b), polation.truth(0, a, b) );
    }

    @Test
    public void testRevisionEquivalence3() {
        Task a = t(1f, 0.5f, 3);
        Task b = t(0f, 0.5f, 6);
        for (int i = 0; i < 10; i++) {
            System.out.println(i + " " + polation.truth(i, a, b));
        }

        System.out.println();

        Truth ab2 = polation.truth(2, a, b);
        assertTrue( ab2.conf() >= 0.5f );

        Truth abneg1 = polation.truth(4, a, b);
        assertTrue( abneg1.freq() > 0.6f );
        assertTrue( abneg1.conf() >= 0.5f );

        Truth ab5 = polation.truth(5, a, b);
        assertTrue( ab5.freq() < 0.35f );
        assertTrue( ab5.conf() >= 0.5f );
    }

    @Test
    public void testRevisionEquivalence4() {
        Task a = t(0f, 0.1f, 3);
        Task b = t(0f, 0.1f, 4);
        Task c = t(1f, 0.1f, 5);
        Task d = t(0f, 0.1f, 6);
        Task e = t(0f, 0.1f, 7);

        for (int i = 0; i < 15; i++) {
            System.out.println(i + " " + polation.truth(i, a, b, c, d, e));
        }

    }

    public static MutableTask t(float freq, float conf, long occ) {
        return new MutableTask("a:b", '.', $.t(freq, conf)).time(0, occ);
    }

//    public static void _main(String[] args) {
//        TruthPolation p = new TruthPolation(4,
//                0f);
//        //0.1f);
//
//        List<Task> l = Global.newArrayList();
//
//        //NAR n = new Default();
//        l.add( new MutableTask("a:b", '.', new DefaultTruth(0f, 0.5f) ).occurr(0).setCreationTime(0) );
//        l.add( new MutableTask("a:b", '.', new DefaultTruth(1f, 0.5f) ).occurr(5).setCreationTime(0) );
//        l.add( new MutableTask("a:b", '.', new DefaultTruth(0f, 0.75f) ).occurr(10).setCreationTime(0) );
//        print(p, l, -5, 15);
//
//
//    }

    public static void print(@NotNull TruthPolation p, @NotNull List<Task> l, int start, int end) {
        //interpolation (revision) and extrapolation (projection)
        System.out.println("INPUT");
        for (Task t : l) {
            System.out.println(t);
        }

        System.out.println();

        System.out.println("TRUTHPOLATION");
        for (long d = start; d < end; d++) {
            Truth a1 = p.truth(d, l, null, Float.NaN, Float.NaN);
            System.out.println(d + ": " + a1);
        }
    }

}