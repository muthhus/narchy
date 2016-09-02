package ideal.vacuum.view;


import ideal.vacuum.FramePlugin;
import ideal.vacuum.agent.behavior.AbstractBehavior;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;


public class SpaceMemoryFrame extends JFrame implements FramePlugin{
	
	private final JPanel contentPane ;
	private final SpaceMemoryPanel spaceMemoryPanel = SpaceMemoryPanel.createSpaceMemoryPanel();
	
	public SpaceMemoryFrame() {
		this.setTitle("Space Memory");
		this.setPreferredSize( this.getProperSize( 600 , 500 ) );
		this.setMinimumSize( this.getProperSize( 600 , 500 ) );
		Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		this.setLocation( screen.width - 600 , screen.height - 500 );
		
		this.contentPane = new JPanel();
		this.contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.contentPane.setLayout(new BorderLayout(0, 0));
		this.setContentPane(this.contentPane);
		
		this.contentPane.add(this.spaceMemoryPanel);
	}
	
	private Dimension getProperSize( int maxX , int maxY ) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = ( screenSize.width < maxX ? screenSize.width : maxX );
		int y = ( screenSize.height < maxY ? screenSize.height : maxY );
		
		return new Dimension( x , y );
	}
	
	public void setMemory(SpaceMemory mem)
	{
		this.spaceMemoryPanel.setSpaceMemory( mem );
	}

	@Override
	public void refresh() {
		this.spaceMemoryPanel.repaint();
	}
	
	@Override
	public void anim( float angle , float x ) {
		float angleRotation = 0 ;
		float xTranslation = 0 ;
		for ( int i = 0; i < 20; i++ ) {
			angleRotation += angle / 20;
			xTranslation += x / 20;
			this.spaceMemoryPanel.updateProperties( angleRotation , xTranslation , true);
			this.spaceMemoryPanel.repaint();
			try {
				Thread.currentThread().sleep( 2 * AbstractBehavior.DELAYMOVE );
			} catch ( InterruptedException e ) {
			}
		}
		this.spaceMemoryPanel.updateProperties( 0 , 0 , false);
		this.spaceMemoryPanel.repaint();
	}

	@Override
	public void display() {
		this.setVisible( true );
	}

	@Override
	public void close() {
		this.setVisible( false );
	}
}