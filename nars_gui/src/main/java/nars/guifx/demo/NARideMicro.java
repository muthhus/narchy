package nars.guifx.demo;

import nars.NAR;
import nars.concept.Concept;
import nars.concept.DefaultConceptBuilder;
import nars.nar.Default;
import nars.term.Term;

import java.util.function.Function;


/**
 * Created by me on 9/7/15.
 */
public enum NARideMicro {
    ;

    public static void main(String[] arg) {


        //Global.DEBUG = true;

        Default nar = new Default(64, 1, 1, 1) {
            @Override
            public Function<Term, Concept> newConceptBuilder() {
                return new DefaultConceptBuilder(random, 4,4);
            }
        };
        //nar.trace();

        //new BagForgettingEnhancer(nar.memory, nar.core.concepts(), 0.75f, 0.75f, 0.75f);


        NARide.show(nar.loop(), (i) -> {

            nar.input("a:b.");
            nar.input("b:c.");


            /*try {
                i.nar.input(new File("/tmp/h.nal"));
            } catch (Throwable e) {
                i.nar.memory().eventError.emit(e);
                //e.printStackTrace();
            }*/
        });

    }
}
