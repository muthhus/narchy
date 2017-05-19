package nars.nal;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.nar.Default;
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
        Param.DEBUG = true;
        this.nar = nar;
    }


    public final TestNAR test() {
        return tester;
    }
    public final TestNAR test(NAR n) {
        return new TestNAR(n);
    }


    public NAR nar() {
        //return the
        return nar.get();
    }


    @Before
    public void start() {
        tester = new TestNAR(nar());
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
                    Default d =
                            new Default(512);

                    d.nal(level);
                    d.termVolumeMax.setValue(24);
                    return d;
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
