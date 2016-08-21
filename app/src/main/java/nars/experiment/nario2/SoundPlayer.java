
package nars.experiment.nario2;

import java.applet.AudioClip;
import java.util.ArrayList;

public class SoundPlayer implements Runnable
{
    ArrayList<AudioClip> sounds; 
    
    public SoundPlayer(ArrayList<AudioClip> clips)
    {
        sounds = clips; 
        
        run();
    }
    
    @Override
    public void run()
    {
        for (AudioClip sound : sounds) {
            sound.play();
        }
    }
}
