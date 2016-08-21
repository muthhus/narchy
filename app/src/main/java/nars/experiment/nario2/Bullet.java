
package nars.experiment.nario2;

import java.awt.Graphics; 
import java.awt.Color; 

public class Bullet 
{
    private final Color color;
    private final int r;
    private double x;
    private double y; 
    
    private final double dX;
    private final double dY;
    
    private double distX; 
    private double distY;
    
    private final double speed;
    
    private boolean dispose; 
    
    private final boolean isEnemy;
    
    private final int damage;
    
    public Bullet(double x, double y, double dX, double dY, boolean e)
    {
        this.x = x; 
        this.y = y;
        
        this.dX = dX; 
        this.dY = dY;
        
        distX = dX - x; 
        distY = dY - y;

        speed = !e ? GameModel.playerBulletSpeed : GameModel.enemyBulletSpeed
                    + (Math.random() * 2); //jitter
        color = new Color((float)(Math.random()/2+0.5), 0.1f, 0.1f);

        this.r = 8 + (int)Math.round(Math.random() * 8);

        isEnemy = e; 
        dispose = false;
        
        damage = r/2;
    }
    
    public boolean isEnemy()
    {
        return isEnemy; 
    }
    
    public int getDamage()
    {
        return damage; 
    }
    
    public boolean getDispose()
    {
        return dispose; 
    }
    
    public void setDispose(boolean b)
    {
        dispose = b; 
    }
    
    public int getX()
    {
        return (int)x+4;
    }
    
    public int getY()
    {
        return (int)y+4;
    }
    
    public void update()
    {
        double hypotenuse = Math.sqrt(distX*distX + distY*distY);
        distX /= hypotenuse;
        distY /= hypotenuse;

        double theta = Math.atan2(distY, distX);

        distX = speed*Math.cos(theta);
        distY = speed*Math.sin(theta);

        x += distX;
        y += distY; 

        if (x > GameModel.viewWidth || x < 0)
        {
            dispose = true;
        }
    }

    public void shift(int x, int y)
    {
        this.x += x; 
        this.y += y;
    }  
    
    public void draw(Graphics g)
    {
//        g.setColor(Color.black);
//        g.drawOval((int)x, (int)y, 8, 8);
//
        g.setColor(color);
        g.fillOval((int)x+1, (int)y+1, r, r);
    }
}
