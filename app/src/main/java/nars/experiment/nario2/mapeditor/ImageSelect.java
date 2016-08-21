
package nars.experiment.nario2.mapeditor;

import nars.experiment.nario2.GameModel;

import java.awt.Image;
import javax.swing.ImageIcon;
import java.awt.event.MouseAdapter; 
import java.awt.event.MouseEvent; 
import java.awt.Color;
import javax.swing.JButton; 
import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener; 
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent; 
import javax.swing.JFileChooser;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JFrame;

public class ImageSelect extends JPanel
{
    MapModel model;
    MapController controller;
    
    public ArrayList<Image> images; 
    
    public ArrayList<Character> ids; 
    
    ArrayList<ImageBox> selection; 
    
    JButton changeMap; 
    JButton getCharsButton;
    JSlider squareSlider;
    
    JButton importTiles; 
    
    JPanel selectPanel; 
    JFrame parent;
    
    public ImageSelect(MapController control, MapModel model, Image[] images, char[] ids, JFrame parent)
    {
        this.parent = parent;
        setPreferredSize(new java.awt.Dimension(180, 560));
        
        controller = control; 
        
        this.model = model; 
    
        this.ids = new ArrayList<>();
        this.images= new ArrayList<>();
        
        for(int i=0; i<images.length; i++)
        {
            this.ids.add(ids[i]);
            this.images.add(images[i]);
        }
        
        squareSlider = new JSlider(); 
        squareSlider.setMaximum(48); 
        squareSlider.setMinimum(8);
        
        squareSlider.addChangeListener(event -> {
            setTileSize(squareSlider.getValue());
            revalidateAndRepaintView();
            repaint();
        });
        squareSlider.setValue(model.tileWidth/4*3);
        
        changeMap = new JButton("Change Game Map!");
        
        changeMap.addActionListener(event -> controller.setGameMap(getMapTextAsArray()));
        
        getCharsButton = new JButton("Get Character Map");
        
        
        getCharsButton.addActionListener(event -> {
            JDialog dialog = new JDialog();
            JTextArea field = new JTextArea();
            field.setEditable(false);
            field.setText(getMapText());
            dialog.add(field);
            dialog.pack();
            dialog.setVisible(true);
        });
        
        importTiles = new JButton("Import Tiles...");
        
        importTiles.addActionListener(new ButtonHandler());
        
        changeMap.setEnabled(true); 
        
        this.setFocusable(true);
        
        setLayout(new java.awt.FlowLayout());
        
        selection = new ArrayList<>();
        
        selectPanel = new JPanel();
        selectPanel.setPreferredSize(new java.awt.Dimension(180, 420));
        selectPanel.setMaximumSize(new java.awt.Dimension(360, 800));
        
        for(int i=0; i<images.length; i++)
        {
            selection.add(new ImageBox(images[i], i));
            selectPanel.add(selection.get(i));
        }
        
        add(selectPanel);
        add(squareSlider); 
        add(changeMap);
        add(getCharsButton);
        add(importTiles);
        
        selection.get(0).mouseClickMethod();
        
        setVisible(true);
    }
    
    public void deselectAll()
    {
        for (ImageBox aSelection : selection) {
            aSelection.selected = false;
        }
    }
    
    public String[] getMapTextAsArray()
    {
        return model.getMapTextAsArray();
    }
    
    public String getMapText()
    {
        return model.getMapText();
    }
    
    public void revalidateAndRepaintView()
    {
        model.view.revalidate();
        model.view.repaint();
    }
    
    public void setTileSize(int size)
    {
        controller.changeTileSize(size);
    }
    
    @Override
    public void paintComponent(java.awt.Graphics g)
    {
        super.paintComponent(g);
    }
    
    private class ButtonHandler implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent event)
        {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.showOpenDialog(ImageSelect.this.model.view);
            File file = chooser.getSelectedFile();
                
            Image image = new ImageIcon(file.getAbsolutePath()).getImage();
            ImageSelect.this.images.add(image.getScaledInstance(GameModel.blockWidth, GameModel.blockHeight, 0));
            ImageSelect.this.selection.add(new ImageBox(image, images.size()-1));
            char id = javax.swing.JOptionPane.showInputDialog("Set Character ID to:").charAt(0);
            while(charIsTaken(id))
            {
                id = javax.swing.JOptionPane.showInputDialog("Set Character ID to:").charAt(0);
            }
            ids.add(id);
            selectPanel.add(selection.get(selection.size()-1));

            if (selection.size() % 2 == 1)
            {
                selectPanel.setPreferredSize(new java.awt.Dimension(
                                    selectPanel.getPreferredSize().width, 
                                    selectPanel.getPreferredSize().height+70));
                parent.setSize(parent.getSize().width, parent.getSize().height + 70);
            }
            selectPanel.revalidate();
            selection.get(selection.size()-1).mouseClickMethod();
            parent.pack();
            repaint();
            parent.repaint();
        }
    }
    
    public boolean charIsTaken(char c)
    {
        for (Character id : ids) {
            if (c == id) {
                return true;
            }
        }
        return false;
    }
    
    private class ImageBox extends JPanel
    {
        Image image;
        
        boolean selected; 
        int id; 
        
        public ImageBox(Image image, int id)
        {
            this.id = id; 
            
            this.image = image.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            
            selected = false;
            
            addMouseListener(new MouseHandler());
            
            setPreferredSize(new java.awt.Dimension(65, 65));
        
        }
        
        @Override
        public void paintComponent(java.awt.Graphics g)
        {
            super.paintComponent(g);
            
            g.drawImage(image,1,1,null);
            
            if (selected)
            {
                g.setColor(Color.black);
                g.drawRect(0, 0, 64,64);
                g.drawRect(0, 0, 65,65);
            }
        }
        
        public void mouseClickMethod()
        {
            deselectAll();

            selected = true;

            model.setCurrentImage(images.get(id));
            model.currentID = ids.get(id);
                
        }
    
        
        private class MouseHandler extends MouseAdapter
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                mouseClickMethod();
                
                ImageSelect.this.repaint();
            }
        }
    }
}
