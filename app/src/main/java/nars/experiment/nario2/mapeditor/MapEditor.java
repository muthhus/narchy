
package nars.experiment.nario2.mapeditor;



import nars.experiment.nario2.GameModel;

import java.awt.Image;
import javax.swing.*;

public class MapEditor
{
    public GameModel gameModel;
    
    public MapModel model; 
    public MapView  view; 
    public MapController controller;
    public ImageSelect imageSelect; 
    
    public Image[] tileImages; 
    public char[] ids;
    
    public JFrame frame; 
    public JFrame frame2;
    
    public MapEditor(GameModel mod)
    {     
        init(mod);
    }
    
    public MapEditor()
    {
        
    }
    
    public void init(GameModel mod)
    {
        gameModel = mod;
        
        frame = new JFrame("Map Editor"); 
       
        tileImages = new Image[]{
            new ImageIcon(this.getClass().getResource("/mapeditor/images/groundImage.gif")).getImage() ,
            new ImageIcon(this.getClass().getResource("/mapeditor/images/brick.gif")).getImage()       ,
            new ImageIcon(this.getClass().getResource("/mapeditor/images/rock.png")).getImage()        ,
            new ImageIcon(this.getClass().getResource("/mapeditor/images/grassUp.png")).getImage()     ,
            new ImageIcon(this.getClass().getResource("/mapeditor/images/grassCornerL.png")).getImage(),
            new ImageIcon(this.getClass().getResource("/mapeditor/images/grassCornerR.png")).getImage(),
            new ImageIcon(this.getClass().getResource("/mapeditor/images/brownBlock.png")).getImage()  ,
            new ImageIcon(this.getClass().getResource("/mapeditor/images/sand.png")).getImage(),
            new ImageIcon(this.getClass().getResource("/mapeditor/images/sandBrick.jpeg")).getImage(),
            new ImageIcon(this.getClass().getResource("/mapeditor/images/questionBlock.png")).getImage(),
            new ImageIcon(this.getClass().getResource("/mapeditor/images/eraser.png")).getImage()      ,
        };
        
        ids = new char[]{'g',
                         'b',
                         'r',
                         'U',
                         'L',
                         'R',
                         'B',
                         's',
                         'S',
                         'Q',
                         ':',
        };
        
        model = new MapModel(tileImages, ids);
        view  = new MapView(frame, model); 
        model.setView(view);
        controller = new MapController(model, view, mod);

        frame2 = new JFrame("Control Panel"); 
        imageSelect = new ImageSelect(controller, model, tileImages, ids, frame2); 
        
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.add(new javax.swing.JScrollPane(view)); 
        frame.setVisible(false);

        frame.pack();
        
        frame2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame2.add(imageSelect);
        frame2.setVisible(false);

        frame2.pack();
    }
    
    public void setNull()
    {
        frame.dispose();
        frame2.dispose();
    }
}
