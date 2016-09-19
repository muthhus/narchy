package nars.experiment.asteroids.pilots;

import nars.experiment.asteroids.Ship;
import nars.experiment.asteroids.Universe;

public class PlayerHunter extends Hunter {
    public PlayerHunter(Ship ship) {
        super(ship, null);
    }

    private Ship getPlayer() {
        return Universe.get().getPlayer();
    }

    public void drive(double dt) {
        if (target == null || !target.isActive()) target = getPlayer();
        super.drive(dt);
    }
}
