package objenome.example.simple;

import objenome.lazy;
import objenome.in;

class CoffeeMaker {
  @in
  lazy<Heater> heater; // Don't want to create a possibly costly heater until we need it.
  @in Pump pump;

  public void brew() {
    heater.get().on();
    pump.pump();
    System.out.println(" [_]P coffee! [_]P ");
    heater.get().off();
  }
}
