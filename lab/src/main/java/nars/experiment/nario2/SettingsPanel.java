
package nars.experiment.nario2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SettingsPanel extends JPanel
{
    GameModel model; 
    
    SetKeyPanel[] keyPanels; 
    
    JSlider gameSpeed; 
    
    public SettingsPanel(GameModel m)
    {
        setPreferredSize(new Dimension(512, 128));
        model = m; 
        
        keyPanels = new SetKeyPanel[3];
        
        String[] ids = {"Jump:"   , "Move Left:", "Move Right:"};
        
        int[] keys={model.upKey, model.leftKey, model.rightKey};
        
        for(int i=0; i<keyPanels.length; i++)
        {
            keyPanels[i] = new SetKeyPanel(keys[i], ids[i]);
            add(keyPanels[i]);
        }
        
        JLabel rate = new JLabel("Frames per Second: ");
        
        gameSpeed = new JSlider(); 
        gameSpeed.setValue(GameModel.frameRate);
        gameSpeed.setMajorTickSpacing(8);
        gameSpeed.setInverted(true);
        gameSpeed.setPaintTicks(true);
        gameSpeed.setMinimum(8);
        gameSpeed.setMaximum(40);
        gameSpeed.setFocusable(false);
        gameSpeed.setSnapToTicks(true);

        gameSpeed.addChangeListener(event -> GameModel.setGameSpeed(gameSpeed.getValue()));
        
        JPanel ratePanel = new JPanel();
        ratePanel.add(rate);
        ratePanel.add(gameSpeed);
        
        add(ratePanel);
    }

    
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
    }
    
    public void disableAllButtons()
    {
        for (SetKeyPanel keyPanel : keyPanels) {
            keyPanel.setFocusable(false);
            keyPanel.setEnabled(false);
            keyPanel.enabled = false;
        }
    }
    
    public void sendKeyValuesToModel()
    {
        model.upKey   = keyPanels[0].keyValue;
        model.leftKey = keyPanels[1].keyValue;
        model.rightKey= keyPanels[2].keyValue;

    }
    
    private class SetKeyPanel extends JPanel
    {
        JLabel  showValue;
        int keyValue;
        JButton button; 
        boolean enabled; 
        
        public SetKeyPanel(int key, String text)
        {
            enabled = false; 
            
            keyValue = key; 
            
            button = new JButton(text);
            button.setEnabled(true); 
            button.setFocusable(false);
            
            button.addActionListener(event -> {
                disableAllButtons();
                enabled = true;
                SetKeyPanel.this.setEnabled(true);
                SetKeyPanel.this.setFocusable(true);
                repaint();
            });
            
            showValue = new JLabel(KeyEvent.getKeyText(keyValue)+"  ");
            
            add(button); 
            add(showValue);
            
            button.addKeyListener(new KeyHandler());
        }
        
        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g); 
            
            g.setColor(Color.lightGray);
            g.fillRect(0, 0, this.getWidth(), this.getWidth());
                        
            if (enabled)
            {
                button.setFocusable(true);
                g.setColor(Color.darkGray);
                g.drawRect(0, 0, this.getWidth(), this.getHeight()); 
                g.drawRect(0, 0, this.getWidth()-1, this.getHeight()-1);
            }
            else
            {
                button.setFocusable(false);
            }
        }
        
        private class KeyHandler extends KeyAdapter
        {
            @Override
            public void keyPressed(KeyEvent event)
            {
                if (enabled)
                {
                    keyValue = event.getKeyCode();
                    
                    if (event.getKeyCode() == KeyEvent.VK_UP)
                    {
                        showValue.setText("UP  ");
                    }
                    else if (event.getKeyCode() == KeyEvent.VK_LEFT)
                    {
                        showValue.setText("LEFT ");
                    }
                    else if (event.getKeyCode() == KeyEvent.VK_RIGHT)
                    {
                        showValue.setText("RIGHT");
                    }
                    else if (event.getKeyCode() == KeyEvent.VK_DOWN)
                    {
                        showValue.setText("DOWN");
                    }                    
                    else if (event.getKeyCode() == KeyEvent.VK_SHIFT)
                    {
                        showValue.setText("SHIFT");
                    }
                    else
                    {
                        showValue.setText(event.getKeyChar()+"  ");
                    }

                    
                    disableAllButtons();
                    sendKeyValuesToModel();
                }
                repaint();
            }
        }
    }
}
