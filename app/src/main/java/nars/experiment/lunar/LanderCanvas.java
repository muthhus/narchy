package nars.experiment.lunar;

import javax.swing.*;
import java.awt.*;

public class LanderCanvas extends JPanel
{
	private Image offscreen;
	private Graphics offgc;

	private Terrain terrain;
	private Lander lander;

	private int width, height;
	private int state;

	public LanderCanvas(int width, int height, Terrain terrain, Lander lander)
	{
		this.terrain = terrain;
		this.lander = lander;
		this.width = width;
		this.height = height;
		state = 0;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
	}

	@Override
	public Dimension getPreferredSize()
	{
	    return new Dimension(width, height);
	}

	@Override
	public void paint(Graphics g)
	{
		if (offscreen == null)
		{
			offscreen = createImage(width, height);
			offgc = offscreen.getGraphics();
		}

		offgc.setColor(Color.BLACK);
		offgc.fillRect(0,0,getWidth(),getHeight());

		terrain.draw(offgc);
		lander.draw(offgc);

		if (state != 0)
		{
			if (state == 1) // victory
			{
				offgc.setColor(Color.GREEN);
				offgc.drawString("You have landed!", width / 2 - 60, height / 2 - 10);
			}
			else // loss
			{
				offgc.setColor(Color.RED);
				offgc.drawString("You crashed, game over.", width / 2 - 60, height / 2 - 10);
			}
		}

		g.drawImage(offscreen, 0, 0, this);
	}

	public void victory()
	{
		state = 1;
		repaint();
	}

	public void loss()
	{
		state = 2;
		repaint();
	}
}
