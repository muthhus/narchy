package nars.experiment.asteroids.gui.menus;

import nars.experiment.asteroids.gui.MenuScreen;
import nars.experiment.asteroids.gui.Menu;
import nars.experiment.asteroids.gui.Viewer;

public class YesNoQuit extends MenuScreen
{
  public void loadMenu() {
    addText(Alignment.CENTER, "Quit Game?");
    String tmp = "";
    for(int i = 0; i <= 10; i ++) tmp += "\u141E";
    addText(Alignment.CENTER, tmp);
    addItem(Alignment.CENTER, "Yes", null, 1);
    addItem(Alignment.CENTER, "No", null, 0);
    selected = "No";
  }
 
  public void functions(int func, String value) {
    switch(func)
    {
      case 0:
        back();
        break;
      case 1:
        System.exit(0);
        break;
    }
  }
}
