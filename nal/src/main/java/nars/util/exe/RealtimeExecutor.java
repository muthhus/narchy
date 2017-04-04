//package nars.util.exe;
//
//import com.ifesdjeen.timer.HashedWheelTimer;
//import com.ifesdjeen.timer.WaitStrategy;
//import nars.NAR;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * https://github.com/ifesdjeen/hashed-wheel-timer
// */
//public class RealtimeExecutor extends MultiThreadExecutor {
//
//    public final HashedWheelTimer timer;
//
//    public RealtimeExecutor() {
//        super(-1, 1024, true);
//        timer = new HashedWheelTimer(
//                TimeUnit.MILLISECONDS.toNanos(10),
//                8,
//                new WaitStrategy.SleepWait()) {
//            @Override
//            public void execute(Runnable command) {
//                RealtimeExecutor.this.execute(command);
//            }
//        };
//
//    }
//
//
//
//}
