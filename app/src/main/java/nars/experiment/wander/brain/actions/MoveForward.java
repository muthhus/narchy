package nars.experiment.wander.brain.actions;

import nars.experiment.wander.Player;
import nars.experiment.wander.brain.Action;

public class MoveForward extends Action {
	private static final long serialVersionUID = 1L;
	private final Player player;

	public MoveForward(Player player) {
		this.player = player;
	}

	@Override
	public void execute() {
		player.moveForward(Player.STEP_SIZE);
	}
}
