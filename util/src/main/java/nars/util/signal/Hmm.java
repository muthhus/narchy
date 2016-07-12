/*****************************************************************************
 * Copyright (c) 1998-2003, Bluecraft Software. All Rights Reserved.
 * This software is the proprietary information of Bluecraft Software
 * and it is supplied subject to license terms.
 * See the HmmSDK Home Page (http://hmmsdk.sourceforge.net/) for more details.
 *****************************************************************************/
// $Id: Hmm.java,v 1.8 2003/05/31 06:00:58 drwye Exp $

package nars.util.signal;


import nars.util.data.random.XorShift128PlusRandom;

import java.util.Arrays;
import java.util.Random;


/**
 * Hmm implements IHmm which encapsulates a discrete Hidden Markov Model (HMM)
 * and some related algorithms.
 * This class adds a sequence of observed symbols to the class Model
 * (so that HMM can be used as a trainer or recognizer),
 * and it implements some of the most important algorithms of HMM.
 * (Please refer to Rabiner 1989 for more information.)
 * <p>
 * NumStates: Number of distinct states in the embedded Markov model.
 * NumSymbols: Number of different observable symbols.
 * LenObSeq: Length of the observation sequence.
 * <p>
 * http://hmmsdk.cvs.sourceforge.net/viewvc/hmmsdk/hmmlib/src/java/com/bluecraft/hmm/Hmm.java?revision=1.8&content-type=text%2Fplain
 * https://github.com/aismail/KinectHacking/tree/master/hmmsdk/hmmlib/src/java/com/bluecraft/hmm
 *
 * @author <A href="mailto:hyoon@bluecraft.com">Hyoungsoo Yoon</A>
 * @author <A href="mailto:hyoon@bluecraft.com">Hyoungsoo Yoon</A>
 * @version $Revision: 1.8 $
 */
public class Hmm {

    // Maximum number of iteration.
    private static final int MAX_ITERATION = 100;

    // Error below which the value is assumed to have converged to a solution.
    private static final double MIN_ERROR = 0.001;


    //    /**
//     * @serial  Underlying Markov model and its parameters (A, B, and pi)
//     */
//    private Model lambda;
    // Maximum number of states allowed in a model.
    private static final int MAX_NUM_STATES = 1000;
    // Maximum number of symbols allowed in a model.
    private static final int MAX_NUM_SYMBOLS = 1000;
    // Smallest value of non-zero probability allowed in a model.
    private static double MIN_PROB = 0.00001;  // Note that MIN_PROB should be smaller than 1.0/num_states!!!

    // Random number generator.
    private static final Random rand = new XorShift128PlusRandom(1);
    /**
     * @serial Forward variables  (Rabiner eq. 18)
     */
    private final double[][] alpha;
    /**
     * @serial Backward variables  (Rabiner eq. 23)
     */
    private final double[][] beta;
    /**
     * @serial Internal state probabilities (Rabiner eq. 26)
     */
    private final double[][] gamma;
    /**
     * @serial Internal pair-state probabilities (Rabiner eq. 36)
     */
    private final double[][][] ksi;
    /**
     * @serial Best score variables (Rabiner eq. 30)
     */
    private final double[][] delta;
    /**
     * @serial Backtracking variables (Rabiner eq. 33b)
     */
    private final int[][] psi;
    /**
     * @serial Most likely path (Rabiner eq. 35)
     */
    private final int[] qstar;
    /**
     * @serial Scale factor (Rabiner eq. 91)
     */
    private final double[] scaleFactor;

    @Deprecated private final int maxSeqLen;


    /**
     * @serial Number of states in the Markov model
     */
    private int num_states = 0;
    /**
     * @serial Number of observable symbols
     */
    private int num_symbols = 0;
    /**
     * @serial Non-negative integer. Lower-left bound of the non-zero band of the transition matrix (diagonal: 0)
     */
    private int ll_bound = 0;
    /**
     * @serial Non-negative integer. Upper-right bound of the non-zero band of the transition matrix (diagonal: 0)
     */
    private int ur_bound = 0;
    /**
     * @serial State transition probability (num_states x num_states)
     */
    private final double[][] a;
    /**
     * @serial Symbol generation probability (num_states x num_symbols)
     */
    private final double[][] b;
    /**
     * @serial Initial state probability (1 x num_states)
     */
    private final double[] pi;
    // a[][] has a band of non-zero elements.
    // For each row, columns below l_limit and above r_limit are zero.
    // Column index below which all elements are zero by definition.
    private final transient int[] l_limit;
    // Column index above which all elements are zero by definition.
    private final transient int[] r_limit;

    /**
     * Construct HMM with a minimum size (length of observation sequence: 1).
     * The elements of the matrices are initialized by random values.
     */
    public Hmm()

    {
        this(true);
    }

    /**
     * Construct HMM with a minimum size (length of observation sequence: 1).
     *
     * @param bRandomize If true, the elements of the matrices are initialized by random values
     */
    public Hmm(boolean bRandomize)

    {
        this(1, bRandomize);
    }

    /**
     * Construct HMM by specifying its length of observation sequence.
     * (number of states: 1, number of symbols: 1).
     * The elements of the matrices are initialized by random values.
     *
     * @param lobseq Length of symbols observation sequence
     */
    public Hmm(int lobseq)

    {
        this(lobseq, true);
    }

    /**
     * Construct HMM by specifying its length of observation sequence.
     * (number of states: 1, number of symbols: 1).
     *
     * @param lobseq     Length of symbols observation sequence
     * @param bRandomize If true, the elements of the matrices are initialized by random values
     */
    public Hmm(int lobseq, boolean bRandomize)

    {
        this(lobseq, 1, 1, 0, 0, bRandomize);
    }


    /**
     * Construct Model by specifying dimensions.
     *
     * @param nstates Number of states of a Markov model
     * @param nsymbols Number of symbols generated by the model
     * @param llb Lower-left bound of the non-zero band of the transition matrix (diagonal: 0)
     * @param urb Upper-right bound of the non-zero band of the transition matrix (diagonal: 0)
     * @param bRandomize If true, the elements of the matrices are initialized by random values
     */


    /**
     * Construct HMM by specifying its length, number of states, and number of symbols.
     * The elements of the matrices are initialized by random values.
     *
     * @param lobseq   Length of symbols observation sequence
     * @param nstates  Number of states
     * @param nsymbols Number of symbols
     */
    public Hmm(int lobseq, int nstates, int nsymbols)

    {
        this(lobseq, nstates, nsymbols, true);
    }

