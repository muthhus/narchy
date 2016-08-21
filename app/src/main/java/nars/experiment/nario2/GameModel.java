
package nars.experiment.nario2;

import nars.experiment.nario2.levels.Level1;
import nars.experiment.nario2.mapeditor.MapEditor;

import java.applet.Applet;
import java.applet.AudioClip;


import java.awt.Graphics; 
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class GameModel 
{
    Image backImage; 
    
    public GamePanel panel;
    
    public static MapEditor editor;
    
    public static int blocksHorizontal = 34; 
    public static int blocksVertical   = 24;
    
    public static int blockWidth  = 64;
    public static int blockHeight = 64;

    public static int screenWidth = blockWidth *blocksHorizontal;  
    public static int screenHeight= blockHeight*blocksVertical;

    public static int scale = 1;
    public static int viewWidth = screenWidth / scale;
    public static int viewHeight= screenHeight / scale;


    public static int frameRate = 25;
    public static int FPS = 1000/frameRate; 
    
    public static int playerSpeed = 20;
    public static int playerBulletSpeed= 25;
    public static int enemyBulletSpeed = playerBulletSpeed;
    
    public int upKey   = KeyEvent.VK_SPACE;
    public int leftKey = KeyEvent.VK_A;
    public int rightKey= KeyEvent.VK_D; 
    public int downKey = KeyEvent.VK_S;
    public int pauseKey= KeyEvent.VK_P;   
    public int soundTog= KeyEvent.VK_T;
    
    public final AudioClip shootSound   =
            Applet.newAudioClip(getClass().getResource("sounds/shoot.wav"));

    public final AudioClip jumpSound    =
            Applet.newAudioClip(getClass().getResource("sounds/jump.wav"));
    
    public final AudioClip brickBreak   =
            Applet.newAudioClip(getClass().getResource("sounds/brickBreak.wav"));
   
    public final AudioClip brickUnbreak =
            Applet.newAudioClip(getClass().getResource("sounds/brickUnbreak.wav"));
    
    public final AudioClip gameOverSound=
            Applet.newAudioClip(getClass().getResource("sounds/gameOver.wav"));
    
    public final AudioClip coinSound    =
            Applet.newAudioClip(getClass().getResource("sounds/coin.wav"));
    
    public final AudioClip oneUp        =
            Applet.newAudioClip(getClass().getResource("sounds/oneUp.wav"));

    public ArrayList<AudioClip> clipsToPlay;
    
    public static int frames;
     
    public int enemiesToSpawn; 

    public boolean leftKeyPress;
    public boolean spaceBarPress; 
    public boolean downKeyPress; 
    public boolean upKeyPress;          
    public boolean rightKeyPress; 
    
    public boolean screenScroll;
    
    public boolean playing; 
    
    public int     checkFrame;
    public boolean clicking; 
    public int[]   clickCoors;

    public Player player; 
    public Level  level ;
    public ArrayList<Enemy> enemies; 
    public ArrayList<Mushroom> mushrooms; 
    
    public NavigationBar navigation; 
    
    public int waveNumber; 
    public int enemiesKilled; 
    public int bulletsShot; 
    public int hitsTaken; 
    public float accuracy; 
    public int bulletsHit; 
    public int bulletsMissed; 
    
    public JFrame statsFrame; 
    public JFrame endStatsFrame;
    public GameStatsPanel stats; 
    
    ExecutorService executor = Executors.newWorkStealingPool();
    
    public GameModel(NavigationBar navBar)
    {           
        clipsToPlay = new ArrayList<>();
        
        navigation = navBar; 

        initGame(null);
        
        stats = new GameStatsPanel(this);
        statsFrame = new JFrame("Game Stats");
        statsFrame.setSize(180,180);
        statsFrame.add(stats);
        statsFrame.setVisible(false) ;
        statsFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
    
    public void initGame(String[] lvl)
    {
        waveNumber   = 0;
        enemiesKilled= 0;
        bulletsShot  = 0;
        hitsTaken    = 0;
        bulletsHit   = 0;
        bulletsMissed= 0;
        
        clicking     =
        leftKeyPress = 
        upKeyPress   =
        spaceBarPress= 
        downKeyPress = 
        rightKeyPress= false;
        
        playing = true;
        pauseKeyPress();
        
        screenScroll = true;

        player = new Player(blockHeight*blocksHorizontal/2, blockHeight*blocksVertical/2, playerSpeed, this);
        
        enemies = new ArrayList<>();
        mushrooms= new ArrayList<>();
    
        if (lvl == null)
            level  = new Level1(null);
        else 
            changeMap(lvl);
        
        clickCoors = new int[2];
        
        enemiesToSpawn = 4;
    }
    
    public void frameUpdate()
    {
        if(checkOutBounds(player) || player.health <= 0)
        {
            clipsToPlay.add(gameOverSound);
            
            showNewStatsPanel();
            
            initGame(level.charMap);
        }

        Future fStats = executor.submit(new StatsFrameUpdater(this));

        if (playing)
        {
            collisionTests(player); 
            player.update();
        
            updateMushrooms();
            
            updateEnemies();

            updateBullets();
            
            keyUpdate();
            
            mouseUpdate();
            
            adjustPlayerCoors(player);

            level.update();

            spawnNewEnemies();

            frames++;
        }
        
        while (!fStats.isDone())
        {
        }
    }
    
    public boolean checkOutBounds(Entity player)
    {        
        player.getRowsAndColumns();
        
        if (player.rightColumn > level.getWidth()-1 || player.leftColumn < 0)
        {
            return true;
        }
        else if (player.bottomRow >= level.getHeight() || player.topRow < 0)
        {
            return true;
        }
        return false;
    }
    
    public void updateMushrooms()
    {
        for(int i=0; i<mushrooms.size(); i++)
        {
            if (mushrooms.get(i).isDead())
            {
                mushrooms.remove(i);
                continue;
            }

            adjustPlayerCoors(mushrooms.get(i));
            mushrooms.get(i).update();
            collisionTests(mushrooms.get(i));
            adjustPlayerCoors(mushrooms.get(i));

            mushroomCollisionTest(mushrooms.get(i), player);
        }
    }
    
    public void updateEnemies()
    {
        for(int i=0; i<enemies.size(); i++)
        {
            if (enemies.get(i).isDead())
            {
                enemies.remove(i);
                continue;
            }
            adjustPlayerCoors(enemies.get(i));
            enemies.get(i).update();
            collisionTests(enemies.get(i));
            adjustPlayerCoors(enemies.get(i));
        }
    }
    
    public void updateBullets()
    {
        player.getBullets().forEach(this::bulletCollisionTests);

        for(Enemy enemy: enemies)
        {
            enemy.getBullets().forEach(this::bulletCollisionTests);
        }
    }
    
    public void keyUpdate()
    {
        if (leftKeyPress)
        {
            if (!player.getLeftBlock())
            {
                playerMove(player, -1*GameModel.playerSpeed, 0);
                player.setWalksLeft(true);               
            }
        }
        if (rightKeyPress)
        {
            if (!player.getRightBlock())
            {
                playerMove(player, GameModel.playerSpeed, 0);
                player.setWalksRight(true);
            }
        }
        if (upKeyPress)
        {
            if (!player.jumpLock)
                clipsToPlay.add(jumpSound);
             
            player.jump();
            
            upKeyPress = false;
        }
    }
    
    public void mouseUpdate()
    {
        if (clicking && frames > checkFrame)
        {
            if (panel.getMousePosition() != null)
            {
                player.shoot(panel.getMousePosition().x , 
                             panel.getMousePosition().y );
                
                bulletsShot++;
                
                if (navigation.fxIsOn)
                    clipsToPlay.add(shootSound);
                
                checkFrame = frames+(int)(FPS*.064);
            }
        }
    }
    
    public void adjustPlayerCoors(Entity player)
    {
        player.getRowsAndColumns();
        
        if (testCollision(player.midColumn, player.bottomRow))
        {
            while (player.getBottomY() % GameModel.blockHeight != 0)
            {
                playerMove(player, 0,-1);
            }
        }
        if (testCollision(player.leftColumn, player.midRow))
        {
            while (player.getLeftX() % GameModel.blockWidth != 0)
            {
                playerMove(player, 1,0);
            }
        }
        if (testCollision(player.rightColumn, player.midRow))
        {
            while (player.getRightX() % GameModel.blockWidth != 0)
            {
                playerMove(player, -1,0);
            }
        }
    }
    
    public void playerMove(Entity player, int x, int y)
    {
        if (screenScroll && player == GameModel.this.player)
        {
            level.shift(-x, -y);
            
            player.shift(x, y);
            
            for(Enemy enemy : enemies)
                enemy.shift(-x,-y);
            
            for(Mushroom mush : mushrooms)
                mush.shift(-x, -y);
        }
        else
        {
            player.move(x, y);
        }
    }
    
    public void leftKeyReleased()
    {
        player.setWalksLeft(false);
        player.setStandsLeft(true);
    }
    
    public void spaceBarReleased()
    {
        
    }
    
    public void upKeyReleased()
    {
        
    }
    
    public void pauseKeyPress()
    {
        if (playing)
        {
            navigation.playButton.setEnabled(true);
            navigation.pauseButton.setEnabled(false);
            navigation.setVisible(true);
        }
        else
        {
            navigation.playButton.setEnabled(false);
            navigation.pauseButton.setEnabled(true);
            navigation.setVisible(false);
            
            if (editor != null)
            {
                try
                {
                    editor.frame.setVisible(false);
                    editor.frame2.setVisible(false);
                }
                catch (NullPointerException ignored)
                {
                    
                }
            }
        }
        playing = !playing;
    }
    
    public void rightKeyReleased()
    {
        player.setWalksRight(false);
        player.setStandsRight(true);
    }
    
    public void bulletCollisionTests(Bullet bullet)
    {
        for(LevelElement tile: level.getAllElements())
        {
            if (tile != null)
            {
                if (squareCollision(bullet.getX(), bullet.getY(), 
                                     tile.getX() , tile.  getY(), 
                                    blockWidth   , blockHeight))
                {
                    if (!bullet.isEnemy())
                        bulletsMissed++;
                    
                    bullet.setDispose(true);
                }
            }
        }
        if (!bullet.isEnemy() && !bullet.getDispose())
        {
            enemies.stream().filter(enemy -> squareCollision(bullet.getX(), bullet.getY(),
                    enemy.screenX, enemy.screenY,
                    Math.abs(enemy.getLeftX() - enemy.getRightX()),
                    Math.abs(enemy.getTopY() - enemy.getBottomY()))).forEach(enemy -> {
                enemy.setHealth(enemy.getHealth() - bullet.getDamage());

                bullet.setDispose(true);

                bulletsHit++;

                if (enemy.getHealth() <= 0) {
                    if (enemy.isDead == false) {
                        enemiesKilled++;
                    }
                    enemy.setIsDead(true);

                }
            });
        }
        else if (bullet.isEnemy() && !bullet.getDispose())
        {
            if (squareCollision(bullet.getX(), bullet.getY(), 
                                player.screenX,player.screenY,
                                Math.abs(player.getLeftX()-player.getRightX()), 
                                Math.abs(player.getTopY()- player.getBottomY())))
            {
                hitsTaken++; 
                player.health -= bullet.getDamage();
                
                bullet.setDispose(true);
            }
        }
    }
    
    public void mushroomCollisionTest(Mushroom mush, Entity player)
    {
        if (squareCollision(mush.screenX+GameModel.blockWidth/2, mush.screenY+GameModel.blockWidth/2, player.screenX, player.screenY, 
                            Math.abs(player.getLeftX()-player.getRightX()), 
                            Math.abs(player.getTopY() -player.getBottomY())))
        {
            mush.setIsDead(true);
            clipsToPlay.add(oneUp);
            player.health += 50;  
        }
    }
    
    public void spawnNewEnemies()
    {

        if (enemies.size() <= 0)
        {
            enemiesToSpawn++; 
            waveNumber++;

            java.util.Random rand = new java.util.Random();
            for(int i = 0; i<enemiesToSpawn; i++)
            {
                enemies.add(
                        new Enemy(blockWidth*2 +
                                  rand.nextInt(
                                      level.getWidth()*blockWidth-5*blockWidth), playerSpeed,
                                  0, player));
            }
        }
    }
    
    public static boolean squareCollision(int x, int y, int dx, int dy,
                                          int xDim, int yDim)
    {
        return x > dx && x < dx + xDim && y > dy && y < dy + yDim;
    }
    
    public void collisionTests(Entity player)
    {
        player.getRowsAndColumns();
        
        if (testCollision(player.midColumn, player.bottomRow))
        {
            player.setFalling(false);
            player.setJumpLock(false);
        }
        else if (!player.isJumping())
        {
            player.setFalling(true);
        }
        
        if (testCollision(player.midColumn, player.topRow))
        {
            player.setFalling(true);
            
            if (player.equals(GameModel.this.player))
            {
                collideWithBlock(player.midColumn, player.topRow);
            }
        }
        
        if (testCollision(player.leftColumn, player.midRow))
        {
            player.setLeftBlock(true);
        }
        else
        {
            player.setLeftBlock(false);
        }
        
        if (testCollision(player.rightColumn, player.midRow))
        {
            player.setRightBlock(true);
        }
        else
        {
            player.setRightBlock(false);
        }
    }
    
    public boolean testCollision(int column, int row)
    {
        return level.getElementAt(row, column) != null;
    }
    
    public void collideWithBlock(int col, int row)
    {
        LevelElement element = level.getElementAt(row, col);

        if (element.id == 'S' || element.id == 'g' || element.id == 'b')
        {
            level.setElementNull(row, col);
            clipsToPlay.add(brickBreak);
        }
        else if (element.id == 'Q')
        {
            level.getElementAt(row, col).setID('B');
            level.getElementAt(row, col).setImage(level.brownBlock);
            mushrooms.add(new Mushroom(col*blockWidth, row*(blockHeight-1), playerSpeed, player));
            clipsToPlay.add(coinSound);
        }
        else
        {
            clipsToPlay.add(brickUnbreak);
        }
    }

    public void paintElements(Graphics g)
    {
        level.draw(g, blockWidth, blockHeight);
        
        for(Enemy enemy : enemies)
        {
            enemy.draw(g, blockWidth, blockHeight);
        }
        
        for(Mushroom mush : mushrooms)
        {
            mush.draw(g, blockWidth, blockHeight);
        }
        
        player.draw(g, blockWidth, blockHeight);
    }
    
    public void setPanel(GamePanel panel)
    {
        this.panel = panel; 
    }
    
    public int getAccuracy()
    {
        return bulletsShot != 0 ? (int) ((float) bulletsHit / (float) bulletsShot * 100) : 0;
    }
    
    public void changeGameMap(String[] newMap)
    {
        initGame(newMap);    
    }
    
    public void changeMap(String[] newMap)
    {
        this.level = new Level1(newMap);
      
        while( level.getElementAt(player.getBottomY()/blockHeight,
                                  player.getMidX()   /blockWidth ) != null ) 
        {
            player.move(0,-blockHeight); 
        }         
    }
    
    public void setBackImage(Image image)
    {
        this.backImage = image;
        Image back = backImage.getScaledInstance(panel.getWidth(),
                                                 panel.getHeight(),
                                                 Image.SCALE_SMOOTH);  
        backImage = back;
        
        panel.repaint();
    }
    
    public void showNewStatsPanel()
    {
        statsFrame.setVisible(false);
        
        endStatsFrame = new JFrame("Game Stats");
        endStatsFrame.setSize(180,180);
        endStatsFrame.add(new GameStatsPanel(this.stats));
        endStatsFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        endStatsFrame.setVisible(true);
    }
    
    public static void setGameSpeed(int value)
    {
        frameRate  = value; 
        FPS        = 1000/frameRate; 
        playerSpeed= value / 8;
        enemyBulletSpeed = playerSpeed*2; 
        playerBulletSpeed= playerSpeed*4; 
    }
}


/*
    public void collisionTests(Entity player)
    {
        if (bottomCollision(player))
        {
            player.setFalling(false);
            player.setJumpLock(false);
        }
        
        else if (!player.isJumping())
        {
            player.setFalling(true);
        }
        
        if (upperCollision(player))
        {
            player.setFalling(true);
        }
        
        if (leftCollision(player))
        {
            System.out.println("ASFSDF");
            player.setLeftBlock(true);
        }
        else
        {
            player.setLeftBlock(false);
        }
        
        if (rightCollision(player))
        {
            player.setRightBlock(true);
        }
        else
        {
            player.setRightBlock(false);
        }
    }
    
    public boolean bottomCollision(Entity player)
    {
        int column = player.getMidX() / GameModel.blockWidth;
        int row    = (player.getBottomY()) / GameModel.blockHeight;
        
        if (level.getElementAt(row, column) != null)
        {
            return true;
        }
        return false;
    }
    
    public boolean upperCollision(Entity player)
    {
        int column = player.getMidX() / GameModel.blockWidth;
        int row    = player.getTopY() / GameModel.blockHeight;
        
        if (level.getElementAt(row, column) != null)
        {
            return true;
        }
        return false;
    }
    
    public boolean leftCollision(Entity player)
    {
        int column = player.getLeftX() / GameModel.blockWidth;
        int row    = (player.getMidY()) / GameModel.blockHeight;
        
        if (level.getElementAt(row, column) != null)
        {
            return true;
        }
        return false;
    }
    
    public boolean rightCollision(Entity player)
    {
        int column = player.getRightX() / GameModel.blockWidth;
        int row    = player.getMidY() / GameModel.blockHeight;
        
        if (level.getElementAt(row, column) != null)
        {
            return true;
        }
        return false;
    }
    
 */