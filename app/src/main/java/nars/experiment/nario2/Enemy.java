
package nars.experiment.nario2;

import java.awt.Graphics; 
import java.awt.Color; 

public class Enemy extends Entity
{
    int maxHealth; 
    
    Player enemy;

    public Enemy(int x, int y, int speed, Player enemy)
    {
        super(x, y, speed);

        screenX = x - (enemy.leftX - enemy.screenX);
        screenY = y - (enemy.topY  - enemy.screenY);
        
        this.enemy = enemy; 
        
        maxHealth = 200; 
        
        setHealth(maxHealth);
    }
    
    @Override
    public void update()
    {
        super.update();
        
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
        
        if (GameModel.frames % 64 == 0)
        {
            shoot(enemy.getOldX(), enemy.getOldY());
        }
        
        if (enemy.getOldX() < getMidX())
        {
            if (!getLeftBlock())
            {
                move(-1*speed,0);
                setWalksLeft(true);
            }
            else
            {
                jump();
            }
        }
        else if (enemy.getOldX() > getMidX())
        {
            if (!this.getRightBlock())
            {
                move(1*speed,0);
                setWalksRight(true);
            }
            else
            {
                jump();
            }
        }
        
        if (getHealth() <= 0)
        {
            setIsDead(true);
        }
    }
    
    @Override
    public void shift(int x, int y)
    {
        screenX += x; 
        screenY += y;
        
        for(Bullet bullet : bullets)
        {
            bullet.shift(x, y);
        }
    }

    @Override
    public void shoot(int x, int y)
    {
        getBullets().add(new Bullet(screenX, screenY, x, y, true));
    }
    
    @Override
    public void draw(Graphics g, int ew, int eh)
    {
        if (!isDead())
        {
            super.draw(g, ew, eh);

            g.setColor(Color.red);

            int len = Math.abs(getLeftX() - getRightX());
            double part = len * ((double)getHealth() / (double)maxHealth);

            g.fillRect(screenX, screenY-len/5, (int)part, len/10);

            g.setColor(Color.black);
            g.drawRect(screenX-1, screenY-len/5-1, (int)part+2, len/10+2);
        }
    }
}
