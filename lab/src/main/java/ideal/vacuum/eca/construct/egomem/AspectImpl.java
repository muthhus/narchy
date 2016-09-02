package ideal.vacuum.eca.construct.egomem;



import ideal.vacuum.ernest.ErnestUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * A visual aspect of a phenomenon type.
 * @author Olivier
 */
public class AspectImpl implements Aspect {

	private static final Map<String , Aspect> ASPECTS = new HashMap<>() ;
	private final int displayCode;
//	private int value;

	/**
	 * @param displayCode this aspect's label
	 * @return The aspect
	 */
	public static Aspect createOrGet(int displayCode){
		String key = ErnestUtils.hexColor(displayCode);
		if (!ASPECTS.containsKey(key))
			ASPECTS.put(key, new AspectImpl(displayCode));
		return ASPECTS.get(key);
	}
	
	private AspectImpl(int displayCode){
		this.displayCode = displayCode;
	}
	
	@Override
    public int getCode() {
		return displayCode;
	}

	/**
	 * Aspects are equal if they have the same display code. 
	 */
	public boolean equals(Object o)
	{
		boolean ret = false;
		
		if (o == this)
			ret = true;
		else if (o == null)
			ret = false;
		else if (!o.getClass().equals(this.getClass()))
			ret = false;
		else
		{
			Aspect other = (Aspect)o;
			ret = (other.getCode() == this.displayCode);
		}
		
		return ret;
	}
	
	public String toString(){
		return ErnestUtils.hexColor(this.displayCode);
	}

//	public void setValue(int value) {
//		this.value = value;
//	}
//
//	public int getValue() {
//		return this.value;
//	}

}
