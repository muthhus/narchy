package cleargl.util.arcball;

import cleargl.GLMatrix;
import com.jogamp.opengl.math.Quaternion;

public class ArcBall
{
	private static final float Epsilon = 1.0e-5f;

	float[] mStartVector;
	float[] mEndVector;
	float mMouseBoundsWidth = 1;
	float mMouseBoundsHeight = 1;

	Quaternion mCurrentQuaternion = new Quaternion();
	Quaternion mRotationQuaternion = new Quaternion();
	Quaternion mResultQuaternion = new Quaternion();

	public ArcBall()
	{
		mStartVector = new float[3];
		mEndVector = new float[3];
		mRotationQuaternion.setIdentity();
	}

	public void setCurrent(Quaternion pQuaternion)
	{
		mCurrentQuaternion.set(pQuaternion);
	}

	public void mapToSphere(float p2DPointX,
													float p2DPointY,
													float[] p3DVector)
	{
		// Copy parameter into temp point

		// Adjust point coords and scale down to range of [-1 ... 1]
		p2DPointX = (p2DPointX * this.mMouseBoundsWidth) - 1.0f;
		p2DPointY = 1.0f - (p2DPointY * this.mMouseBoundsHeight);

		// Compute the square of the length of the vector to the point from the
		// center
		final double length = (p2DPointX * p2DPointX) + (p2DPointY * p2DPointY);

		// If the point is mapped outside of the sphere... (length > radius squared)
		if (length > 1.0f)
		{
			// Compute a normalizing factor (radius / sqrt(length))
			final float norm = (float) (1.0 / Math.sqrt(length));

			// Return the "normalized" vector, a point on the sphere
			p3DVector[0] = p2DPointX * norm;
			p3DVector[1] = p2DPointY * norm;
			p3DVector[2] = 0.0f;
		}
		else
		// Else it's on the inside
		{
			// Return a vector to a point mapped inside the sphere sqrt(radius squared
			// - length)
			p3DVector[0] = p2DPointX;
			p3DVector[1] = p2DPointY;
			p3DVector[2] = (float) Math.sqrt(1.0f - length);
		}

	}

	public void setBounds(float pMouseBoundsWidth,
												float pMouseBoundsHeight)
	{
		assert ((pMouseBoundsWidth > 1.0f) && (pMouseBoundsHeight > 1.0f));

		// Set adjustment factor for width/height
		mMouseBoundsWidth = 1.0f / ((pMouseBoundsWidth - 1.0f) * 0.5f);
		mMouseBoundsHeight = 1.0f / ((pMouseBoundsHeight - 1.0f) * 0.5f);
	}

	// Mouse down
	public void click(float p2DPointX, float p2DPointsY)
	{
		mapToSphere(p2DPointX, p2DPointsY, this.mStartVector);
	}

	// Mouse drag, calculate rotation
	public Quaternion drag(float p2DPointX, float p2DPointY)
	{
		// Map the point to the sphere
		this.mapToSphere(p2DPointX, p2DPointY, mEndVector);

		final float[] lPerp = new float[3];

		// Compute the vector perpendicular to the begin and end vectors
		GLMatrix.cross(lPerp, mStartVector, mEndVector);

		final float lLength = GLMatrix.norm(lPerp);

		// Compute the length of the perpendicular vector
		if (lLength > Epsilon) // if its non-zero
		{
			// We're ok, so return the perpendicular vector as the transform after
			// all
			mRotationQuaternion.setX(lPerp[0]);
			mRotationQuaternion.setY(lPerp[1]);
			mRotationQuaternion.setZ(lPerp[2]);
			// In the quaternion values, w is cosine (theta / 2), where theta is
			// rotation angle
			mRotationQuaternion.setW(GLMatrix.dot(mStartVector, mEndVector));
		}
		else
		// if its zero
		{
			// The begin and end vectors coincide, so return an identity transform
			mRotationQuaternion.setIdentity();
		}

		mResultQuaternion.set(mRotationQuaternion);
		mResultQuaternion.mult(mCurrentQuaternion);

		return mResultQuaternion;

	}

}
