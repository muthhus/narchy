package nars.util.event;

import nars.Memory;
import nars.NAR;
import org.jetbrains.annotations.NotNull;

/**
 * Class whch manages the registration and unregistration of event handlers
 * with an EventEmitter. it may be enabled and disabled repeatedly with
 * different event classes as selector keys for event bus messages.
 */
public abstract class NARReaction extends AbstractReaction<Class,Object[]> {



    protected NARReaction(@NotNull NAR n, Class... events) {
        this(n.memory.event, true, events);
    }

    protected NARReaction(@NotNull Memory m, boolean active, Class... events) {
        this(m.event, active, events);
    }

    protected NARReaction(@NotNull Memory m, Class... events) {
        this(m.event, true, events);
    }
    protected NARReaction(EventEmitter n, Class... events) {
        this(n, true, events);
    }

    protected NARReaction(@NotNull NAR n, boolean active, Class... events) {
        this(n.memory.event, active, events);
    }

    protected NARReaction(EventEmitter source, boolean active, Class... events) {
        super(source, active, events);
    }



}
