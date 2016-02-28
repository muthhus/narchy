package nars.guifx.demo;

import javassist.scopedpool.SoftValueHashMap;
import nars.Memory;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.DefaultConceptBuilder;
import nars.nar.Default;
import nars.op.mental.Abbreviation;
import nars.op.mental.Anticipate;
import nars.op.mental.Inperience;
import nars.term.Term;
import nars.term.index.MapIndex2;
import nars.time.RealtimeMSClock;
import nars.util.data.random.XORShiftRandom;

import java.util.function.Function;


/**
 * Created by me on 9/7/15.
 */
public enum NARideRealtimeDefault {
    ;

    public static void main(String[] arg) {


        //Global.DEBUG = true;


        Memory mem = new Memory(new RealtimeMSClock(),
                new MapIndex2(
                        new SoftValueHashMap(128*1024), new DefaultConceptBuilder(new XORShiftRandom(), 32, 32)
                )
            //new MapCacheBag(
                    //new WeakValueHashMap<>()

                //GuavaCacheBag.make(1024*1024)
                /*new InfiniCacheBag(
                    InfiniPeer.tmp().getCache()
                )*/
            //)
        );

        Default nar = new Default(1024, 1, 2, 2) {
            @Override
            public Function<Term, Concept> newConceptBuilder() {
                return new DefaultConceptBuilder(random, 32, 128);
            }
        };
        nar.with(
                Anticipate.class,
                Inperience.class
        );
        nar.with(new Abbreviation(nar,"is"));

 



        /*nar.memory.conceptForgetDurations.set(10);
        nar.memory.termLinkForgetDurations.set(100);*/

        nar.duration.set(750 /* ie, milliseconds */);
        //nar.spawnThread(1000/60);



        NARide.show(nar.loop(), (i) -> {
            /*try {
                i.nar.input(new File("/tmp/h.nal"));
            } catch (Throwable e) {
                i.nar.memory().eventError.emit(e);
                //e.printStackTrace();
            }*/
        });

    }
}
