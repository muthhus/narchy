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

    @Override public final void enable(Consumer<V> o) {
        if(!this.add(o)) {
            throw new RuntimeException(this + " not added " + o);
        }
    }

    @Override public final void disable(Consumer<V> o) {
        if(!this.remove(o)) {
            throw new RuntimeException(this + " not removed " + o);
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