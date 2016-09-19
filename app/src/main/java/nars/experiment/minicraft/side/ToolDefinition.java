package nars.experiment.minicraft.side;

/**
 * Created by me on 9/19/16.
 */
public class ToolDefinition extends ItemDefinition {
	Tool.ToolType type;
	Tool.ToolPower power;

	public ToolDefinition(int id, String n, String s, int[][] t, int y, Tool.ToolType tt,
			Tool.ToolPower tp) {
		super(id, n, s, y, t);
		type = tt;
		power = tp;
	}

	public Tool makeTool(int size) {
		return new Tool(spriteRef, size, item_id, name, recipe, yield, type, power);
	}
}
