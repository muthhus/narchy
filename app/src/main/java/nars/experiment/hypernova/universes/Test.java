package nars.experiment.hypernova.universes;

import java.awt.Color;
import nars.experiment.hypernova.NewUniverse;
import nars.experiment.hypernova.Activity;
import nars.experiment.hypernova.Faction;
import nars.experiment.hypernova.SaveGame;
import nars.experiment.hypernova.UniNames;
import nars.experiment.hypernova.activities.CountDown;
import nars.experiment.hypernova.activities.FactoryBattle;
import nars.experiment.hypernova.activities.OneBeamer;
import nars.experiment.hypernova.gui.Viewer;
import nars.experiment.hypernova.gui.Wormhole;
import nars.experiment.hypernova.gui.Transition;
//import nars.experiment.hypernova.gui.backgrounds.LineField;
import nars.experiment.hypernova.gui.backgrounds.BlankBackground;
import nars.experiment.hypernova.sounds.WarbleEffect;

public class Test extends NewUniverse {
   public static Test INSTANCE = new Test();
   static final long serialVersionUID = 7533472837495L;

   private boolean countDone = false;
   
   private static OneBeamer oneBeamer;
   public static void finishOneBeamer(){INSTANCE.oneBeamer.finish();}
   public static void setCountDone(){INSTANCE.countDone = true;}   

   public void begin()
   {
        Faction.clear();
        Faction.create("None", Color.WHITE);
        Faction.create("Humans", Color.GREEN);
                //Faction.ColorType.TEST_HUMAN);
        Faction.create("Invaders", Color.RED);
                //Faction.ColorType.TEST_INVADER);
        SaveGame.setCheckpoint(0, 0, UniNames.TEST);

        //Viewer.setBackground(new LineField());
        Viewer.setBackground(new BlankBackground());

        Activity battle = new FactoryBattle();
        u.addActivity(battle, -500, -500);
        if( !INSTANCE.countDone ) u.addActivity(new CountDown(), 500, -500);
        Wormhole.add(0,-1500,400,400,UniNames.START,Transition.Types.DIAGONAL);
        Wormhole.add(1500,0,400,400, UniNames.ALTER, Transition.Types.FOUR);
        u.queueMessage("You are there");
        oneBeamer = new OneBeamer();
        u.addActivity(oneBeamer, 0, 2000);
        //MinimWrapper.addEffect(new WarbleEffect());
        WarbleEffect.l = false;
        WarbleEffect.r = false;
   }
} 
