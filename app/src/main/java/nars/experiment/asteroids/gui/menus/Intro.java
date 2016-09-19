package nars.experiment.asteroids.gui.menus;

import nars.experiment.asteroids.gui.MenuScreen;
import nars.experiment.asteroids.gui.Menu;
import nars.experiment.asteroids.universes.Alter;
import nars.experiment.asteroids.gui.Viewer;
import nars.experiment.asteroids.gui.Info;
import nars.experiment.asteroids.Universe;
import nars.experiment.asteroids.SaveGame;
import nars.experiment.asteroids.Hypernova;
import java.awt.image.BufferedImage;

public class Intro extends MenuScreen
{
  public void back() {
    /* Start screen, so no back */
  }

  public void loadMenu() {
    addImg(Alignment.CENTER, "images/logo.png", 550, 250);
    addItem(Alignment.CENTER, "Continue", null, 0);
    addItem(Alignment.CENTER, "New Game", null, 1);
    addItem(Alignment.CENTER, "Load", null, 2);
    addItem(Alignment.CENTER, "Options", null, 3);
    addItem(Alignment.CENTER, "Exit", null, 4);
    if(!SaveGame.hasAutoSave()) updateItem("Continue", null, true);
  }
 
  public void functions(int func, String value) {
    switch(func)
    {
      case 0:
      case 1:
        Viewer.showMinimap = true;
        Info.showInfo = true;
        Universe.get().togglePause(false);
        Menu.setInMenu(false);
        Menu.setKeepBg(false);
        if( func == 0) SaveGame.load(0);
        else Universe.get().loadUniverse(new Alter());
        break;
      case 2:
        newScreen(new Load());       
        break;
      case 4:
        newScreen(new YesNoQuit());       
        break;
    }
  }
}
