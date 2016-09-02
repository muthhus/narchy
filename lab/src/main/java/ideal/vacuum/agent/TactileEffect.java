package ideal.vacuum.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 285 $
 */
public enum TactileEffect {
	FALSE( "f" ) ,
	TRUE( "t" ) ,
	BRICK( "b" ) ,
	ALGA( "a" ) ,
	FOOD( "a" ) ;

	private final static Map<String , TactileEffect> BY_LABEL = new HashMap<>() ;
	private final String label ;

	static {
		for ( TactileEffect stimuli : TactileEffect.values() ) {
			BY_LABEL.put( stimuli.label , stimuli ) ;
		}
	}

	TactileEffect(String label) {
		this.label = label ;
	}

	public String getLabel() {
		return this.label ;
	}

	public static boolean isExist( String label ) {
		return BY_LABEL.containsKey( label ) ;
	}

	public static TactileEffect getByLabel( String label ) throws IllegalArgumentException {
		if ( BY_LABEL.containsKey( label ) ) {
			return BY_LABEL.get( label ) ;
		}

		throw new IllegalArgumentException() ;
	}
}