    /**
     * Construct HMM by specifying its length, number of states, and number of symbols.
     *
     * @param lobseq     Length of symbols observation sequence
     * @param nstates    Number of states
     * @param nsymbols   Number of symbols
     * @param bRandomize If true, the elements of the matrices are initialized by random values
     */
    public Hmm(int lobseq, int nstates, int nsymbols, boolean bRandomize)

    {
        this(lobseq, nstates, nsymbols, nstates - 1, nstates - 1, bRandomize);
    }

    /**
     * Construct HMM by specifying its length, number of states, and number of symbols.
     * Lower bound and upper bound  along the diagonal direction are also specified, outside which all probability elements are identically zero.
     * The elements of the matrices are initialized by random values.
     *
     * @param lobseq   Length of symbols observation sequence
     * @param nstates  Number of states
     * @param nsymbols Number of symbols
     * @param llb      Lower bound below which all probability elements are identically zero.
     * @param urb      Upper bound beyond which all probability elements are identically zero.
     */
    public Hmm(int lobseq, int nstates, int nsymbols, int llb, int urb)

    {
        this(lobseq, nstates, nsymbols, llb, urb, true);
    }

    /**
     * Construct HMM by specifying its length, number of states, and number of symbols.
     * Lower bound and upper bound along the diagonal direction are also specified, outside which all probability elements are identically zero.
     *
     * @param maxSeqLen     Length of symbols observation sequence
     * @param nstates    Number of states
     * @param nsymbols   Number of symbols
     * @param llb        Lower bound below which all probability elements are identically zero.
     * @param urb        Upper bound beyond which all probability elements are identically zero.
     * @param bRandomize If true, the elements of the matrices are initialized by random values
     */
    public Hmm(int maxSeqLen, int nstates, int nsymbols, int llb, int urb, boolean bRandomize)

    {
        num_states = (nstates < MAX_NUM_STATES) ? nstates : MAX_NUM_STATES;
        num_symbols = (nsymbols < MAX_NUM_SYMBOLS) ? nsymbols : MAX_NUM_SYMBOLS;

        a = new double[num_states][num_states];
        b = new double[num_states][num_symbols];
        pi = new double[num_states];
        l_limit = new int[num_states];
        r_limit = new int[num_states];

        ll_bound = (llb < num_states && llb >= 0) ? llb : (num_states - 1);
        ur_bound = (urb < num_states && urb >= 0) ? urb : (num_states - 1);
        setLimits();
        setProbability(bRandomize);


        this.maxSeqLen = maxSeqLen;

        alpha = new double[this.maxSeqLen][num_states];
        beta = new double[this.maxSeqLen][num_states];
        gamma = new double[this.maxSeqLen][num_states];
        ksi = new double[this.maxSeqLen - 1][num_states][num_states];
        delta = new double[this.maxSeqLen][num_states];
        psi = new int[this.maxSeqLen][num_states];
        qstar = new int[this.maxSeqLen];
        scaleFactor = new double[this.maxSeqLen];

        initializeModel(bRandomize);
    }

    /**
     * Returns the maximum number of states allowed in a model.
     *
     * @return Maximum number of states allowed in a model
     */
    public static int getMaxNumStates() {
        return MAX_NUM_STATES;
    }

    /**
     * Returns the maximum number of symbols allowed in a model.
     *
     * @return Maximum number of symbols allowed in a model
     */
    public static int getMaxNumSymbols() {
        return MAX_NUM_SYMBOLS;
    }

    /**
     * Returns the smallest value of non-zero probability.
     * Any probability values (allowed to be non-zero) in a model should be bigger than this value.
     *
     * @return Smallest value of non-zero probability allowed in a model
     */
    public static double getMinProb() {
        return MIN_PROB;
    }

    /**
     * Sets the smallest value of non-zero probability.
     * This value should be bigger than 1.0/getMaxNumStates() and 1.0/getMaxNumSymbols().
     * TODO: Once a model is constructed, MIN_PROB cannot be changed.....???
     *
     * @param p New value to be used as the smallest value of probability allowed in a model
     * @return Smallest value of non-zero probability allowed in a model
     */
    private static double resetMinProb(double p) {
        double epsilon = (1.0 / MAX_NUM_STATES < 1.0 / MAX_NUM_SYMBOLS) ? 1.0 / MAX_NUM_STATES : 1.0 / MAX_NUM_SYMBOLS;
        if (p >= 0.0 && p <= epsilon) {
            MIN_PROB = p;
        }
        return MIN_PROB;
    }

    /**
     * This function is provided for testing purposes.
     * Direct modification of this class is not recommended
     * unless there is a bug. (In which case please notify me.)
     * Please use inheritance or aggregation (composition).
     */
    public static void main(String[] args) {
        Hmm h = new Hmm(3, 4, 5, 0, 1, false);

        for (int i = 0; i < h.num_states; i++) {
            for (int j = 0; j < h.num_states; j++) {
                System.out.print(h.getA(i, j) + "\t");
            }
            System.out.println();
        }
        System.out.println();
        for (int j = 0; j < h.num_states; j++) {
            System.out.print(h.getPi(j) + "\t");
        }
        System.out.println();
        System.out.println();
        for (int i = 0; i < h.num_states; i++) {
            for (int k = 0; k < h.num_symbols; k++) {
                System.out.print(h.getB(i, k) + "\t");
            }
            System.out.println();
        }
        System.out.println();

        for (int z = 0; z < 10; z++) {
            int[] seq = h.generateSeq(3);
            for (int t = 0; t < seq.length; t++) {
                System.out.print(seq[t] + "\t");
            }
            System.out.println();
        }
        System.out.println();

            /*
            h.setObSeq();
            for(int t=0;t<h.getLenObSeq();t++) {
                System.out.print(h.getObSeq(t) + "\t");
            }
            System.out.println();
            System.out.println();
            */

        //double p = h.optimizeLambda();
        //double p = h.getObSeqProbability();
        //System.out.println(p);

    }

    /**
     * Sets the left and right column indices for each row beyond which all elements are assumed to be zero by definition.
     */
    private void setLimits() {
        for (int i = 0; i < num_states; i++) {
            l_limit[i] = (i - ll_bound >= 0) ? (i - ll_bound) : 0;
            r_limit[i] = (i + ur_bound <= num_states - 1) ? (i + ur_bound) : (num_states - 1);
        }
    }

