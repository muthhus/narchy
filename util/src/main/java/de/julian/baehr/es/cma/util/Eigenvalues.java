package de.julian.baehr.es.cma.util;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

public class Eigenvalues {

	private RealMatrix diagonal;
	private RealMatrix eigenvalues;
	
	public Eigenvalues(BlockRealMatrix matrix){

		//MatrixUtil.floatToReal(matrix)
		EigenDecomposition decomposition = new EigenDecomposition(matrix);
		diagonal = decomposition.getD();
		diagonal = fixRotationD(diagonal);
		
		eigenvalues = decomposition.getV();
		eigenvalues = fixRotationE(eigenvalues);
		eigenvalues.scalarMultiply(-1); //.opposite();
	}
	
	private RealMatrix fixRotationD(RealMatrix matrix){
		int cols = matrix.getColumnDimension();
		int rows = matrix.getRowDimension();
		double[][] values = new double[cols][rows];
		
		for(int row = 0; row < rows; row++)
			for(int col = 0; col < cols; col++)
				values[rows - row - 1][cols -col-1] = matrix.getEntry(row, col);
		
		return new Array2DRowRealMatrix(values);
	}
	
	private RealMatrix fixRotationE(RealMatrix matrix){
		int cols = matrix.getColumnDimension();
		int rows = matrix.getRowDimension();

		double[][] values = new double[cols][rows];
		
		for(int row = 0; row < rows; row++)
			for(int col = 0; col < cols; col++)
				values[row][cols - col - 1] = matrix.getEntry(row, col);
		
		return new Array2DRowRealMatrix(values);
	}

	public RealMatrix getEigenvalueMatrix() {
		return eigenvalues;
	}

	public RealMatrix getDiagonal() {
		return diagonal;
	}
}
