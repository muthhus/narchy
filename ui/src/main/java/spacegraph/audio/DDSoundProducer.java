package spacegraph.audio;

import marytts.util.data.audio.DDSAudioInputStream;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

public class DDSoundProducer implements SoundProducer {

    private final DDSAudioInputStream in;
    private Line line;
    int pos = 0;
    AudioFormat format;
    byte[] bytes;
    int samplesPerFrame;
    private boolean alive = true;
    @Nullable
    public Runnable onFinish;

//    int frameRate;
//    int calculateBufferSize(double suggestedOutputLatency) {
//        int numFrames = (int) (suggestedOutputLatency * frameRate);
//        int numBytes = numFrames * samplesPerFrame * 2 /*BYTES_PER_SAMPLE*/;
//        return numBytes;
//    }

    public DDSoundProducer(DDSAudioInputStream i) {
        format = i.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, format);

        this.in = i;
//            try {
//                this.line = AudioSystem.getLine(info);
//            } catch (LineUnavailableException e) {
//                e.printStackTrace();
//            }

//            try {
//                Clip clip = (Clip) AudioSystem.getLine(info);
//
//                clip.open(i);
//                clip.
//                        clip.loop(0); //plays once
//                if (waitUntilCompleted) {
//                    clip.drain();
//                }
//            } catch (LineUnavailableException | IOException var8) {
//                var8.printStackTrace();
//            }


    }


    public int read(float[] buffer) {
        return read(buffer, 0, buffer.length);
    }


    public int read(float[] buffer, int start, int count) {
        // Allocate byte buffer if needed.
        if ((bytes == null) || ((bytes.length * 2) < count)) {
            bytes = new byte[count * 2];
        }
        int bytesRead = 0;//line.read(bytes, 0, bytes.length);

        try {

            int available = in.available();
            if (available > 0) {
                bytesRead = in.read(bytes, start, Math.min(available, bytes.length));
                if (bytesRead == 0) {
                    alive = false;
                }
            } else {
                alive = false;
            }
        } catch (IOException e) {
            MaryTTSpeech.logger.error("read {} {}", in, e);
            alive = false;
            return 0;
        }

        // Convert BigEndian bytes to float samples
        int bi = 0;
        byte[] b = this.bytes;
        for (int i = 0; i < bytesRead / 2; i++) {
            int sample = b[bi++] & 0x00FF; // little end
            sample = sample + (this.bytes[bi++] << 8); // big end
            buffer[i] = sample / 32767.0f;
        }
        return bytesRead / 4;
    }

    @Override
    public float read(float[] buf, int readRate) {
        pos += read(buf, 0, buf.length);
        return 1;
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {

        float step = format.getSampleRate() / readRate;
        pos += step * samplesToSkip;

//            if (alive && pos >= sample.buf.length) {
//                alive = false;
//            }
    }

    @Override
    public boolean isLive() {

        if (!alive) {
            synchronized (in) {
                @Nullable Runnable r = this.onFinish;
                this.onFinish = null;
                if (r!=null)
                    ForkJoinPool.commonPool().execute(r);
            }
        }

        return alive;
    }

    @Override
    public void stop() {
        alive = false;
    }
}
