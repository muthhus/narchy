package ideal.vacuum.agent;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 377 $
 */
public enum Move {
	MOVE_FORWARD( ">" ) ,
	MOVE_BACKWARD( "<" ) ,
	TURN_LEFT( "^" ) ,
	TURN_RIGHT( "v" ) ,
	TOUCH( "-" ) ,
	TOUCH_RIGHT( "\\" ) ,
	TOUCH_LEFT( "/" ) ;

	private final static Map<String , Move> BY_LABEL = new HashMap<>() ;
	private final String label ;

	static {
		for ( Move schema : Move.values() ) {
			BY_LABEL.put( schema.label , schema ) ;
		}
	}

	Move(String label) {
		this.label = label ;
	}

	public String getLabel() {
		return this.label ;
	}

	public static boolean isExist( String label ) {
		return BY_LABEL.containsKey( label ) ;
	}

	public static Move getByLabel( String interactionLabel ) throws IllegalArgumentException {
		
		// We assume that the move is given by the first character of a primitive interaction label.
		String moveLabel = interactionLabel.substring(0,1); // OG
		if ( BY_LABEL.containsKey( moveLabel ) ) {
			return BY_LABEL.get( moveLabel ) ;
		}
		
		System.out.println("Illegal move label: " + moveLabel); // OG
		throw new IllegalArgumentException() ;
	}
}
