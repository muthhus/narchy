/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.experiment.nario2;


import nars.experiment.nario2.mapeditor.MapEditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Color; 
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.*;

public class NavigationBar extends JMenuBar 
{
    GameModel model; 
    
    boolean musicIsOn; 
    boolean fxIsOn; 
    
    JMenuBar playPause; 
    JMenuBar otherButtons; 
    
    JButton pauseButton; 
    JButton playButton; 
    
    JButton highScoresButton; 
    JButton changeBackground; 
    
    Image soundOn; 
    Image soundOff; 
    Image backgroundChange; 
    JLabel soundIcon;
    JButton soundToggle; 
    
    JLabel playLabel; 
    JLabel pauseLabel; 
    JLabel highScoresLabel; 
    JLabel backgroundLabel;
    
    JButton mapChange;
    JLabel  mapChangeLbl;
    
    JButton statsButton; 
    JLabel  statsLabel; 
    
    JButton settingsButton;
    JLabel settingsLabel;
    
    JButton helpButton; 
    JLabel  helpLabel; 
    
    public NavigationBar()
    {
        musicIsOn = false; 
        fxIsOn    = false; 
        
        ButtonHandler handler = new ButtonHandler();
        
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());
        
        playPause = new JMenuBar(); 
        playPause.setLayout(new FlowLayout()); 
        playPause.setBackground(Color.black);
        
        otherButtons = new JMenuBar();
        otherButtons.setLayout(new FlowLayout());
        otherButtons.setBackground(Color.black);
     
        highScoresLabel = new JLabel("High Scores"); 
        
        Image img =  new ImageIcon(getClass().getResource("images/high-scores.png")).getImage();
        Image resized = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        
        highScoresLabel.setIcon(new ImageIcon(resized));

        highScoresButton = new JButton();
        highScoresButton.add(highScoresLabel);
        
        highScoresButton.addActionListener(handler);
        
        otherButtons.add(highScoresButton, BorderLayout.CENTER);
        
        
        playLabel = new JLabel("Play"); 

        img =  new ImageIcon(getClass().getResource("images/play.png")).getImage();
        resized = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        playLabel.setIcon(new ImageIcon(resized)); 
        
        playButton = new JButton();
        playButton.add(playLabel); 
        
        playButton.addActionListener(handler); 
                
        pauseLabel = new JLabel("Pause"); 

        img =  new ImageIcon(getClass().getResource("images/pause.png")).getImage();
        resized = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        pauseLabel.setIcon(new ImageIcon(resized)); 
        
        pauseButton = new JButton();
        pauseButton.add(pauseLabel); 
        
        pauseButton.addActionListener(handler); 
        
        playPause.add(playButton); 
       // playPause.add(pauseButton); 

        soundOn =  new ImageIcon(getClass().getResource("images/sound-on.png")).getImage();
        soundOn = soundOn.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        
        soundOff =  new ImageIcon(getClass().getResource("images/sound-off.png")).getImage();
        soundOff = soundOff.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        
        soundIcon = new JLabel(); 
        soundIcon.setIcon(new ImageIcon(soundOn));
        
        JMenuBar othersPanel = new JMenuBar();
        othersPanel.setLayout(new FlowLayout()); 
        othersPanel.setBackground(null);
        
        soundToggle = new JButton(); 
        soundToggle.add(soundIcon);
        soundToggle.addActionListener(handler);
        
        backgroundLabel = new JLabel("Background");
                
        img = new ImageIcon(getClass().getResource("images/background.png")).getImage();
        resized = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        
        backgroundLabel.setIcon(new ImageIcon(resized));
        
        changeBackground = new JButton(); 
        changeBackground.add(backgroundLabel);
        changeBackground.addActionListener(handler);
        
        
        img = new ImageIcon(getClass().getResource("images/settings.png")).getImage();
        resized = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        
        settingsLabel = new JLabel("Settings");
        settingsLabel.setIcon(new ImageIcon(resized));
        
        settingsButton = new JButton(); 
        settingsButton.add(settingsLabel);        
        settingsButton.addActionListener(handler);
        
        helpLabel = new JLabel();
        
        img = new ImageIcon(getClass().getResource("images/help.png")).getImage();
        resized = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        
        helpLabel.setIcon(new ImageIcon(resized));
        
