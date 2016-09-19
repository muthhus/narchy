package nars.experiment.asteroids.gui.menus;

import nars.experiment.asteroids.gui.MenuScreen;
import nars.experiment.asteroids.gui.Menu;
import nars.experiment.asteroids.gui.Info;
import nars.experiment.asteroids.gui.Viewer;
import nars.experiment.asteroids.SaveGame;
import nars.experiment.asteroids.Universe;

public class Load extends MenuScreen
{
  public void loadMenu() {
    for(int i = 1; i <= 5; i ++)
      addItem(Alignment.LEFT, SaveGame.saveStats(i), null, i);
    
    addItem(Alignment.RIGHT, "Back", null, 6);
  }
 
  public void functions(int func, String value) {
    if(func <= 5)
    {
      Universe.get().togglePause(false);
      Menu.setInMenu(false);
      Viewer.showMinimap = true;
      Info.showInfo = true;
      Menu.setKeepBg(false);
      SaveGame.load(func);
    } else back();
  }
}
