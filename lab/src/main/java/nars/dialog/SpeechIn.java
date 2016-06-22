package nars.dialog;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.WordResult;
import nars.util.Util;


/**
 * http://cmusphinx.sourceforge.net/wiki/tutorialsphinx4
 */
public class SpeechIn {

    public static void main(String[] args) throws Exception {

        Configuration configuration = new Configuration();
        configuration
                .setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration
                .setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration
                .setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
        configuration.setSampleRate(8000);



        LiveSpeechRecognizer recognizer = new LiveSpeechRecognizer(configuration);

        recognizer.startRecognition(true);
        System.out.println("recognition start");

        Util.pause(5000);

        System.out.println("recognition stop");
        SpeechResult result = recognizer.getResult();
        recognizer.stopRecognition();

        for (WordResult r : result.getWords()) {
            System.out.println(r);
        }

        System.out.println(result.getNbest(10));
        System.out.println(result.getHypothesis());
        System.out.println(result.getResult().getActiveTokens());
        System.out.println(result.getLattice());
        System.out.println(result);

    }

}
