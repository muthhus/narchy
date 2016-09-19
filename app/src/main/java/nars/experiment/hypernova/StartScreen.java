package nars.experiment.hypernova;

import java.awt.Color;
//import nars.experiment.hypernova.gui.backgrounds.EqualizerBackground;
import nars.experiment.hypernova.gui.Viewer;
import nars.experiment.hypernova.gui.Menu;
import nars.experiment.hypernova.gui.backgrounds.BlankBackground;
import nars.experiment.hypernova.gui.menus.Intro;

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