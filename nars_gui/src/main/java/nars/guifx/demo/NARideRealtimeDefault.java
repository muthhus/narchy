package nars.guifx.demo;

import nars.Memory;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.DefaultConceptBuilder;
import nars.nar.Default;
import nars.op.mental.Abbreviation;
import nars.op.mental.Anticipate;
import nars.op.mental.Inperience;
import nars.term.Term;
import nars.term.TermIndex;
import nars.time.RealtimeMSClock;

import java.util.function.Function;


/**
 * Created by me on 9/7/15.
 */
public enum NARideRealtimeDefault {
    ;

    public static void main(String[] arg) {


        //Global.DEBUG = true;


        Memory mem = new Memory(new RealtimeMSClock(),
            //new MapCacheBag(
                    //new WeakValueHashMap<>()
                TermIndex.softMemory(128*1024)
                //GuavaCacheBag.make(1024*1024)
                /*new InfiniCacheBag(
                    InfiniPeer.tmp().getCache()
                )*/
            //)
        );

        Default nar = new Default(mem, 1024, 1, 2, 2) {
            @Override
            public Function<Term, Concept> newConceptBuilder() {
                return new DefaultConceptBuilder(this, 32, 128);
            }
        }.with(
                Anticipate.class,
                Inperience.class
        );
        nar.with(new Abbreviation(nar,"is"));

 



        /*nar.memory.conceptForgetDurations.set(10);
        nar.memory.termLinkForgetDurations.set(100);*/

        nar.memory.duration.set(750 /* ie, milliseconds */);
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