    /**
     * Sets the left and right band width.
     *
     * @param llb Lower-left bound (Non-negative integer)
     * @param urb Upper-right bound (Non-negative integer)
     */
    private void setLeftRight(int llb, int urb) {
        ll_bound = (llb < num_states && llb >= 0) ? llb : (num_states - 1);
        ur_bound = (urb < num_states && urb >= 0) ? urb : (num_states - 1);
        setLimits();
        resetA();
    }

    /**
     * Sets all elements of A to zero.
     */
    private void clearA() {
        for (int i = 0; i < num_states; i++) {
            for (int j = 0; j < num_states; j++) {
                a[i][j] = 0.0;
            }
        }
    }

    /**
     * Sets all elements of B to zero.
     */
    private void clearB() {
        for (int i = 0; i < num_states; i++) {
            for (int k = 0; k < num_symbols; k++) {
                b[i][k] = 0.0;
            }
        }
    }

    /**
     * Sets all elements of Pi to zero.
     */
    private void clearPi() {
        for (int j = 0; j < num_states; j++) {
            pi[j] = 0.0;
        }
    }

    /**
     * Resets A with random probability values.
     */
    private void resetA() {
        resetA(true);
    }

    /**
     * Resets A with new probability values.
     *
     * @param bRandomize If true, use random values.
     */
    private void resetA(boolean bRandomize) {
        clearA();
        setA(bRandomize);
    }

    /**
     * Resets B with random probability values.
     */
    private void resetB() {
        resetB(true);
    }

    /**
     * Resets B with new probability values.
     *
     * @param bRandomize If true, use random values.
     */
    private void resetB(boolean bRandomize) {
        clearB();
        setB(bRandomize);
    }

    /**
     * Resets B with random probability values.
     */
    private void resetPi() {
        resetPi(true);
    }

    /**
     * Resets Pi with new probability values.
     *
     * @param bRandomize If true, use random values.
     */
    private void resetPi(boolean bRandomize) {
        clearPi();
        setPi(bRandomize);
    }

    /**
     * Normalizes the i-th row of the transition probability matrix, A.
     * If any of the probability values becomes smaller than MIN_PROB,
     * it is reset to MIN_PROB.
     *
     * @param i Row index
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    private void normalizeRowA(int i) {
        // Check first if the argument is in the valid range.
        if (i < 0 || i >= num_states) {
            throw new RuntimeException("A: Row " + i + " is out of vaild range!");
        }

        synchronized (a[i]) {
            double sum = 0.0;
            for (int j = l_limit[i]; j <= r_limit[i]; j++) {
                // The probability is a non-negative number.
                if (a[i][j] < 0.0) {
                    throw new RuntimeException("A: Row " + i + " contains negative probabilites!");
                }
                sum += a[i][j];
            }
            // This checking is unnecessary.
            if (sum <= 0.0) {
                throw new RuntimeException("A: Row " + i + " unnormalizable!");
            }

            // Normalize the row i.
            for (int j = l_limit[i]; j <= r_limit[i]; j++) {
                a[i][j] /= sum;
            }

            // Make sure that all elements are at least as big as MIN_PROB.
            double deficit = 0.0;
            for (int j = l_limit[i]; j <= r_limit[i]; j++) {
                if (a[i][j] < MIN_PROB) {
                    deficit += MIN_PROB - a[i][j];
                    a[i][j] = MIN_PROB;
                }
            }

            // Re-normalize the row if needed.
            if (deficit > 0.0) {
                //double sum2 = 0.0;
                //for(int j=l_limit[i];j<=r_limit[i];j++) {
                //    sum2 += a[i][j];
                //}
                double sum2 = 1.0 + deficit;
                sum2 -= (r_limit[i] - l_limit[i] + 1) * MIN_PROB;
                double factor = (1.0 - (r_limit[i] - l_limit[i] + 1) * MIN_PROB) / sum2;
                for (int j = l_limit[i]; j <= r_limit[i]; j++) {
                    a[i][j] *= factor;
                    a[i][j] += (1.0 - factor) * MIN_PROB;
                }
            }
        }

    }

    /**
     * Normalizes the transition probability matrix, A.
     * If any of the probability values becomes smaller than MIN_PROB,
     * it is reset to MIN_PROB.
     *
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    private void normalizeRowA() {
        for (int i = 0; i < num_states; i++) {
            normalizeRowA(i);
        }
    }

    /**
     * Normalizes the i-th row of the symbol-generation probability matrix, B.
     * If any of the probability values becomes smaller than MIN_PROB,
     * it is reset to MIN_PROB.
     *
     * @param i Row index
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    private void normalizeRowB(int i) {
        // Check first if the argument is in the valid range.
        if (i < 0 || i >= num_states) {
            throw new RuntimeException("B: Row " + i + " is out of vaild range!");
        }

        synchronized (b[i]) {
            double sum = 0.0;
            for (int k = 0; k < num_symbols; k++) {
                // The probability is a non-negative number.
                if (b[i][k] < 0.0) {
                    throw new RuntimeException("B: Row " + i + " contains negative probabilites!");
                }
                sum += b[i][k];
            }
            // This checking is unnecessary.
            if (sum <= 0.0) {
                throw new RuntimeException("B: Row " + i + " unnormalizable!");
            }

            // Normalize the row i.
            for (int k = 0; k < num_symbols; k++) {
                b[i][k] /= sum;
            }

            // Make sure that all elements are at least as big as MIN_PROB.
            double deficit = 0.0;
            for (int k = 0; k < num_symbols; k++) {
                if (b[i][k] < MIN_PROB) {
                    deficit += MIN_PROB - b[i][k];
                    b[i][k] = MIN_PROB;
                }
            }

            // Re-normalize the row if needed.
            if (deficit > 0.0) {
                //double sum2 = 0.0;
                //for(int k=0;k<num_symbols;k++) {
                //    sum2 += b[i][k];
                //}
                double sum2 = 1.0 + deficit;
                sum2 -= num_symbols * MIN_PROB;
                double factor = (1.0 - num_symbols * MIN_PROB) / sum;
                for (int k = 0; k < num_symbols; k++) {
                    b[i][k] *= factor;
                    b[i][k] += (1.0 - factor) * MIN_PROB;
                }
            }
        }

    }

    /**
     * Normalizes the symbol-generation probability matrix, B.
     * If any of the probability values becomes smaller than MIN_PROB,
     * it is reset to MIN_PROB.
     *
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    private void normalizeRowB() {
        for (int i = 0; i < num_states; i++) {
            normalizeRowB(i);
        }
    }

    /**
     * Normalizes the initial state probability vector, Pi.
     * If any of the probability values becomes smaller than MIN_PROB,
     * it is reset to MIN_PROB.
     *
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    private void normalizeRowPi() {
        synchronized (pi) {
            double sum = 0.0;
            for (int j = 0; j < num_states; j++) {
                // The probability is a non-negative number.
                if (pi[j] < 0.0) {
                    throw new RuntimeException("Pi: State-vector contains negative probabilites!");
                }
                sum += pi[j];
            }
            // This checking is unnecessary.
            if (sum <= 0.0) {
                throw new RuntimeException("B: State-vector unnormalizable!");
            }

            // Normalize the state-vector.
            for (int j = 0; j < num_states; j++) {
                pi[j] /= sum;
            }

            // Make sure that all elements are at least as big as MIN_PROB.
            double deficit = 0.0;
            for (int j = 0; j < num_states; j++) {
                if (pi[j] < MIN_PROB) {
                    deficit += MIN_PROB - pi[j];
                    pi[j] = MIN_PROB;
                }
            }

            // Re-normalize the row if needed.
            if (deficit > 0.0) {
                //double sum2 = 0.0;
                //for(int j=0;j<num_states;j++) {
                //    sum2 += pi[j];
                //}
                double sum2 = 1.0 + deficit;
                sum2 -= num_states * MIN_PROB;
                double factor = (1.0 - num_states * MIN_PROB) / sum;
                for (int j = 0; j < num_states; j++) {
                    pi[j] *= factor;
                    pi[j] += (1.0 - factor) * MIN_PROB;
                }
            }
        }

    }

    /**
     * Returns the column index below which all elements of the state-transition matrix A are zero by definition.
     *
     * @param i Row number of the state-transition matrix A
     * @return Left limit (column number) outside of which the transition probability is zero
     */
    public int getLLimit(int i) {
        return l_limit[i];
    }

