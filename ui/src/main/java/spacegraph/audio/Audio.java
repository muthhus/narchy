package spacegraph.audio;

import spacegraph.audio.sample.SamplePlayer;
import spacegraph.audio.sample.SoundSample;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class Audio implements Runnable {
    private final AudioFormat audioFormat;
    private final Line.Info info;
    private final int bufferBytes;
    public final int maxChannels;
    private final SoundSample silentSample;
    private final SourceDataLine sdl;

    private final int rate = 44100;
    private final int bufferSize = rate / 32;

    private final ListenerMixer listenerMixer;


    private final ByteBuffer soundBuffer = ByteBuffer.allocate(bufferSize * 4);
    private final float[] leftBuf, rightBuf;
    //private float amplitude = 1;
    //private float targetAmplitude = 1;
    private boolean alive = true;
    private float alpha;


    private FileOutputStream rec;
    public Thread thread;


    public Audio(int maxChannels) throws LineUnavailableException {

        this.maxChannels = maxChannels;
        silentSample = new SoundSample(new float[]{0}, 44100);
        Mixer mixer = AudioSystem.getMixer(null);

        bufferBytes = bufferSize * 2 * 2;

        sdl = (SourceDataLine) mixer.getLine(info = new Line.Info(SourceDataLine.class));
        sdl.open(audioFormat = new AudioFormat(rate, 16, 2, true, false), bufferBytes);
        soundBuffer.order(ByteOrder.LITTLE_ENDIAN);
        sdl.start();


        try {
            FloatControl volumeControl = (FloatControl) sdl.getControl(FloatControl.Type.MASTER_GAIN);
            volumeControl.setValue(volumeControl.getMaximum());
        } catch (IllegalArgumentException ignored) {
            System.out.println("Failed to set the sound volume");
        }

        listenerMixer = new ListenerMixer(maxChannels);
        setListener(SoundSource.center);

        leftBuf = new float[bufferSize];
        rightBuf = new float[bufferSize];

        thread = new Thread(this);
        thread.setDaemon(true);
        //thread.setPriority(10);
        thread.start();

    }

    public void record(String path) throws java.io.FileNotFoundException {

        //if (rec != null) //...

        System.out.println("recording to: " + path);
        rec = new FileOutputStream(new File(path), false);

//        PipedInputStream pi = new PipedInputStream();
//        pi.connect(rec = new PipedOutputStream());
//
//        AudioInputStream ais = new AudioInputStream(
//                pi,
//                audioFormat, bufferBytes);
//
//
//        // start recording
//        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(path));

    }

    public void setListener(SoundSource soundSource) {
        listenerMixer.setSoundListener(soundSource);
    }

    public void shutDown() {
        alive = false;
    }


    static class DefaultSource implements SoundSource {

        private final SoundProducer producer;
        final float distanceFactor = 1.0f;
        private final float balance;

        DefaultSource(SoundProducer p, float balance) {
            this.producer = p;
            this.balance = balance;
        }

        @Override
        public float getY(float alpha) {
            return 0 + (1.0f - producer.getAmplitude()) * distanceFactor;
        }

        @Override
        public float getX(float alpha) {
            return balance;
        }
    }


    public void play(SoundSample sample, SoundSource soundSource, float volume, float priority) {
        play(new SamplePlayer(sample, rate), soundSource, volume, priority);
    }

    public <S extends SoundProducer> Sound<S> play(S p, float volume, float priority, float balance) {
        return play(p, new DefaultSource(p, balance), volume, priority);
    }

    public <S extends SoundProducer> Sound<S> play(S p, SoundSource soundSource, float volume, float priority) {
//        if (!alive)
//            return;
        return listenerMixer.addSoundProducer(p, soundSource, volume, priority);
    }


    public void clientTick(float alpha) {
        listenerMixer.update(alpha);
    }

    public void tick() {
        //soundBuffer.clear();

        //        targetAmplitude = (targetAmplitude - 1) * 0.9f + 1;
        //        targetAmplitude = (targetAmplitude - 1) * 0.9f + 1;
        listenerMixer.read(leftBuf, rightBuf, rate);
        ////            if (maxAmplitude > targetAmplitude) targetAmplitude = maxAmplitude;


        soundBuffer.clear();
        int max16 = 32767;
        float gain = max16;
        for (int i = 0; i < bufferSize; i++) {
            //            amplitude += (targetAmplitude - amplitude) / rate;
            //          amplitude = 1;
            //              float gain = 30000;

            int l = Math.round(leftBuf[i] * gain);
            if (l > max16) l = max16;
            else if (l < -max16) l = -max16;
            soundBuffer.putShort((short) l);

            int r = Math.round(rightBuf[i] * gain);
            if (r > max16) r = max16;
            else if (r < -max16) r = -max16;
            soundBuffer.putShort((short) r);

            //doesnt work right:
            //soundBuffer.putInt( ((short)r) | ( ((short)l) << 16));


        }

        byte[] ba = soundBuffer.array();
        sdl.write(ba, 0, bufferSize * 2 * 2);
        if (rec != null) {
            try {
                rec.write(ba, 0, bufferSize * 2 * 2);
                rec.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        while (alive) {
            synchronized (listenerMixer) {
                clientTick(alpha);
                tick();
            }
            Thread.yield();
        }
    }


    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
}