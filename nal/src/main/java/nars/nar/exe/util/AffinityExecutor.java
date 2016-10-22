package nars.nar.exe.util;

import net.openhft.affinity.AffinityLock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;

/**
 * uses affinity locking to pin new threads to their own unique, stable CPU core/hyperthread etc
 */
public class AffinityExecutor implements Executor {

    public final Collection<Thread> threads = new ConcurrentLinkedDeque();
    public final String id;

    public AffinityExecutor(String id) {
        this.id = id;
    }

    static final class AffinityThread extends Thread {

        Runnable cmd;
        public AffinityThread(String name, Runnable cmd) {
            super(name);
            this.cmd = cmd;
        }

        @Override
        public void run() {
            try (AffinityLock al = AffinityLock.acquireLock()) {
                cmd.run(); //avoid virtual call to super etc
            }
        }
    }



    @Override
    public final void execute(Runnable command) {

        final Thread thread = new AffinityThread(id + ":" + threads.size(), command);


        threads.add(thread);


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