    /**
     * Returns the column index above which all elements of the state-transition matrix A are zero by definition.
     *
     * @param i Row number of the state-transition matrix A
     * @return Right limit (column number) outside of which the transition probability is zero
     */
    public int getRLimit(int i) {
        return r_limit[i];
    }

    /**
     * Initializes the state-transition matrix A with random values.
     */
    public void setA() {
        setA(true);
    }

    /**
     * Initializes the state-transition matrix A.
     * If the given argument, bRandomize, is true, then it initializes A with random values.
     * Otherwise, A will be initialized to the one close to the identity matrix
     * with small non-zero transition probabilities between different states (when allowed by the model).
     *
     * @param bRandomize If true, initialize A with random values
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    public void setA(boolean bRandomize) {
        if (bRandomize == true) {
            for (int i = 0; i < num_states; i++) {
                for (int j = l_limit[i]; j <= r_limit[i]; j++) {
                    a[i][j] = rand.nextDouble() + MIN_PROB;
                }
            }
            normalizeRowA();
        } else {
            for (int i = 0; i < num_states; i++) {
                for (int j = l_limit[i]; j <= r_limit[i]; j++) {
                    a[i][j] = MIN_PROB;
                }
                // TODO: Make sure that a >= 0.
                a[i][i] = 1.0 - (r_limit[i] - l_limit[i]) * MIN_PROB;
            }
        }
    }

    /**
     * Sets the ij-th element of the state-transition matrix A with the given value, p.
     * If p is smaller than MIN_PROB, it is set to MIN_PROB.
     *
     * @param i Row number of the state-transition matrix A
     * @param j Column number of state-transition matrix A
     * @param p Probability value
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     */
    private void setA(int i, int j, double p) {
        if (p > 1.0 || p < 0.0) {
            throw new RuntimeException();
        }
        if (i < 0 || i >= num_states) {
            throw new RuntimeException();
        }
        if (j < l_limit[i] || j > r_limit[i]) {
            if (j < 0 || j >= num_states) {
                throw new RuntimeException();
            } else {
                // TODO: Is this necessary???
                if (p > 0.0) {
                    throw new RuntimeException();
                } else {
                    a[i][j] = 0.0;
                    return;
                }
            }
        }

        // If p is smaller than MIN_PROB, then set it to MIN_PROB.
        a[i][j] = (p > MIN_PROB) ? p : MIN_PROB;
    }

    /**
     * Sets the i-th row of the state-transition matrix A with the given vector, p.
     * p is assumed to be properly normalized.
     *
     * @param i Row number of the state-transition matrix A
     * @param p Row vector of probability values
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     */
    public void setA(int i, double[] p) {
        setA(i, p, false);
    }

    /**
     * Sets the i-th row of the state-transition matrix A with the given vector, p.
     *
     * @param i          Row number of the state-transition matrix A
     * @param p          Row vector of probability values
     * @param bNormalize If true, renormalize the row in case p is not normalized
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    public void setA(int i, double[] p, boolean bNormalize) {
        synchronized (a[i]) {
            //for(int j=0;j<=num_states;j++) {
            for (int j = l_limit[i]; j <= r_limit[i]; j++) {
                setA(i, j, p[j]);
            }
        }
        if (bNormalize == true) {
            normalizeRowA(i);
        }
    }

    /**
     * Sets the state-transition matrix A with the given matrix, p.
     *
     * @param p          Matrix of probability values
     * @param bNormalize If true, renormalize each row in case p is not normalized
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    public void setA(double[][] p, boolean bNormalize) {
        for (int i = 0; i < num_states; i++) {
            setA(i, p[i], bNormalize);
        }
    }

    /**
     * Initializes the symbol-generation matrix B with random values.
     *
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     */
    public void setB() {
        setB(true);
    }

    /**
     * Initializes the symbol-generation matrix B.
     * If the given argument, bRandomize, is true, then it initializes B with random values.
     * Otherwise, B will be initialized to a constant matrix.
     *
     * @param bRandomize If true, initialize B with random values
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    public void setB(boolean bRandomize) {
        if (bRandomize == true) {
            for (int i = 0; i < num_states; i++) {
                for (int k = 0; k < num_symbols; k++) {
                    b[i][k] = rand.nextDouble() + MIN_PROB;
                }
            }
            normalizeRowB();
        } else {
            for (int i = 0; i < num_states; i++) {
                for (int k = 0; k < num_symbols; k++) {
                    b[i][k] = 1.0 / num_symbols;
                }
            }
        }
    }

    /**
     * Sets the ik-th element of the symbo-generation matrix B with the given value, p.
     * If p is smaller than MIN_PROB, it is set to MIN_PROB.
     *
     * @param i Row number of the symbol-generation matrix B
     * @param k Column number of symbol-generation matrix B
     * @param p Probability value
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     */
    private void setB(int i, int k, double p) {
        if (p > 1.0 || p < 0.0) {
            throw new RuntimeException();
        }
        if (i < 0 || i >= num_states) {
            throw new RuntimeException();
        }
        if (k < 0 || k >= num_symbols) {
            throw new RuntimeException();
        }

        // If p is smaller than MIN_PROB, then set it to MIN_PROB.
        b[i][k] = (p > MIN_PROB) ? p : MIN_PROB;
    }

