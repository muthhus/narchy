package nars.guifx.demo;

import javassist.scopedpool.SoftValueHashMap;
import nars.Global;
import nars.Memory;
import nars.concept.DefaultConceptBuilder;
import nars.nar.AbstractNAR;
import nars.nar.Default;
import nars.term.TermIndex;
import nars.term.index.MapIndex2;
import nars.time.FrameClock;

/**
 * Created by me on 9/7/15.
 */
public enum NARideDefault {
    ;

    public static void main(String[] arg) {

        Global.DEBUG = false;

        FrameClock clock = new FrameClock();

        Default n = new Default(
                new Memory(
                        clock,
                        new AbstractNAR.DefaultTermIndex(2048)),
                        //TermIndex.memoryGuava(clock, 100)),
//                        memoryWeak(1024 * 128)),
                1024, 1, 3, 2);
        //new Inperience(n);

        NARide.show(n.loop(), (i) -> {

            /*try {
                i.nar.input(new File("/tmp/h.nal"));
            } catch (Throwable e) {
                i.nar.memory().eventError.emit(e);
                //e.printStackTrace();
            }*/
        });

    }

    //    static TermIndex memorySoft(int capacity) {
    //        return new MapIndex(
    //                new SoftValueHashMap(capacity),
    //                new SoftValueHashMap(capacity*2)
    //        );
    //    }
//        public static TermIndex memoryWeak(int capacity) {
//    //        return new MapIndex(
//    //            new SoftValueHashMap(capacity),
//    //            new SoftValueHashMap(capacity*2)
//    //        );
//            return new MapIndex2(
//                new SoftValueHashMap(capacity),
//                new DefaultConceptBuilder()
//                    );
//        }
}
