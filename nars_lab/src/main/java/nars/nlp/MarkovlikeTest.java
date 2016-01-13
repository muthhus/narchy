package nars.nlp;

import nars.$;
import nars.NAR;
import nars.guifx.demo.NARide;
import nars.nar.Default;
import nars.task.in.Twenglish;
import nars.util.event.On;

/**
 * Created by me on 1/13/16.
 */

public class MarkovlikeTest {

    public static String getSentence() {
        switch ((int)(Math.random() * 5)) {
            case 0: return "hi how are you?";
            case 1: return "im fine thanks. and you?";
            case 2: return "hello!";
            case 3: return "im talking to you.";
            case 4: return "now whats new?";
        }
        return null;
    }

    public static void main(String[] args) {
    //public void testLanguage() {

        Default d = new Default(1000,2,1,3);

        d.memory.conceptForgetDurations.setValue(2);
        //d.log();

        //d.input("$1.0$ ((echo(#a) ==> echo(#b)) <-> echo(#a,#b)). %1.0;0.99%");

        int repeats = 8;
        int wordDelay = 10;
        int sentenceDelay = 50;
        int speakTime = 500;
        int silenceTime = 200;

        for (int i = 0; i < repeats; i++) {
            String sentenceID = Twenglish.learnSentence(d, wordDelay,
                    getSentence()
                    //"this is a sentence for learning language."
                    //"a b c."
                    //"a b c d e f g."
            );
            d.frame(sentenceDelay);

            speak(d, sentenceID, speakTime, silenceTime);
        }


        NARide.show(d.loop(), e-> {
        });

    }

    public static void speak(NAR n, String sentenceID, int speakTime, int silenceTime) {
        //d.frame(1000);

        //d.log();
        On l = n.onExec("echo", e -> {
            $.logger.info(e.task.toString());
            //$.logger.debug(e.task.getExplanation());
        });

        $.logger.warn("on");
        n.input("$1.0$ sentence(" + sentenceID + ")! :|: %1%");
        //d.input("$1.0$ echo(#x)! :|: %0.55%");

        n.frame(speakTime);

        n.input("$1.0$ sentence(" + sentenceID + ")! :|: %0%"); //stop signal
        n.input("$1.0$ echo(?x)! :|: %0%");
        $.logger.warn("off");


        n.frame(silenceTime); //verify it is quiet

        l.off();

    }

}
