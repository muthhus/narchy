//package jcog.version;
//
//import jcog.data.pool.DequePool;
//import org.jetbrains.annotations.NotNull;
//
///**
// * Created by me on 5/9/16.
// */
//public final class intDequePool extends DequePool<int[]> {
//    private final int stackLimit;
//
//    public intDequePool(int capacity, int stackLimit) {
//        super(capacity);
//        this.stackLimit = stackLimit;
//    }
//
//    @NotNull
//    @Override
//    public int[] create() {
//        return new int[stackLimit];
//    }
//}
