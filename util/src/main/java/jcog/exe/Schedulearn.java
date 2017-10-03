package jcog.exe;

import jcog.list.FasterList;
import no.birkett.kiwi.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;

/**
 * learning cost/benefit multi-iteration stochastic scheduler
 * with high-dynamic range of iteration demand
 */
public interface Schedulearn {

    static interface Can {
        /**
         * in seconds
         */
        float iterationTimeMean();

        /**
         * max iterations that can/should be requested
         */
        int supply();

        /**
         * relative value of an iteration; ie. past value estimate divided by the actual supplied unit count
         */
        float value();

        /**
         * total iterations requested for the next cycle;
         * eventually reports asynchronously the # of cycles supplied and the relative value to calculate the next cycle
         */
        void demand(int iterations);

    }

    static class MyCan implements Can {

        final int WINDOW = 4;

        final DescriptiveStatistics iterationTime = new DescriptiveStatistics(WINDOW);
        final DescriptiveStatistics supply = new DescriptiveStatistics(WINDOW);
        final DescriptiveStatistics value = new DescriptiveStatistics(WINDOW);

        @Override
        public float iterationTimeMean() {
            return (float) iterationTime.getMean();
        }

        @Override
        public int supply() {
            return (int) Math.ceil(supply.getMean());
        }

        @Override
        public float value() {
            return (float) value.getMean();
        }

        @Override
        public void demand(int iterations) {

        }

        /**
         * totalTime in sec
         */
        public void update(int supplied, float totalValue, float totalTime) {
            supply.addValue(supplied);
            value.addValue(totalValue / supplied);
            iterationTime.addValue(totalTime / supplied);
        }

    }

    /**
     * uses linear constraint solver
     */
    public static class ConstraintSchedulearn implements Schedulearn {

        public ConstraintSchedulearn() {

        }

        public void solve(List<Can> can, float timeslice) {
            Solver solver = new Solver();

            List<Variable> iterations = new FasterList(can.size());

            float OVER_DEMAND = 1.5f; //factor for additional iterations to request above the observed supply, ie. demand growth rate

            float totalValue = 0;
            for (int i = 0, canSize = can.size(); i < canSize; i++) {
                totalValue += can.get(i).value();
            }

            List<Term> times = new FasterList(can.size());

            for (int i = 0, canSize = can.size(); i < canSize; i++) {
                Can x = can.get(i);

                Variable xi = new Variable(String.valueOf(i));
                iterations.add(xi);


                Term xt = C.multiply(xi, x.iterationTimeMean());
                times.add(xt);

                //fraction of component time is proportional to
                solver.add(C.lessThanOrEqualTo(
                        C.divide(xt, timeslice),
                        x.value() / totalValue
                ));

                //demand slightly more than supply limit
                solver.add(C.lessThanOrEqualTo(xi, Math.ceil(x.supply() * OVER_DEMAND)));

            }

            //sum to entire timeslice
            Constraint totalTimeConstraint = C.lessThanOrEqualTo(timeslice, C.add(times));
            totalTimeConstraint.setStrength(2);

            solver.add(totalTimeConstraint);

            solver.update();

            for (int i = 0, canSize = can.size(); i < canSize; i++) {
                System.out.println(iterations.get(i));
            }
        }
    }

    public static void main(String[] args) {
        MyCan a = new MyCan();
        MyCan b = new MyCan();
        MyCan c = new MyCan();

        a.update(1, 0.5f, 1f);
        b.update(2, 0.25f, 2f);
        c.update(10, 5f, 10f); //same as 'a' but bigger observed supply

        ConstraintSchedulearn s = new ConstraintSchedulearn();
        s.solve(List.of(a, b, c), 5f);

    }

}
