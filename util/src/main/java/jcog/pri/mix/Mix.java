package jcog.pri.mix;

import jcog.pri.Prioritized;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * mixes inputs from different identified sources in different amounts
 * @K source identifier
 * @P type of mixable content
 */
public class Mix<K, P extends Prioritized>  {


    public final Map<K, PSink> streams = new ConcurrentHashMap();
        //TODO use a WeakValue map?

    final Consumer<P> target;

    public Mix(Consumer<P> target) {
        this.target = target;
    }

    /** gets or creates a mix stream for the given key */
    public PSink stream(K x) {
        return streams.computeIfAbsent(x, xx -> new PSink(xx, target));
    }

    /** reset gathered statistics */
    public void commit() {
        streams.forEach((k,s)->s.commit());
    }

}
