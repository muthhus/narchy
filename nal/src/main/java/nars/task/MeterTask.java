//package nars.task;
//
//import nars.Memory;
//import nars.term.Compound;
//import nars.term.Termed;
//import nars.util.event.Active;
//import org.jetbrains.annotations.NotNull;
//
///**
// * Task which is specifically for collecting statistics about
// * its budget dynamics across time and reacting to
// * lifecycle events which are empty stubs in its
// * super-classes
// *
// */
//public abstract class MeterTask extends MutableTask {
//
//    private final Active active = new Active();
//
//    public MeterTask(@NotNull Termed<Compound> c) {
//        super(c);
//    }
//
//    @Override
//    protected void onInput(@NotNull Memory memory) {
//        active.add(
//                memory.eventFrameStart.on((n) -> onFrame(memory))
//        );
//    }
//
//    abstract void onFrame(Memory memory);
//
//}
