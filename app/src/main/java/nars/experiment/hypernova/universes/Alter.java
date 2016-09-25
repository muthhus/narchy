package nars.experiment.hypernova.universes;

import nars.experiment.hypernova.Faction;
import nars.experiment.hypernova.NewUniverse;
import nars.experiment.hypernova.SaveGame;
import nars.experiment.hypernova.UniNames;
import nars.experiment.hypernova.activities.MovieEvent;
import nars.experiment.hypernova.gui.Info;
import nars.experiment.hypernova.gui.Transition;
import nars.experiment.hypernova.gui.Viewer;
import nars.experiment.hypernova.gui.Wormhole;
import nars.experiment.hypernova.gui.backgrounds.BlankBackground;

import java.awt.*;

public class Alter extends NewUniverse {
   public static Alter INSTANCE = new Alter();
   static final long serialVersionUID = 137133472837495L;  
 
   public void begin()
   {
        Faction.clear();
        Faction.create("None", Color.BLUE);
        Faction.create("Humans", Color.WHITE);
        Faction.create("Invaders", Color.WHITE);
        Viewer.setBackground(new BlankBackground());

        Info.visibleTimer = false;
        Info.visibleCounter = false;
        
        u.addActivity(new MovieEvent(), 0, -1500);
        Wormhole.add(-1500,100,400,400,UniNames.START, Transition.Types.DIAGONAL);
        Wormhole.add(1500,100,400,400,UniNames.TEST, Transition.Types.BLOCKING);
        SaveGame.setCheckpoint(0, 0, UniNames.ALTER);
   }

} 
