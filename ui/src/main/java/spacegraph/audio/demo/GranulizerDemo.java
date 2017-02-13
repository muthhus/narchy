package spacegraph.audio.demo;

import spacegraph.audio.Audio;
import spacegraph.audio.SoundListener;
import spacegraph.audio.granular.Granulize;
import spacegraph.audio.granular.TimeStretchGui;
import spacegraph.audio.sample.SampleLoader;
import spacegraph.audio.sample.SonarSample;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public enum GranulizerDemo {
    ;

    @SuppressWarnings("HardcodedFileSeparator")
    public static void main(String[] args) throws LineUnavailableException, InterruptedException {

        Audio audio = new Audio(4);

        Granulize ts =
            new Granulize(SampleLoader.load("/tmp/awake.wav"), 0.25f, 0.9f)
                    .setStretchFactor(0.25f);

        audio.play(ts, SoundListener.zero, 1, 1);

        //audio.play(new SamplePlayer(smp, 0.5f), SoundListener.zero, 1, 1);

        audio.thread.join();
    }


}
