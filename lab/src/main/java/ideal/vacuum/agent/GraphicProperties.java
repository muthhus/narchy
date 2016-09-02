package ideal.vacuum.agent;

import spacegraph.math.v3;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 390 $
 */
public class GraphicProperties implements Cloneable{
	private v3 mPosition ;
	private v3 mOrientation ;
	private v3 mTranslation ;
	private v3 mRotation ;
	private v3 mPreviousPosition ;
	private v3 mPreviousOrientation ;
	private int displayCode;

	public GraphicProperties( v3 mPosition ,
			v3 mOrientation ,
			v3 mTranslation ,
			v3 mRotation ,
			v3 mPreviousPosition ,
			v3 mPreviousOrientation,
			int displayCode) {
		this.mPosition = mPosition ;
		this.mOrientation = mOrientation ;
		this.mTranslation = mTranslation ;
		this.mRotation = mRotation ;
		this.mPreviousPosition = mPreviousPosition ;
		this.mPreviousOrientation = mPreviousOrientation ;
		this.displayCode = displayCode;
	}

	@Override
	protected GraphicProperties clone() throws CloneNotSupportedException {
		GraphicProperties object = (GraphicProperties) super.clone();
		object.mPosition = (v3) this.mPosition.clone();
		object.mOrientation = (v3) this.mOrientation.clone();
		object.mTranslation = (v3) this.mTranslation.clone();
		object.mRotation = (v3) this.mRotation.clone();
		object.mPreviousPosition = (v3) this.mPreviousPosition.clone();
		object.mPreviousOrientation = (v3) this.mPreviousOrientation.clone();
		
		return object;
	}
	
	public v3 getmPosition() {
		return this.mPosition ;
	}

	public v3 getmOrientation() {
		return this.mOrientation ;
	}

	public v3 getmTranslation() {
		return this.mTranslation ;
	}

	public v3 getmRotation() {
		return this.mRotation ;
	}

	public v3 getmPreviousPosition() {
		return this.mPreviousPosition ;
	}

	public v3 getmPreviousOrientation() {
		return this.mPreviousOrientation ;
	}

	public void setmPosition( v3 mPosition ) {
		this.mPosition = mPosition ;
	}

	public void setmOrientation( v3 mOrientation ) {
		this.mOrientation = mOrientation ;
	}

	public void setmTranslation( v3 mTranslation ) {
		this.mTranslation = mTranslation ;
	}

	public void setmRotation( v3 mRotation ) {
		this.mRotation = mRotation ;
	}

	public void setmPreviousPosition( v3 mPreviousPosition ) {
		this.mPreviousPosition = mPreviousPosition ;
	}

	public void setmPreviousOrientation( v3 mPreviousOrientation ) {
		this.mPreviousOrientation = mPreviousOrientation ;
	}

	public int getDisplayCode() {
		return displayCode;
	}

	public void setDisplayCode(int displayCode) {
		this.displayCode = displayCode;
	}
}