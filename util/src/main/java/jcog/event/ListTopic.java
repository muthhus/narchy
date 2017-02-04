package jcog.event;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/** single-thread simple ArrayList impl */
public class ListTopic<V> extends ArrayList<Consumer<V>> implements Topic<V> {

    public ListTopic() {
        super();
    }

    @Override
    public final void emit(V arg) {
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            this.get(i).accept(arg);
        }

    }

    @Override
    public final On on(Consumer<V> o) {
        On d = new On(this, o);
        this.add(o);
        return d;
    }

    @Override
    public final void off(On<V> o) {
        if(!this.remove(o.reaction)) {
            throw new RuntimeException(this + " has not " + o.reaction);
        }
    }

    @Override
    public void emitAsync(V v, ExecutorService executorService) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        clear();
    }
}