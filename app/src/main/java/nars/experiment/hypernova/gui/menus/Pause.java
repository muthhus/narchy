package nars.experiment.hypernova.gui.menus;

import nars.experiment.hypernova.Hypernova;
import nars.experiment.hypernova.Universe;
import nars.experiment.hypernova.gui.Menu;
import nars.experiment.hypernova.gui.MenuScreen;

import java.awt.image.BufferedImage;

public class Pause extends MenuScreen
{
  private BufferedImage img;

  public Pause() {
    img = Hypernova.getViewer().getImage();
  }

  public void back() {
    Universe.get().togglePause(false);
    Menu.setInMenu(false);
  }

  public void loadMenu() {
    addItem(Alignment.CENTER, "Continue", null, 0);
    addItem(Alignment.CENTER, "Save", null, 1);
    addItem(Alignment.CENTER, "Load", null, 2);
    addItem(Alignment.CENTER, "Exit", null, 3);
  }
 
  public void functions(int func, String value) {
    switch(func)
    {
      case 0:
        back();
        break;
      case 1:
        newScreen(new Save());       
        break;
      case 2:
        newScreen(new Load());       
        break;
      case 3:
        newScreen(new YesNoQuit());       
        break;
    }
  }
}
