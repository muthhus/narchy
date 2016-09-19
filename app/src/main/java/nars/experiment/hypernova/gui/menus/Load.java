package nars.experiment.hypernova.gui.menus;

import nars.experiment.hypernova.gui.MenuScreen;
import nars.experiment.hypernova.gui.Menu;
import nars.experiment.hypernova.gui.Info;
import nars.experiment.hypernova.gui.Viewer;
import nars.experiment.hypernova.SaveGame;
import nars.experiment.hypernova.Universe;

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
