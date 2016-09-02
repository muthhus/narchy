package ideal.vacuum.agent.spacememory;

import ideal.vacuum.agent.Move;

import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 391 $
 */
public enum SpaceMemoryMove {
	DEFAULT(
			"" ,
			circleShape() ,
			leftHalfCircleShape() ,
			rightHalfCircleShape() ) ,
	MOVE_FORWARD(
			Move.MOVE_FORWARD.getLabel() ,
			squareShape() ,
			leftHalfSquareShape() ,
			rightHalfSquareShape() ) ,
	TURN_LEFT(
			Move.TURN_LEFT.getLabel() ,
			trapezoidShape() ,
			leftHalfTrapezoidShape() ,
			rightHalfTrapezoidShape() ) ,
	TURN_RIGHT(
			Move.TURN_RIGHT.getLabel() ,
			trapezoidShape() ,
			//arcShape() ,
			leftHalfTrapezoidShape() ,
			rightHalfTrapezoidShape() ) ,
	TOUCH(
			Move.TOUCH.getLabel() ,
			squareShape() ,
			leftHalfSquareShape() ,
			rightHalfSquareShape() ) ,
	TOUCH_RIGHT(
			Move.TOUCH_RIGHT.getLabel() ,
			squareShape() ,
			leftHalfSquareShape() ,
			rightHalfSquareShape() ) ,
	TOUCH_LEFT(
			Move.TOUCH_LEFT.getLabel() ,
			squareShape() ,
			leftHalfSquareShape() ,
			rightHalfSquareShape() ) ;

	private final String moveLabel ;
	private final Area shape ;
	private final Area leftHalfShape ;
	private final Area rightHalfShape ;
	private final static float SCALE = (float)SpaceMemoryDesigner.SCALE / 40;

	private final static Map<String , SpaceMemoryMove> BY_MOVE_LABEL = new HashMap<>() ;

	static {
		for ( SpaceMemoryMove smInteractions : SpaceMemoryMove.values() ) {
			BY_MOVE_LABEL.put( smInteractions.moveLabel , smInteractions ) ;
		}
	}

	SpaceMemoryMove(String moveLabel,
                    Area shape,
                    Area leftHalfShape,
                    Area rightHalfShape) {
		this.moveLabel = moveLabel ;
		this.shape = shape ;
		this.leftHalfShape = leftHalfShape ;
		this.rightHalfShape = rightHalfShape ;
	}

