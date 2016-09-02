package ideal.vacuum.agent.vision;

import ideal.vacuum.ernest.Ernest;
import spacegraph.math.v3;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Objects;

public class PhotoreceptorCell implements Cloneable {

	private int xBlockPosition ;
	private int yBlockPosition ;
	private Color blockColor ;

	public PhotoreceptorCell( int xBlockPosition , int yBlockPosition , Color blockColor ) {
		super() ;
		this.xBlockPosition = xBlockPosition ;
		this.yBlockPosition = yBlockPosition ;
		this.blockColor = blockColor ;
	}

	public int getxBlockPosition() {
		return this.xBlockPosition ;
	}

	public int getyBlockPosition() {
		return this.yBlockPosition ;
	}

	public v3 getBlockPosition() {
		return new v3( this.xBlockPosition , this.yBlockPosition , 0 ) ;
	}

	public Color getBlockColor() {
		return this.blockColor ;
	}

	public void applyTransformation( AffineTransform matrix ){
		Point2D blockPosition = matrix.transform( new Point( this.xBlockPosition , this.yBlockPosition ) ,
				new Point() ) ;
		this.xBlockPosition = (int) blockPosition.getX();
		this.yBlockPosition = (int) blockPosition.getY() ;
	}
	
	public void orienteAxis( double theta ) {
		AffineTransform aff = new AffineTransform() ;
		aff.rotate( -theta ) ;
		Point2D blockPosition = aff.transform(
				new Point( this.xBlockPosition , this.yBlockPosition ) ,
				new Point() ) ;
		this.xBlockPosition = (int) blockPosition.getX() ;
		this.yBlockPosition = (int) blockPosition.getY() ;
	}

	public float distanceAccurateToTheBlock() {
		if ( Math.abs( this.xBlockPosition ) == Ernest.INFINITE ||
				Math.abs( this.yBlockPosition ) == Ernest.INFINITE )
			return Ernest.INFINITE ;
		return this.calculateHypotenuse() ;
	}

	private float calculateHypotenuse() {
		return ( (float) Math.sqrt( this.xBlockPosition *
				this.xBlockPosition +
				this.yBlockPosition *
				this.yBlockPosition ) ) ;
	}

	@Override
	public PhotoreceptorCell clone()  {
		try {
			PhotoreceptorCell object = (PhotoreceptorCell) super.clone() ;
			object.xBlockPosition = this.xBlockPosition ;
			object.yBlockPosition = this.yBlockPosition ;
			object.blockColor = this.blockColor ;
			return object ;
		} catch ( CloneNotSupportedException e ) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31 ;
		int result = 1 ;
		result = prime * result + ( ( this.blockColor == null ) ? 0 : this.blockColor.hashCode() ) ;
		result = prime * result + this.xBlockPosition ;
		result = prime * result + this.yBlockPosition ;
		return result ;
	}

	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true ;
		}
		if ( obj == null ) {
			return false ;
		}
		if ( ! ( obj instanceof PhotoreceptorCell ) ) {
			return false ;
		}
		PhotoreceptorCell other = (PhotoreceptorCell) obj ;
		if ( this.blockColor == null ) {
			if ( other.blockColor != null ) {
				return false ;
			}
		} else if ( !Objects.equals(blockColor, blockColor)) {
			return false ;
		}
		if ( this.xBlockPosition != other.xBlockPosition ) {
			return false ;
		}
        return this.yBlockPosition == other.yBlockPosition;
    }
}
