//package spacegraph.audio;
//
//import marytts.LocalMaryInterface;
//import marytts.MaryInterface;
//import marytts.exceptions.MaryConfigurationException;
//import marytts.exceptions.SynthesisException;
//import marytts.util.data.BufferedDoubleDataSource;
//import marytts.util.data.audio.DDSAudioInputStream;
//import marytts.util.data.audio.MaryAudioUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.sound.sampled.*;
//import java.io.IOException;
//import java.util.concurrent.Executor;
//import java.util.concurrent.Executors;
//
///**
// * https://github.com/marytts/marytts/wiki/MaryInterface
// */
//public class SpeechOut {
//
//    final static Logger logger = LoggerFactory.getLogger(SpeechOut.class);
//
//    final static MaryInterface marytts;
//    final static Executor speechQueue = Executors.newCachedThreadPool();
//    static {
//        System.setProperty("java.version", "1.9.0"); //HACK
//        LocalMaryInterface m;
//        try {
//            m = new LocalMaryInterface();
//            logger.info("Speech System READY");
//        } catch (MaryConfigurationException e) {
//            m = null;
//        }
//        marytts = m;
//    }
//
//    public static void main(String[] args) throws Exception {
//
////        System.out.println("I currently have " + marytts.getAvailableVoices() + " voices in "
////                + marytts.getAvailableLocales() + " languages available.");
////        System.out.println("Out of these, " + marytts.getAvailableVoices(Locale.US) + " are for US English.");
////
////
////        AudioInputStream audio = marytts.generateAudio("This is my text.");
////        MaryAudioUtils.writeWavFile(MaryAudioUtils.getSamplesAsDoubleArray(audio), "/tmp/thisIsMyText.wav", audio.getFormat());
////        MaryAudioUtils.playWavFile("/tmp/thisIsMyText.wav", 3);
//
//
//        //speak("hello 1234 abc this is a sentence!!! now what?");
//
//    }
//
//
//    /** async */
//    public static void speak(String text) {
//        speak(text, null);
//    }
//
//    /** async */
//    public static void speak(String text, Runnable whenFinished) {
//        speechQueue.execute( ()->{
//            try {
//                speakNow(text, whenFinished!=null ? true : false);
//
//            } catch (SynthesisException e) {
//                e.printStackTrace();
//            }
//            if (whenFinished!=null) {
//                whenFinished.run();
//            }
//        });
//    }
//
//
//    /** synchronous */
//    public static void speakNow(String text, boolean waitUntilCompleted) throws SynthesisException {
//
//
//        AudioInputStream audio = marytts.generateAudio(text);
//        double[] x = MaryAudioUtils.getSamplesAsDoubleArray(audio);
//        DDSAudioInputStream audioInputStream = new DDSAudioInputStream(new BufferedDoubleDataSource(x), audio.getFormat());
//
//
//        if(audioInputStream != null) {
//            AudioFormat format = audioInputStream.getFormat();
//            DataLine.Info info = new DataLine.Info(Clip.class, format);
//
//            Clip m_clip;
//            try {
//                m_clip = (Clip)AudioSystem.getLine(info);
//                m_clip.open(audioInputStream);
//                m_clip.loop(0); //plays once
//                if(waitUntilCompleted) {
//                    m_clip.drain();
//                }
//            } catch (LineUnavailableException var8) {
//                var8.printStackTrace();
//            } catch (IOException var9) {
//                var9.printStackTrace();
//            }
//
//        } else {
//            throw new NullPointerException();
//        }
//
//    }
//
//
//}
