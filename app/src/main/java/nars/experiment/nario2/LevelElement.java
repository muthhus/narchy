
package nars.experiment.nario2;

import java.awt.Image; 
import java.awt.Graphics; 

public class LevelElement
{
    //Values used to display the element
    private int x; 
    private int y; 
    
    //Actual x value of element on map
    int actualX; 
    int actualY; 
    
    private boolean onScreen; 
    
    public Image image; 
    
    public char id; 
    
    public LevelElement(int x, int y, Image image, char id)
    {
        this.x = x; 
        this.y = y; 
        
        this.id = id; 
        
        actualX = x;
        actualY = y;
        
        this.image = image;
        
        onScreen = true;
    }
    
    //Shifts the display value of the element,
    //but leaves the actual value intact; scrolls.
    public void move(int x, int y)
    {
        this.x += x; 
        this.y += y; 
    }
    
    public boolean getOnScreen()
    {
        return onScreen; 
    }
    
    public void setID(char id)
    {
        this.id = id; 
    }
    
    public void setImage(Image image)
    {
        this.image = image; 
    }
    
    public void setOnScreen(boolean b)
    {
        onScreen = b; 
    }
    
    public int getX()
    {
        return x; 
    }
    
    public int getY()
    {
        return y; 
    }
    
    public void draw(Graphics g, int w, int h)
    {
        if (onScreen)
        {
            g.drawImage(image, x, y, w, h, null);
        }
    }
}
