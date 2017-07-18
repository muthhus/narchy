package nars;

import jcog.random.XorShift128PlusRandom;
import nars.conceptualize.ConceptBuilder;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.BasicTermIndex;
import nars.index.term.TermIndex;
import nars.index.term.map.CaffeineIndex;
import nars.nar.exe.BufferedExecutioner;
import nars.nar.exe.Executioner;
import nars.time.CycleTime;
import nars.time.Time;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Supplier;

/** NAR builder */
public class NARS {

    public NAR get() {
        return new NAR(index.get(), exe.get(), time, rng.get(), concepts.get());
    }

    private Supplier<TermIndex> index;

    private Time time;

    private Supplier<Executioner> exe;

    private Supplier<Random> rng;

    private Supplier<ConceptBuilder> concepts;


    public NARS index(@NotNull TermIndex concepts) {
        this.index = () -> concepts;
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

    public NARS concepts(ConceptBuilder cb) {
        this.concepts = () -> cb;
        return this;
    }

    /** defaults */
    public NARS() {

        index = () ->
            //new CaffeineIndex(new DefaultConceptBuilder(), 8*1024, 16*1024, null)
            new BasicTermIndex(2 * 1024 );

        time = new CycleTime();

        exe = () ->new BufferedExecutioner(128, 32, 0.2f);

        rng = () -> new XorShift128PlusRandom(1);

        concepts = () -> new DefaultConceptBuilder();

    }

    public static NAR single() {
        return new NARS().get();
    }

    public static NAR threadSafe() {
        return new NARS().index(new CaffeineIndex(-1)).get();
    }
}
