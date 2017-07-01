package nars.nal;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.nar.NARBuilder;
import nars.op.stm.STMTemporalLinkage;
import nars.test.TestNAR;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import java.util.List;
import java.util.function.Supplier;

import static jcog.data.LabeledSupplier.supply;

/**
 * Created by me on 2/10/15.
 */
@Ignore
public abstract class AbstractNALTest {


    private final Supplier<NAR> nar;
    protected TestNAR tester;

    protected AbstractNALTest(NAR nar) {
        this(() -> nar);
    }

    protected AbstractNALTest(Supplier<NAR> nar) {
        this.nar = nar;
    }


    public TestNAR test() {
        return tester = test(nar());
    }
    public TestNAR test(NAR n) {
        return new TestNAR(n);
    }


    public NAR nar() {
        //return the
        return nar.get();
    }


    @Before
    public void start() {
        tester = test();
    }

    @After
    public void end() {
        tester.run();
    }

    @NotNull
    @Deprecated
    public static Iterable<Supplier<NAR>> nars(int level) {


        List<Supplier<NAR>> l = $.newArrayList(1);


        l.add(supply("Default[NAL<=" + level + ']', () -> {
                    NAR n = new NARBuilder().get();
                    n.termVolumeMax.setValue(40);
                    n.nal(level);
//                    n.DEFAULT_BELIEF_PRIORITY = 0.5f;
//                    n.DEFAULT_GOAL_PRIORITY = 0.5f;
//                    n.DEFAULT_QUEST_PRIORITY = 0.25f;
//                    n.DEFAULT_QUESTION_PRIORITY = 0.25f;
                    if (level >= 7) {
                        new STMTemporalLinkage(n, 1, false);
                    }
                    Param.DEBUG = true;
                    return n;
                }
        ));
                /*if (level < 8) {
                    l.add(supply("Default[NAL8, c=" + c + "]", () -> {
                                Default d = new Default(512, c, 2, 3);
                                d.nal(8);
                                return d;
                            }
                    ));
                }*/


//            l.add(supply("Alann[NAL<=" + level + ']',
//                () -> {
//                    DefaultAlann d = new DefaultAlann(
//                        new Memory(new FrameClock(), TermIndex.memorySoft(1024)),
//                        8 /* input cycler capacity */,
//                        24 /* derivelets */
//                    );
//                    d.nal(finalLevel);
//                    return d;
//                }
//            ));


//        if (single) {
//            //throw new RuntimeException("depr");
////            l.add( supply("SingleStep[NAL<=" + level + ']',
////                    () -> new SingleStepNAR(512, 1, 2, 3).nal(finalLevel) ) );
//        }

        return l;
    }

}
