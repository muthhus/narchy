
package nars.experiment.nario2;

public class FrameUpdater implements Runnable
{
    GameModel model;
    
    public FrameUpdater(GameModel m)
    {
        model = m;
        
        run();
    }
    
    @Override
    public void run()
    {
        model.frameUpdate();
    }
}
