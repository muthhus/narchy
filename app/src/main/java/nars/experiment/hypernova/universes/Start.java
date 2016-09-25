package nars.experiment.hypernova.universes;

import nars.experiment.hypernova.Faction;
import nars.experiment.hypernova.NewUniverse;
import nars.experiment.hypernova.SaveGame;
import nars.experiment.hypernova.UniNames;
import nars.experiment.hypernova.audio.MinimWrapper;
import nars.experiment.hypernova.gui.Info;
import nars.experiment.hypernova.gui.Transition;
import nars.experiment.hypernova.gui.Viewer;
import nars.experiment.hypernova.gui.Wormhole;
import nars.experiment.hypernova.gui.backgrounds.MusicStarfield;
import nars.experiment.hypernova.sounds.VolumeEffect;

import java.awt.*;

//import nars.experiment.hypernova.activities.ChuckToTheFuture;
//import nars.experiment.hypernova.activities.ChuckToTheFuture2;

public class Start extends NewUniverse {
   public static Start INSTANCE = new Start();
   static final long serialVersionUID = 137533472837495L;  
 
   private boolean chuckDone = false;
   private boolean chuck2Done = false;

   private static MusicStarfield ms = new MusicStarfield();

   public static boolean isChuckDone()   { return INSTANCE.chuckDone; }
   public static void setChuckDone()  { INSTANCE.chuckDone = true;  SaveGame.autosave(); }
   public static void setChuck2Done() { INSTANCE.chuck2Done = true; SaveGame.autosave(); }   

   public void begin()
   {
        Faction.clear();
        Faction.create("None", Color.WHITE);
        Faction.create("Humans", Color.GREEN);
        Faction.create("Aliens", new Color(0xcc, 0x00, 0xcc));
        Faction.create("Invaders", Color.RED);

        Info.visibleTimer = false;
        Info.visibleCounter = false;
        
        MusicStarfield.setClearScreen(true);
        if(INSTANCE == null) INSTANCE = new Start();
        SaveGame.setCheckpoint(0, 0, UniNames.START);
        if(INSTANCE.chuck2Done) MusicStarfield.bg = MusicStarfield.BackgroundType.ROTATE;

        Viewer.setBackground(ms);
        //u.addActivity("test", 0, 0);

//        if( !INSTANCE.chuckDone ) u.addActivity(new ChuckToTheFuture(), 500, -500);
//        else if ( !INSTANCE.chuck2Done ) u.addActivity(ChuckToTheFuture.chuck2, 1500, 1500);
        
        Wormhole.add(0,1500,400,400,UniNames.TEST, Transition.Types.BLOCKING);
        Wormhole.add(-1500,0,400,400, UniNames.ALTER, Transition.Types.FOUR);
        u.queueMessage("You are here");
        MinimWrapper.addEffect(new VolumeEffect());
   }

} 
