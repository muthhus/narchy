package nars.nar;

import nars.NAR;
import nars.index.term.TermIndex;
import nars.time.Time;
import nars.util.exe.Executioner;

import java.util.Random;

/**
 * Created by me on 12/27/16.
 */
public interface NARBuilder {

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
