package nars.guifx.demo;

import nars.Global;
import nars.nar.Default;


public enum NARideDefault {
    ;

    public static void main(String[] arg) {

        Global.DEBUG = true;

        Default n = new Default(
                //TermIndex.memoryGuava(clock, 100)),
//                        memoryWeak(1024 * 128)),
                1024, 1, 2, 3);
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
