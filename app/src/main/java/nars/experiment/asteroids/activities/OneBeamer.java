package nars.experiment.asteroids.activities;

import nars.experiment.asteroids.ActivitySimple;

public class OneBeamer extends ActivitySimple {
    public void begin(double px, double py) {
        addShip("beamer", "Invaders", PilotType.BEAMER, px, py + 200);
    }
}