	private static Area circleShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Ellipse2D.Double( -10 * SCALE, -10 * SCALE , 20 * SCALE , 20 * SCALE ) , true ) ;
		return new Area( shape ) ;
	}

	private static Area leftHalfCircleShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Arc2D.Double( -10 * SCALE , -10 * SCALE , 20 * SCALE , 20 * SCALE , 0 , 180 , Arc2D.PIE ) , true ) ;
		return new Area( shape ) ;
	}

	private static Area rightHalfCircleShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Arc2D.Double( -10 * SCALE , -10 * SCALE , 20 * SCALE , 20 * SCALE , 180 , 180 , Arc2D.PIE ) , true ) ;
		return new Area( shape ) ;
	}

	private static Area arcShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Arc2D.Double( -10 * SCALE , -10 * SCALE , 20 * SCALE , 20 * SCALE , -180 , 180 , Arc2D.PIE ) , true ) ;
		return new Area( shape ) ;
	}

	private static Area leftHalfArcShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Arc2D.Double( -10 * SCALE , -10 * SCALE , 20 * SCALE , 20 * SCALE , -90 , 90 , Arc2D.PIE ) , true ) ;
		return new Area( shape ) ;
	}

	private static Area rightHalfArcShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Arc2D.Double( -10 * SCALE , -10 * SCALE , 20 * SCALE , 20 * SCALE , -180 , 90 , Arc2D.PIE ) , true ) ;
		return new Area( shape ) ;
	}

	private static Area triangleShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Line2D.Double( -10 * SCALE , -10 * SCALE , 10 * SCALE , 0 ) , false ) ;
		shape.append( new Line2D.Double( 10 * SCALE , 0 , -10 * SCALE , 10 * SCALE ) , true ) ;
		return new Area( shape ) ;
	}

	private static Area leftHalfTriangleShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Line2D.Double( -10 * SCALE , -10 * SCALE , 10 * SCALE , 0 ) , false ) ;
		shape.append( new Line2D.Double( 10 * SCALE , 0 , -10 * SCALE , 0 ) , true ) ;
		return new Area( shape ) ;

	}

	private static Area rightHalfTriangleShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Line2D.Double( -10 * SCALE , 0 , 10 * SCALE , 0 ) , false ) ;
		shape.append( new Line2D.Double( 10 * SCALE , 0 , -10 * SCALE , 10 * SCALE ) , true ) ;
		return new Area( shape ) ;

	}

	private static Area trapezoidShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Line2D.Double( -7 * SCALE , 14 * SCALE , 7 * SCALE , 7 * SCALE ) , false ) ;
		shape.append( new Line2D.Double( 7 * SCALE , 7 * SCALE , 7 * SCALE , -7 * SCALE ) , true ) ;
		shape.append( new Line2D.Double( 7 * SCALE , -7 * SCALE , -7 * SCALE , 0 ) , true ) ;
		return new Area( shape ) ;

	}

	private static Area leftHalfTrapezoidShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Line2D.Double( -7 * SCALE , 7 * SCALE , 7 * SCALE , 0 ) , false ) ;
		shape.append( new Line2D.Double( 7 * SCALE , 0 , 7 * SCALE , -7 * SCALE ) , true ) ;
		shape.append( new Line2D.Double( 7 * SCALE , -7 * SCALE , -7 * SCALE , 0 ) , true ) ;
		return new Area( shape ) ;

	}

	private static Area rightHalfTrapezoidShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Line2D.Double( -7 * SCALE , 14 * SCALE , 7 * SCALE , 7 * SCALE ) , false ) ;
		shape.append( new Line2D.Double( 7 * SCALE , 7 * SCALE , 7 * SCALE , 0 ) , true ) ;
		shape.append( new Line2D.Double( 7 * SCALE , 0 , -7 * SCALE , 7 * SCALE ) , true ) ;
		return new Area( shape ) ;

	}

	private static Area squareShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Rectangle2D.Double( -7 * SCALE , -7 * SCALE , 14 * SCALE , 14 * SCALE ) , true ) ;
		return new Area( shape ) ;
	}

	private static Area leftHalfSquareShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Rectangle2D.Double( -7 * SCALE , -7 * SCALE , 14 * SCALE , 7 * SCALE ) , true ) ;
		return new Area( shape ) ;
	}

	private static Area rightHalfSquareShape() {
		GeneralPath shape = new GeneralPath() ;
		shape.append( new Rectangle2D.Double( -7 * SCALE , 0 , 14 * SCALE , 7 * SCALE ) , true ) ;
		return new Area( shape ) ;
	}

	public static SpaceMemoryMove getSpaceMemoryMove( String moveLabel ) {
		if ( BY_MOVE_LABEL.containsKey( moveLabel ) ) {
			return BY_MOVE_LABEL.get( moveLabel ) ;
		} else {
			return SpaceMemoryMove.DEFAULT ;
		}
	}

	public static String extractMoveLabel( String interaction ) {
		for ( char inter : interaction.toCharArray() ) {
			if ( Move.isExist( String.valueOf( inter ) ) ) {
				return String.valueOf( inter ) ;
			}
		}

		throw new RuntimeException( "Can't extract the move in interaction : " + interaction ) ;
	}

	public String getMoveLabel() {
		return this.moveLabel ;
	}

	public Area getShape() {
		return this.shape ;
	}

	public Area getLeftHalfShape() {
		return this.leftHalfShape ;
	}

	public Area getRightHalfShape() {
		return this.rightHalfShape ;
	}
	
}
