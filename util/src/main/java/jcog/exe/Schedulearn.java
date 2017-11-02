package jcog.exe;

import com.google.common.base.Joiner;
import jcog.constraint.continuous.*;
import jcog.list.FasterList;
import jcog.pri.Pri;

import java.util.Arrays;
import java.util.List;

import static jcog.Texts.strNS;

/**
 * learning cost/benefit multi-iteration stochastic scheduler
 * with high-dynamic range of iteration demand
 */
public class Schedulearn {

    //float OVER_DEMAND = 1.1f; //factor for additional iterations to request above the observed supply, ie. demand growth rate
    float OVER_DEMAND_IMPL = 1.5f; //factor to hard multiply total iterations after solution.  this effectively boosts the demand even further, but beyond the solution's expectations

    final static double minIterationTime = 1.0E-9;

    /**
     * timeslice in seconds
     */
    public void solve(List<Can> can, double timeslice) {
        int canSize = can.size();
        if (canSize == 0)
            return;

        ContinuousConstraintSolver solver = new ContinuousConstraintSolver();

        float minValue = Float.POSITIVE_INFINITY,
                maxValue = Float.NEGATIVE_INFINITY,
                totalValue = 0;
        float[] v = new float[canSize]; //normalized value
        for (int i = 0; i < canSize; i++) {
            float cv = can.get(i).value();
            if (cv > maxValue) maxValue = cv;
            if (cv < minValue) minValue = cv;
            v[i] = cv;
        }

        //normalize
        float range = maxValue - minValue;
        if (jcog.Util.equals(range, 0, Pri.EPSILON)) {
            range = 1f;
            minValue = 0f;
            Arrays.fill(v, 0.5f);
        }

        float base = 1/(canSize*canSize); //HEURISTIC
        for (int i = 0; i < canSize; i++) {
            v[i] = base + (v[i] - minValue) / range;
            totalValue += v[i];
        }


        List<DoubleTerm> times = new FasterList(canSize);

        for (int i = 0; i < canSize; i++) {
            Can x = can.get(i);

            DoubleVar xi = x.iterations;

            DoubleTerm xt = C.multiply(xi, Math.max(minIterationTime, x.iterationTimeMean()));
            times.add(xt);

            //fraction of component time is proportional to
            ContinuousConstraint proportionalToValue =
                C.equals(
                //C.lessThanOrEqualTo(
                    v[i]/totalValue,
                    C.divide(xt, timeslice)
            );
            proportionalToValue.setStrength(Strength.STRONG);
            solver.add(proportionalToValue);

            //demand slightly more than supply limit
            double supply = x.supply();
            double overSupply = Math.max(1, Math.ceil((1 + supply) * OVER_DEMAND_IMPL));

            ContinuousConstraint meetsSupply = C.lessThanOrEqualTo(xi, overSupply);
            meetsSupply.setStrength(Strength.STRONG);
            solver.add(meetsSupply);

        }

        //sum to entire timeslice
        ContinuousConstraint totalTimeConstraint =
                //C.lessThanOrEqualTo
                C.equals
                        (C.add(times), timeslice);
        totalTimeConstraint.setStrength(Strength.REQUIRED);

        solver.add(totalTimeConstraint);

        solver.update();

        can.forEach(c -> {
            c.iterations.value(Math.max(1, c.iterations.value() * OVER_DEMAND_IMPL));
            c.commit();
        });

    }


    public static double estimatedTimeTotal(Iterable<Can> cc) {
        double tEst = 0;

        //double tMax = 0; //max if full supply were demanded

        for (Can c : cc) {
            float im = c.iterationTimeMean();
            tEst += c.iterations() * im;
            //tMax += c.supply() * im;
        }

        System.out.println(strNS(tEst*1.0E9) + " est cycle time\t" + Joiner.on(",").join(cc));

        return tEst;
    }

}
