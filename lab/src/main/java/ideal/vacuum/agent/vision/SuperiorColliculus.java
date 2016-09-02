package ideal.vacuum.agent.vision;

import ideal.vacuum.agent.VisualEffect;
import ideal.vacuum.ernest.Entry;
import ideal.vacuum.ernest.Ernest;
import ideal.vacuum.ernest.ErnestUtils;
import spacegraph.math.v3;

import java.awt.geom.AffineTransform;
import java.util.*;

public class SuperiorColliculus {

	private final Eye eye ;
	private Queue<PhotoreceptorCell> memoryCells ;
	private Map<PhotoreceptorCell , VisualEffect> visualsEffects ;

	public SuperiorColliculus( Eye eye ) {
		this.eye = eye ;
		this.memoryCells = new LinkedList<>() ;
		this.visualsEffects = new LinkedHashMap<>() ;
	}

	public Set<PhotoreceptorCell> listOfMemoryAndActiveCells() {
		HashSet<PhotoreceptorCell> merged = new HashSet<>(this.eye.getActivePhotoreceptorCells()) ;
        for ( PhotoreceptorCell photoreceptorCell : this.memoryCells ) {
			merged.add( photoreceptorCell ) ;
		}

		return merged ;
	}

	public void saccade() {
		this.memoryCells = this.eye.getActivePhotoreceptorCells() ;
		this.eye.activeRetina() ;
	}

	public Map<PhotoreceptorCell , VisualEffect> visualEffect( float angle , float xTranslation ) {
		this.visualsEffects = new LinkedHashMap<>() ;
		AffineTransform matrix = this.getTransformationMatrix( angle , xTranslation ) ;

		for ( PhotoreceptorCell previousCell : this.memoryCells ) {
			PhotoreceptorCell currentPredictedCell = this.predictNextPositionFrom(
					previousCell ,
					matrix ) ;
			Map.Entry<PhotoreceptorCell , VisualEffect> visualEffect = this.visualEffectFromMemory(
					previousCell ,
					currentPredictedCell ) ;
			this.visualsEffects.put( visualEffect.getKey() , visualEffect.getValue() ) ;
		}

		for ( PhotoreceptorCell currentCell : this.eye.getActivePhotoreceptorCells() ) {
//			if ( !this.memoryCells.contains( currentCell ) ) {
//				this.visualsEffects.put( currentCell , VisualEffect.APPEAR ) ;
//			}
			if ( !this.visualsEffects.containsKey( currentCell ) ) { // OG
				this.visualsEffects.put( currentCell , VisualEffect.APPEAR ) ;
			}
		}
		return this.visualsEffects ;
	}

	private AffineTransform getTransformationMatrix( float angle , float x ) {
		AffineTransform matrix = new AffineTransform() ;
		matrix.rotate( angle ) ;
		matrix.translate( x , 0 ) ;
		return matrix ;
	}

	private PhotoreceptorCell predictNextPositionFrom( PhotoreceptorCell current ,
			AffineTransform transformation ) {
		PhotoreceptorCell prediction = current.clone() ;
		prediction.applyTransformation( transformation ) ;
		return prediction ;
	}

	private Map.Entry<PhotoreceptorCell , VisualEffect> visualEffectFromMemory(
			PhotoreceptorCell previousCell , PhotoreceptorCell currentPredictedCell ) {
		List<PhotoreceptorCell> activeCells = new ArrayList<>(
                this.eye.getActivePhotoreceptorCells()) ;

		if ( !activeCells.contains( currentPredictedCell ) ) {
			return new Entry<>(
                    //previousCell ,
                    currentPredictedCell,
                    VisualEffect.DISAPPEAR) ;
		} else {
			int indexOfCurrentCell = activeCells.indexOf( currentPredictedCell ) ;
			PhotoreceptorCell currentCell = activeCells.get( indexOfCurrentCell ) ;
			VisualEffect visualEffect = this.getVisualEffect( previousCell , currentCell ) ;
			return new Entry<>(currentCell, visualEffect) ;
		}
	}

	private VisualEffect getVisualEffect( PhotoreceptorCell previousCell ,
			PhotoreceptorCell currentCell ) {
		VisualEffect stimuli = VisualEffect.UNCHANGED ;
		float previousDistance = previousCell.distanceAccurateToTheBlock() ;
		float currentDistance = currentCell.distanceAccurateToTheBlock() ;
		if ( Math.abs( previousDistance - currentDistance ) < .01 ) {
			if ( currentDistance == Ernest.INFINITE )
				stimuli = VisualEffect.UNCHANGED ;
			else if ( Math.abs( ErnestUtils.polarAngle( currentCell.getBlockPosition() ) ) < .1f )
				stimuli = VisualEffect.CLOSER ;
			else if ( Math.abs( ErnestUtils.polarAngle( previousCell.getBlockPosition() ) ) < .1f )
				stimuli = VisualEffect.FARTHER ;
			else
				stimuli = VisualEffect.MOVE ;
		} else if ( previousDistance < Ernest.INFINITE && currentDistance < previousDistance ) {
			stimuli = VisualEffect.CLOSER ;
		} else if ( previousDistance == Ernest.INFINITE && currentDistance < Ernest.INFINITE ) {
			stimuli = VisualEffect.APPEAR ;
		} else if ( previousDistance < Ernest.INFINITE && currentDistance == Ernest.INFINITE ) {
			stimuli = VisualEffect.DISAPPEAR ;
		}

		return stimuli ;
	}

	public v3 getEventPosition(PhotoreceptorCell cell , VisualEffect stimuli ) {
		//float d = 0 ;
		v3 position = new v3() ;

		switch ( stimuli ) {
			case CLOSER:
			case APPEAR:
			case FARTHER:
			case MOVE:
			case DISAPPEAR:
				position = cell.getBlockPosition() ;
				//d = cell.distanceAccurateToTheBlock() ;
				//if ( d > 0 && d < Ernest.INFINITE )
				//	position.scale( Eye.DISTANCE_VISION / d ) ;
				break ;
			default:
				break ;
		}

		return position ;
	}
}
