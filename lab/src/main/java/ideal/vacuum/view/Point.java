package ideal.vacuum.view;

import ideal.vacuum.ernest.Ernest;
import spacegraph.math.Matrix3f;
import spacegraph.math.v3;

import java.awt.*;

public class Point {

	public v3 position=new v3(0,0,0);
	public v3 positionAllocentric=new v3(0,0,0);
	public int angle;
	public double distance;
	
	public int ix,jy;
	
	public Color leftColor=Color.black;
	public Color rightColor=Color.black;
	
	public int type;
	
	public v3 speed=new v3();
	
	public float m_relativeOrientation = Ernest.INFINITE;
	
	public Point(float x, float y, int a, int t){
		angle=a;
		type=t;
		position=new v3(x,y,0);
		positionAllocentric=new v3(x,y,0);
		distance=Math.sqrt(x*x+y*y);
		
		speed=new v3();
	}

	
	/**
	 * create point according to the distance
	 * @param d  distance from the agent
	 * @param i  angle of the point (in degree)
	 * @param t
	 */
	public Point(double d, int i,int t){
		angle=i;
		type=t;
		distance=d;
		/*double a=(double) (-i+90)*Math.PI/180;
		/*position=new v3((float)(d*Math.cos(a)),
				              (float)(d*Math.sin(a)),
				              0);*/
		speed=new v3();
	}
	
	public void setColors(Color l,Color r){
		leftColor=l;
		rightColor=r;
	}
	
	public void setColorsLeft(Color l){
		leftColor=l;
	}
	
	public void setColorsRight(Color r){
		rightColor=r;
	}
	
	/**
	 * change the reference orientation
	 */
	public void rotate(double rad, int deg){
		angle=(angle+deg+360)%360;
		
		double x,y;
		x=(float) (position.x*Math.cos(rad) -position.y*Math.sin(rad));
		y=(float) (position.x*Math.sin(rad) +position.y*Math.cos(rad));
		
		position.x=(float) x;
		position.y=(float) y;
		
		x=(float) (speed.x*Math.cos(rad) -speed.y*Math.sin(rad));
		y=(float) (speed.x*Math.sin(rad) +speed.y*Math.cos(rad));
		
		speed.x=(float) x;
		speed.y=(float) y;
	}
	
	public void setSpeed(v3 s){
		speed=s;
	}
	
	public void addSpeed(v3 s){
		speed.add(s);
	}
	
	public void subSpeed(v3 s){
		speed.sub(s);
	}
	
	public void addRotation(v3 r){
		v3 localSpeed=new v3();
		
		localSpeed.scale(0);
        localSpeed.y=(float) distance * r.z;
        
        Matrix3f rot = new Matrix3f();
        rot.rotZ((float) ( -(angle*Math.PI/180)));
        rot.transform(localSpeed, localSpeed);

        speed.add(localSpeed);
	}
	
	public float getAngle()
	{
		return (float)Math.atan2((double)position.y, position.x);			
	}
}
