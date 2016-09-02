package ideal.vacuum.view;

import javax.swing.*;
import java.awt.*;

public class SpaceMemoryPanel extends JPanel {

	private float angleRotation ;
	private float xTranslation ;
	public SpaceMemory spaceMemory;
	
	public SpaceMemoryPanel() {
		// TODO Auto-generated constructor stub
	}
	
	public void setSpaceMemory( SpaceMemory spaceMemory ) {
		this.spaceMemory = spaceMemory ;
	}
	
	public void updateProperties( float angleRotation , float xTranslation , boolean anim){
		this.angleRotation = angleRotation;
		this.xTranslation = xTranslation;
		if (!anim)
			spaceMemory.setPlaceList();
	}
	
	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g ) ;
		if( this.spaceMemory != null && this.spaceMemory.m_model != null )
			//spaceMemory.m_model.paintSpaceMemory(g, spaceMemory.m_model.getErnest().getPlaceList() , this.angleRotation , this.xTranslation );
			spaceMemory.m_model.paintSpaceMemory(g, spaceMemory.getPlaceList() , this.angleRotation , this.xTranslation );
	}
	
	/**
	 * @wbp.factory
	 */
	public static SpaceMemoryPanel createSpaceMemoryPanel() {
		SpaceMemoryPanel spaceMemoryPanel = new SpaceMemoryPanel();
		return spaceMemoryPanel;
	}
}
