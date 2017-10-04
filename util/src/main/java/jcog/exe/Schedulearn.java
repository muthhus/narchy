package jcog.exe;

import jcog.list.FasterList;
import no.birkett.kiwi.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * learning cost/benefit multi-iteration stochastic scheduler
 * with high-dynamic range of iteration demand
 */
public class Schedulearn {

    float OVER_DEMAND = 1.5f; //factor for additional iterations to request above the observed supply, ie. demand growth rate

    public static class Can {

        final static AtomicInteger ids = new AtomicInteger();

        final static int WINDOW = 8;

        final DescriptiveStatistics iterationTime = new DescriptiveStatistics(WINDOW);
        final DescriptiveStatistics supply = new DescriptiveStatistics(WINDOW);
        final DescriptiveStatistics value = new DescriptiveStatistics(WINDOW);

        /**
         * next iterations, to be solved
         */
        public final Variable iterations = new Variable(String.valueOf(ids.incrementAndGet()));

        /**
         * in seconds
         */
        public float iterationTimeMean() {
            double mean = iterationTime.getMean();
            if (mean!=mean) return 1.0f;
            return (float) mean;
        }

        /**
         * max iterations that can/should be requested
         */
        public int supply() {
            double mean = supply.getMax();
            if (mean != mean) return 1;
            return (int) Math.ceil(mean);
        }

        /**
         * relative value of an iteration; ie. past value estimate divided by the actual supplied unit count
         * between 0..1.0
         */
        public float value() {
            double mean = value.getMean();
            if (mean!=mean) mean = 0.5f;
            return (float) mean;
        }

        /**
         * totalTime in sec
         */
        public void update(int supplied, double totalValue, double totalTime) {
            supply.addValue(supplied);
            value.addValue(totalValue / supplied);
            iterationTime.addValue(totalTime / supplied);
        }

        /** called after the iterations has been determined */
        public void commit() {

        }
    }




    public void solve(List<Can> can, double timeslice) {
        if (can.isEmpty())
            return;

        Solver solver = new Solver();

        float totalValue = 0;
        for (int i = 0, canSize = can.size(); i < canSize; i++) {
            totalValue += can.get(i).value();
        }

        List<Term> times = new FasterList(can.size());

        for (int i = 0, canSize = can.size(); i < canSize; i++) {
            Can x = can.get(i);

            Variable xi = x.iterations;

            Term xt = C.multiply(xi, x.iterationTimeMean());
            times.add(xt);

            //fraction of component time is proportional to
            Constraint proportionalToValue = C.lessThanOrEqualTo(
                    C.divide(xt, timeslice),
                    x.value() / totalValue
            );
            proportionalToValue.setStrength(1);
            solver.add(proportionalToValue);

            //demand slightly more than supply limit
            int prevIter = x.supply();
            double maxIter = Math.max(1, Math.ceil((1+prevIter) * OVER_DEMAND));

            Constraint meetsSupply = C.lessThanOrEqualTo(xi, maxIter);
            meetsSupply.setStrength(2);
            solver.add(meetsSupply);

        }

        //sum to entire timeslice
        Constraint totalTimeConstraint = C.lessThanOrEqualTo(C.add(times), timeslice);
        totalTimeConstraint.setStrength(3);

        solver.add(totalTimeConstraint);

        solver.update();

//        for (int i = 0, canSize = can.size(); i < canSize; i++) {
//            System.out.println(can.get(i).iterations);
//        }
    }

    public static void main(String[] args) {
        Can a = new Can();
        Can b = new Can();
        Can c = new Can();

        a.update(1, 0.5f, 1f);
        b.update(20, 5f, 2f);
        c.update(20, 5f, 3f); //same as 'a' but bigger observed supply

        Schedulearn s = new Schedulearn();
        s.solve(List.of(a, b, c), 10f);

    }

}
