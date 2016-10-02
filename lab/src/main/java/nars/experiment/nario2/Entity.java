
package nars.experiment.nario2;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class Entity 
{
    int leftX; 
    int midX; 
    int rightX; 
    
    int topY; 
    int bottomY; 
    int midY;
    
    int screenX; 
    int screenY; 
    
    int leftColumn; 
    int rightColumn; 
    int topRow; 
    int bottomRow; 
    int midRow; 
    int midColumn; 
    
    int health; 
    
    boolean standsLeft; 
    boolean standsRight; 
    boolean walksLeft; 
    boolean walksRight;
    
    boolean runsLeft; 
    boolean runsRight; 
    
    private boolean leftBlock; 
    private boolean rightBlock;
    
    boolean falling; 
    boolean jumping;
    
    boolean jumpLock;
    
    int     jumpCount; 
    int speed;

    boolean walkCount; 
    
    boolean isDead; 
    
    CopyOnWriteArrayList<Bullet> bullets;
    

    static Image img(String i) {
        String path = "images/" + i;
        URL r = res(path);

        return new ImageIcon(r).getImage();
    }

    static URL res(String path) {
        return Entity.class.getClassLoader().getResource(
                    "nars/experiment/nario2/" + path);
    }

    Image walkLeft = img("MarioSmallWalkingLeft.gif");
    public Image walkingLeft = walkLeft.getScaledInstance(32, 32, Image.SCALE_SMOOTH);

    Image walkRight = img("MarioSmallWalkingRight.gif");
    public Image walkingRight = walkRight.getScaledInstance(32, 32, Image.SCALE_SMOOTH);

    Image standLeft = img("MarioSmallStandingLeft.gif");
    public Image standingLeft = standLeft.getScaledInstance(32, 32, Image.SCALE_SMOOTH);

    Image standRight = img("MarioSmallStandingRight.gif");
    public Image standingRight= standRight.getScaledInstance(32, 32, Image.SCALE_SMOOTH);

    
    public Entity(int x, int y, int speed)
    {
        bullets = new CopyOnWriteArrayList<>();

        this.speed = speed;

        walkCount = jumpLock = false; 
        
        standsLeft = walksLeft = walksRight = false;
        
        falling = jumping = false;
        
        standsRight = true;
        
        leftBlock = rightBlock = false;
        
        jumpCount = 0;
        
        setFalling(true);
        
        leftX = x; 
        topY  = y;
       
        screenX = leftX; 
        screenY = topY; 
        
        rightX  = leftX + GameModel.blockWidth;
        bottomY = topY  + GameModel.blockHeight;
        
        midX  = leftX + GameModel.blockWidth/2; 
        midY  = topY + GameModel.blockWidth/2;
        
        isDead = false;
    }
    
    public void move(int x, int y)
    {
        leftX += x; 
        rightX+= x; 
        midX  += x;
        
        bottomY+=y; 
        topY   +=y; 
        midY   +=y;
        
        screenX += x; 
        screenY += y;
    }
    
    public void shift(int x, int y)
    {
        leftX += x; 
        rightX+= x; 
        midX  += x;
        
        bottomY+=y; 
        topY   +=y; 
        midY   +=y;
    }
    
    public void jump()
    {
        if (!jumpLock)
        {
            setFalling(false);
            jumping = true;
            
            jumpLock = true;
            
            jumpCount = 0;
        }
    }
    
    public void shoot(int x, int y)
    {
        bullets.add(new Bullet(screenX+GameModel.blockWidth/2f, screenY, x, y, false));
    }
    
    public void update()
    {
        if (GameModel.frames % (GameModel.FPS/4) == 0)
        {
            walkCount = !walkCount;
        } 
        
        if (falling)
        {
            move(0, GameModel.playerSpeed);
        }
        if (jumping)
        {
            move(0, (int)(-.25*(22-jumpCount)*speed));
            jumpCount++;
            
            if (jumpCount > 44)
            {
                setFalling(true);
            }
        }           
    }
    
    public void draw(Graphics g, int ew, int eh)
    {
        Image img = null;
        if (standsLeft)
        {
            img = standingLeft;
        }
        else if (standsRight)
        {
            img = standingRight;
        }
        else if (walksLeft && walkCount)
        {
            img = walkingLeft;
        }
        else if (walksLeft)
        {
            img = standingLeft;
        }
        else if (walksRight && walkCount)
        {
            img = walkingRight;

        }
        else if (walksRight)
        {
            img = standingRight;
        }

        if (img!=null)
            g.drawImage(img, screenX, screenY, ew, eh, null);

        for (Bullet bullet : bullets) {
            bullet.draw(g);
        }
    }
    
    public int getHealth()
    {
        return health;
    }
    
    public void setHealth(int h)
    {
        health = h;
    }
    
    public boolean isDead()
    {
        return isDead; 
    }
    
    public void setIsDead(boolean b)
    {
        isDead = b; 
    }
    
    public List<Bullet> getBullets()
    {
        return bullets; 
    }
    
    public boolean isJumping()
    {
        return jumping;
    }
    
    public int getLeftX()
    {
        return leftX;
    }
    
    public int getMidX()
    {
        return midX;
    }
    
    public int getTopY()
    {
        return topY;
    }
    
    public int getMidY()
    {
        return midY;
    }
    
    public int getBottomY()
    {
        return bottomY;
    }
    
    public int getRightX()
    {
        return rightX; 
    }
    
    public boolean getRightBlock()
    {
        return rightBlock; 
    }
    
    public boolean getLeftBlock()
    {
        return leftBlock; 
    }
    
    public void setLeftBlock(boolean b)
    {
        leftBlock = b;
    }
    
    public void setRightBlock(boolean b)
    {
        rightBlock = b;
    }
    
    public void setJumpLock(boolean b)
    {
        jumpLock = b;
    }
    
    public void setFalling(boolean b)
    {
        falling = b; 
        
        if (falling)
        {
            jumping  = false;
            //jumpLock = false;
            jumpCount= 0; 
        }
    }

    public void setStandsLeft(boolean b)
    {
        standsLeft = b; 
        
        if (standsLeft)
        {
            standsRight = false; 
            walksRight  = false;
            walksLeft = false;
        }
    }
    
    public void setWalksLeft(boolean b)
    {
        walksLeft = b; 
        
        if (walksLeft)
        {
            standsRight = false; 
            walksRight  = false;
            standsLeft  = false;
        }
    }
    
    public void setStandsRight(boolean b)
    {
        standsRight = b; 
        
                if (standsRight)
        {
            standsLeft = false; 
            walksRight  = false;
            walksLeft  = false;
        }
    }
    
    public void setWalksRight(boolean b)
    {
        walksRight = b; 
        
         if (walksRight)
        {
            standsRight = false; 
            standsLeft  = false;
            walksLeft  = false;
        }
    }
    
    public void getRowsAndColumns()
    {
        leftColumn = (getLeftX()-1) / GameModel.blockWidth;
        rightColumn= getRightX()/ GameModel.blockWidth;
        topRow     = getTopY()  / GameModel.blockHeight;
        bottomRow  = getBottomY()/GameModel.blockHeight;
        midColumn  = getMidX()  / GameModel.blockWidth; 
        midRow     = getMidY()  / GameModel.blockHeight; 
    }
}
