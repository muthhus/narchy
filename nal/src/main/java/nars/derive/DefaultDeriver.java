package nars.derive;

import nars.premise.Derivation;

/**
 * Created by me on 12/26/16.
 */
public class DefaultDeriver implements Deriver {

    static final Deriver[] modules = Deriver.get(
        "nal1.nal",
        "nal2.nal",
        "nal3.nal",
        "nal4.nal",
        "nal6.nal",
        "induction.nal",
        "misc.nal"
    );


    @Override
    public void accept(Derivation x) {
        for (Deriver d : modules)
            d.accept(x);
    }
}
