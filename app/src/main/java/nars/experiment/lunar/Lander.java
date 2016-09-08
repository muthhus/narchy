package nars.experiment.lunar;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

public class Lander
{
	final private int baseX[] = {11,13,27,29,30,26,37,40,40,30,30,33,24,
			   						21,24,16,19, 16, 7, 0, 0,10,10, 3,14,10};

	final private int baseY[] = { 5, 0,0, 5, 20,20,35,35,40,40,35,35,20,
									 20,25,25,20,20,35,35,40,40,35,35,20,20};

	private double x, y, vx, vy, m;
	private boolean thrusting = false;
	private long thrustTime;
	private double fuel;

	public Lander(double x, double y, double vx, double vy, double m, double fuel)
	{
		this.x = x;
		this.y = y;
		this.vx = vx;
		this.vy = vy;
		this.m = m;
		this.fuel = fuel;
	}

	private void applyForce(double fx, double fy, double dt)
	{
		this.vx += fx / m * dt;
		this.vy += fy / m * dt;

		this.x += this.vx * dt;
		this.y += this.vy * dt;
	}

	public void applyGravity(double g, double dt)
	{
		applyForce(0, g, dt);
	}

	public void thrust(double fx, double fy, double dt)
	{
		double fuelRequired = Math.min(fuel, Math.sqrt(fx * fx + fy * fy) * dt);

		if (fuel > 0)
		{
			applyForce(fx, fy, dt);
			thrusting = true;
			thrustTime = System.currentTimeMillis();
			fuel -= fuelRequired;
		}
	}

	public void draw(Graphics g)
	{
		int posX[] = new int[baseX.length];
		int posY[] = new int[baseY.length];
		int cx = 0, cy = 0;

		for (int i = 0; i < baseX.length; ++i)
		{
			posX[i] = (int)(baseX[i] + x);
			posY[i] = (int)(baseY[i] + y);
			cx += posX[i];
			cy += posY[i];
		}

		cx = cx / baseX.length;
		cy = cy / baseX.length;

		g.setColor(Color.WHITE);
		g.fillPolygon(posX, posY, baseX.length);

		if (thrusting)
		{
			if (System.currentTimeMillis() - thrustTime < 150)
			{
				g.setColor(Color.YELLOW);

				for (int i = 5; i < 12; ++i) g.fillOval(cx - 3, cy + i, 5, 5);
			}
		}

		g.setColor(Color.CYAN);
		/*g.drawString("vx = " + vx, 410, 10);
		g.drawString("vy = " + vy, 410, 23);
		g.drawString("mass = " + m, 410, 36);*/
		g.drawString("fuel remaining = " + (int)fuel, 470, 15);
	}

	public Rectangle2D boundingBox()
	{
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;

		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < baseX.length; ++i)
		{
			if (baseX[i] + x < minX) minX = baseX[i] + x;
			if (baseY[i] + y < minY) minY = baseY[i] + y;

			if (baseX[i] + x > maxX) maxX = baseX[i] + x;
			if (baseY[i] + y > maxY) maxY = baseY[i] + y;
		}

		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
	}

	public Point2D randomPoint(Rectangle2D bounds)
	{
		int posX[] = new int[baseX.length];
		int posY[] = new int[baseY.length];

		for (int i = 0; i < baseX.length; ++i)
		{
			posX[i] = (int)(baseX[i] + x);
			posY[i] = (int)(baseY[i] + y);
		}

		Polygon poly = new Polygon(posX, posY, posX.length);
		Random random = new Random();

		while (true)
		{
			double x = bounds.getMinX() + bounds.getWidth() * random.nextDouble();
			double y = bounds.getMinY() + bounds.getHeight() * random.nextDouble();

			Point2D p = new Point2D.Double(x, y);

			if (poly.contains(p)) return p;
		}
	}

	public Point2D pointUnder()
	{
		Rectangle2D bb = boundingBox();

		Random random = new Random();

		double x = bb.getMinX() + random.nextDouble() * bb.getWidth();
		double y = bb.getMaxY();

		return new Point2D.Double(x, y);
	}

	public double getVerticalSpeed()
	{
		return vy;
	}

	public double getLateralSpeed()
	{
		return vx;
	}

}