    /**
     * Sets the i-th row of the symbol-generation matrix B with the given vector, p.
     * p is assumed to be properly normalized.
     *
     * @param i Row number of the symbol-generation matrix B
     * @param p Row vector of probability values
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     */
    public void setB(int i, double[] p) {
        setB(i, p, false);
    }

    /**
     * Sets the i-th row of the symbol-generation matrix B with the given vector, p.
     *
     * @param i          Row number of the symbol-generation matrix B
     * @param p          Row vector of probability values
     * @param bNormalize If true, renormalize the row in case p is not normalized
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    public void setB(int i, double[] p, boolean bNormalize) {
        synchronized (b[i]) {
            for (int k = 0; k < num_symbols; k++) {
                setB(i, k, p[k]);
            }
        }
        if (bNormalize == true) {
            normalizeRowB(i);
        }
    }

    /**
     * Sets the state-transition matrix A with the given matrix, p.
     *
     * @param p          Matrix of probability values
     * @param bNormalize If true, renormalize each row in case p is not normalized
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    public void setB(double[][] p, boolean bNormalize) {
        for (int i = 0; i < num_states; i++) {
            setB(i, p[i], bNormalize);
        }
    }

    /**
     * Initialize the initial-state row vector Pi with random values.
     *
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     */
    public void setPi() {
        setPi(true);
    }

    /**
     * Initialize the initial-state row vector Pi.
     * If the given argument, bRandomize, is true, then it initializes Pi with random values.
     * Otherwise, Pi will be initialized to a constant matrix.
     *
     * @param bRandomize If true, initialize Pi with random values
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    public void setPi(boolean bRandomize) {
        if (bRandomize == true) {
            for (int j = 0; j < num_states; j++) {
                pi[j] = rand.nextDouble() + num_states * MIN_PROB;
            }
            normalizeRowPi();
        } else {
            for (int j = 0; j < num_states; j++) {
                pi[j] = 1.0 / num_states;
            }
        }
    }

    /**
     * Sets the j-th element of the state-vector Pi with the given value, p.
     * If p is smaller than MIN_PROB, it is set to MIN_PROB.
     *
     * @param j Column number of initial-state row vector Pi
     * @param p Probability value
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     */
    private void setPi(int j, double p) {
        if (p > 1.0 || p < 0.0) {
            throw new RuntimeException();
        }
        if (j < 0 || j >= num_states) {
            throw new RuntimeException();
        }

        // If p is smaller than MIN_PROB, then set it to MIN_PROB.
        pi[j] = (p > MIN_PROB) ? p : MIN_PROB;
    }

    /**
     * Sets the state-vector Pi with the given vector, p.
     *
     * @param p          Row vector of probability values
     * @param bNormalize If true, renormalize Pi in case p is not normalized
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    public void setPi(double[] p, boolean bNormalize) {
        synchronized (pi) {
            for (int j = 0; j < num_states; j++) {
                setPi(j, p[j]);
            }
        }
        if (bNormalize == true) {
            normalizeRowPi();
        }
    }

    /**
     * Returns the ij-th element of A.
     *
     * @param i Row number of the state-transition matrix A
     * @param j Column number of state-transition matrix A
     * @return Probability value of A[i][j]
     */
    public double getA(int i, int j) {
        return a[i][j];
    }

    /**
     * Returns the i-th row of A.
     *
     * @param i Row number of the state-transition matrix A
     * @return Row vector of probability values of A[i][]
     */
    public double[] getA(int i) {
        return a[i];
    }

    /**
     * Returns A.
     *
     * @return State-transition matrix A
     */
    public double[][] getA() {
        return a;
    }

    /**
     * Sets the state-transition matrix A with the given matrix, p.
     * p is assumed to be properly normalized.
     *
     * @param p Matrix of probability values
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     */
    public void setA(double[][] p) {
        setA(p, false);
    }

    /**
     * Returns ik-th element of B.
     *
     * @param i Row number of the symbol-generation matrix B
     * @param k Column number of symbol-generation matrix B
     * @return Probability value of B[i][k]
     */
    public double getB(int i, int k) {
        return b[i][k];
    }


//    /**
//     * This function is provided for testing purposes.
//     * Direct modification of this class is not recommended
//     * unless there is a bug. (In which case please notify me.)
//     * Please use inheritance or aggregation (composition).
//     */
//    public static void main(String[] args)
//    {
//        try {
//            Model node = new Model(4, 5, 0, 1, false);
//
//            node.setProbability(true);
//
//            for(int i=0;i<node.num_states;i++) {
//                for(int j=0;j<node.num_states;j++) {
//                    System.out.print(node.getA(i,j) + "\t");
//                }
//                System.out.println();
//            }
//            System.out.println();
//            for(int j=0;j<node.num_states;j++) {
//                System.out.print(node.getPi(j) + "\t");
//            }
//            System.out.println();
//            System.out.println();
//            /*
//            for(int i=0;i<node.num_states;i++) {
//                for(int k=0;k<node.num_symbols;k++) {
//                    System.out.print(node.getB(i,k) + "\t");
//                }
//                System.out.println();
//            }
//            */
//
//            try {
//                ObjectOutputStream out =
//                        new ObjectOutputStream(
//                                new FileOutputStream("model.hmm"));
//                out.writeObject(node);
//                out.close();
//            } catch(Exception exc) {
//                exc.printStackTrace();
//            }
//
//            try {
//                ObjectInputStream in =
//                        new ObjectInputStream(
//                                new FileInputStream("model.hmm"));
//                newNode = (Model) in.readObject();
//                in.close();
//            } catch(Exception exc) {
//                exc.printStackTrace();
//            }
//
//            for(int i=0;i<newNode.num_states;i++) {
//                for(int j=0;j<newNode.num_states;j++) {
//                    System.out.print(newNode.getA(i,j) + "\t");
//                }
//                System.out.println();
//            }
//            System.out.println();
//            for(int j=0;j<newNode.num_states;j++) {
//                System.out.print(newNode.getPi(j) + "\t");
//            }
//            System.out.println();
//            System.out.println();
//            /*
//            for(int i=0;i<newNode.num_states;i++) {
//                for(int k=0;k<newNode.num_symbols;k++) {
//                    System.out.print(newNode.getB(i,k) + "\t");
//                }
//                System.out.println();
//            }
//            */
//
//            newNode.saveModel("test.xml", true);
//
//            newNode.loadModel("test.xml", true);
//
//            //
//            //newNode = new Model("model.xml", true);
//
//            System.out.println(newNode.num_states);
//            System.out.println(newNode.num_symbols);
//            System.out.println(newNode.ll_bound);
//            System.out.println(newNode.ur_bound);
//            for(int i=0;i<newNode.num_states;i++) {
//                for(int jj=0;jj<newNode.num_states;jj++) {
//                    System.out.print(newNode.a[i][jj]);
//                    System.out.print("\t");
//                }
//                System.out.println();
//            }
//            for(int i=0;i<newNode.num_states;i++) {
//                for(int k=0;k<newNode.num_symbols;k++) {
//                    System.out.print(newNode.b[i][k]);
//                    System.out.print("\t");
//                }
//                System.out.println();
//            }
//            for(int jjj=0;jjj<newNode.num_states;jjj++) {
//                System.out.print(newNode.pi[jjj]);
//                System.out.print("\t");
//            }
//            System.out.println();
//        } catch(Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//

