package nars.derive;

/**
 * Created by me on 12/26/16.
 */
public class DefaultDeriver  {

    public static final Deriver the =
        //new InstrumentedDeriver(
            (TrieDeriver)Deriver.get(
                "nal1.nal",
                "nal2.nal",
                "nal3.nal",
                //"nal4.nal",
                "nal6.nal",
                "induction.nal",
                "misc.nal"
                //"relation_introduction.nal"
            )
        //)
    ;


//    @Override
//    public void accept(Derivation x) {
//        int start = x.now();
//        for (Deriver d : modules) {
//            d.accept(x);
//            if (x.now()!=start)
//                throw new RuntimeException("revert fault");
//        }
//    }
}
