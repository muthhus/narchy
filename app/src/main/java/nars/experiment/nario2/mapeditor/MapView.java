
package nars.experiment.nario2.mapeditor;

import java.awt.Color;
import java.awt.Component;

public class MapView extends javax.swing.JPanel implements Runnable
{
    MapModel model; 
    Component parent; 
    
    boolean mousePressed;
    
    public MapView(Component frame, MapModel model)
    {
        this.parent = frame;
        this.model = model;
        
        this.setBackground((Color.white));
        
        this.addMouseListener(new MouseHandler());
        this.addComponentListener(new ComponentHandler());
                
        Thread thread = new Thread(this); 
        thread.start();
        
    }
    
    public void drawGrid(java.awt.Graphics g)
    {
        g.setColor(Color.lightGray);
        
        for(int i=0; i<model.width*model.tileWidth; i+=model.tileWidth)
        {
            g.drawLine(i, 0, i, model.height*model.tileHeight);
        }
        for(int i=0; i<model.height*model.tileHeight; i+=model.tileHeight)
        {
            g.drawLine(0, i, model.width*model.tileWidth, i);
        }
    }
    
    public void drawMap(java.awt.Graphics g)
    {
        for(int i=0; i<model.charMap.length; i++)
        {
            for(int j=0; j<model.charMap[i].length; j++)
            {
                if (model.tiles[i][j] != null)
                {
                    model.tiles[i][j].draw(g);
                }
            }
        }
    }
    
    @Override
    public void run()
    {
        while(true)
        {
            try
            {
                if (mousePressed && getMousePosition() != null && isVisible())
                {
                    int col = getMousePosition().x / (model.tileWidth);
                    int row = getMousePosition().y / (model.tileHeight);

                    if (row < model.tiles.length && col < model.tiles[0].length)
                    {
                        model.tiles[row][col] = new Tile(col        ,          row, 
                                                    model.tileWidth, model.tileHeight, 
                                                    model.currentImage, model.currentID);
                        model.updateCharArray();
                    }
                    model.updateTileArray();
                    
                    repaint();

                }
            }
            catch (NullPointerException ignored)
            {
                
            }
        }
    }
    
    @Override
    public void paintComponent(java.awt.Graphics g)
    {
        super.paintComponent(g);
        
        drawGrid(g);
        
        drawMap(g);
    }
    
    private class MouseHandler extends java.awt.event.MouseAdapter
    {
        @Override
        public void mousePressed(java.awt.event.MouseEvent event)
        {
            mousePressed = true;
        }
        
        @Override
        public void mouseReleased(java.awt.event.MouseEvent event)
        {
            mousePressed = false;
        }
    }
    
    private class ComponentHandler extends java.awt.event.ComponentAdapter
    {
        @Override
        public void componentResized(java.awt.event.ComponentEvent event)
        {
            if ((getWidth() / model.tileWidth >= model.width) &&
            (getHeight()/model.tileHeight >= model.height))
            {                 
                model.width = getWidth()/model.tileWidth;
                model.height= getHeight()/model.tileHeight;

                model.resizeMap();
                
                setPreferredSize(getSize()); 
            }
        }
    }
}
