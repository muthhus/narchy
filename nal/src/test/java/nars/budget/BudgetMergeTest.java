package nars.budget;

import nars.$;
import nars.budget.merge.BudgetMerge;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by me on 2/2/16.
 */
public class BudgetMergeTest {

    final static float tol = 0.01f;

    static final RawBudget a = new RawBudget(1, 0.7f, 0.3f);
    static final RawBudget b = new RawBudget(0.5f, 0.4f, 0.2f);
    static final RawBudget c = new RawBudget(0.25f, 0.2f, 0.1f);

    @Test
    public void testPlusDQBlend() {
        BudgetMerge m = BudgetMerge.plusBlend;

        testMerge(m, z(), a, 1f, 1, 0.7f, 0.3f, 0 /*overflow*/);  //adding to zero equals the incoming
        testMerge(z(), a, 0.5f, m, 0.5f, 0.7f, 0.3f); //scale of half should affect priority only
        testMerge(a, z(), 1f, m, a.pri(), a.dur(), a.qua());  //merging with zero should hae no effect

        testMerge(b, b, 0, m, b.pri(), b.dur(), b.qua()); //scale of zero should have no effect

        testMerge(b, c, 1, m, (c.pri() + b.pri()), 0.33f, 0.16f); //test correct affect of components
        testMerge(b, c, 0.5f, m, (c.pri() / 2f + b.pri()), 0.36f, 0.18f); //lesser affect (dur and qua closer to original values)
    }
    @Test
    public void testPlusDQBlend2() {
        BudgetMerge m = BudgetMerge.plusBlend;

        testMerge(m, a, c, 1f, 1, 0.6f, 0.26f, //priority saturation behavior
                0.25f); //with overflow

        testMerge(m, a, c, 0.5f, 1, 0.64f, 0.27f, //priority saturation behavior, lesser affect (dur and qua closer to original values)
                0f);  //no overflow

        testMerge(a, a, 1f, m, a.pri(), a.dur(), a.qua()); //no change since saturated with the same incoming values

    }

//    @Test
//    public void testPlusDQBlendOld() {
//        BudgetMerge m = BudgetMerge.plusBlend;
//
//        testMerge(m, z(), a, 1f, 1, 0.7f, 0.3f, 0 /*overflow*/);  //adding to zero equals the incoming
//        testMerge(m, z(), a, 0.5f, 0.5f, 0.7f, 0.3f); //scale of half should affect priority only
//        testMerge(m, a, z(), 1f, a.pri(), a.dur(), a.qua());  //merging with zero should hae no effect
//
//        testMerge(m, b, b, 0, b.pri(), b.dur(), b.qua()); //scale of zero should have no effect
//
//        testMerge(m, b, c, 1, (c.pri() + b.pri()), 0.33f, 0.16f); //test correct affect of components
//        testMerge(m, b, c, 0.5f, (c.pri()/2f + b.pri()), 0.33f, 0.16f); //lesser affect (dur and qua closer to original values)
//
//        testMerge(m, a, c, 1f, 1, 0.57f, 0.25f, //priority saturation behavior
//            0.25f); //with overflow
//
//        testMerge(m, a, c, 0.5f, 1, 0.57f, 0.24f, //priority saturation behavior, lesser affect (dur and qua closer to original values)
//            0f);  //no overflow
//
//        testMerge(m, a, a, 1f, a.pri(), a.dur(), a.qua()); //no change since saturated with the same incoming values
//
//    }

    @Test
    public void testAvg() {
        BudgetMerge AVG = BudgetMerge.avgBlend;

        testMerge(z(), a, 1.0f, AVG, a.pri(), a.dur(), a.qua());  //adding to zero equals the incoming
        testMerge(z(), a, 0.5f, AVG, a.pri()/2f, a.dur(), a.qua()); //scale of half should affect priority only
        testMerge(a, z(), 1.0f, AVG, a.pri(), a.dur(), a.qua());  //merging with zero should hae no effect

        testMerge(b, b, 0.0f, AVG, b.pri(), b.dur(), b.qua()); //scale of zero should have no effect with itself
        testMerge(b, b, 0.5f, AVG, b.pri(), b.dur(), b.qua()); //scale of anything also should have no effect with itself in avg
        testMerge(b, b, 1.0f, AVG, b.pri(), b.dur(), b.qua()); //scale of one also should have no effect with itself

        testMerge(b, c, 1, AVG, 0.375f, 0.33f, 0.16f); //test correct affect of components; values closer to b since it is dominant
        testMerge(b, c, 0.5f, AVG, 0.4375f, 0.36f, 0.18f); //lesser affect (dur and qua closer to original values)

        testMerge(a, c, 1f, AVG, 0.625f, 0.6f, 0.26f); //priority decrease

    }

    @NotNull
    private RawBudget z() {
        return new RawBudget(0,0,0);
    }

    @NotNull
    private static RawBudget testMerge(float scale, @NotNull BudgetMerge m,
                                        float exPri, float exDur, float exQua, //start value
                                        float inPri, float inDur, float inQua, //incoming merge
                                        float ouPri, float ouDur, float ouQua  //expected result
    )    {
        RawBudget x = new RawBudget(exPri, exDur, exQua);
        testMerge(m, x, inPri, inDur, inQua, scale, ouPri, ouDur, ouQua);
        return x;
    }

    private static Budget testMerge(@NotNull BudgetMerge m, Budget x, float inPri, float inDur, float inQua, float scale, float ouPri, float ouDur, float ouQua) {
        RawBudget y = new RawBudget(inPri, inDur, inQua);
        return testMerge(x, y, scale, m, ouPri, ouDur, ouQua);
    }
    private static Budget testMerge(Budget x, Budget y, float scale, @NotNull BudgetMerge m, float ouPri, float ouDur, float ouQua) {
        return testMerge(m, x, y, scale, ouPri, ouDur, ouQua, -1f);
    }
    private static Budget testMerge(@NotNull BudgetMerge m, Budget x, Budget y, float scale, float ouPri, float ouDur, float ouQua, float expectedOverflow) {
        x = x.clone();

        Budget x0 = x.clone();

        float overflow = m.merge(x, y, scale);

        System.out.println(x0 + " <-merge<- " + y + " x " + scale + "\t\texpect:" + $.b(ouPri,ouDur, ouQua) + " ?? actual:" + x);
        assertEquals(ouPri, x.pri(), tol);
        assertEquals(ouDur, x.dur(), tol);
        assertEquals(ouQua, x.qua(), tol);

        if (expectedOverflow > 0)
            assertEquals(expectedOverflow, overflow, 0.01f);

        return x;
    }


}