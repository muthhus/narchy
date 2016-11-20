package objenome.example.simple;

import objenome.O;
import objenome.in;

public class CoffeeApp implements Runnable {

  @in CoffeeMaker coffeeMaker;

  @Override public void run() {
    coffeeMaker.brew();
  }

  public static void main(String[] args) {
    O.of(
        //new DripCoffeeModule()
        DripCoffeeModule.class
    ).a(CoffeeApp.class).run();
  }
}
