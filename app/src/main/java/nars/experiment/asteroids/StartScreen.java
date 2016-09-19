package nars.experiment.asteroids;

import java.awt.Color;
//import nars.experiment.asteroids.gui.backgrounds.EqualizerBackground;
import nars.experiment.asteroids.gui.Viewer;
import nars.experiment.asteroids.gui.Menu;
import nars.experiment.asteroids.gui.backgrounds.BlankBackground;
import nars.experiment.asteroids.gui.menus.Intro;

public class StartScreen extends NewUniverse {
   static final long serialVersionUID = 137133L;  
   public void begin()
   {
        //Viewer.setBackground(new EqualizerBackground());
       Viewer.setBackground(new BlankBackground());
        Faction.clear();
        Faction.create("Humans", Color.GREEN);
        Menu.begin(new Intro());
   }
} 
