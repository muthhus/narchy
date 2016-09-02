package ideal.vacuum.agent.spacememory;

import ideal.vacuum.Environment;
import ideal.vacuum.agent.TactileEffect;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 350 $
 */
public enum SpaceMemoryTactileEffect {
	DEFAULT( "" , Color.WHITE ) ,
	FALSE( TactileEffect.FALSE.getLabel() , Color.RED ) ,
	TRUE( TactileEffect.TRUE.getLabel() , Color.WHITE ) ,
	BRICK( TactileEffect.BRICK.getLabel() , Color.WHITE ) ,
	ALGA( TactileEffect.ALGA.getLabel() , Environment.FISH1 ) ,
	FOOD( TactileEffect.FOOD.getLabel() , Environment.FISH1 ) ;
	
	private final String tactileEffectLabel ;
	private final Color effectColor;

	private final static Map<String , SpaceMemoryTactileEffect> BY_TACTILE_EFFECT_LABEL = new HashMap<>() ;

	static {
		for ( SpaceMemoryTactileEffect smInteractions : SpaceMemoryTactileEffect.values() ) {
			BY_TACTILE_EFFECT_LABEL.put( smInteractions.tactileEffectLabel , smInteractions ) ;
		}
	}

	SpaceMemoryTactileEffect(String tactileEffectLabel, Color effectColor) {
		this.tactileEffectLabel = tactileEffectLabel ;
		this.effectColor = effectColor;
	}

	public static SpaceMemoryTactileEffect getSpaceMemoryTactileEffect(String tactileEffectLabel ) {
		if ( BY_TACTILE_EFFECT_LABEL.containsKey( tactileEffectLabel ) ) {
			return BY_TACTILE_EFFECT_LABEL.get( tactileEffectLabel ) ;
		} else {
			return SpaceMemoryTactileEffect.DEFAULT ;
		}
	}

	public static boolean containTactileEffect( String interaction ) {
		for ( TactileEffect effect : TactileEffect.values() ) {
			if ( interaction.contains( effect.getLabel() ) ) {
				return true;
			}
		}
		return false;
	}
	
	public static String extractTactileEffectLabel( String interaction ) {
		for ( char inter : interaction.toCharArray() ) {
			if ( TactileEffect.isExist( String.valueOf( inter ) ) ) {
				return String.valueOf( inter ) ;
			}
		}
		throw new RuntimeException( "Can't extract the tactile effect : " + interaction ) ;
	}
		
	public Color getEffectColor() {
		return this.effectColor ;
	}
}
