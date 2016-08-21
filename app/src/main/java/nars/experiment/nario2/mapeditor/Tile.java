
package nars.experiment.nario2.mapeditor;

import java.awt.Image;

public class Tile 
{
    int x; 
    int y; 
    
    int width; 
    int height; 
    
    char id; 

    Image image;
    
    public Tile(int x, int y, int width, int height, Image image, char id)
    {
        this.id = id; 
        
        this.x = x; 
        this.y = y; 
        
        this.width = width; 
        this.height= height; 
        
        if (image != null)
        {
            this.image = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        }
    }
    
    public void changeSize(int size)
    {
        width = height = size; 
        
        this.image = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }
    
    public void draw(java.awt.Graphics g)
    {
        g.drawImage(image, x*width, y*height, null);
    }
}
