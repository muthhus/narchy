package ideal.vacuum.agent.vision;

import ideal.vacuum.Environment;
import ideal.vacuum.ErnestModel;
import ideal.vacuum.Model;
import ideal.vacuum.ernest.Ernest;
import spacegraph.math.v3;

import java.awt.*;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import static ideal.vacuum.Environment.WALL_COLOR;

public class RayTracing {

	private final Environment env;
	private final ErnestModel model;
	//private v3 position;
	private final String agentName;
	
	private final double angleOrigin;
	private final double angleSpan;
	private double currentAngle;
	
	public RayTracing( Environment env , ErnestModel ernestModel , String agentName , double angleOrigin, double angleSpan) {
		this.env = env;
		this.model = ernestModel;
//		this.position = startPosition;
		this.agentName = agentName;

		this.angleOrigin = angleOrigin;
		this.angleSpan = angleSpan;
	}
	
	public Queue<PhotoreceptorCell> rayTrace(){
		this.currentAngle = this.angleOrigin;
		Queue<PhotoreceptorCell> cellQueue = new LinkedList<>() ;
		while( this.currentAngle < this.angleOrigin + this.angleSpan + .001 ){
			PhotoreceptorCell cell = this.scanArc() ;
			if (cell.getBlockPosition().length()< 1000) // OG do not see the external walls;
				if( ! cellQueue.contains( cell ) )
					cellQueue.add( cell );
			
		}
		return cellQueue;
	}
	
	private PhotoreceptorCell scanArc() {
		int[] eyeFixation = null; //new int[] {Ernest.INFINITE,Ernest.INFINITE,WALL_COLOR.getRGB()};
		double step = this.angleSpan / 40; // OG
		double angle;
		v3 position = new v3(this.model.getPosition());// OG
		for (angle = this.currentAngle; angle <= this.angleOrigin + this.angleSpan + .001; angle += step) {
			float x0 = (float) (position.x + 20 * Math.cos(angle));
			float y0 = (float) (position.y + 20 * Math.sin(angle)); // Y axis is downwards.
			eyeFixation = rayTrace(position.x,position.y, x0, y0);
			// We stop when we find a singularity.
			if (eyeFixation[2] != Model.WALL_COLOR.getRGB()){
				break;
			}
		}
		this.currentAngle = angle + step;
		
		return new PhotoreceptorCell( eyeFixation[0] , eyeFixation[1] , new Color( eyeFixation[2] ) );
	}
	
	/**
	 * Scan the squares that are on a ray from a viewpoint to a target square
	 *  http://playtechs.blogspot.com/2007/03/raytracing-on-grid.html 
	 * @return Distance to the dirty square if any, Ernest.INFINITE if no dirt. 
	 */
	//protected Pair<Integer, Color> rayTrace(float x0, float y0, float x1, float y1) {
	private int[] rayTrace(float x0, float y0, float x1, float y1) {
		float dx = Math.abs(x1 - x0);
		float dy = Math.abs(y1 - y0);
	    int i = Math.round(x0);
	    int j = Math.round(y0);
	    int n = 1;
	    int i_inc, j_inc;
	    float error;
	    //int k = Math.round(mPosition.getZ());
	    float cornerTresh = .05f * dx * dy;

	    if (dx == 0) {
	        i_inc = 0;
	        error = Float.POSITIVE_INFINITY;
	    } else if (x1 > x0) {
	        i_inc = 1;
	        n += Math.round(x1) - i;
	        error = ((Math.round(x0) + .5f) - x0) * dy;
	    } else {
	        i_inc = -1;
	        n += i - Math.round(x1);
	        error = (x0 - (Math.round(x0) - .5f)) * dy;
	    }
	    if (dy == 0) {
	        j_inc = 0;
	        error -= Float.POSITIVE_INFINITY;
	    } else if (y1 > y0) {
	        j_inc = 1;
	        n += Math.round(y1) - j;
	        error -= ((Math.round(y0) + .5f) - y0) * dx;
	    } else {
	        j_inc = -1;
	        n += j - Math.round(y1);
	        error -= (y0 - (Math.round(y0) - .5f)) * dx;
	    }
	    for (; n > 0; --n) 
	    {
	        // move on along the ray
	        if (error > cornerTresh) {
	            j += j_inc;
	            error -= dx;
	        } else if (error < -cornerTresh) {
	            i += i_inc;
	            error += dy;
	        } else {
	        	i += i_inc;
	    		j += j_inc;
	    		error += dy - dx;
	    		--n;
	        }

	        // Don't go outside the grid
	    	if ( this.model.isOutOfBoard( i , j ) ) 
	    		//return Pair.create(Ernest.INFINITE, WALL_COLOR);
	    		return new int[] {Ernest.INFINITE,Ernest.INFINITE,Model.WALL_COLOR.getRGB()};
	    	
	    	// Examine the block on the ray. Return wall or uninhibited dirty squares.
	    	Color bgc = this.env.m_blocks[i][j].seeBlock();
	    	if (Objects.equals(bgc, WALL_COLOR)) // don't see walls (for Ernest 11.4)
	    		//return Pair.create(Ernest.INFINITE, WALL_COLOR);
	    		return new int[] {Ernest.INFINITE,Ernest.INFINITE,Model.WALL_COLOR.getRGB()};
	    	
	    	if (this.env.isWall(i,j) || this.env.isFood(i,j) || this.env.isAlga(i,j))
	    	{
				//int dist = (int) Math.sqrt(((i-x0)*(i-x0) + (j-y0)*(j-y0)) * Ernest.INT_FACTOR * Ernest.INT_FACTOR);
				//return Pair.create(dist, bgc);
	    		return new int[] {(i- Math.round(x0)) , (j- Math.round(y0)) , bgc.getRGB()};

    		}
	    	//if (m_env.isAgent(i, j, mName))
	    	ErnestModel entity = this.env.getEntity(new v3(i,j,0), this.agentName);
	    	if (entity != null)
	    	{
				//int dist = (int) Math.sqrt(((i-x0)*(i-x0) + (j-y0)*(j-y0)) * Ernest.INT_FACTOR * Ernest.INT_FACTOR);
				//return Pair.create(dist, entity.getColor());//AGENT_COLOR);
	    		return new int[] {(i- Math.round(x0)) , (j- Math.round(y0)) , -entity.getColor().getRGB()};
	    	}

	    }
		//return Pair.create(Ernest.INFINITE, WALL_COLOR);
		return new int[] {Ernest.INFINITE,Ernest.INFINITE,Model.WALL_COLOR.getRGB()};
	}
}
