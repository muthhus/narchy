
package nars.experiment.nario2;

import java.awt.Color;

public class Player extends Entity
{
    private int oldX; 
    private int oldY; 
    
    int maxHealth; 
    
    private final GameModel model;
        
    public Player(int x, int y, int speed, GameModel m)
    {
        super(x, y, speed);
        
        model = m; 
        
        maxHealth = 100; 
        health = maxHealth; 
    }
    
    @Override
    public void update()
    {
        if (GameModel.frames % (GameModel.FPS/4) == 0)
        {
            walkCount = !walkCount;
        } 
        
        if (falling)
        {
            model.playerMove(this, 0, speed);
        }
        if (jumping)
        {
            model.playerMove(this, 0, (int)(-.25*(22-jumpCount)*speed/5));
            jumpCount++;

            if (jumpCount > 44)
            {
                setFalling(true);
            }
        }        
        for(int i=0; i<bullets.size(); i++)
        {
            if (bullets.get(i).getDispose())
            {
                bullets.remove(i);
            }
            else
            {
                bullets.get(i).update();
            }
        }
        if (GameModel.frames % (GameModel.FPS*4) == 0)
        {
            setEnemyPerceievedValues();
        }
    }

    public int getOldX()
    {
        return oldX;
    }
    
    public int getOldY()
    {
        return oldY;
    }

    public void setEnemyPerceievedValues()
    {
        oldX = getMidX(); 
        oldY = getMidY();
    }
    
    @Override
    public void draw(java.awt.Graphics g, int ew, int eh)
    {
        super.draw(g, ew, eh);
        
        g.setColor(Color.green);

        int len = Math.abs(getLeftX() - getRightX());
        double part = len * ((double)health / (double)maxHealth);

        g.fillRect(screenX, screenY-len/5, (int)part, len/10);
        
        g.setColor(Color.black);
        g.drawRect(screenX-1, screenY-len/5-1, (int)part+2, len/10+2);
    }
}