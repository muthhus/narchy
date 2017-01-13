package nars.nar;

import jcog.data.random.XorShift128PlusRandom;
import nars.NAR;
import nars.conceptualize.DefaultConceptBuilder;
import nars.control.AlannControl;
import nars.index.term.TermIndex;
import nars.index.term.map.CaffeineIndex;
import nars.op.stm.STMTemporalLinkage;
import nars.time.Time;
import nars.util.exe.Executioner;
import nars.util.exe.MultiThreadExecutioner;
import nars.util.exe.SynchronousExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Created by me on 12/27/16.
 */
public interface NARBuilder {

    public static NAR newALANN(@NotNull Time time, int cores, int coreSize, int coreFires, int coreThreads, int auxThreads) {

        Executioner exe = auxThreads == 1 ? new SynchronousExecutor() {
            @Override public int concurrency() {
                return auxThreads + coreThreads;
            }
        } : new MultiThreadExecutioner(auxThreads, 1024 * auxThreads).sync(false);

        NAR n = new NAR(time,
                    new CaffeineIndex(new DefaultConceptBuilder(), 128 * 1024, false, exe),
                        //new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 512 * 1024, 1024 * 32, 3),
                    new XorShift128PlusRandom(1),
                    exe
        );

        new STMTemporalLinkage(n, 2);

        n.setControl(new AlannControl(n, cores, coreSize, coreFires, coreThreads));

        return n;
    }

    NAR get();

    //Control getControl(NAR n);
        //n.setControl(getControl(n));

    Executioner getExec();

    Time getTime();

    TermIndex getIndex();

    Random getRandom();/* {
        return new XorShift128PlusRandom(1);
    }*/

    class MutableNARBuilder implements NARBuilder {


        private Executioner exec = null;
        private Time time = null;
        private TermIndex index = null;
        private Random rng = null;

        @Override
        public NAR get() {
            NAR n = new NAR(getTime(), getIndex(), getRandom(), getExec());

            return n;
        }

        public MutableNARBuilder exec(Executioner exec) {
            this.exec = exec;
            return this;
        }

        public MutableNARBuilder time(Time time) {
            this.time = time;
            return this;
        }

        public MutableNARBuilder index(TermIndex index) {
            this.index = index;
            return this;
        }

        public MutableNARBuilder random(Random rng) {
            this.rng = rng;
            return this;
        }

        @Override
        public Executioner getExec() {
            return exec;
        }

        @Override
        public Time getTime() {
            return time;
        }

        @Override
        public TermIndex getIndex() {
            return index;
        }

        @Override
        public Random getRandom() {
            return rng;
        }
    }


}
