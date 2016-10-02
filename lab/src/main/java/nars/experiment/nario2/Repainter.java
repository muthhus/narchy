
package nars.experiment.nario2;

public class Repainter implements Runnable
{
    GamePanel panel;
    
    public Repainter(GamePanel p)
    {
        panel = p;

        run();
    }
    
    @Override
    public void run()
    {
        panel.repaint();
    }
}
