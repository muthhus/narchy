package nars.index.util;

import nars.Op;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;


public class TermContainerToOpMap<X>
        /*extends ConcurrentHashMap<Op,X>  */
        extends AtomicReferenceArray<X>
        implements Comparable<TermContainerToOpMap> {

    public final TermContainer id;

    public final static int CAPACITY = Op.values().length - (4);

    public TermContainerToOpMap(TermContainer id) {
        super(CAPACITY);
        this.id = id;
    }

    @Override
    public String toString() {
        return id + ":" + super.toString();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return id.equals(o);
    }


    @Override
    public int compareTo(@NotNull TermContainerToOpMap o) {
        throw new UnsupportedOperationException("TODO");
    }

    public void forEach(Consumer<X> each) {
        for (int i = 0; i < CAPACITY; i++) {
            each.accept(get(i));
        }
    }
}