    /**
     * Returns i-th row of B.
     *
     * @param i Row number of the symbol-generation matrix B
     * @return Row vector of probability value of B[i][]
     */
    public double[] getB(int i) {
        return b[i];
    }

    /**
     * Returns B.
     *
     * @return Symbol-generation matrix B
     */
    public double[][] getB() {
        return b;
    }

    /**
     * Sets the symbol-generation matrix B with the given matrix, p.
     * p is assumed to be properly normalized.
     *
     * @param p Matrix of probability values
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     */
    public void setB(double[][] p) {
        setB(p, false);
    }

    /**
     * Returns the j-th element of Pi.
     *
     * @param j Column number of inital-state row vector Pi
     * @return Probability value of Pi[j]
     */
    public double getPi(int j) {
        return pi[j];
    }

    /**
     * Returns Pi.
     *
     * @return Inital-state row vector Pi
     */
    public double[] getPi() {
        return pi;
    }

    /**
     * Sets the state-vector Pi with the given vector, p.
     * p is assumed to be properly normalized.
     *
     * @param p Row vector of probability values
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     */
    public void setPi(double[] p) {
        setPi(p, false);
    }

    /**
     * Returns the j-th element of Pi at time t.
     *
     * @param t Index of observation sequence
     * @param j Column number of state vector Pi[t][] at time t
     * @return Probability value of Pi[t][j]
     */
    public double getPi(int t, int j) {
        if (t == 0) {
            return pi[j];
        } else {
            double sum = 0.0;
            for (int i = 0; i < num_states; i++) {
                sum += getPi(t - 1, i) * a[i][j];
            }
            return sum;
        }
    }

    /**
     * Initialize probability matrices, A, B, and Pi, with random values.
     *
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    public void setProbability() {
        setProbability(true);
    }

//    /**
//     * Construct HMM by loading model parameters from an XML file.
//     *
//     * @param file Name of input file
//     */
//    public Hmm(String file)
//
//    {
//        this(file, 1, true);
//    }

//    /**
//     * Construct HMM by loading model parameters from an XML file.
//     *
//     * @param file Name of input file
//     * @param lobseq Length of observation sequence
//     */
//    public Hmm(String file, int lobseq)
//
//    {
//        this(file, lobseq, true);
//    }


//    /**
//     * Construct HMM by loading model parameters from a file.
//     *
//     * @param file Name of input file
//     * @param lobseq Length of observation sequence
//     * @param bXml If true, read input file in XML format
//     */
//    public Hmm(String file, int lobseq, boolean bXml)
//
//    {
//        lambda = new Model(file, bXml);
//        len_obseq = lobseq;
//
//        alpha = new double[len_obseq][num_states];
//        beta = new double[len_obseq][num_states];
//        gamma = new double[len_obseq][num_states];
//        ksi = new double[len_obseq-1][num_states][num_states];
//        delta = new double[len_obseq][num_states];
//        psi = new int[len_obseq][num_states];
//        qstar = new int[len_obseq];
//        scaleFactor = new double[len_obseq];
//        obSeq = new int[len_obseq];
//    }

    /**
     * Initialize probability matrices, A, B, and Pi.
     *
     * @param bRandomize If true, initialize probability matrices with random values
     * @throws com.bluecraft.hmm.RuntimeException
     * @throws com.bluecraft.hmm.
     * @throws com.bluecraft.hmm.RuntimeException
     */
    public void setProbability(boolean bRandomize) {
        setA(bRandomize);
        setB(bRandomize);
        setPi(bRandomize);
    }

    /**
     * Computes scale factors.
     */
    private void computeScaleFactor() {
        for (int t = 0; t < maxSeqLen; t++) {
            computeScaleFactor(t);
        }
    }

    /**
     * Computes the scale factor at time t.
     *
     * @param t Observation time
     */
    private void computeScaleFactor(int t) {
        scaleFactor[t] = 0.0;
        for (int i = 0; i < num_states; i++) {
            scaleFactor[t] += alpha[t][i];
        }
    }

    /**
     * Rescales Alpha with a new scale.
     */
    private void rescaleAlpha() {
        rescaleAlpha(true);
    }

    /**
     * Rescales Alpha.
     *
     * @param bNewScale If true, recompute the scale factors
     */
    private void rescaleAlpha(boolean bNewScale) {
        if (bNewScale) {
            computeScaleFactor();
        }
        for (int t = 0; t < maxSeqLen; t++) {
            rescaleAlpha(t);
        }
    }

    /**
     * Rescales Alpha at time t with a new scale factor.
     *
     * @param t Observation time
     */
    private void rescaleAlpha(int t) {
        rescaleAlpha(t, true);
    }

    /**
     * Rescales Alpha at time t.
     *
     * @param t         Observation time
     * @param bNewScale If true, recompute the scale factors
     */
    private void rescaleAlpha(int t, boolean bNewScale) {
        if (bNewScale) {
            computeScaleFactor(t);
        }
        for (int i = 0; i < num_states; i++) {
            alpha[t][i] /= scaleFactor[t];
        }
    }

