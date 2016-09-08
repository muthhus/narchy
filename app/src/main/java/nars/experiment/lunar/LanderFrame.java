package nars.experiment.lunar;

import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class LanderFrame extends JFrame implements KeyListener, FloatProcedure
{
	private Terrain terrain = new Terrain();
	private Lander lander = new Lander(0, 0, 0, 0, 0.9, 250);
	private LanderCanvas canvas;
	private Timer timer;
	private boolean gameOver;

	public LanderFrame()
	{
		super("Moon Lander");
		canvas = new LanderCanvas(600, 600, terrain, lander);
		 // create canvas
		setLayout(new BorderLayout());
		 // use border layour
		add(canvas, BorderLayout.CENTER);
		 // add canvas
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		 // pack components tightly together
		setResizable(false);
		 // prevent us from being resizeable
		setVisible(true);
		 // make sure we are visible!

		addKeyListener(this);

		timer = new Timer(this);
		timer.start();
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		int code = e.getKeyCode();
		if(code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_KP_RIGHT)
		{
			moveLander(+15, 0, 0.03);
		}
		else if(code == KeyEvent.VK_LEFT || code == KeyEvent.VK_KP_LEFT)
		{
			moveLander(-15, 0, 0.03);
		}
		else if(code == KeyEvent.VK_UP || code == KeyEvent.VK_KP_UP)
		{
			moveLander(0, -45, 0.03);
		}
	}

	private boolean checkCollision()
	{
		/* Get bounding box of lander. */
		Rectangle2D bb = lander.boundingBox();

		/* Check intersection with terrain. */
		if (!terrain.shape().intersects(bb)) return false;

		/* Else try random points inside the lander for intersection... */
		final int nPoints = 250;

		for (int i = 0; i < nPoints; ++i)
			if (terrain.shape().contains(lander.randomPoint(bb))) return true;

		return false;
	}

	public static void main(String[] args) {
		new LanderFrame();
	}


	private int traceLanderLine()
	{
		while (true)
		{
			Point2D point = lander.pointUnder();
			int x = (int)point.getX();
			int y = (int)point.getY();

			for (int i = 0; i < 100; ++i)
			{
				Point2D below = new Point2D.Double(x, y + i);
				if (terrain.shape().contains(below)) return y + i;
			}
		}
	}

	private boolean flatTerrain()
	{
		/* Take a set of terrain points below the lander, check they are about the same height. */
		int height[] = new int[10];
		for (int i = 0; i < height.length; ++i) height[i] = traceLanderLine();

		/* check all heights are at most 30 pixels difference. */
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;

		for (int i = 0; i < height.length; ++i)
		{
			if (height[i] < min) min = height[i];
			if (height[i] > max) max = height[i];
		}

		return (max - min < 30);
	}

	private boolean landingSpeed()
	{
		if (lander.getVerticalSpeed() > 27) return false;
		if (lander.getLateralSpeed() > 15) return false;

		return true;
	}

	private boolean winCondition()
	{
		/* Conditions: not going too fast, landing on flat terrain. */
		if (!flatTerrain()) return false; /* crashed! */
		if (!landingSpeed()) return false; /* crashed! */
		return true;
	}

	private void checkGameState()
	{
		if (checkCollision())
		{
			gameOver = true;
			if (winCondition())
			{
				canvas.victory();
			}
			else
			{
				canvas.loss();
			}
		}
	}

	private void moveLander(double fx, double fy, double dt)
	{
		if (gameOver) return;

		lander.thrust(fx, fy, dt);
		checkGameState();
	}


	@Override
	public void value(float dt)
	{
		if (gameOver) return;

		lander.applyGravity(9.8 / 6.0, 0.1);
		canvas.repaint();
		checkGameState();
	}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {}

	public static class Timer extends Thread {
        private FloatProcedure callback;
        private boolean flag = false;

        public Timer(FloatProcedure callback) {
            this.callback = callback;
        }

        public void term() {
            flag = true;
        }

        public void run() {
            while (!flag) {
                try {
                    Thread.sleep(16); // 0.1s delay
                    callback.value(1.0f / 60.0f);
                } catch (InterruptedException e) {

                }
            }
        }


    }

}
