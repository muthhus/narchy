package nars;

import jcog.list.FasterList;
import jcog.random.XorShift128PlusRandom;
import nars.concept.builder.ConceptBuilder;
import nars.concept.builder.DefaultConceptBuilder;
import nars.control.BatchActivate;
import nars.control.Derivation;
import nars.derive.PrediTerm;
import nars.derive.PrediTrie;
import nars.derive.rule.PremiseRuleSet;
import nars.exe.Exec;
import nars.exe.MultiExec;
import nars.exe.SynchExec;
import nars.index.term.BasicTermIndex;
import nars.index.term.PatternIndex;
import nars.index.term.TermIndex;
import nars.index.term.map.CaffeineIndex;
import nars.op.stm.STMLinkage;
import nars.time.CycleTime;
import nars.time.RealTime;
import nars.time.Time;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * NAR builder
 */
public class NARS {

    public NAR get() {
        NAR n = new NAR(index.get(), exe.get(), time, rng.get(), concepts.get(), deriver);
        init(n);
        after.forEach(x -> x.accept(n));
        n.time.synch(n);
        return n;
    }

    /**
     * subclasses may override this to configure newly constructed NAR's
     */
    protected void init(NAR n) {

    }

    protected Supplier<TermIndex> index;

    protected Time time;

    protected Supplier<Exec> exe;

    protected Supplier<Random> rng;

    protected Supplier<ConceptBuilder> concepts;

    protected Function<NAR, PrediTerm<Derivation>> deriver;

    /**
     * applied in sequence as final step before returning the NAR
     */
    protected final List<Consumer<NAR>> after = new FasterList(0);


    public NARS index(@NotNull TermIndex concepts) {
        this.index = () -> concepts;
        return this;
    }

    public NARS time(@NotNull Time time) {
        this.time = time;
        return this;
    }

    public NARS exe(Exec exe) {
        this.exe = () -> exe;
        return this;
    }

    public NARS concepts(ConceptBuilder cb) {
        this.concepts = () -> cb;
        return this;
    }

    public NARS deriver(String... rules) {

        deriver = (n) -> (PrediTrie.the(
                new PremiseRuleSet(
                        new PatternIndex(), n, rules
                )));

        return this;
    }

    public NARS deriver(Function<NAR, PrediTerm<Derivation>> dBuilder) {
        this.deriver = dBuilder;
        return this;
    }

    public NARS deriver(PrediTerm<Derivation> d) {
        return deriver((nar) -> d);
    }

    /**
     * defaults
     */
    public NARS() {

        index = () ->
                //new CaffeineIndex(new DefaultConceptBuilder(), 8*1024, 16*1024, null)
                new BasicTermIndex(1 * 256);

        time = new CycleTime();

        exe = () -> new SynchExec(24, 4);

        rng = () -> new XorShift128PlusRandom(1);

        concepts = DefaultConceptBuilder::new;

        deriver = nars.control.Deriver.getDefault(8);
    }

    /**
     * temporary, disposable NAR. safe for single-thread access only.
     * full NAL8 with STM Linkage
     */
    public static NAR tmp() {
        return tmp(8);
    }


    /**
     * temporary, disposable NAR. useful for unit tests or embedded components
     * safe for single-thread access only.
     *
     * @param nal adjustable NAL level. level >= 7 include STM (short-term-memory) Linkage plugin
     */
    public static NAR tmp(int nal) {
        return new Default(nal, false).get();
    }

    /**
     * single-thread, limited to NAL6 so it should be more compact than .tmp()
     */
    public static NAR tmpEternal() {
        return new Default(6, false).get();
    }

    /**
     * single thread but for multithread usage:
     * unbounded soft reference index
     */
    public static NAR threadSafe() {
        return new Default(8, true).get();
    }

    public NARS nal(int nal) {
        after.add((x) -> {
            x.nal(nal);
        });
        return this;
    }

    public NARS threadable() {
        index = () -> new CaffeineIndex(64 * 1024 /*HACK */);
        return this;
    }

    public static NARS realtime() {
        return realtime(1);
    }

    /**
     * default: thread-safe, with centisecond (0.01) precision realtime clock
     */
    public static NARS realtime(int threads) {
        return new Default(8, true)
                .exe(new MultiExec(threads))
                .time(new RealTime.CS());
    }

    public static NARS realtime(float durFPS) {
        return new Default(8, true).time(new RealTime.CS().durFPS(durFPS));
    }

    /**
     * provides only low level functionality.
     * an empty deriver, but allows any kind of term
     */
    public static NAR shell() {
        return tmp(0).nal(8);
    }

    public NARS memory(String s) {
        return then(n -> {
            File f = new File(s);

            try {
                n.inputBinary(f);
            } catch (FileNotFoundException ignored) {
                //ignore
            } catch (IOException e) {
                NAR.logger.error("input: {} {}", s, e);
            }

            Runnable save = () -> {
                try {
                    n.outputBinary(f, false);
                } catch (IOException e) {
                    NAR.logger.error("output: {} {}", s, e);
                }
            };
            Runtime.getRuntime().addShutdownHook(new Thread(save));
        });
    }

    /**
     * adds a post-processing step before ready NAR is returned
     */
    public NARS then(Consumer<NAR> n) {
        after.add(n);
        return this;
    }

    public @NotNull NARLoop startFPS(float framesPerSecond) {
        return get().startFPS(framesPerSecond);
    }


    /**
     * generic defaults
     */
    @Deprecated
    public static class Default extends NARS {

        final int nal;

        public Default(int nal, boolean threadSafe) {

            this.nal = nal;
            this.deriver = nars.control.Deriver.getDefault(nal);


            if (threadSafe)
                index = () -> new CaffeineIndex(64 * 1024 );

        }

        @Override
        protected void init(NAR nar) {

            nar.nal(nal);

            nar.termVolumeMax.setValue(32);
            //nar.confMin.setValue(0.05f);

            nar.DEFAULT_BELIEF_PRIORITY = 0.5f;
            nar.DEFAULT_GOAL_PRIORITY = 0.5f;
            nar.DEFAULT_QUEST_PRIORITY = nar.DEFAULT_QUESTION_PRIORITY = 0.5f;

            if (nal >= 7)
                new STMLinkage(nar, 1, false);

            nar.defaultWants();

        }
    }

}
