package spacegraph.audio;

import spacegraph.audio.sample.SoundSample;

import javax.sound.sampled.LineUnavailableException;


public class FakeSoundEngine extends Audio
{
    public FakeSoundEngine(int maxChannels) {
        super(maxChannels);
    }

    @Override
    public void setListener(SoundSource soundSource)
    {
    }

    @Override
    public void shutDown()
    {
    }

    public static SoundSample loadSample(String resourceName)
    {
        return null;
    }

    public void play(SoundSample sample, SoundSource soundSource, float volume, float priority, float rate)
    {
    }

    @Override
    public void clientTick(float alpha)
    {
    }

    @Override
    public void tick()
    {
    }

    @Override
    public void run()
    {
    }
}