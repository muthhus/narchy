package nars;

import nars.index.term.TermIndex;
import nars.nal.Deriver;
import nars.time.Time;
import nars.util.exe.Executioner;

import java.util.Random;

/**
 * Created by me on 12/27/16.
 */
abstract public class NARBuilder {

    public NAR get() {
        NAR n = new NAR(getTime(), getIndex(), getRandom(), getExec());
        n.setControl(getControl(n));
        return n;
    }

    abstract public Control getControl(NAR n);

    abstract public Executioner getExec();

    abstract public Time getTime();

    abstract public Deriver getDeriver();

    abstract public TermIndex getIndex();

    abstract public Random getRandom();/* {
        return new XorShift128PlusRandom(1);
    }*/

}
