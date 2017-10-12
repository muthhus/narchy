package jcog.exe;

import jcog.list.FasterList;
import jcog.pri.Pri;
import no.birkett.kiwi.*;

import java.util.Arrays;
import java.util.List;

/**
 * learning cost/benefit multi-iteration stochastic scheduler
 * with high-dynamic range of iteration demand
 */
public class Schedulearn {

    float OVER_DEMAND = 1.1f; //factor for additional iterations to request above the observed supply, ie. demand growth rate
    float OVER_DEMAND_IMPL = 1.1f; //factor to hard multiply total iterations after solution.  this effectively boosts the demand even further, but beyond the solution's expectations


    /**
     * timeslice in seconds
     */
    public void solve(List<Can> can, double timeslice) {
        int canSize = can.size();
        if (canSize == 0)
            return;

        Solver solver = new Solver();

        float minValue = Float.POSITIVE_INFINITY,
                maxValue = Float.NEGATIVE_INFINITY,
                totalValue = 0;
        float[] v = new float[canSize];
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

        float base = 1f/canSize;
        for (int i = 0; i < canSize; i++) {
            v[i] = base + (v[i] - minValue) / range;
            totalValue += v[i];
        }


        List<Term> times = new FasterList(canSize);

        for (int i = 0; i < canSize; i++) {
            Can x = can.get(i);

            Variable xi = x.iterations;

            Term xt = C.multiply(xi, x.iterationTimeMean());
            times.add(xt);

            //fraction of component time is proportional to
            Constraint proportionalToValue =
                C.equals(
                //C.lessThanOrEqualTo(
                    v[i] / totalValue,
                    C.divide(xt, timeslice)

            );
            proportionalToValue.setStrength(1);
            solver.add(proportionalToValue);

            //demand slightly more than supply limit
            double prevIter = x.supply();
            double maxIter = Math.max(1, Math.ceil((1 + prevIter) * OVER_DEMAND));

            Constraint meetsSupply = C.lessThanOrEqualTo(xi, maxIter);
            meetsSupply.setStrength(0.5f);
            solver.add(meetsSupply);

        }

        //sum to entire timeslice
        Constraint totalTimeConstraint = C.lessThanOrEqualTo(C.add(times), timeslice);
        totalTimeConstraint.setStrength(2);

        solver.add(totalTimeConstraint);

        solver.update();

        can.forEach(c -> {
            c.iterations.value(c.iterations.value() * OVER_DEMAND_IMPL);
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

        //System.out.println(strNS(tEst) + " est / " + strNS(tMax) + " max");

        return tEst;
    }

}
