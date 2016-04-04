package nars.budget;

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
    public void testPlusPQBlend() {
        BudgetMerge m = BudgetMerge.plusDQBlend;

        testMerge(1f, m, z(),  a,   1, 0.7f, 0.3f, 0 /*overflow*/);  //adding to zero equals the incoming
        testMerge(0.5f, m, z(),  a,   0.5f, 0.7f, 0.3f); //scale of half should affect priority only
        testMerge(1f, m, a,  z(),   a.pri(), a.dur(), a.qua());  //merging with zero should hae no effect

        testMerge(0, m, b,  b,   b.pri(), b.dur(), b.qua()); //scale of zero should have no effect

        testMerge(1, m, b,  c,   (c.pri() + b.pri()), 0.33f, 0.16f); //test correct affect of components
        testMerge(0.5f, m, b,  c,   (c.pri()/2f + b.pri()), 0.36f, 0.18f); //lesser affect (dur and qua closer to original values)

        testMerge(1f, m, a,  c,   1, 0.6f, 0.26f, //priority saturation behavior
            0.25f); //with overflow

        testMerge(0.5f, m, a,  c,   1, 0.65f, 0.277f, //priority saturation behavior, lesser affect (dur and qua closer to original values)
            0f);  //no overflow

        testMerge(1f, m, a,  a,   a.pri(), a.dur(), a.qua()); //no change since saturated with the same incoming values

    }

    @Test
    public void testAvg() {
        BudgetMerge m = BudgetMerge.avgDQBlend;

        testMerge(1f, m, z(),  a,   1, 0.7f, 0.3f);  //adding to zero equals the incoming
        testMerge(0.5f, m, z(),  a,   0.5f, 0.7f, 0.3f); //scale of half should affect priority only
        testMerge(1f, m, a,  z(),   a.pri(), a.dur(), a.qua());  //merging with zero should hae no effect

        testMerge(0, m, b,  b,   b.pri(), b.dur(), b.qua()); //scale of zero should have no effect

        testMerge(1, m, b,  c,   0.41f, 0.33f, 0.16f); //test correct affect of components; values closer to b since it is dominant
        testMerge(0.5f, m, b,  c,  0.425f, 0.36f, 0.18f); //lesser affect (dur and qua closer to original values)

        testMerge(1f, m, a,  c,   0.85f, 0.6f, 0.26f); //priority decrease

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
        testMerge(scale, m, x, inPri, inDur, inQua, ouPri, ouDur, ouQua);
        return x;
    }

    @NotNull
    private static Budget testMerge(float scale, @NotNull BudgetMerge m, Budget x, float inPri, float inDur, float inQua, float ouPri, float ouDur, float ouQua) {
        UnitBudget y = new UnitBudget(inPri, inDur, inQua);
        return testMerge(scale, m, x, y, ouPri, ouDur, ouQua);
    }
    @NotNull
    private static Budget testMerge(float scale, @NotNull BudgetMerge m, Budget x, Budget y, float ouPri, float ouDur, float ouQua) {
        return testMerge(scale, m, x, y, ouPri, ouDur, ouQua, -1f);
    }
    @NotNull
    private static Budget testMerge(float scale, @NotNull BudgetMerge m, Budget x, Budget y, float ouPri, float ouDur, float ouQua, float expectedOverflow) {
        x = x.clone();
        float overflow = m.merge(x, y, scale);
        assertEquals(ouPri, x.pri(), tol);
        assertEquals(ouDur, x.dur(), tol);
        assertEquals(ouQua, x.qua(), tol);

        if (expectedOverflow > 0)
            assertEquals(overflow, expectedOverflow, 0.01f);

        return x;
    }
}