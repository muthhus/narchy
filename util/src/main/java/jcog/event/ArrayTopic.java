package jcog.event;

import jcog.list.ArraySharingList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * single-thread synchronous (in-thread) event emitter with direct array access
 */
public class ArrayTopic<V> extends ArraySharingList<Consumer<V>> implements Topic<V> {

    private static final Logger logger = LoggerFactory.getLogger(ArrayTopic.class);

    //TODO extract this to Topics and a graph metamodel of the events

    //static Map<String, Topic<?>> topics = new HashMap();


//    public static void register(Topic<?> t) {
//        topics.put(t.name(), t);
//    }
//    public static void unregister(Topic<?> t) {
//        topics.remove(t.name());
//    }

//    static AtomicInteger topicSerial = new AtomicInteger();
//    static int nextTopicID() {
//        return topicSerial.incrementAndGet();
//    }

//    final String id;

//    @Override
//    public String name() {
//        return id;
//    }

//    @Override
//    public String toString() {
//        return super.toString();
//    }

//    public DefaultTopic() {
//        this(Integer.toString(nextTopicID(), 36));
//    }


    public ArrayTopic() {
        super(Consumer[]::new);
        //this.id = id;
        //register(this);
    }

     @Override
    public final void emit(Object /* V */ arg) {
        Consumer[] vv = getCachedNullTerminatedArray();
        if (vv == null) return;

        for (int i = 0; ; ) {
            Consumer c = vv[i++];
            if (c == null)
                break; //null terminator hit

            c.accept(arg);
        }
    }


    public final void emitSafe(Object /* V */ arg) {
        Consumer[] vv = getCachedNullTerminatedArray();
        if (vv == null) return;

        for (int i = 0; ; ) {
            Consumer c = vv[i++];
            if (c == null)
                break; //null terminator hit

            try {  c.accept(arg);  } catch (Exception e) {  logger.warn("{}: {}", c, e); }
        }
    }

    @Override
    public void emitAsync(V arg, Executor exe) {
        Consumer[] vv = getCachedNullTerminatedArray();
        if (vv != null) {
            for (int i = 0; ; ) {
                Consumer c = vv[i++];
                if (c == null)
                    break; //null terminator hit

                exe.execute(()-> {
                    //try {  c.accept(arg);  } catch (Exception e) {  logger.warn("{}: {}", c, e); }
                    c.accept(arg);
                });
            }
        }

    }




    /** called by On<V> instances */
    @Override public void enable(Consumer<V> o) {
        //assert(!contains(o))...
        add(o);
    }

    /** called by On<V> instances */
    @Override public final void disable(Consumer<V> o) {
        if (!remove(o))
            throw new RuntimeException(this + " has not " + o);
    }


    @Override
    public void delete() {
        //unregister(this);
        data.clear();
    }

}
