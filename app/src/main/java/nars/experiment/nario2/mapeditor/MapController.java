
package nars.experiment.nario2.mapeditor;


import nars.experiment.nario2.GameModel;

import java.awt.*;

public class MapController 
{
    MapModel model; 
    MapView  view; 
    GameModel gameModel;
    
    public MapController(MapModel mapModel, MapView mapView, GameModel mod)
    {
        gameModel = mod; 
        model = mapModel; 
        view  = mapView; 
    }
    
    public void changeTileSize(int size)
    {
        model.setTileSize(size);
        
        view.repaint();
        
        view.setPreferredSize(new Dimension(model.tileWidth*model.width, model.tileHeight*model.height));
        view.revalidate();
    }
    
    public void setGameMap(String[] newMap)
    {
        gameModel.changeGameMap(newMap);
    }
}
