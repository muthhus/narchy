
package nars.experiment.nario2;

public class StatsFrameUpdater implements Runnable
{
    GameModel model; 
    
    public StatsFrameUpdater(GameModel m)
    {
        model = m;
        run();
    }
    
    @Override
    public void run()
    {
       if (model.statsFrame.isVisible())
            model.stats.setModelData();
    }
}