    /**
     * Rescales Beta.
     * (It reuses the current scale factors.)
     */
    private void rescaleBeta() {
        rescaleBeta(false);
    }

    /**
     * Rescales Beta.
     *
     * @param bNewScale If true, recompute the scale factors
     */
    private void rescaleBeta(boolean bNewScale) {
        if (bNewScale) {
            computeScaleFactor();
        }
        for (int t = 0; t < maxSeqLen; t++) {
            rescaleBeta(t);
        }
    }

    /**
     * Rescales Beta at time t.
     * (It reuses the current scale factor.)
     *
     * @param t Observation time
     */
    private void rescaleBeta(int t) {
        rescaleBeta(t, false);
    }

    /**
     * Rescales Beta at time t.
     *
     * @param t         Observation time
     * @param bNewScale If true, recompute the scale factors
     */
    private void rescaleBeta(int t, boolean bNewScale) {
        if (bNewScale) {
            computeScaleFactor(t);
        }
        for (int i = 0; i < num_states; i++) {
            beta[t][i] /= scaleFactor[t];
        }
    }

    /**
     * Rescales Alphas and Betas.
     * (New scale factors will be used for Alphas, whereas the current scale factors will be reused for Betas.)
     */
    private void rescaleTrellis() {
        for (int t = 0; t < maxSeqLen; t++) {
            rescaleTrellis(t);
        }
    }

    /**
     * Rescalse Alpha and Beta at time t.
     * (A new scale factor will be used for Alpha, whereas the current scale factor will be reused for Beta.)
     *
     * @param t Observation time
     */
    private void rescaleTrellis(int t) {
        rescaleAlpha(t, true);
        rescaleBeta(t, false);
    }

    /**
     * Resets all Alphas to zero.
     */
    private void clearAlpha() {
        for (double[] g : alpha) {
            Arrays.fill(g, 0);
        }
    }

    /**
     * Resets all Betas to zero.
     */
    private void clearBeta() {
        for (double[] g : beta) {
            Arrays.fill(g, 0);
        }
    }

    /**
     * Resets all Gammas to zero.
     */
    private void clearGamma() {
        for (double[] g : gamma) {
            Arrays.fill(g, 0);
        }
    }

    /**
     * Resets all Alphas, Betas, and Gammas to zero.
     */
    private void clearTrellis() {
        clearAlpha();
        clearBeta();
        clearGamma();
    }

    /**
     * "Forward" part of the forward-backward algorithm.
     *
     * @return Probability of the given sequence given lambda
     */
    private double forwardAlpha(int[] obSeq) {
        int len_obseq = obSeq.length;
        clearAlpha();
        for (int j = 0; j < num_states; j++) {
            alpha[0][j] = getPi(j) * getB(j, obSeq[0]);
        }
        rescaleAlpha(0);

        for (int t = 1; t < len_obseq; t++) {
            for (int i = 0; i < num_states; i++) {
                double sum = 0.0;
                for (int j = getLLimit(i); j <= getRLimit(i); j++) {
                    sum += alpha[t - 1][i] * getA(i, j);
                }
                alpha[t][i] = sum * getB(i, obSeq[t]);
            }
            rescaleAlpha(t);
        }
        double sum = 0.0;
        for (int i = 0; i < num_states; i++) {
            sum += alpha[len_obseq - 1][i];
        }
        return sum;
    }

    /**
     * "Backward" part of the forward-backward algorithm.
     */
    private void backwardBeta(int obSeq[]) {
        int len_obseq = obSeq.length;
        clearBeta();
        for (int j = 0; j < num_states; j++) {
            beta[len_obseq - 1][j] = 1.0;
        }
        rescaleBeta(len_obseq - 1);

        for (int t = len_obseq - 2; t >= 0; t--) {
            for (int i = 0; i < num_states; i++) {
                double sum = 0.0;
                for (int j = getLLimit(i); j <= getRLimit(i); j++) {
                    sum += beta[t + 1][i] * getA(i, j) * getB(j, obSeq[t + 1]);
                }
                beta[t][i] = sum;
            }
            rescaleBeta(t);
        }
    }

    /**
     * Computes Gammas in the forward-backward algorithm.
     */
    private void computeGamma(int obSeq[]) {
        int len_obseq = obSeq.length;
        for (int t = 0; t < len_obseq; t++) {
            double sum = 0.0;
            for (int i = 0; i < num_states; i++) {
                gamma[t][i] = alpha[t][i] * beta[t][i];
                sum += gamma[t][i];
            }
            for (int i = 0; i < num_states; i++) {
                gamma[t][i] /= sum;
            }
        }
    }

    /**
     * Computes Deltas.
     *
     * @param t Observation time
     * @param j State index
     * @return Delta value at time t for the state j
     */
    private double computeDelta(int t, int j, int[] obSeq) {
        if (t == 0) {
            psi[t][j] = 0;
            return delta[t][j] = getPi(j) * getB(j, obSeq[t]);
        } else if (t == maxSeqLen) {
            double max = 0.0;
            double tmp;
            int indx = 0;
            for (int i = 0; i < num_states; i++) {
                tmp = computeDelta(t - 1, i, obSeq);
                if (tmp >= max) {
                    max = tmp;
                    indx = i;
                }
            }
            qstar[t - 1] = indx;
            for (int s = t - 2; s >= 0; s--) {
                qstar[s] = psi[s + 1][qstar[s + 1]];
            }
            return max;
        } else {
            double max = 0.0;
            for (int i = 0; i < num_states; i++) {
                double tmp = computeDelta(t - 1, i, obSeq) * getA(i, j) * getB(j, obSeq[t]);
                if (tmp >= max) {
                    max = tmp;
                }
            }
            double amax = 0.0;
            int indx = 0;
            for (int i = 0; i < num_states; i++) {
                double atmp = computeDelta(t - 1, i, obSeq) * getA(i, j);
                if (atmp >= amax) {
                    amax = atmp;
                    indx = i;
                }
            }
            psi[t][j] = indx;
            return delta[t][j] = max;
        }
    }

    /**
     * Viterbi Algorithm.
     *
     * @return Delta value at the end of observation
     */
    public double viterbiAlgorithm(int[] seq) {
        return computeDelta(maxSeqLen, 0, seq); // 0 arbitrary
    }

