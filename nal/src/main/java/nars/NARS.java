package nars;

import jcog.random.XorShift128PlusRandom;
import nars.index.term.BasicTermIndex;
import nars.index.term.TermIndex;
import nars.time.CycleTime;
import nars.time.Time;
import nars.nar.exe.Executioner;
import nars.nar.exe.BufferedExecutioner;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Supplier;

import static jcog.Texts.n2;
import static jcog.Texts.n4;

/** NAR builder */
public class NARS {

    public NAR get() {
        return new NAR(concepts.get(), exe.get(), time, rng.get());
    }

    private Supplier<TermIndex> concepts;

    private Time time;

    private Supplier<Executioner> exe;

    private Supplier<Random> rng;


    public NARS index(@NotNull TermIndex concepts) {
        this.concepts = () -> concepts;
        return this;
    }

    public NARS time(@NotNull Time time) {
        this.time = time;
        return this;
    }

    public NARS exe(Executioner exe) {
        this.exe = () -> exe;
        return this;
    }


    /** defaults */
    public NARS() {

        concepts = () ->
            //new CaffeineIndex(new DefaultConceptBuilder(), 8*1024, 16*1024, null)
            new BasicTermIndex(2 * 1024 );

        time = new CycleTime();

        exe = () ->new BufferedExecutioner(128, 64, 0.1f);

        rng = () -> new XorShift128PlusRandom(1);

    }

}
