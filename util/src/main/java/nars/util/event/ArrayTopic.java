package nars.util.event;

import nars.util.list.ArraySharingList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
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
        if (vv != null) {
            for (int i = 0; ; ) {
                Consumer c = vv[i++];
                if (c != null) {
                    try {  c.accept(arg);  } catch (Exception e) {  logger.warn("{}: {}", c, e); }
                } else
                    break; //null terminator hit
            }
        }
    }

    @Override
    public void emitAsync(V arg, ExecutorService exe) {
        Consumer[] vv = getCachedNullTerminatedArray();
        if (vv != null) {
            for (int i = 0; ; ) {
                Consumer c = vv[i++];
                if (c != null) {
                    exe.submit(()-> {
                        try {  c.accept(arg);  } catch (Exception e) {  logger.warn("{}: {}", c, e); }
                    });
                } else
                    break; //null terminator hit
            }
        }

    }

    @Override
    public final On on(Consumer<V> o) {
        On<V> d = new On<>(this, o);
        add(o);
        return d;
    }


    @Override
    public final void off(On<V> o) {
        if (!remove(o.reaction))
            throw new RuntimeException(this + " has not " + o.reaction);
    }


    @Override
    public void delete() {
        //unregister(this);
        data.clear();
    }

}
