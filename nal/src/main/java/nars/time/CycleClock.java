//package nars.time;
//
//import nars.Memory;
//import nars.util.event.On;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.function.Consumer;
//
///** increments time on each frame */
//public class CycleClock implements Clock, Consumer<Memory> {
//
//
//    long t;
//    @Nullable
//    private Memory memory;
//    @Nullable
//    private On handler = null;
//
//    @Override
//    public void clear(@Nullable Memory m) {
//
//        if (this.memory!=null && this.memory!=m) {
//            handler.off();
//            handler = null;
//        }
//
//        t = 0;
//
//        this.memory = m;
//
//        if (m!=null) {
//            handler = m.eventCycleEnd.on(this);
//        }
//    }
//
//    @Override
//    public final long time() {
//        return t;
//    }
//
//
//    @Override
//    public final void preFrame() {
//    }
//
//    @Override
//    public long elapsed() {
//        return 1;
//    }
//
//
//
//    @NotNull
//    @Override
//    public String toString() {
//        return Long.toString(t);
//    }
//
//    /** called per-cycle */
//    @Override public void accept(Memory memory) {
//        t++;
//    }
// }
