package objenome.example.simple;

import objenome.out;
import objenome.the;

@the(complete = false, library = true)
class PumpModule {
  @out Pump pump(Thermosiphon pump) {
    return pump;
  }
}
