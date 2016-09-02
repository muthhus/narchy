package ideal.vacuum.eca.construct.egomem;

/**
 * A visual aspect of a phenomenon type.
 * @author Olivier
 */
public interface Aspect 
{

	/** Predefined aspects */
    Aspect MOVE = AspectImpl.createOrGet(0xFFFFFF);
	//public static Aspect WALL = AspectImpl.createOrGet(0x646464);
    Aspect BUMP = AspectImpl.createOrGet(0xFF0000);
	Aspect CONSUME = AspectImpl.createOrGet(0x9680FF);
	
	/**
	 * @return The area's label
	 */
    int getCode();
	
//	public void setValue(int value);
//	
//	public int getValue();
}