    /**
     * Computes Ksis in the forward-backward algorithm.
     */
    private void computeKsi(int[] seq) {
        for (int t = 0; t < maxSeqLen - 1; t++) {
            double sum = 0.0;
            for (int i = 0; i < num_states; i++) {
                for (int j = getLLimit(i); j <= getRLimit(i); j++) {
                    ksi[t][i][j] = alpha[t][i] * beta[t + 1][i] * getA(i, j) * getB(j, seq[t + 1]);
                    sum += ksi[t][i][j];
                }
            }
            for (int i = 0; i < num_states; i++) {
                for (int j = getLLimit(i); j <= getRLimit(i); j++) {
                    ksi[t][i][j] /= sum;
                }
            }
        }
    }

    /**
     * Forward-backward algorithm.
     *
     * @return Probability of the given sequence given lambda
     */
    private double forwardBackwardTrellis(int[] seq) {
        double ret = forwardAlpha(seq);
        backwardBeta(seq);
        computeGamma(seq);
        computeKsi(seq);
        return ret;
    }

    /**
     * Returns the sum of Gammas at time t for the given state i.
     *
     * @param t Observation time
     * @param i State index
     * @return Sum of Gammas
     */
    private double sumGamma(int t, int i) {
        double sum = 0.0;
        for (int s = 0; s < t; s++) {
            sum += gamma[s][i];
        }
        return sum;
    }

    /**
     * Returns the sum of Gammas at time t for the given state i and for the observation symbol k.
     *
     * @param t Observation time
     * @param i State index
     * @param k Observation symbol index
     * @return Sum of Gammas
     */
    private double sumGamma(int t, int i, int k, int[] obSeq) {
        double sum = 0.0;
        for (int s = 0; s < t; s++) {
            if (obSeq[s] == k) {
                sum += gamma[s][i];
            }
        }
        return sum;
    }

    /**
     * Returns the sum of Ksis at time t for the given states i and j.
     *
     * @param t Observation time
     * @param i State index
     * @param j State index
     * @return Sum of Ksis
     */
    private double sumKsi(int t, int i, int j) {
        double sum = 0.0;
        for (int s = 0; s < t; s++) {
            sum += ksi[s][i][j];
        }
        return sum;
    }

    /**
     * Re-estimates
     *
     * @param seq
     */
    private void reestimateLambda(int[] seq) {
        int len_obseq = seq.length;
        // (1) pi
        double[] rowPi = new double[num_states];
        for (int i = 0; i < num_states; i++) {
            rowPi[i] = gamma[0][i];
        }
        setPi(rowPi);

        // (2) a
        for (int i = 0; i < num_states; i++) {
            double[] rowA = new double[num_states];
            double a_denom = sumGamma(len_obseq - 1, i);
            for (int j = getLLimit(i); j <= getRLimit(i); j++) {
                rowA[j] = sumKsi(len_obseq - 1, i, j) / a_denom;
            }
            setA(i, rowA);
        }

        // (3) b
        for (int i = 0; i < num_states; i++) {
            double[] rowB = new double[num_symbols];
            double b_denom = sumGamma(len_obseq, i);
            for (int k = 0; k < num_symbols; k++) {
                rowB[k] = sumGamma(len_obseq, i, k, seq) / b_denom;
            }
            setB(i, rowB);
        }
    }




    /**
     * Initializes the model with random parameters.
     */
    public void initializeModel() {
        initializeModel(true);
    }

    /**
     * Initializes the model.
     *
     * @param bRandomize If true, initialize the parameters with random values
     */
    public void initializeModel(boolean bRandomize) {
        setProbability(bRandomize);
    }


    /**
     * Baum-Welch (EM) algorithm.
     * Train the HMM until MIN_ERROR is reached.
     * It also stops if the iteration reaches MAX_ITERATION.
     *
     * @param sequence
     * @return Likelihood of the model given the observation sequence after training
     */
    public double baumWelchAlgorithm(int[] sequence) {
        return baumWelchAlgorithm(sequence, MIN_ERROR);
    }

    /**
     * Baum-Welch (EM) algorithm.
     * Train the HMM until likelihood difference becomes smaller than the given min_error.
     * It also stops if the iteration reaches MAX_ITERATION.
     *
     * @param seq
     * @param min_error Minimum desired error
     * @return Likelihood of the model given the observation sequence after training
     */
    public double baumWelchAlgorithm(int[] seq, double min_error) {
        double diff = 1.0;
        double p1 = forwardBackwardTrellis(seq);
        double p2 = 1.0;
        for (int n = 0; n < MAX_ITERATION; n++) {
            reestimateLambda(seq);
            p2 = forwardBackwardTrellis(seq);
            diff = p2 - p1;
            p1 = p2;
            if (diff <= min_error) {
                break;
            }
        }
        return p1;
    }

    /**
     * Generates random observation sequence.
     *
     * @return Randomly generated observation sequence according to the model
     */
    public int[] generateSeq(int len_obseq) {
        int[] seq = new int[len_obseq];
        for (int t = 0; t < len_obseq; t++) {
            double r = rand.nextDouble();
            double p = 0.0;
            for (int k = 0; k < num_symbols; k++) {
                double s = 0.0;
                for (int i = 0; i < num_states; i++) {
                    s += getB(i, k) * getPi(t, i);
                }
                p += s;
                if (p > r) {
                    seq[t] = k;
                    break;
                }
            }
        }
        return seq;
    }

    /**
     * One of the three canonical questions of HMM a la Ferguson-Rabiner.
     * [1] Given the observation sequence obSeq and a model lambda,
     * what is the probability of the given sequence given lambda?
     *
     * @return Probability of the given sequence given lambda
     */
    public double getObSeqProbability(int[] seq) {
        /*
        double logP = Math.log(forwardAlpha());
        for(int t=0;t<len_obseq;t++) {
            logP += Math.log(scaleFactor[t]);
        }
        return logP;
        */
        return forwardAlpha(seq);
    }

    /**
     * One of the three canonical questions of HMM a la Ferguson-Rabiner.
     * [2] Given the observation sequence obSeq and a model lambda,
     * what is the "optimal" sequence of hidden states?
     *
     * @return Most likely sequence of hidden states
     */
    public int[] getMaxLikelyState(int[] seq) {
        viterbiAlgorithm(seq);
        return qstar;
    }

    /**
     * One of the three canonical questions of HMM a la Ferguson-Rabiner.
     * [3] How do we adjust the model parameters lambda
     * to maximize the likelihood of the given sequence obSeq?
     *
     * @return Locally optimal likelihood value of the given sequence
     */
    public double optimizeLambda(int[] sequence) {
        return baumWelchAlgorithm(sequence);
    }

}
