package jcog.event;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/**
 * Represents the active state of a topic stream
 */
abstract public class On<V> {

    public Topic<V> topic;

    On(Topic<V> t) {
        this.topic = t;
    }

    abstract public void off();

    protected void off(Consumer<V> x) {

        Topic<V> t = this.topic;
        if (topic!=null) { //TODO use atomic operation here
            topic = null;
            t.disable(x);
        }
    }

    public static class Strong<V> extends On<V> {

        public final Consumer<V> reaction;

        Strong(Topic<V> t, Consumer<V> o) {
            super(t);
            reaction = o;
            t.enable(o);
        }

        public void off() {
            off(reaction);
        }

        @Override
        public String toString() {
            return "On:" + topic + "->" + reaction;
        }
    }

    public static class Weak<V> extends On<V> implements Consumer<V> {

        public final WeakReference<Consumer<V>> reaction;


        Weak(Topic<V> t, Consumer<V> o) {
            super(t);
            reaction = new WeakReference<Consumer<V>>(o);
            t.enable(this);
        }

        @Override
        public void accept(V v) {
            Consumer<V> c = reaction.get();
            if (c != null) {
                c.accept(v);
            } else {
                //reference has been lost, so unregister:
                off();
            }
        }

        public void off() {

            off(this);

        }

//        @Override
//        public String toString() {
//            return "On.weak:" + topic + "->" + reaction.get();
//        }
    }

}
