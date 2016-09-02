package ideal.vacuum.agent.behavior;

import ideal.vacuum.agent.AgentDesigner;
import ideal.vacuum.agent.vision.PhotoreceptorCell;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 390 $
 */
public class BehaviorState implements Cloneable {

	private Color focusColor = AgentDesigner.UNANIMATED_COLOR ;
	private Color leftColor = AgentDesigner.UNANIMATED_COLOR ;
	private Color rightColor = AgentDesigner.UNANIMATED_COLOR ;
	private Set<PhotoreceptorCell> photoreceptorCells ;

	public BehaviorState( Color focusColor , Color leftColor , Color rightColor , Set<PhotoreceptorCell> cells ) {
		super() ;
		this.focusColor = focusColor ;
		this.leftColor = leftColor ;
		this.rightColor = rightColor ;
		this.photoreceptorCells = cells ;
	}

	public Color getFocusColor() {
		return this.focusColor ;
	}
	
	public void setFocusColor(int colorCode){
		this.focusColor = new Color(colorCode);
	}

	public Color getLeftColor() {
		return this.leftColor ;
	}

	public Color getRightColor() {
		return this.rightColor ;
	}

	public Set<PhotoreceptorCell> getCells() {
		return this.photoreceptorCells ;
	}

	public List<PhotoreceptorCell> getCellsArray() {
		return new ArrayList<>(this.photoreceptorCells);
	}
	
	@Override
	protected BehaviorState clone() throws CloneNotSupportedException {
		BehaviorState object = (BehaviorState) super.clone() ;
		object.photoreceptorCells = this.photoreceptorCells ;
		object.focusColor = this.focusColor ;
		object.leftColor = this.leftColor ;
		object.rightColor = this.rightColor ;

		return object ;
	}
}
