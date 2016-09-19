package nars.experiment.minicraft.side;

/**
 * Created by me on 9/19/16.
 */
public class ItemDefinition {
	int item_id;
	String name;
	String spriteRef;
	int[][] recipe;
	int yield;

	public ItemDefinition(int id, String n, String s, int y) {
		this(id, n, s, y, null);
	}

	public ItemDefinition(int id, String n, String s, int y, int[][] t) {
		item_id = id;
		name = n;
		spriteRef = s;
		recipe = t;
		yield = y;
	}

	public Item makeItem(int size) {
		return new Item(spriteRef, size, item_id, name, recipe, yield);
	}
}
