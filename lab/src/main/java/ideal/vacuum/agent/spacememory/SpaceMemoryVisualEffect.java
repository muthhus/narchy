package ideal.vacuum.agent.spacememory;

import ideal.vacuum.agent.VisualEffect;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 362 $
 */
public enum SpaceMemoryVisualEffect {
	DEFAULT( "" , Color.WHITE ) ,
	APPEAR( VisualEffect.APPEAR.getLabel() , new Color(0xA0E000) ) ,
	MOVE( VisualEffect.MOVE.getLabel() , new Color(0x8CDAE1) ) ,
	CLOSER( VisualEffect.CLOSER.getLabel() , new Color(0x00c000) ) ,
	DISAPPEAR( VisualEffect.DISAPPEAR.getLabel() , new Color(0x808080) ) ,
	FARTHER( VisualEffect.FARTHER.getLabel() , new Color(0x93C1C6) ) ,
	UNCHANGED( VisualEffect.UNCHANGED.getLabel() , new Color(0xc0c0c0) ) ;
	
	private final String visualEffectLabel ;
	private final Color effectColor;

	private final static Map<String , SpaceMemoryVisualEffect> BY_VISUAL_EFFECT_LABEL = new HashMap<>() ;

	static {
		for ( SpaceMemoryVisualEffect smInteractions : SpaceMemoryVisualEffect.values() ) {
			BY_VISUAL_EFFECT_LABEL.put( smInteractions.visualEffectLabel , smInteractions ) ;
		}
	}

	SpaceMemoryVisualEffect(String visualEffectLabel, Color effectColor) {
		this.visualEffectLabel = visualEffectLabel ;
		this.effectColor = effectColor;
	}

	public static SpaceMemoryVisualEffect getSpaceMemoryVisualEffect(String visualEffectLabel ) {
		if ( BY_VISUAL_EFFECT_LABEL.containsKey( visualEffectLabel ) ) {
			return BY_VISUAL_EFFECT_LABEL.get( visualEffectLabel ) ;
		} else {
			return SpaceMemoryVisualEffect.DEFAULT ;
		}
	}

	public static boolean containVisualEffect( String interaction ) {
		for ( VisualEffect effect : VisualEffect.values() ) {
			if ( interaction.contains( effect.getLabel() ) ) {
				return true;
			}
		}
		return false;
	}

	public static String extractLeftVisualEffectLabel( String interaction ) {
		for ( char inter : interaction.toCharArray() ) {
			if ( VisualEffect.isExist( String.valueOf( inter ) ) ) {
				return String.valueOf( inter ) ;
			}
		}
		throw new RuntimeException( "Can't extract the visual effect : " + interaction ) ;
	}
	
	public Color getEffectColor() {
		return this.effectColor ;
	}
}
