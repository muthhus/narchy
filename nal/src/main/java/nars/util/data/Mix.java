package nars.util.data;

import jcog.bag.Prioritized;
import jcog.data.FloatParam;
import nars.budget.Budgeted;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * mixes inputs from different identified sources in different amounts
 * @K source identifier
 * @P type of mixable content
 */
public class Mix<K, P extends Budgeted>  {

    public final Map<K, FloatParam> _gain = new ConcurrentHashMap();
    final static float MAX_GAIN = 1f;

    public Stream<P> input(K source, Stream<P> p) {
        float g = gain(source);
        if (g <= 0)
            return Stream.empty();
        else if (g==1f) //TODO epsilon?
            return p; //unchanged
        else
            return p.peek(x -> x.budget().priMult(g));
    }

    public Mix gain(K k, float g) {
        _gain.computeIfAbsent(k, (kk)->new FloatParam(0f, 0f, MAX_GAIN)).setValue(g);
        return this;
    }

    public float gain(K k) {
        FloatParam p = _gain.get(k);
        if (p!=null)
            return p.floatValue();
        return 0f;
    }

}
