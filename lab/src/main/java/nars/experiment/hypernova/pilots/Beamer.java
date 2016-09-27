//package nars.experiment.asteroids.pilots;
//
//import nars.experiment.hypernova.Ship;
//import nars.experiment.hypernova.SaveGame;
//import nars.experiment.hypernova.Universe;
//import nars.experiment.hypernova.UniNames;
//import nars.experiment.hypernova.Mass;
//import nars.experiment.hypernova.sounds.WarbleEffect;
//import nars.experiment.hypernova.DestructionListener;
//import nars.experiment.hypernova.audio.MinimWrapper;
//import nars.experiment.hypernova.activities.ChuckToTheFuture;
//import nars.experiment.hypernova.universes.Test;
//
//public class Beamer extends Hunter implements DestructionListener{
//    private boolean raceConditionFix = false;
//    public Beamer(Ship ship) {
//        super(ship, null);
//    }
//
//    public void setShip(Ship ship) {
//      ship.onDestruct(this);
//      super.setShip(ship);
//    }
//
//    public void destroyed(Mass m)
//    {
//      if( SaveGame.getUniName() == UniNames.START ) ChuckToTheFuture.shipDead();
//      else {
//        raceConditionFix = true;
//        WarbleEffect.r = false;
//        WarbleEffect.l = false;
//        Test.finishOneBeamer();
//      }
//    }
//
//    public void fireShots(Ship ship)
//    {
//      int max = 0;
//      if(raceConditionFix) return;
//      if(MinimWrapper.fft(4)[1] > MinimWrapper.fft(4)[max]) max = 1;
//      if(MinimWrapper.fft(4)[2] > MinimWrapper.fft(4)[max]) max = 2;
//      if(MinimWrapper.fft(4)[3] > MinimWrapper.fft(4)[max]) max = 3;
//
//      if(max < 2) { WarbleEffect.r = false; WarbleEffect.l = true; }
//      else { WarbleEffect.r = true; WarbleEffect.l = false; }
//
//      if(max != 0 || MinimWrapper.fft(4)[0] > 10) ship.fire(max);
//      else { WarbleEffect.r = false; WarbleEffect.l = false; }
//
//    }
//
//    public void noFire()
//    {
//      WarbleEffect.r = false;
//      WarbleEffect.l = false;
//    }
//
//    private Ship getPlayer() {
//        return Universe.get().getPlayer();
//    }
//
//    public void drive(double dt) {
//        if (target == null || !target.isActive()) target = getPlayer();
//        super.drive(dt);
//    }
//}
