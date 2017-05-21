package nars.util.exe;


import jcog.Util;
import nars.NAR;
import nars.Narsese;
import nars.nar.Default;
import nars.task.ITask;
import nars.task.NALTask;
import nars.time.CycleTime;
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.RoaringBitmap;

import static nars.Op.*;

/** instruments tasks for tracking causal factors, as a precursor for full feedback system */
public class InstrumentedSynchronousExecutor extends BufferedSynchronousExecutor {

    public class InstrumentedTask extends ProxyTask {

        public String log = "";
        final RoaringBitmap cls;

        public InstrumentedTask(ITask x, InstrumentedTask parent) {
            super(x);

            cls = classify(this);
            if (parent!=null)
                learn(parent, this);

            StackWalker.StackFrame callee = StackWalker.getInstance().walk(s ->
                    s.skip(3).findFirst().get()); //limit(3) .collect(Collectors.toList()
            log = callee.toString();
        }

        private void learn(InstrumentedTask cause, InstrumentedTask effect) {
            float u = utility(effect);
            cause.cls.forEach((int row) -> {
                effect.cls.forEach((int col) -> {
                    //matrix.put(...
                });
            });
        }

        private float utility(InstrumentedTask instrumentedTask) {
            return 0;

        }


        private RoaringBitmap classify(InstrumentedTask t) {
            final RoaringBitmap b = new RoaringBitmap();
            Object tt = t.key();
            if (tt instanceof NALTask) {
                NALTask n = (NALTask) tt;


                switch (n.punc) {
                    case BELIEF: b.add(0);
                        break;
                    case GOAL: b.add(1);
                        break;
                    case QUESTION: b.add(2); break;
                    case QUEST: b.add(3); break;
                }

                if (n.isBeliefOrGoal()) {
                    int conf = Util.bin(n.conf(), 5);
                    b.add(4 + conf);
                    int freq = Util.bin(n.conf(), 5);
                    b.add(4 + 5 + freq);
                }

            }
            return b;
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


    @Override public boolean run(@NotNull ITask x) {
        InstrumentedTask y = new InstrumentedTask(x, null);
        return super.run(y);
    }

    protected void run(InstrumentedTask parent, ITask x) {
        InstrumentedTask y = new InstrumentedTask(x, parent);
        super.run(y);
    }

    @Override
    protected void actuallyRun(@NotNull ITask x) {

        ITask[] next = x.run(nar);
        if (next!=null) {
            for (ITask z : next)
                run((InstrumentedTask)x, z);
        }

    }


    public static void main(String[] args) throws Narsese.NarseseException {
        Default n = new Default(128, new Default.DefaultTermIndex(128),new CycleTime(),
                new InstrumentedSynchronousExecutor());
        n.input("a:b.");
        n.input("b:c.");
        n.run(16);
    }
}
