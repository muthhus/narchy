package nars.guifx.demo;

import nars.Global;
import nars.nar.Default;

/**
 * Created by me on 9/7/15.
 */
public enum NARideDefault {
    ;

    public static void main(String[] arg) {

        Global.DEBUG = false;

        Default n = new Default(
                //TermIndex.memoryGuava(clock, 100)),
//                        memoryWeak(1024 * 128)),
                1024, 1, 3, 2);
        //new Inperience(n);

        //new PrologCore(n); //TEMPORARY

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
