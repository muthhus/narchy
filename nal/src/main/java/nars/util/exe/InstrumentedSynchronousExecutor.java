package nars.util.exe;


import nars.NAR;
import nars.Narsese;
import nars.nar.Default;
import nars.task.ITask;
import nars.time.CycleTime;
import org.jetbrains.annotations.NotNull;

/** instruments tasks for tracking causal factors, as a precursor for full feedback system */
public class InstrumentedSynchronousExecutor extends BufferedSynchronousExecutor {

    public static class InstrumentedTask extends ProxyTask {

        public String log = "";

        public InstrumentedTask(ITask x) {
            super(x);

            StackWalker.StackFrame callee = StackWalker.getInstance().walk(s ->
                    s.skip(3).findFirst().get()); //limit(3) .collect(Collectors.toList()
            log = callee.toString();
        }

        @Override
        public ITask merge(ITask incoming) {
            ITask next = super.merge(incoming);
            assert(next==this);
            if (incoming instanceof InstrumentedTask)
                log += " " + ((InstrumentedTask) incoming).log;
            return this;
        }

        @Override
        public ITask[] run(NAR n) {
            return super.run(n);
        }

        @Override
        public String toString() {
            return the.toString();
        }

        public void print() {
            System.out.println(toString());
            System.out.println("\t" + log);
        }
    }

    @Override
    public boolean run(@NotNull ITask x) {
        InstrumentedTask y = new InstrumentedTask(x);
        y.print();
        return super.run(y);
    }

    public static void main(String[] args) throws Narsese.NarseseException {
        Default n = new Default(128, new Default.DefaultTermIndex(128),new CycleTime(),
                new InstrumentedSynchronousExecutor());
        n.input("a:b.");
        n.input("b:c.");
        n.run(16);
    }
}
