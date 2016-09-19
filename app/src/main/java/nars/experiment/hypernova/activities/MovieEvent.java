package nars.experiment.hypernova.activities;

import nars.experiment.hypernova.ActivitySimple;
import nars.experiment.hypernova.gui.Movie;

public class MovieEvent extends ActivitySimple {
    public void begin(double px, double py) {
      Movie.begin("testMovie");
      this.finish();
    }

}
