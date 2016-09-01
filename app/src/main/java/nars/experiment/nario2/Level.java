
package nars.experiment.nario2;

import javax.swing.*;
import java.awt.*;

import static nars.experiment.nario2.Entity.res;


public abstract class Level 
{
    private LevelElement[]   allElements; 
    private LevelElement[][] tiles; 
    
    String charMap[];
    
    public Image groundImage = new ImageIcon(res("images/groundImage.gif")).getImage();
    public Image brick       = new ImageIcon(res("images/brick.gif")).getImage();
    public Image rock        = new ImageIcon(res("images/rock.png")).getImage();
    public Image grassUp     = new ImageIcon(res("images/grassUp.png")).getImage();
    public Image grassCornerL= new ImageIcon(res("images/grassCornerL.png")).getImage();
    public Image grassCornerR= new ImageIcon(res("images/grassCornerR.png")).getImage();
    public Image brownBlock  = new ImageIcon(res("images/brownBlock.png")).getImage();
    public Image sand        = new ImageIcon(res("images/sand.png")).getImage();
    public Image sandBrick   = new ImageIcon(res("images/sandBrick.jpeg")).getImage();
    public Image questionBlock=new ImageIcon(res("images/questionBlock.png")).getImage();

    public Level()
    {

    }
    
    public void initMap(String[] charMap)
    {
        this.charMap = charMap; 
        
        allElements = new LevelElement[charMap.length * charMap[0].length()];
        tiles       = new LevelElement[charMap.length] [charMap[0].length()];
                
        int counter = 0; 
        for(int i=0; i<charMap.length; i++)
        {
            for(int j=0; j<charMap[i].length(); j++)
            {
                try
                {
                    for(int k=0; k<GameModel.editor.imageSelect.ids.size(); k++)
                    {
                        if (charMap[i].charAt(j) == ':')
                        {
                            tiles[i][j] = null; 
                            allElements[counter] = tiles[i][j];
                        }
                        else if (charMap[i].charAt(j) == GameModel.editor.imageSelect.ids.get(k))
                        {
                            tiles[i][j] = new LevelElement(j*GameModel.blockWidth, 
                                                    i*GameModel.blockHeight, 
                                                    GameModel.editor.imageSelect.images.get(k),
                                                    GameModel.editor.imageSelect.ids.get(k));
                        }
                    }
                }
                catch (NullPointerException e)
                {
                    if (charMap[i].charAt(j) == ':')
                    {
                        tiles[i][j] = null; 
                        allElements[counter] = tiles[i][j];
                    }
                    else if (charMap[i].charAt(j) == 'g')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth , 
                                                    i*GameModel.blockHeight,
                                                    groundImage, 'g');
                    }
                    else if (charMap[i].charAt(j) == 'c')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth , 
                                                    i*GameModel.blockHeight,
                                                    groundImage, 'c');
                    }
                    else if (charMap[i].charAt(j) == 'r')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth , 
                                                    i*GameModel.blockHeight,
                                                    rock, 'r');
                    }
                    else if (charMap[i].charAt(j) == 'U')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth , 
                                                    i*GameModel.blockHeight,
                                                    grassUp, 'U');
                    }
                    else if (charMap[i].charAt(j) == 'L')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth , 
                                                    i*GameModel.blockHeight,
                                                    grassCornerL, 'L');
                    }
                    else if (charMap[i].charAt(j) == 'R')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth , 
                                                    i*GameModel.blockHeight,
                                                    grassCornerR, 'R');
                    }
                    else if (charMap[i].charAt(j) == 'B')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth , 
                                                    i*GameModel.blockHeight,
                                                    brownBlock, 'B');
                    }
                    else if (charMap[i].charAt(j) == 'b')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth , 
                                                    i*GameModel.blockHeight,
                                                    brick, 'b');
                    }
                    else if (charMap[i].charAt(j) == 's')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth, 
                                                    i*GameModel.blockHeight,
                                                    sand, 's');
                    }
                    else if (charMap[i].charAt(j) == 'S')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth, 
                                                    i*GameModel.blockHeight, 
                                                    sandBrick, 'S');
                    }
                    else if (charMap[i].charAt(j) == 'Q')
                    {
                        tiles[i][j] = new LevelElement(j*GameModel.blockWidth, 
                                                    i*GameModel.blockHeight, 
                                                    questionBlock, 'Q');
                    }
                }
                
                allElements[counter] = tiles[i][j];

                counter++;
            }
        }
    }
    
    public LevelElement getElementAt(int row, int col)
    {
        return tiles[row][col] != null ? tiles[row][col] : null;
    }
    
    public void setElementNull(int row, int col)
    {
        tiles[row][col] = null;
        int num = row*tiles[0].length;
        num += col; 
        
        allElements[num] = null;
    }
    
    public LevelElement[] getAllElements()
    {
        return allElements; 
    }
    
    public void shift(int x, int y)
    {
        for (LevelElement allElement : allElements) {
            if (allElement != null) {
                allElement.move(x, y);
            }
        }
    }
    
    public int getWidth()
    {
        return tiles[0].length;
    }
    
    public int getHeight()
    {
        return tiles.length; 
    }
    
    public void update()
    {
        for (LevelElement allElement : allElements) {
            if (allElement != null) {
                if (allElement.getX() > GameModel.viewWidth + GameModel.blockWidth) {
                    allElement.setOnScreen(false);
                } else if (allElement.getX() < -GameModel.blockWidth) {
                    allElement.setOnScreen(false);
                } else if (allElement.getY() > GameModel.viewHeight + GameModel.blockHeight) {
                    allElement.setOnScreen(false);
                } else if (allElement.getY() < -GameModel.blockHeight) {
                    allElement.setOnScreen(false);
                } else {
                    allElement.setOnScreen(true);
                }
            }
        }
    }
    
    public boolean stopScrollingLeft(int i)
    {
        for(int j=0; j<1; j++)
        {
            if (tiles[i][j] != null)
            {
                if (tiles[i][j].id == 'c' && tiles[i][j].getOnScreen())
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean stopScrollingRight(int i)
    {
        for(int j=tiles[i].length-1; j<tiles[i].length; j++)
        {
            if (tiles[i][j] != null)
            {
                if (tiles[i][j].id == 'c' && tiles[i][j].getOnScreen())
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    public void draw(Graphics g, int ew, int eh)
    {
        for (LevelElement allElement : allElements) {
            if (allElement != null) {
                allElement.draw(g, ew, eh);
            }
        }
    }
}
