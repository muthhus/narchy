package jcog.event;

import jcog.list.FasterList;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**  arraylist implementation, thread safe */
public class ListTopic<V> extends FasterList<Consumer<V>> implements Topic<V> {

    static final Consumer[] EMPTY_CONSUMER_ARRAY = new Consumer[0];

    private Consumer[] copy = EMPTY_CONSUMER_ARRAY;

    public ListTopic() {
        super();
    }

    @Override
    public final void emit(V x) {
        final Consumer[] cc = this.copy;
        for (Consumer c : cc)
            c.accept(x);
    }

    @Override
    public void emitAsync(V x, Executor executorService) {
        final Consumer[] cc = this.copy;
        for (Consumer c : cc)
            executorService.execute(()->c.accept(x));
    }

    @Override public final synchronized void enable(Consumer<V> o) {
        if(this.add(o)) {
            commit();
        }
    }

    @Override public final synchronized void disable(Consumer<V> o) {
        if(this.remove(o)) {
            commit();
        }
    }

    void commit() {
        if (size == 0)
            copy = EMPTY_CONSUMER_ARRAY;
        else {
            copy = toArray(new Consumer[size]);
        }
    }



}