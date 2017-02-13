package nars.budget;

import nars.$;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by me on 2/2/16.
 */
public class BudgetMergeTest {

    final static float tol = 0.01f;

    static final RawBudget a = new RawBudget(1, 0.5f);
    static final RawBudget b = new RawBudget(0.5f, 0.25f);
    static final RawBudget c = new RawBudget(0.25f, 0.1f);

    @Test
    public void testPlusDQBlend() {
        BudgetMerge m = BudgetMerge.plusBlend;

        testMerge(z(), a, m, 1f, 1f, 0.5f, 0 /*overflow*/);  //adding to zero equals the incoming
        testMerge(z(), a, 0.5f, m, 0.5f, 0.5f); //scale of half should affect priority only
        testMerge(a, z(), 1f, m, a.pri(), a.qua());  //merging with zero should hae no effect

        testMerge(b, b, 0, m, b.pri(), b.qua()); //scale of zero should have no effect

        testMerge(b, c, 1, m,0.75f, 0.25f); //test correct affect of components
        testMerge(b, c, 0.5f, m, 0.625f, 0.25f); //lesser affect (dur and qua closer to original values)


        testMerge(a, c, m, 1f, 1, 0.5f, //priority saturation behavior
                0.25f); //with overflow

        testMerge(a, c, m, 0.5f, 1, 0.5f, //priority saturation behavior, lesser affect (dur and qua closer to original values)
                0f);  //no overflow

        testMerge(a, a, 1f, m, a.pri(), a.qua()); //no change since saturated with the same incoming values

    }

    @Test
    public void testAvg() {
        BudgetMerge AVG = BudgetMerge.avgBlend;

        //z,a - averaging with zero results in half of the incoming
        testMerge(z(), a, 1.0f, AVG, 0.5f * a.pri(), a.qua());
        //z,a(scale=0.5)
        testMerge(z(), a, 0.5f, AVG, 0.25f * a.pri(), a.qua());
        //a,z - should be identical to z,a being that AVG is commutive:
        testMerge(a, z(), 1.0f, AVG, 0.5f * a.pri(), a.qua());

        //average with itself should have no effect regardless of the applied scale factor
        testMerge(b, b, 0.0f, AVG, b.pri(), b.qua()); //scale of zero should have no effect with itself
        testMerge(b, b, 0.5f, AVG, b.pri(), b.qua()); //scale of anything also should have no effect with itself in avg
        testMerge(b, b, 1.0f, AVG, b.pri(), b.qua()); //scale of one also should have no effect with itself

        testMerge(b, c, 1, AVG, 0.375f, 0.175f); //test correct affect of components; values closer to b since it is dominant
        testMerge(c, b, 1, AVG, 0.375f, 0.175f); //test correct affect of components; values closer to b since it is dominant

        testMerge(b, c, 0.5f, AVG, 0.4375f, 0.21f); //lesser affect (dur and qua closer to original values)

        testMerge(a, c, 1f, AVG, 0.625f, 0.3f); //priority decrease but less than the previous test which involves a weaker existing quality

    }

    @NotNull
    private RawBudget z() {
        return new RawBudget(0, 0.5f);
    }

//    @NotNull
//    private static RawBudget testMerge(float scale, @NotNull BudgetMerge m,
//                                       float exPri, float exDur, float exQua, //start value
//                                       float inPri, float inQua, //incoming merge
//                                       float ouPri, float ouQua  //expected result
//    )    {
//        RawBudget x = new RawBudget(exPri, exQua);
//        testMerge(m, x, inPri, inQua, scale, ouPri, ouQua);
//        return x;
//    }
//
//    private static Budget testMerge(@NotNull BudgetMerge m, Budget x, float inPri, float inQua, float scale, float ouPri, float ouQua) {
//        RawBudget y = new RawBudget(inPri, inQua);
//        return testMerge(x, y, scale, m, ouPri, ouQua);
//    }
    private static Budget testMerge(Budget x, Budget y, float scale, @NotNull BudgetMerge m, float ouPri, float ouQua) {
        return testMerge(x, y, m, scale, ouPri, ouQua, -1f);
    }
    private static Budget testMerge(Budget x, Budget y, @NotNull BudgetMerge m, float scale, float ouPri, float ouQua, float expectedOverflow) {
        x = x.clone();

        Budget x0 = x.clone();

        float overflow = m.merge(x, y, scale);

        System.out.println(x0 + " <-merge<- " + y + " x " + scale + "\t\texpect:" + $.b(ouPri, ouQua) + " ?? actual:" + x);
        assertEquals(ouPri, x.pri(), tol);
        assertEquals(ouQua, x.qua(), tol);

        if (expectedOverflow > 0)
            assertEquals(expectedOverflow, overflow, 0.01f);

        return x;
    }


}