//package de.julian.baehr.es.cma;
//
//import static de.julian.baehr.es.cma.util.MatrixUtil.fromDiagonal;
//import static de.julian.baehr.es.cma.util.MatrixUtil.identity;
//import static de.julian.baehr.es.cma.util.MatrixUtil.repeatHorizontally;
//import static de.julian.baehr.es.cma.util.MatrixUtil.upperTriangle;
//import static de.julian.baehr.es.cma.util.VectorUtil.copyVectorValues;
//import static de.julian.baehr.es.cma.util.VectorUtil.empty;
//import static de.julian.baehr.es.cma.util.VectorUtil.logEach;
//import static de.julian.baehr.es.cma.util.VectorUtil.normalize;
//import static de.julian.baehr.es.cma.util.VectorUtil.powEach;
//import static de.julian.baehr.es.cma.util.VectorUtil.randomVector;
//import static de.julian.baehr.es.cma.util.VectorUtil.randomVectorGauss;
//import static de.julian.baehr.es.cma.util.VectorUtil.scale;
//import static de.julian.baehr.es.cma.util.VectorUtil.sqrtEach;
//import static de.julian.baehr.es.cma.util.VectorUtil.sum;
//import static de.julian.baehr.es.cma.util.VectorUtil.vectorFromTo;
//import static de.julian.baehr.es.cma.util.VectorUtil.vectorOf;
//import static jcog.Util.floatToDoubleArray;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.List;
//
//
//
//
//import de.julian.baehr.es.IObjectiveFunction;
//import de.julian.baehr.es.cma.util.Eigenvalues;
//import de.julian.baehr.es.cma.util.VectorUtil;
//import jcog.tensor.Tensor;
//import org.apache.commons.math3.linear.Array2DRowRealMatrix;
//import org.apache.commons.math3.linear.MatrixUtils;
//import org.apache.commons.math3.linear.RealMatrix;
//import org.apache.commons.math3.util.ArithmeticUtils;
//import org.apache.commons.math3.util.MathArrays;
//
//public class CMAES {
//
//	public static class CmaesResult{
//
//		private long steps;
//		private long evals;
//		private Individual[] individuals;
//
//		public CmaesResult(long steps, long evals, Individual[] individuals){
//			this.steps = steps;
//			this.evals = evals;
//			this.individuals = individuals;
//		}
//
//		public long getSteps() {
//			return steps;
//		}
//		public long getEvals() {
//			return evals;
//		}
//		public Individual[] getIndividuals() {
//			return individuals;
//		}
//	}
//
//	public static CmaesResult run(int problemDimension, double standardDeviation, double stopFitness, long maxFunctionEvals, IObjectiveFunction objectiveFunction, boolean output) {
//
//		int childCount = (int) (4 + Math.floor(3*Math.log(problemDimension)));
//		double parentCount = childCount/2.;
//		Tensor weights = logEach(vectorFromTo(1, (int)parentCount))
//				.scale(-1)
//		Tensor pc = vectorOf( (float)Math.log(parentCount + 0.5d), (int) parentCount););
//		weights.add(pc);
//		normalize(weights);
//
//
//		double varianceEffectiveness = sum(weights) / sum(powEach(weights, 2));
//
//		Tensor objectiveVariable = randomVector(problemDimension, 0, 1);
//
//		double timeConstantCumulation = (4 + varianceEffectiveness / problemDimension) / (problemDimension + 4 + 2*varianceEffectiveness/problemDimension);
//		double timeConstantSigma = (varianceEffectiveness+2) / (problemDimension+varianceEffectiveness+5);
//		double learningRate = 2. / (Math.pow((problemDimension+1.3), 2) + varianceEffectiveness);
//		double learningMu = Math.min(1 - learningRate, 2 * (varianceEffectiveness-2 + 1/varianceEffectiveness) / (Math.pow(problemDimension + 2, 2) + varianceEffectiveness));
//		double damping = 1 + 2*Math.max(0, Math.sqrt((varianceEffectiveness-1)/(problemDimension+1))-1) + timeConstantSigma;
//
//		Tensor pathCovariance = empty(problemDimension);
//		Tensor pathStandardDeviation = empty(problemDimension);
//		RealMatrix coordinateSystem = identity(problemDimension);
//		Tensor diagonal = VectorUtil.vectorOf(1f, problemDimension); //MatrixUtil.coordinateSystem.getDiagonal()
//		RealMatrix  covariance = coordinateSystem.multiply(fromDiagonal(powEach(diagonal, 2))).multiply(coordinateSystem.transpose());
//		RealMatrix  invsqrtCovariance = coordinateSystem.multiply(fromDiagonal(powEach(diagonal, -1))).multiply(coordinateSystem.transpose());
//		double eigenval = 0;
//		double excpectation = Math.pow(problemDimension, 0.5) * (1 - 1./(4*problemDimension) + 1./(21*Math.pow(problemDimension, 2)));
//
//		//child container
//		Individual[] individuals = new Individual[childCount];
//		for(int i = 0; i < individuals.length; i++)
//			individuals[i] = new Individual();
//
//		//init best vector container
//		List<Tensor> bestVectors = new ArrayList<>();
//		for(int i = 0; i < (int)parentCount; i++)
//			bestVectors.add(null);
//
//		int counter = 0;
//		int loops = 0;
//		//generation loop
//		while(counter < maxFunctionEvals){
//
//			//new generation
//			for(int i = 0; i < childCount; i++){
//
//				Tensor childObjectiveVariable =
//						objectiveVariable.scale(coordinateSystem.times(scale(diagonal, randomVectorGauss(problemDimension, 0, 1)).times(standardDeviation)));
//
//				individuals[i].solution = childObjectiveVariable;
//				individuals[i].fitness = objectiveFunction.evaluate(copyVectorValues(childObjectiveVariable));
//
//				counter++;
//			}
//
//			//find best
//			Arrays.sort(individuals, Comparator.comparingDouble(i2 -> i2.fitness));
//			for(int i = 0; i < (int)parentCount; i++)
//				bestVectors.set(i, individuals[i].solution);
//
//			//new mean value
//			Tensor oldObjectiveVariable = objectiveVariable;
//
//			objectiveVariable = MatrixUtils.createRowRealMatrix(floatToDoubleArray(weights.get()))
//					.multiply(RealMatrix .valueOf(bestVectors)).getRow(0);
//
//			pathStandardDeviation = pathStandardDeviation.times(1.-timeConstantCumulation)
//					.plus(invsqrtCovariance.times(Float64.valueOf(Math.sqrt(timeConstantSigma*(2-timeConstantSigma)*varianceEffectiveness)))
//					.times(objectiveVariable.minus(oldObjectiveVariable))
//					.times(Float64.valueOf(1./standardDeviation)));
//
//			boolean hsig = pathStandardDeviation.normValue() / Math.sqrt(1. - Math.pow((1. - timeConstantSigma), 2.*counter/childCount)) / excpectation < 1.4 + 2./(problemDimension+1);
//
//			pathCovariance = pathCovariance.times(1.-timeConstantCumulation)
//					.plus(objectiveVariable.minus(oldObjectiveVariable)
//					.times(asInt(hsig) * Math.sqrt(timeConstantCumulation * (2. - timeConstantCumulation) * varianceEffectiveness))
//					.times(1./standardDeviation));
//
//			//adapt covariance matrix
//			RealMatrix  artmp = RealMatrix .valueOf(bestVectors).transpose()
//					.minus(repeatHorizontally(oldObjectiveVariable, (int)parentCount))
//					.times(Float64.valueOf(1./standardDeviation));
//
//
//
//			RealMatrix  temp1 = covariance.times(Float64.valueOf(1.-learningRate-learningMu));
//
//			RealMatrix  temp2 = RealMatrix .valueOf(pathCovariance).transpose().times(RealMatrix .valueOf(pathCovariance))
//					.plus(covariance.times(Float64.valueOf(timeConstantCumulation*(2.-timeConstantCumulation)* (1.-asInt(hsig)))));
//			temp2 = temp2.times(Float64.valueOf(learningRate));
//
//			RealMatrix  temp3 = artmp.times(Float64.valueOf(learningMu)).times(fromDiagonal(weights))
//							.times(artmp.transpose());
//
//			covariance = temp1.plus(temp2).plus(temp3);
//
//			//adapt step size
//			standardDeviation = standardDeviation * Math.exp((timeConstantSigma/damping) * (pathStandardDeviation.normValue()/excpectation -1.));
//
//
//		    //Decomposition of C into B*diag(D.^2)*B' (diagonalization)
//			if(counter - eigenval > childCount/(learningRate+learningMu)/problemDimension/10){
//
//				eigenval = counter;
//				covariance = upperTriangle(covariance).plus(upperTriangle(covariance, 1).transpose());//enforce symmetry
//
//				Eigenvalues eigenvalues = new Eigenvalues(covariance);
//				coordinateSystem = eigenvalues.getEigenvalueMatrix();
//
//				diagonal = sqrtEach(eigenvalues.getDiagonal().getDiagonal());
//				invsqrtCovariance = coordinateSystem.times(fromDiagonal(powEach(diagonal, -1)))
//						.times(coordinateSystem.transpose());
//			}
//
//			//stop condition
//			if(individuals[0].fitness <= stopFitness || (double) diagonal.max() > 1e7 * (double) diagonal.max())
//				break;
//
//			if(output && loops++ % 100 == 0){
//				System.out.println();
//				System.out.println("loop: " + loops + " best: value=" + individuals[0].fitness);
//				System.out.println(individuals[0].solution);
//			}
//		}
//
//		return new CmaesResult(loops, counter, individuals);
//	}
//
//	private static int asInt(boolean b){
//		return b ? 1 : 0;
//	}
//}
