
package nars.experiment.nario2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GamePanel extends JPanel implements Runnable
{
    GameModel model;
    ExecutorService executor; 
    Future future; 
    Future fPaint;
    Future fSounds;
    
    public GamePanel(GameModel model)
    {
        future = null;
        
        setLayout(null); 
        setBackground(java.awt.Color.darkGray);
        
        this.model = model;
        
        addKeyListener(new KeyHandler());
        addMouseListener(new MouseHandler());
        addComponentListener(new ComponentHandler());
        setFocusable(true);
        
        executor = Executors.newFixedThreadPool(3);
        
        start();
    }
    
    public void start()
    {
        Thread thread = new Thread(this);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }
    
    @Override
    public void run()
    {
        FrameUpdater frameUpdater = new FrameUpdater(model);
        Repainter repainter = new Repainter(this);

        while (true)
        {
            if (model.navigation.fxIsOn)
                fSounds = executor.submit(new SoundPlayer((ArrayList)model.clipsToPlay.clone()));

            model.clipsToPlay = new ArrayList<>();

            future = executor.submit(frameUpdater);

            fPaint = executor.submit(repainter);
            
            try 
            {
                Thread.sleep(GameModel.frameRate);
            }
            catch (InterruptedException e)
            {
                break;
            }
            while (!future.isDone() || !fPaint.isDone() || !fSounds.isDone())
            {
                System.out.println("waiting");
            }    
        }
    }
    
    @Override
    public void paintComponent(Graphics g)
    {
       //super.paintComponent(g);

       if (model.backImage==null) {
           //model.backImage = new BufferedImage(model.viewWidth, model.viewHeight, TYPE_INT_RGB);
       }

        //Graphics g2 = model.backImage.getGraphics();

        //Color fade = new Color(0,0,0,0.1f);
//        g2.setColor(fade);
//        g2.fillRect(0, 0, model.viewWidth, model.viewHeight);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, GameModel.viewWidth, GameModel.viewHeight);
        model.paintElements(g);

        //g.drawImage(model.backImage, 0, 0, getWidth(), getHeight(), null);
        
    }
    
    private class KeyHandler extends KeyAdapter
    {
        @Override
        public void keyPressed(KeyEvent event)
        {
            if (event.getKeyCode() == model.leftKey)
            {
                model.leftKeyPress = true;
            }
            if (event.getKeyCode() == model.rightKey)
            {
                model.rightKeyPress = true;
            }
            if (event.getKeyCode() == model.upKey)
            {
                model.upKeyPress = true;
            }
            
            else if (event.getKeyCode() == model.pauseKey)
            {
                if (model.playing)
                {
                    model.navigation.pauseButton.doClick();
                }
                else
                {
                    model.navigation.playButton.doClick();
                }
            }
            else if (event.getKeyCode() == model.soundTog)
            {
                model.navigation.soundToggle.doClick();
            }
        }
        
        @Override
        public void keyReleased(KeyEvent event)
        {
            if (event.getKeyCode() == model.leftKey)
            {
                model.leftKeyPress = false;
                model.leftKeyReleased();

            }
            if (event.getKeyCode() == model.rightKey)
            {
                model.rightKeyPress = false;
                model.rightKeyReleased();
            }
            if (event.getKeyCode() == model.upKey)
            {
                model.upKeyPress = false;
                model.upKeyReleased();
            }
        }
    }
    
    private class MouseHandler extends MouseAdapter
    {
        @Override
        public void mousePressed(MouseEvent event)
        {
            model.clicking = true;
        }
        @Override
        public void mouseReleased(MouseEvent event)
        {
            model.clicking = false;
        }
    }
    
    private class ComponentHandler extends java.awt.event.ComponentAdapter
    {
        @Override
        public void componentResized(java.awt.event.ComponentEvent event)
        {
            GameModel.screenWidth = getWidth();
            GameModel.screenHeight= getHeight();
            
            repaint();
        }
    }
}
