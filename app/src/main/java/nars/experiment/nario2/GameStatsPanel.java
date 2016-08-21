
package nars.experiment.nario2;

import javax.swing.JPanel; 
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import java.awt.Graphics; 

public class GameStatsPanel extends JPanel //implements Runnable
{
    GameModel model;
    
    StatElement[] elements; 
    
    public GameStatsPanel(GameModel model)
    {
        this.model = model; 
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        elements = new StatElement[5];
        
        elements[0] = new StatElement("Wave: ", String.valueOf(model.waveNumber));
        elements[1] = new StatElement("Enemies Killed: ", String.valueOf(model.enemiesKilled));
        elements[2] = new StatElement("Bullets Shot: ", String.valueOf(model.bulletsShot));
        elements[3] = new StatElement("Hits Taken: ", String.valueOf(model.hitsTaken));
        elements[4] = new StatElement("Accuracy: ", String.valueOf(model.accuracy));
        
        for(StatElement element : elements)
        {
            add(element);
        }
        
     //   Thread thread = new Thread(this);
     //   thread.start();
    }
    
    public GameStatsPanel(GameStatsPanel stats)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        elements = new StatElement[5];
        
        this.model = stats.model; 
        
        for(int i=0; i<stats.elements.length; i++)
        {
            this.elements[i] = stats.elements[i];
            add(elements[i]);
        }
        setModelData();
    }
    
    public void setModelData()
    {
        elements[0].value.setText(String.valueOf(model.waveNumber));
        elements[1].value.setText(String.valueOf(model.enemiesKilled));
        elements[2].value.setText(String.valueOf(model.bulletsShot));
        elements[3].value.setText(String.valueOf(model.hitsTaken));
        elements[4].value.setText(model.getAccuracy()+" %");

        repaint();
    }
 /*   
    public void run()
    {
        while(true)
        {
            if (isVisible())
            {
                setModelData();
            }
            
            try
            {
                Thread.sleep(32);
            }
            catch (InterruptedException e)
            {
                break;
            }
            repaint();
        }
    }
    * 
    */
    
    private static class StatElement extends JPanel
    {
        public JLabel label; 
        public JLabel value;
        
        public StatElement(String lbl, String val)
        {
            label = new JLabel(lbl);
            value = new JLabel(val);
            
            add(label); 
            add(value); 
        }
        
        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            
            g.setColor(java.awt.Color.gray);
            g.drawRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        }
    }
}
