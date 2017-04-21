package nars.audio;

import jcog.random.XorShift128PlusRandom;
import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import nars.time.Time;
import nars.truth.Truth;
import spacegraph.audio.Audio;
import spacegraph.audio.Sound;
import spacegraph.audio.SoundProducer;
import spacegraph.audio.granular.Granulize;
import spacegraph.audio.sample.SampleLoader;
import spacegraph.audio.sample.SonarSample;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nars.$.$;

/**
 * NAR sonification
 */
public class SoNAR extends TimerTask {

    private final NAR nar;
    public final Audio audio;
    final static Timer real = new Timer("Audio");

    public static class SampleDirectory {
        final Map<String, SonarSample> samples = new ConcurrentHashMap<>();

        final Random random = new XorShift128PlusRandom(System.currentTimeMillis());

        public SonarSample sample(String file) {
            return samples.computeIfAbsent(file, SampleLoader::load);
        }

        public void samples(String dirPath) {
            for (File f : new File(dirPath).listFiles()) {
                String path = f.getAbsolutePath();
                samples.computeIfAbsent(path, SampleLoader::load);
            }
        }

        /**
         * gets a random sample from what is loaded
         */
        public SonarSample sample(int hash) {
            List<SonarSample> l = samples.values().stream().collect(Collectors.toList());         //HACK
            if (l != null && !l.isEmpty()) {
                SonarSample s;
                do {
                    s = l.get(Math.abs(hash) % l.size());
                } while (s == null);
                return s;
            } else
                return null;
        }

        public SoundProducer byHash(Object x) {
//            return new SamplePlayer(
//                sample(term.hashCode()), 1f
//            );
            return new Granulize(
                sample(x.hashCode()), 0.5f, 1.0f, random
            );
        }
    }

    public SoNAR(NAR n) throws LineUnavailableException {
        this(n, new Audio(32));
    }


    public SoNAR(NAR n, Audio audio) {
        this(n, audio, 10);
    }

    public SoNAR(NAR n, Audio audio, int updatePeriodMS) {

        this.nar = n;
        this.audio = audio;


        real.scheduleAtFixedRate(this, 0, updatePeriodMS);

//        Granulize ts =
//                new Granulize(sample("/tmp/awake.wav"), 0.25f, 0.9f)
//                        .setStretchFactor(0.25f);

        //audio.play(ts, SoundListener.zero, 1, 1);

        //audio.play(new SamplePlayer(smp, 0.5f), SoundListener.zero, 1, 1);

        //n.onCycle(this::update);

    }


    /**
     * updated each cycle
     */
    final Map<Term, Sound> termSounds = new ConcurrentHashMap();

    /** vol of each individual sound */
    float soundVolume = 0.1f;

    public void listen(Term k, Function<? super Term, ? extends SoundProducer> p) {
        termSounds.computeIfAbsent(k, kk -> {
            SoundProducer ss = p.apply(kk);
            return audio.play(ss, soundVolume,
                    0.5f, /* priority */
                    (float) (Math.random() - 0.5f) /* balance */
            );
        });
    }
//    protected void _listen(Term k, Function<? super Term, ? extends Sound> p) {
//
//        Sound a = termSounds.computeIfAbsent(k, p);
//
////        kk -> {
////            Granulize g = new Granulize(sampleRandom(), 0.25f, 1.5f);
////
////            return audio.play(g, 0.25f, 0.5f, (float) (Math.random() - 0.5f));
////        });
//
//    }

    @Override public void run() {
        termSounds.forEach(this::update);
    }

    private boolean update(Term k, Sound<Granulize> s) {
        if (s.producer instanceof Granulize) {
            Granulize v = s.producer;
            Concept c = nar.concept(k);
            if (c != null) {
                //float p = nar.pri(k);
                //if (p == p && p > 0) {

                    //v.setAmplitude(0.1f * p);

                    Truth b = c.belief(nar.time(), nar.dur());

                    //System.out.println(c + " "+ b + " " + nar.time() + " " + nar.dur());

                    if (b != null && b.freq() > 0.5f) {
                        float stretchFactor = (b.freq() - 0.5f) * 2f;
                        if (stretchFactor > 0 && stretchFactor < 0.05f) stretchFactor = 0.05f;
                        else if (stretchFactor < 0 && stretchFactor > -0.05f) stretchFactor = -0.05f;

                        v.setStretchFactor(stretchFactor);
                        //v.setAmplitude(1f);
                        v.setAmplitude(b.conf());
                        //v.setAmplitude(b.expectation());
                        //v.play();
                    } else {
                        v.setAmplitude(0f);
                        //v.stop();
                        //v.setStretchFactor(1f);
                    }

                    //
                    //v.setStretchFactor();
                    //v.pitchFactor.setValue(1f / Math.log(c.volume()));
                    //g.setStretchFactor(1f/(1f+kk.volume()/4f));
                    return true;
               //}
            }

            v.setAmplitude(0f);
            //v.stop();
            //return false;
        }
        return true;
    }

    public void join() throws InterruptedException {
        audio.thread.join();
    }

    public static void main(String[] args) throws LineUnavailableException, InterruptedException, Narsese.NarseseException {
        Default n = new Default();
        n.deriver.conceptsFiredPerCycle.set(2);
        //n.log();
        n.input("a:b. :|: (--,b:c). c:d. d:e. (--,e:f). f:g. b:f. a:g?");
        n.loop(64);
        SoNAR s = new SoNAR(n);
        SampleDirectory d = new SampleDirectory();
        d.samples("/home/me/wav");
        s.listen($("a"), d::byHash);
        s.listen($("b"), d::byHash);
        s.listen($("c"), d::byHash);
        s.listen($("d"), d::byHash);
        s.listen($("e"), d::byHash);
        s.listen($("f"), d::byHash);
        s.listen($("g"), d::byHash);
        s.listen($("a:b"), d::byHash);
        s.listen($("b:c"), d::byHash);
        s.listen($("c:d"), d::byHash);
        s.listen($("d:e"), d::byHash);
        s.listen($("e:f"), d::byHash);
        s.listen($("f:g"), d::byHash);
        s.listen($("a:g"), d::byHash);
        try {
            s.audio.record("/tmp/test.raw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        s.join();
    }
}
