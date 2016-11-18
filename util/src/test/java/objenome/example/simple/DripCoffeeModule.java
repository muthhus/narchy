package objenome.example.simple;

import objenome.out;
import objenome.the;
import javax.inject.Singleton;

@the(
    in = CoffeeApp.class,
    extend = PumpModule.class
)
public class DripCoffeeModule {
  @out
  @Singleton Heater heater() {
    return new ElectricHeater();
  }
}
