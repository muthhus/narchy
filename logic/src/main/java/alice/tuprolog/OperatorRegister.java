package alice.tuprolog;

import java.util.concurrent.ConcurrentHashMap;

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
public class OperatorRegister extends ConcurrentHashMap<String, Operator> /*Castagna 06/2011*//**/ {
    //map of operators by name and type
    //key is the nameType of an operator (for example ":-xfx") - value is an Operator

    public OperatorRegister() {
        super(128, 0.9f);
    }

    public void addOperator(Operator op) {
        put(op.name + op.type, op);
    }

    public Operator getOperator(String name, String type) {
        return get(name + type);
    }

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
