package alice.tuprolog;

/**
 * Register for operators
 * Cashes operator by name+type description.
 * Retains insertion order as LinkedHashSet.
 * <p/>
 * todo Not 100% sure if 'insertion-order-priority' should be completely replaced
 * by the explicit priority given to operators.
 *
 * @author ivar.orstavik@hist.no
 */
public class OperatorRegister  /*Castagna 06/2011*//**/ {
    //map of operators by name and type
    //key is the nameType of an operator (for example ":-xfx") - value is an Operator


//        /*Castagna 06/2011*/
//        @Override
//        public Object clone() {
//        	OperatorRegister or =
//        	Iterator<Operator> ior = or.iterator();
//        	or.nameTypeToKey = new HashMap<>();
//        	while(ior.hasNext()) {
//        		Operator o = ior.next();
//        		or.nameTypeToKey.put(o.name + o.type, o);
//        	}
//        	return or;
//        }
//        /**/
}
