//package nars.experiment.asteroids.activities;
//
//import java.util.Random;
//
//import java.awt.Shape;
//import java.awt.geom.Rectangle2D;
//
//import nars.experiment.hypernova.Ship;
//import nars.experiment.hypernova.Universe;
//import nars.experiment.hypernova.Activity;
//import nars.experiment.hypernova.Realization;
//import nars.experiment.hypernova.audio.MinimWrapper;
//import nars.experiment.hypernova.pilots.Pilot;
//import nars.experiment.hypernova.gui.backgrounds.MusicStarfield;
//import nars.experiment.hypernova.gui.Viewer;
//import nars.experiment.hypernova.gui.MapMarker;
//import nars.experiment.hypernova.universes.Start;
//
//public class ChuckToTheFuture extends Activity implements Realization {
//    private static final Random RNG = new Random();
//    public static final double SPREAD = 250.0;
//
//    private static int totalShipsDead = 0;
//    private double x, y;
//    private Shape zone;
//    private static MapMarker m;
//
//    public static ChuckToTheFuture2 chuck2 = new ChuckToTheFuture2();
//
//    public static void shipDead()
//    {
//        Universe u = Universe.get();
//        totalShipsDead ++;
//        if(!Start.isChuckDone())
//        {
//           if(totalShipsDead >= 4)
//           {
//             Start.setChuckDone();
//             u.queueMessage("The End?");
//             m.setVisible(false);
//             u.addActivity(chuck2, 1500, 1500);
//           }
//        } else {
//           MusicStarfield.bg = MusicStarfield.BackgroundType.ROTATE;
//           MusicStarfield.setClearScreen(true);
//           chuck2.finish();
//           Start.setChuck2Done();
//           u.queueMessage("True End?");
//        }
//    }
//
//    public void realize(double x, double y) {
//        m = new MapMarker(x,y,0,255,255);
//        MapMarker.add(m);
//        this.x = x;
//        this.y = y;
//        zone = new Rectangle2D.Double(x - SPREAD, y - SPREAD,
//                                      SPREAD * 2, SPREAD * 2);
//        Universe.get().add(this);
//    }
//
//    public boolean shouldTrigger(double x, double y) {
//        return zone.contains(x, y);
//    }
//
//    public void trigger(double px, double py) {
//        Universe u = Universe.get();
//        u.remove(this);
//        totalShipsDead = 0;
//        //MinimWrapper.playSound(new SawTooth());
// //       MinimWrapper.playSoundAsync("sounds/chuckToTheFuture.mp3");
//        u.queueMessage("CHUCK TO THE FUTURE");
//        for(int i = 0; i < 4; i ++)
//        {
//           Ship s = Ship.get("beamer");
//           s.setFaction("Invaders");
//           if(i == 0) s.setPosition(px - 500, py + 500, 0);
//           else if(i == 1) s.setPosition(px - 500, py - 500, 0);
//           else if(i == 2) s.setPosition(px + 500, py - 500, 0);
//           else if(i == 3) s.setPosition(px + 500, py + 500, 0);
//           Pilot p = new Beamer(s);
//           p.setShip(s);
//           s.setPilot(p);
//           u.add(s);
//        }
//    }
//
//}