        helpButton = new JButton(); 
        helpButton.add(helpLabel);        
        helpButton.addActionListener(handler);
        
        
        othersPanel.add(settingsButton);
        othersPanel.add(changeBackground);
        othersPanel.add(soundToggle);
        othersPanel.add(helpButton);


        mapChange = new JButton(); 
        mapChange.addActionListener(handler);
        mapChangeLbl = new JLabel("Create Map");
        
        img =  new ImageIcon(getClass().getResource("images/changeMap.png")).getImage();
        resized = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        mapChangeLbl.setIcon(new ImageIcon(resized)); 
        mapChange.add(mapChangeLbl);
        
        statsButton = new JButton(); 
        statsButton.addActionListener(handler); 
        statsLabel = new JLabel("Game Stats");
        
        img =  new ImageIcon(getClass().getResource("images/statsIcon.png")).getImage();
        resized = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        statsLabel.setIcon(new ImageIcon(resized)); 
        statsButton.add(statsLabel);
        
        playPause.add(mapChange);
        playPause.add(statsButton);
                
        add(playPause     , BorderLayout.WEST); 
        add(othersPanel  , BorderLayout.EAST);
       // add(otherButtons, BorderLayout.CENTER);
        
        pauseButton.setEnabled(false);
        
        setButtonsOff();
        
        fxIsOn = true;
    }
    
    public void setModel(GameModel model)
    {
        this.model = model; 
    }

    private static class ItemHandler implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent event)
        {
            
        }
    }
    
    private class ButtonHandler implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent event)
        {            
            if (event.getSource() == playButton)
            {
                if (model.endStatsFrame != null)
                    model.endStatsFrame.dispose();
                model.pauseKeyPress();
            }
            
            else if (event.getSource() == pauseButton)
            {
                model.pauseKeyPress();
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
            }
            
            else if (event.getSource() == soundToggle)
            {
                fxIsOn = !fxIsOn;
                
                if (!fxIsOn)
                    soundToggle.setIcon(new ImageIcon(soundOff));
                else
                    soundToggle.setIcon(new ImageIcon(soundOn)); 
            }

            else if (event.getSource() == changeBackground)
            {
                JFileChooser chooseDirec = new JFileChooser();
                chooseDirec.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooseDirec.showOpenDialog(model.navigation);
                File file = chooseDirec.getSelectedFile();
                
                model.setBackImage(new ImageIcon(file.getAbsolutePath()).getImage());
            }
            
            else if (event.getSource() == highScoresButton)
            {
                
            }  
            
            else if (event.getSource() == mapChange)
            {
                try
                {
                    GameModel.editor.frame.setVisible(true);
                    GameModel.editor.frame2.setVisible(true);
                }
                catch(NullPointerException e)
                {
                    GameModel.editor = new MapEditor(model);
                }
                GameModel.editor.frame.setVisible(true);
                GameModel.editor.frame2.setVisible(true);
                GameModel.editor.frame.repaint();
                GameModel.editor.frame2.repaint();
            }
            
            else if (event.getSource() == statsButton)
            {
                model.statsFrame.setVisible(true);
            }
            
            else if (event.getSource() == settingsButton)
            {
                JFrame settings = new JFrame("Game Settings");
                settings.add(new SettingsPanel(model));
                settings.setVisible(true);
                settings.pack();
                settings.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            }
            
            else if (event.getSource() == helpButton)
            {
                JDialog dialog = new JDialog();
                JTextArea field = new JTextArea();
                field.setEditable(false);
                dialog.add(field);
                field.setText("Defend against the waves enemy Marios \n\n"
                             +"Click to Shoot, Aim with Mouse        \n\n"
                             + "Default Controls:                    \n"
                             + "SPACE - Jump \n"
                             + "A - Move Left \n"
                             + "D - Move Right \n\n"
                             + "P - Pause Game \n"
                             + "T - Toggle Sound");
                dialog.setVisible(true);
                dialog.pack();
                dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            }
            
            repaint();
            
            setButtonsOff();
        }
    }
    
    public void setButtonsOff()
    {
        playButton.setFocusable(false);
        pauseButton.setFocusable(false);
        highScoresButton.setFocusable(false);
        soundToggle.setFocusable(false);
        changeBackground.setFocusable(false);
        mapChange.setFocusable(false);
        statsButton.setFocusable(false);
        settingsButton.setFocusable(false);
        helpButton.setFocusable(false);
    }
}