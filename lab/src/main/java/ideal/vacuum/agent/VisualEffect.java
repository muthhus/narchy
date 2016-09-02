package ideal.vacuum.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 357 $
 */
public enum VisualEffect {
	APPEAR( "*" ) ,
	MOVE("="), // Moves in the visual field but remains at the same distance . OG
	CLOSER( "+" ) ,
	DISAPPEAR( "o" ) ,
	FARTHER( "-" ) , // Farther in angular distance
	UNCHANGED( "_" ) ;

	private final static Map<String , VisualEffect> BY_LABEL = new HashMap<>() ;
	private final String label ;

	static {
		for ( VisualEffect stimuli : VisualEffect.values() ) {
			BY_LABEL.put( stimuli.label , stimuli ) ;
		}
	}

	VisualEffect(String label) {
		this.label = label ;
	}

	public String getLabel() {
		return this.label ;
	}

	public static boolean isExist( String label ) {
		return BY_LABEL.containsKey( label ) ;
	}

	public static VisualEffect getByLabel( String label ) throws IllegalArgumentException {
		if ( BY_LABEL.containsKey( label ) ) {
			return BY_LABEL.get( label ) ;
		}

		throw new IllegalArgumentException() ;
	}
}
