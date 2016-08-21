
package nars.experiment.nario2;

import java.awt.Graphics; 
import java.awt.Image;
import javax.swing.ImageIcon;

public class Mushroom extends Enemy
{
    Image image; 
    int direc; 
    
    public Mushroom(int x, int y, int speed, Player player)
    {
        super(x, y, speed, player);
        
        direc = 1; 
        
        image = new ImageIcon(this.getClass().getResource("images/mushroom.gif")).getImage();
    }
    
    @Override
    public void update()
    {
        if (falling)
        {
            move(0, GameModel.playerSpeed);
        }
        if (!getRightBlock() && !walksLeft)
        {
            direc = 1;
            setWalksRight(true);
        }
        else if (getRightBlock())
        {
            direc = -1;
            setWalksLeft(true);
        }
        if (getLeftBlock())
        {
            setWalksLeft(false);
        }
        move(direc*GameModel.playerSpeed/2, 0);
    }
    
    @Override
    public void draw(Graphics g, int ew, int eh)
    {
        g.drawImage(image, screenX, screenY, ew, eh, null);
    }
}
