package nars.nlp;

import nars.$;
import nars.NAR;
import nars.nar.Default;
import nars.task.in.Twenglish;
import nars.term.Term;
import nars.util.event.On;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by me on 1/13/16.
 */

public class MarkovlikeTest {

    public static String getSentence() {
        switch ((int)(Math.random() * 5)) {
            case 0: return "how you?";
            case 1: return "what is?"; //what is #x
            case 2: return "thanks im ok.";
            case 3: return "what new?";
            case 4: return "hello you!";
        }
        return null;
    }

    public static void main(String[] args) {
    //public void testLanguage() {

        Default d = new Default(1000,2,1,3);


        d.memory.conceptForgetDurations.setValue(2);
        //d.log();

        final AtomicReference<Term> prev = new AtomicReference($.the(""));
        final On l = d.onExecTerm("say", e -> {
            Term r;
            synchronized (prev) {
                //$.logger.debug(e.task.getExplanation());
                Term nn = e[0];
                Term mm = prev.get();
                if (nn.equals(mm)) {
                    //return Atom.the("_stutter");
                    return null;
                }

                $.logger.info(Arrays.toString(e));

                r = $.p(mm, nn);
                prev.set(nn);
            }
            return r;
        });

        //d.input("$1.0$ ((echo(#a) ==> echo(#b)) <-> echo(#a,#b)). %1.0;0.99%");

        int repeats = 256;
        int wordDelay = 20;
        int sentenceDelay = 75;
        int speakTime = 1500;
        int silenceTime = 500;

        for (int i = 0; i < repeats; i++) {

            $.logger.warn("train");

            String sentenceID = Twenglish.learnSentence(d, wordDelay,
                    getSentence()
                    //"this is a sentence for learning language."
                    //"a b c."
                    //"a b c d e f g."
            );
            d.frame(sentenceDelay);

            $.logger.warn("speak");

            speak(d, sentenceID, speakTime);

            $.logger.warn("off");

            d.frame(silenceTime); //verify it is quiet

        }


//        NARide.show(d.loop(), e-> {
//        });

    }

    public static void speak(NAR n, String sentenceID, int speakTime) {
        //d.frame(1000);





        n.input("$1.0$ sentence(" + sentenceID + ")! :|: %1%");
        //d.input("$1.0$ echo(#x)! :|: %0.55%");

        n.frame(speakTime);

        n.input("$1.0$ sentence(" + sentenceID + ")! :|: %0%"); //stop signal
        n.input("$1.0$ echo(?x)! :|: %0%");



    }

}
