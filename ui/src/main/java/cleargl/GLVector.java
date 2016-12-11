package cleargl;

import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

/*
@author Ulrik Günther, Loïc Royer
 */

public class GLVector {
	protected float[] mElements;
	protected int mDimension;

	public GLVector(final float... pElements) {
		super();
		mElements = Arrays.copyOf(pElements, pElements.length);
		mDimension = pElements.length;
	}

	public GLVector(final float element, final int dimension) {
		super();
		mElements = new float[dimension];
		for (int i = 0; i < dimension; i++) {
			mElements[i] = element;
		}

		mDimension = dimension;
	}

	public GLVector(final GLVector pGLVector) {
		super();
		mElements = Arrays.copyOf(pGLVector.mElements,
				pGLVector.mElements.length);
		mDimension = pGLVector.mElements.length;
	}

	@Override
	public GLVector clone() {
		return new GLVector(this);
	}

	public float x() {
		return mElements[0];
	}

	public float y() {
		return mElements[1];
	}

	public float z() {
		return mElements[2];
	}

	public float w() {
		return mElements[3];
	}

	public GLVector xyz() {
		return new GLVector(mElements[0], mElements[1], mElements[2]);
	}

	public GLVector xyzw() {
		return new GLVector(mElements[0], mElements[1], mElements[2], mElements[3]);
	}

	public float get(final int pIndex) {
		return mElements[pIndex];
	}

	public void set(final int pIndex, final float pValue) {
		mElements[pIndex] = pValue;
	}

	public void plusAssign(final GLVector pGLVector) {
		final float[] lElements = pGLVector.mElements;
		for (int i = 0; i < mDimension; i++)
			mElements[i] += lElements[i];
	}

	public GLVector minus(final GLVector pGLVector) {
		final GLVector lMinus = this.clone();
		lMinus.minusAssign(pGLVector);
		return lMinus;
	}

	public GLVector plus(final GLVector pGLVector) {
		final GLVector lPlus = this.clone();
		lPlus.plusAssign(pGLVector);
		return lPlus;
	}

	public void minusAssign(final GLVector pGLVector) {
		final float[] lElements = pGLVector.mElements;
		for (int i = 0; i < mDimension; i++)
			mElements[i] -= lElements[i];
	}

	public void timesAssign(final GLVector pGLVector) {
		final float[] lElements = pGLVector.mElements;
		for (int i = 0; i < mDimension; i++)
			mElements[i] *= lElements[i];
	}

	public void divAssign(final GLVector pGLVector) {
		final float[] lElements = pGLVector.mElements;
		for (int i = 0; i < mDimension; i++)
			mElements[i] /= lElements[i];
	}

	public float times(final GLVector pGLVector) {
		float lResult = 0;
		final float[] lElements = pGLVector.mElements;
		for (int i = 0; i < mDimension; i++)
			lResult += mElements[i] * lElements[i];
		return lResult;
	}

	public GLVector times(final Quaternion q) {
		final float[] in = this.mElements.clone();
		final float[] out = new float[mDimension];

		q.rotateVector(out, 0, in, 0);

		return new GLVector(out);
	}

	public GLVector times(final float num) {
		final GLVector n = this.clone();
		for (int i = 0; i < mDimension; i++) {
			n.mElements[i] = num * n.mElements[i];
		}

		return n;
	}

	public float magnitude() {
		float lResult = 0;
		for (int i = 0; i < mDimension; i++) {
			final float lValue = mElements[i];
			lResult += lValue * lValue;
		}
		return (float)Math.sqrt(lResult);
	}

	public float length2() {
		float lResult = 0;
		for (int i = 0; i < mDimension; i++) {
			final float lValue = mElements[i];
			lResult += lValue * lValue;
		}
		return lResult;
	}

	public GLVector normalize() {
		final float lFactor = 1f / magnitude();
		for (int i = 0; i < mDimension; i++)
			mElements[i] *= lFactor;
		return this;
	}

	public GLVector cross(final GLVector v) {
		final float result[] = new float[3];
		VectorUtil.crossVec3(result, this.toFloatBuffer().array(), v.toFloatBuffer().array());

		return new GLVector(result);
	}

	public GLVector getNormalized() {
		return this.clone().normalize();
	}

	public FloatBuffer toFloatBuffer() {
		return FloatBuffer.wrap(mElements);
	}

	public float[] toFloatArray() {
		return mElements;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mDimension;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final GLVector other = (GLVector) obj;
		if (mDimension != other.mDimension)
			return false;
		return true;
	}

	public ByteBuffer push(ByteBuffer buffer) {
		int pos = buffer.position();
		buffer.asFloatBuffer().put(mElements);
		buffer.position(pos);

		return buffer;
	}

	public ByteBuffer put(ByteBuffer buffer) {
		buffer.asFloatBuffer().put(mElements);

		return buffer;
	}

	@Override
	public String toString() {
		return "[" + Arrays.toString(mElements) + "]";
	}

	public static GLVector getOneVector(final int dimension) {
		return new GLVector(1.0f, dimension);
	}

	public static GLVector getNullVector(final int dimension) {
		return new GLVector(0.0f, dimension);
	}

}
