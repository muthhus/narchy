package nars.util.exe;


import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.nar.Default;
import nars.task.ITask;
import nars.task.util.InvalidTaskException;
import nars.term.util.InvalidTermException;
import nars.time.CycleTime;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

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
        public void merge(ITask incoming) {
            super.merge(incoming);
            if (incoming instanceof InstrumentedTask)
                log += " " + ((InstrumentedTask) incoming).log;
        }

        @Override
        public void run(NAR n) throws Concept.InvalidConceptException, InvalidTermException, InvalidTaskException {
            super.run(n);
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
