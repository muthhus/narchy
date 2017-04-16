package nars.budget;

import jcog.pri.Pri;
import jcog.pri.PriMerge;
import jcog.pri.Priority;
import nars.$;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by me on 2/2/16.
 */
public class PriMergeTest {

    final static float tol = 0.01f;

    static final Pri a = new Pri(1);
    static final Pri b = new Pri(0.5f);
    static final Pri c = new Pri(0.25f);

    @Test
    public void testPlusDQBlend() {
        PriMerge m = PriMerge.plusBlend;

        testMerge(z(), a, m, 1f, 1f, 0 /*overflow*/);  //adding to zero equals the incoming
        testMerge(z(), a, 0.5f, m, 0.5f); //scale of half should affect priority only
        testMerge(a, z(), 1f, m, a.pri());  //merging with zero should hae no effect

        testMerge(b, b, 0, m, b.pri()); //scale of zero should have no effect

        testMerge(b, c, 1, m,0.75f); //test correct affect of components
        testMerge(b, c, 0.5f, m, 0.625f); //lesser affect (dur and qua closer to original values)


        testMerge(a, c, m, 1f, 1,  //priority saturation behavior
                0.25f); //with overflow

        testMerge(a, c, m, 0.5f, 1,  //priority saturation behavior, lesser affect (dur and qua closer to original values)
                0f);  //no overflow

        testMerge(a, a, 1f, m, a.pri()); //no change since saturated with the same incoming values

    }

    @Test
    public void testAvg() {
        PriMerge AVG = PriMerge.avgBlend;

        //z,a - averaging with zero results in half of the incoming
        testMerge(z(), a, 1.0f, AVG, 0.5f * a.pri());
        //z,a(scale=0.5)
        testMerge(z(), a, 0.5f, AVG, 0.25f * a.pri());
        //a,z - should be identical to z,a being that AVG is commutive:
        testMerge(a, z(), 1.0f, AVG, 0.5f * a.pri());

        //average with itself should have no effect regardless of the applied scale factor
        testMerge(b, b, 0.0f, AVG, b.pri()); //scale of zero should have no effect with itself
        testMerge(b, b, 0.5f, AVG, b.pri()); //scale of anything also should have no effect with itself in avg
        testMerge(b, b, 1.0f, AVG, b.pri()); //scale of one also should have no effect with itself

        testMerge(b, c, 1, AVG, 0.375f); //test correct affect of components; values closer to b since it is dominant
        testMerge(c, b, 1, AVG, 0.375f); //test correct affect of components; values closer to b since it is dominant

        testMerge(b, c, 0.5f, AVG, 0.4375f); //lesser affect (dur and qua closer to original values)

        testMerge(a, c, 1f, AVG, 0.625f); //priority decrease but less than the previous test which involves a weaker existing quality

    }

    @NotNull
    private Pri z() {
        return new Pri(0);
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
    private static Priority testMerge(Priority x, Priority y, float scale, @NotNull PriMerge m, float ouPri) {
        return testMerge(x, y, m, scale, ouPri, -1f);
    }
    private static Priority testMerge(Priority x, Priority y, @NotNull PriMerge m, float scale, float ouPri, float expectedOverflow) {
        x = x.clone();

        Priority x0 = x.clone();

        float overflow = m.merge(x, y, scale);

        System.out.println(x0 + " <-merge<- " + y + " x " + scale + "\t\texpect:" + $.b(ouPri) + " ?? actual:" + x);
        assertEquals(ouPri, x.pri(), tol);

        if (expectedOverflow > 0)
            assertEquals(expectedOverflow, overflow, 0.01f);

        return x;
    }


}