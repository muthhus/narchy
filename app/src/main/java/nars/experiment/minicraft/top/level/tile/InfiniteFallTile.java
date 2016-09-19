package nars.experiment.minicraft.top.level.tile;

import nars.experiment.minicraft.top.entity.AirWizard;
import nars.experiment.minicraft.top.entity.Entity;
import nars.experiment.minicraft.top.gfx.Screen;
import nars.experiment.minicraft.top.level.Level;

public class InfiniteFallTile extends Tile {
	public InfiniteFallTile(int id) {
		super(id);
	}

	public void render(Screen screen, Level level, int x, int y) {
	}

	public void tick(Level level, int xt, int yt) {
	}

	public boolean mayPass(Level level, int x, int y, Entity e) {
		if (e instanceof AirWizard) return true;
		return false;
	}
}
