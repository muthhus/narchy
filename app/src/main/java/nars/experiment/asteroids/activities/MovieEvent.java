package nars.experiment.asteroids.activities;

import nars.experiment.asteroids.ActivitySimple;
import nars.experiment.asteroids.gui.Movie;

public class MovieEvent extends ActivitySimple {
    public void begin(double px, double py) {
      Movie.begin("testMovie");
      this.finish();
    }

}
