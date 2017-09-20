package jcog.math;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.util.Precision;

import java.util.Arrays;
import java.util.Collection;



public class RecyclingPolynomialFitter extends AbstractCurveFitter {

    private final LeastSquaresProblem problem;
    private final double[] x;
    private final double[] y;
    private final double[] weights;
    int nextObs = 0;
//    private LeastSquaresOptimizer optimizer;
    private boolean changed = true;
    private double[] solution;

    public RecyclingPolynomialFitter(int degree, int window, int iter) {
        this.param = new double[degree + 1];
        this.iter = iter;

//        this.optimizer =
//                new LevenbergMarquardtOptimizer(iter, 1e-10, 1e-10, 1e-10, Precision.SAFE_MIN);
//                //new GaussNewtonOptimizer();

        // Prepare least-squares problem.
        x = new double[window];
        y = new double[window];
        weights = new double[window];
        Arrays.fill(weights, 1f);

        final int len = x.length;
        final double[] values = new double[len];
        final MultivariateVectorFunction modelFunction = (p) -> {
            for (int i = 0; i < len; i++)
                values[i] = FUNCTION.value(x[i], p);
            return values;
        };

        final double[][] jacobian = new double[len][];
        MultivariateMatrixFunction jacobianFunc = (p) -> {
            for (int i = 0; i < len; i++)
                jacobian[i] = FUNCTION.gradient(x[i], p);
            return jacobian;
        };

        // Return a new least squares problem set up to fit a polynomial curve to the
        // observed points.
        problem = new LeastSquaresBuilder().
                maxEvaluations(Integer.MAX_VALUE).
                maxIterations(iter).
                //lazyEvaluation(true).
                start(param).
                target(y).
                //checker(new EvaluationRmsChecker(0.01f)).
                weight(new DiagonalMatrix(weights)).
                model(modelFunction, jacobianFunc).
                build();
    }

    public void learn(double xi, double yi) {
        int nextObs = this.nextObs;
        x[nextObs] = xi;
        y[nextObs] = yi;
        this.nextObs = (nextObs +1)%x.length;
        //solution = null; //trigger re-solve
    }

    public double guess(double  xi) {
        if (solution ==null) {
            param[0] = xi;

            try {
                solution = //cast to get the raw array zero-copy
                        ((ArrayRealVector) getOptimizer().optimize(problem).getPoint()).getDataRef();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        return (float) FUNCTION.value(xi, solution);
    }

//
//    @Override
//    public LeastSquaresOptimizer getOptimizer() {
//        return optimizer;
//    }

    /**
     * Parametric function to be fitted.
     */
    private static final PolynomialFunction.Parametric FUNCTION = new PolynomialFunction.Parametric();
    /**
     * Initial guess.
     */
    private final double[] param;
    /**
     * Maximum number of iterations of the optimization algorithm.
     */
    private final int iter;


    /**
     * {@inheritDoc}
     */
    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> observations) {
        return problem;
    }

}
