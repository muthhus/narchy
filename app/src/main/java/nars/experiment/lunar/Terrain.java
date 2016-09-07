package nars.experiment.lunar;

import java.awt.*;

public class Terrain
{
	final private int terrainX[] = {0 ,30 ,40 ,100,140,160,180,200,220,230,300,310,330,350,
		       360,400,410,435,460,465,500,545,560,575,580,600,600,0};

	final private int terrainY[] = {500,450,480,510,350,400,395,480,490,480,480,520,515,520,
			   515,550,400,350,360,400,410,480,455,465,480,500,600,600};

	public void draw(Graphics g)
	{
		g.setColor(Color.WHITE);
		g.fillPolygon(terrainX, terrainY, terrainX.length);
	}

	public Shape shape()
	{
		return new Polygon(terrainX, terrainY, terrainX.length);
	}
}
