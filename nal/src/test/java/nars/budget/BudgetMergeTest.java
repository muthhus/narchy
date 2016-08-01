package nars.budget;

import nars.budget.merge.BudgetMerge;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by me on 2/2/16.
 */
public class BudgetMergeTest {

    final static float tol = 0.01f;

    static final UnitBudget a = new UnitBudget(1, 0.7f, 0.3f);
    static final UnitBudget b = new UnitBudget(0.5f, 0.4f, 0.2f);
    static final UnitBudget c = new UnitBudget(0.25f, 0.2f, 0.1f);

    @Test
    public void testPlusDQBlend() {
        BudgetMerge m = BudgetMerge.plusBlend;

        testMerge(m, z(), a, 1f, 1, 0.7f, 0.3f, 0 /*overflow*/);  //adding to zero equals the incoming
        testMerge(m, z(), a, 0.5f, 0.5f, 0.7f, 0.3f); //scale of half should affect priority only
        testMerge(m, a, z(), 1f, a.pri(), a.dur(), a.qua());  //merging with zero should hae no effect

        testMerge(m, b, b, 0, b.pri(), b.dur(), b.qua()); //scale of zero should have no effect

        testMerge(m, b, c, 1, (c.pri() + b.pri()), 0.33f, 0.16f); //test correct affect of components
        testMerge(m, b, c, 0.5f, (c.pri()/2f + b.pri()), 0.33f, 0.16f); //lesser affect (dur and qua closer to original values)

        testMerge(m, a, c, 1f, 1, 0.56f, 0.24f, //priority saturation behavior
                0.25f); //with overflow

        testMerge(m, a, c, 0.5f, 1, 0.57f, 0.24f, //priority saturation behavior, lesser affect (dur and qua closer to original values)
                0f);  //no overflow

        testMerge(m, a, a, 1f, a.pri(), a.dur(), a.qua()); //no change since saturated with the same incoming values

    }

    @Test
    public void testPlusDQBlendOld() {
        BudgetMerge m = BudgetMerge.plusBlend;

        testMerge(m, z(), a, 1f, 1, 0.7f, 0.3f, 0 /*overflow*/);  //adding to zero equals the incoming
        testMerge(m, z(), a, 0.5f, 0.5f, 0.7f, 0.3f); //scale of half should affect priority only
        testMerge(m, a, z(), 1f, a.pri(), a.dur(), a.qua());  //merging with zero should hae no effect

        testMerge(m, b, b, 0, b.pri(), b.dur(), b.qua()); //scale of zero should have no effect

        testMerge(m, b, c, 1, (c.pri() + b.pri()), 0.33f, 0.16f); //test correct affect of components
        testMerge(m, b, c, 0.5f, (c.pri()/2f + b.pri()), 0.33f, 0.16f); //lesser affect (dur and qua closer to original values)

        testMerge(m, a, c, 1f, 1, 0.57f, 0.25f, //priority saturation behavior
            0.25f); //with overflow

        testMerge(m, a, c, 0.5f, 1, 0.57f, 0.24f, //priority saturation behavior, lesser affect (dur and qua closer to original values)
            0f);  //no overflow

        testMerge(m, a, a, 1f, a.pri(), a.dur(), a.qua()); //no change since saturated with the same incoming values

    }

    @Test
    public void testAvg() {
        BudgetMerge m = BudgetMerge.avgBlend;

        testMerge(m, z(), a, 1f, 0.5f, 0.7f, 0.3f);  //adding to zero equals the incoming
        testMerge(m, z(), a, 0.5f, 0.25f, 0.7f, 0.3f); //scale of half should affect priority only
        testMerge(m, a, z(), 1f, a.pri()/2f, a.dur(), a.qua());  //merging with zero should hae no effect

        testMerge(m, b, b, 0, b.pri(), b.dur(), b.qua()); //scale of zero should have no effect

        testMerge(m, b, c, 1, 0.375f, 0.33f, 0.16f); //test correct affect of components; values closer to b since it is dominant
        testMerge(m, b, c, 0.5f, 0.4375f, 0.33f, 0.16f); //lesser affect (dur and qua closer to original values)

        testMerge(m, a, c, 1f, 0.625f, 0.56f, 0.247f); //priority decrease

    }

    @NotNull
    private UnitBudget z() {
        return new UnitBudget(0,0,0);
    }

    @NotNull
    private static UnitBudget testMerge(float scale, @NotNull BudgetMerge m,
                                        float exPri, float exDur, float exQua, //start value
                                        float inPri, float inDur, float inQua, //incoming merge
                                        float ouPri, float ouDur, float ouQua  //expected result
    )    {
        UnitBudget x = new UnitBudget(exPri, exDur, exQua);
        testMerge(m, x, inPri, inDur, inQua, scale, ouPri, ouDur, ouQua);
        return x;
    }

    private static Budget testMerge(@NotNull BudgetMerge m, Budget x, float inPri, float inDur, float inQua, float scale, float ouPri, float ouDur, float ouQua) {
        UnitBudget y = new UnitBudget(inPri, inDur, inQua);
        return testMerge(m, x, y, scale, ouPri, ouDur, ouQua);
    }
    private static Budget testMerge(@NotNull BudgetMerge m, Budget x, Budget y, float scale, float ouPri, float ouDur, float ouQua) {
        return testMerge(m, x, y, scale, ouPri, ouDur, ouQua, -1f);
    }
    private static Budget testMerge(@NotNull BudgetMerge m, Budget x, Budget y, float scale, float ouPri, float ouDur, float ouQua, float expectedOverflow) {
        x = x.clone();
        float overflow = m.merge(x, y, scale);
        assertEquals(ouPri, x.pri(), tol);
        assertEquals(ouDur, x.dur(), tol);
        assertEquals(ouQua, x.qua(), tol);

        if (expectedOverflow > 0)
            assertEquals(expectedOverflow, overflow, 0.01f);

        return x;
    }


}