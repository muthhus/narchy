package ideal.vacuum;

import java.awt.*;
import java.util.Objects;

import static ideal.vacuum.Environment.FIELD_COLOR;


public class Block {

	
	public Color color;
	public int touch;
	public String name;
	
	public Block(int tactile, Color visual, String n){
		color=visual;
		touch=tactile;
		name=n;
	}
	
	public Color seeBlock(){
		return color;
	}
	
	public int touchBlock(){
		return touch;
	}
	
	public boolean isVisible(){
		return (!Objects.equals(color, FIELD_COLOR));
	}
	
	public boolean isWall(){
		return (touch==Model.HARD);
	}
	
	public boolean isFood(){
		return (touch==Model.FOOD);
	}
	
	public boolean isAlga(){
		return (touch==Model.SMOOTH);
	}
	
	public boolean isEmpty(){
		return (touch==Model.EMPTY);
	}
	
	public boolean isWalkthroughable(){
		return (touch!=Model.HARD);
	}
	
	public boolean isTraversableObject(){
		return (touch==Model.FOOD || touch==Model.SMOOTH);
	}
	
}
