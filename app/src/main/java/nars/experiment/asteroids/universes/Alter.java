package nars.experiment.asteroids.universes;

import java.awt.Color;
import nars.experiment.asteroids.NewUniverse;
import nars.experiment.asteroids.Activity;
import nars.experiment.asteroids.Faction;
import nars.experiment.asteroids.gui.backgrounds.BlankBackground;
import nars.experiment.asteroids.Realization;
import nars.experiment.asteroids.SaveGame;
import nars.experiment.asteroids.UniNames;
import nars.experiment.asteroids.gui.Viewer;
import nars.experiment.asteroids.gui.Info;
import nars.experiment.asteroids.gui.Wormhole;
import nars.experiment.asteroids.gui.Transition;
import nars.experiment.asteroids.audio.MinimWrapper;
import nars.experiment.asteroids.activities.MovieEvent;

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
