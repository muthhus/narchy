package jcog.math;

import jcog.pri.Pri;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DiagonalMatrix;

import java.util.Arrays;
import java.util.Collection;



public class RecyclingPolynomialFitter extends AbstractCurveFitter {

    private final LeastSquaresProblem problem;
    private final double[] x;
    private final double[] y;
    private final double[] weights;
//    private LeastSquaresOptimizer optimizer;
    private boolean changed = true;
    private double[] solution;

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

    /** tolerance in X and Y for replacement policy */
    double tolX = Pri.EPSILON, tolY = Pri.EPSILON;

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
        clear();

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

    public void clear() {
        Arrays.fill(param, 0);
        Arrays.fill(x, Float.NaN);
        Arrays.fill(y, Float.NaN);
        Arrays.fill(weights, 1f);
    }

    public RecyclingPolynomialFitter tolerate(double tolX, double tolY) {
        this.tolX = tolX;
        this.tolY = tolY;
        return this;
    }

    public void learn(double xi, double yi) {
        double[] xx = this.x;
        double[] yy = this.y;

        if (xx[0]!=xx[0] /* NaN */) {
            //special case: empty; fill entirely with the first value
            Arrays.fill(xx, xi);
            Arrays.fill(yy, yi);
            return;
        }

        int farX = -1;
        double farDX = Float.NEGATIVE_INFINITY;
        for (int i = 0, x1Length = xx.length; i < x1Length; i++) {
            double x = xx[i];
            double dx = Math.abs(x - xi);
            if (dx <= tolX) {
                double y = yy[i];
                if (Math.abs(y - yi) <= tolY) {
                    //nothing new to be learned, but merge anyway
                    merge(i, xi, yi);
                    return;
                }
            }
            if (dx >= farDX) {
                farX = i;
                farDX = dx;
            }
        }

        replace(farX, xi, yi);

        //solution = null; //trigger re-solve
    }

    public void merge(int i, double xi, double yi) {
        this.x[i] = (this.x[i] + xi)/2;
        this.y[i] = (this.y[i] + yi)/2;
    }
    public void replace(int i, double xi, double yi) {
        this.x[i] = xi;
        this.y[i] = yi;
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
     * {@inheritDoc}
     */
    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> observations) {
        return problem;
    }

}
