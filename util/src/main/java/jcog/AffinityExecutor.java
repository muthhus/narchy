package jcog;

import net.openhft.affinity.AffinityLock;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

/**
 * uses affinity locking to pin new threads to their own unique, stable CPU core/hyperthread etc
 */
public class AffinityExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(AffinityExecutor.class);

    public final Collection<Thread> threads = new CopyOnWriteArraySet<>();
    public final String id;

    public AffinityExecutor(String id) {
        this.id = id;
    }


    final class AffinityThread extends Thread {

        Runnable cmd;
        public AffinityThread(@NotNull String name, Runnable cmd) {
            super(name);
            this.cmd = cmd;
        }

        @Override
        public void run() {

            threads.add( this );

            try (AffinityLock lock = AffinityLock.acquireLock()) {
                cmd.run(); //avoid virtual call to super etc
            }

            threads.remove(this);

        }
    }



    @Override
    public final void execute(Runnable command) {

        final Thread thread = new AffinityThread(id + ":" + threads.size(), command);

        thread.start();

    }

//        @Override
//        public String toString()
//        {
//            return "BasicExecutor{" +
//                    "threads=" + dumpThreadInfo() +
//                    '}';
//        }

    private String dumpThreadInfo() {
        final StringBuilder sb = new StringBuilder();

        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        for (Thread t : threads) {
            ThreadInfo threadInfo = threadMXBean.getThreadInfo(t.getId());
            sb.append("{");
            sb.append("name=").append(t.getName()).append(",");
            sb.append("id=").append(t.getId()).append(",");
            sb.append("state=").append(threadInfo.getThreadState()).append(",");
            sb.append("lockInfo=").append(threadInfo.getLockInfo());
            sb.append("}");
        }

        return sb.toString();
    }

    public long[] threadIDs() {
        return threads.stream().mapToLong(t -> t.getId()).toArray();
    }
}
