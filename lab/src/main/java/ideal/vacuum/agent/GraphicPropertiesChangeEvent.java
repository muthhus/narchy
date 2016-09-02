package ideal.vacuum.agent;

import spacegraph.math.v3;

import java.util.EventObject;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 390 $
 */
public class GraphicPropertiesChangeEvent extends EventObject {

	private GraphicProperties graphicProperties ;

	public GraphicPropertiesChangeEvent( Object source ,
			GraphicProperties graphicProperties ) {
		super( source ) ;
		try {
			this.graphicProperties = graphicProperties.clone() ;
		} catch ( CloneNotSupportedException e ) {
			e.printStackTrace() ;
		}
	}

	public GraphicProperties getGraphicProperties() {
		return this.graphicProperties ;
	}

	
	public v3 getmPosition() {
		return this.graphicProperties.getmPosition() ;
	}

	public v3 getmOrientation() {
		return this.graphicProperties.getmOrientation() ;
	}

	public v3 getmTranslation() {
		return this.graphicProperties.getmTranslation() ;
	}

	public v3 getmRotation() {
		return this.graphicProperties.getmRotation() ;
	}

	public v3 getmPreviousPosition() {
		return this.graphicProperties.getmPreviousPosition() ;
	}

	public v3 getmPreviousOrientation() {
		return this.graphicProperties.getmPreviousOrientation() ;
	}
	
	public int getDisplayCode(){
		return this.graphicProperties.getDisplayCode();
	}

	public void setmPosition( v3 mPosition ) {
		this.graphicProperties.setmPosition( mPosition ) ;
	}

	public void setmOrientation( v3 mOrientation ) {
		this.graphicProperties.setmOrientation( mOrientation ) ;
	}

	public void setmTranslation( v3 mTranslation ) {
		this.graphicProperties.setmTranslation( mTranslation ) ;
	}

	public void setmRotation( v3 mRotation ) {
		this.graphicProperties.setmRotation( mRotation ) ;
	}

	public void setmPreviousPosition( v3 mPreviousPosition ) {
		this.graphicProperties.setmPreviousPosition( mPreviousPosition ) ;
	}

	public void setmPreviousOrientation( v3 mPreviousOrientation ) {
		this.graphicProperties.setmPreviousOrientation( mPreviousOrientation ) ;
	}
	
	public void setDisplayCode(int displayCode){
		this.graphicProperties.setDisplayCode(displayCode);
	}
}