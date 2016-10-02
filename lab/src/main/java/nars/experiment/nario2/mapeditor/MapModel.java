
package nars.experiment.nario2.mapeditor;

import java.awt.*;

public class MapModel 
{
    int tileWidth = 32; 
    int tileHeight= 32; 
    int width     = 34;
    int height    = 24;
    
    Image currentImage; 
    
    MapView view; 
    
    Image images[]; 
    char ids[];
    char currentID; 
    
    Tile[][] tiles; 
    
    char[][] charMap = 
    { 
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
        {':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':',':'},
    };
    
        
    public MapModel(Image[] images, char[] ids)
    {
        this.images = images; 
        this.ids    = ids; 
        
        tiles = new Tile[charMap.length][charMap[0].length];
        
        for(int i=0; i<charMap.length; i++)
        {
            for(int j=0; j<charMap[i].length; j++)
            {
                if (charMap[i][j] == ':')
                {
                    tiles[i][j] = null; 
                }
            }
        }
    }
    
    public void updateCharArray()
    {
        charMap = new char[height][width]; 
        
        for(int i=0; i<tiles.length; i++)
        {
            for(int j=0; j<tiles[i].length; j++)
            {
                charMap[i][j] = tiles[i][j] != null ? tiles[i][j].id : ':';
            }
        }
    }
    
    public void updateTileArray()
    {
        for(int i=0; i<charMap.length; i++)
        {
            for(int j=0; j<charMap[i].length; j++)
            {
                 if (charMap[i][j] == ':' && tiles[i][j] != null)
                 {
                     tiles[i][j] = new Tile(j, i, tileWidth, tileHeight, null, ':'); 
                 }
            }
        }
    }
    
    /*
     *           else
                 {
                    for(int k=0; k<images.length-1; k++)
                    {
                        if (charMap[i][j] == ids[k])
                        {
                            tiles[i][j] = new Tile(j, i, tileWidth, tileHeight, images[k], ids[k]);
                        }
                    }
                 }
     * 
     */
    public void resizeMap()
    {
        Tile[][] newArray = new Tile[height][width];
        
        for(int i=0; i<tiles.length; i++)
        {
            System.arraycopy(tiles[i], 0, newArray[i], 0, tiles[i].length);
        }
        for(int i=tiles.length; i<newArray.length; i++)
        {
            for(int j=newArray[i].length; j<newArray[i].length; j++)
            {
                newArray[i][j] = null;
            }
        }
        
        tiles = newArray; 
        
        updateCharArray();
    }
    
    public String getMapText()
    {
        String map = "";

        for (char[] aCharMap : charMap) {
            for (int j = 0; j < aCharMap.length; j++) {
                map += aCharMap[j];
            }
            map += "\n";
        }
        return map; 
    }
    
    public String[] getMapTextAsArray()
    {
        String map[]  = new String[charMap.length];
        
        for(int i=0; i<charMap.length; i++)
        {
            map[i] = "";
            for(int j=0; j<charMap[i].length; j++)
            {
                map[i] += charMap[i][j]; 
            }
        }
        return map;
    }
    
    public void setCurrentImage(Image image)
    {
        currentImage = image; 
    }
    
        
    public void setView(MapView v)
    {
        this.view = v; 
    }
    
    public void setTileSize(int size)
    {
        tileWidth = tileHeight = size;

        for (Tile[] tile : tiles) {
            for (int j = 0; j < tile.length; j++) {
                if (tile[j] != null) {
                    tile[j].changeSize(size);
                }
            }
        }
    }
}
