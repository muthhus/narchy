package objenome.example.simple;

import objenome.in;

class Thermosiphon implements Pump {
  private final Heater heater;

  @in
  Thermosiphon(Heater heater) {
    this.heater = heater;
  }

  @Override public void pump() {
    if (heater.isHot()) {
      System.out.println("=> => pumping => =>");
    }
  }
}
