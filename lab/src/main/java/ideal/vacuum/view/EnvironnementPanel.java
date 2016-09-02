package ideal.vacuum.view;

import ideal.vacuum.Environment;
import ideal.vacuum.ErnestModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.*;
import java.util.ArrayList;

public class EnvironnementPanel extends JPanel implements MouseListener{

	public final static int CLICK_AGENT = 1; 
	public final static int CLICK_TARGET = 2; 
	public final static int CLICK_WALL = 3; 
	public final static int CLICK_MOVING_TARGET = 5; 
	public final static int CLICK_ALGA = 6; 
	public final static int CLICK_BRICK = 7; 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Environment m_env;
	public ArrayList<ErnestModel> m_modelList;
	//private int m_h=1;
	//private int m_w=1;
	public int m_clicked;
	public int m_clickX;
	public int m_clickY;
	
	public float m_FclickX;
	public float m_FclickY;
	
	public int e_x;
	public int e_y;
	
	public float[] ernest_x={ (float) -0.5,0,(float) 0.5};
	public float[] ernest_y={(float) 0.7,(float) -0.8,(float) 0.7};
	
	public float[] fish_x={(float) 0.8,(float) 0.6,(float) 0.4,(float) 0.2,0,(float)-0.2,(float)-0.4,(float)-0.6,(float)-0.8,
			               (float)-0.8,(float)-0.6,(float)-0.4,(float)-0.2,0,(float) 0.2,(float) 0.4,(float) 0.6,(float) 0.8};
	public float[] fish_y={0,(float) 0.4,(float) 0.5,(float) 0.5,(float) 0.5,(float) 0.4,(float) 0.3,0,(float) 0.5,
			               (float)-0.5,0,(float)-0.3,(float)-0.4,(float)-0.5,(float)-0.5,(float)-0.5,(float)-0.4,0};
	
	public float[] leaf_x={ 0, (float) 0.8 , (float) 0.7,(float) 0.2,0};
	public float[] leaf_y={ 0, (float) 0.2 , (float) 0.7,(float) 0.8,0};
	
	private final CubicCurve2D.Double petal1 = new CubicCurve2D.Double(0, 0,  0,80, 80,0,   0, 0);
	private final CubicCurve2D.Double petal2 = new CubicCurve2D.Double(0, 0, 80, 0,  0,-80, 0, 0);
	private final CubicCurve2D.Double petal3 = new CubicCurve2D.Double(0, 0,  0,-80,-80, 0, 0, 0);
	private final CubicCurve2D.Double petal4 = new CubicCurve2D.Double(0, 0, -80, 0, 0, 80, 0, 0);

	private final GeneralPath m_leaf = new GeneralPath();
	private final GeneralPath m_fish = new GeneralPath();
		
	public EnvironnementPanel(ArrayList<ErnestModel> modelList, Environment env){
		m_modelList=modelList;
		m_env=env;
		
		//m_h=env.getHeight();
		//m_w=env.getWidth();
		addMouseListener(this);
		
		m_leaf.append(petal1, false);
		m_leaf.append(petal2, false);
		m_leaf.append(petal3, false);
		m_leaf.append(petal4, false);
		
		// Fish shape
		//m_fish.append(new CubicCurve2D.Double(-40, 15,  -30, 0, 40, -40,   40, 0), false);
		//m_fish.append(new CubicCurve2D.Double( 40,  0,  40, 40, -30,  0,   -40, -15), true);
		//m_fish.append(new Area(new Ellipse2D.Double( 20,  -10,  8, 8)), false);
		
		m_fish.append(new Area(new Ellipse2D.Double( -30,  -30,  60, 60)), false);

	}
	
	
	public int[] transform_x(float[] polygon_x,float[] polygon_y,float angle_x,float scale_x,int translation_x){
		int[] ret=new int[polygon_x.length];
		for (int i=0;i<polygon_x.length;i++){
			// rotation and scale
			ret[i]=(int)( ( (polygon_x[i]*Math.cos(angle_x)) - (polygon_y[i]*Math.sin(angle_x)) )*scale_x);
			
			// translation
			ret[i]+=translation_x;
		}
		
		return ret;
	}
	
	public int[] transform_y(float[] polygon_x,float[] polygon_y,float angle_y,float scale_y,int translation_y){
		int[] ret=new int[polygon_y.length];
		for (int i=0;i<polygon_y.length;i++){
			// rotation and scale
			ret[i]=(int) (( (polygon_x[i]*Math.sin(angle_y)) + (polygon_y[i]*Math.cos(angle_y)) )*scale_y);
			
			// translation
			ret[i]+=translation_y;
		}
		
		return ret;
	}
	
	
	@Override
	public void paintComponent(Graphics g)
	{
		int m_w = m_env.getWidth();
		int m_h = m_env.getHeight();
		
		Graphics2D g2d = (Graphics2D)g;

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		AffineTransform boardReference = g2d.getTransform();

		int h=this.getHeight();
		int w=this.getWidth();
		
		int c_w=w/m_w;
		int c_h=h/m_h;

		g2d.setColor(Color.white);
		g2d.fillRect(0, 0, w, h);
		
		// draw agent
		for (int i=0;i<m_modelList.size();i++){
			m_modelList.get(i).paintAgent((Graphics2D)g.create(),(int) (m_modelList.get(i).mPosition.x*c_w+(c_w/2)),(int) (h-(m_modelList.get(i).mPosition.y+1)*c_h+(c_h/2)),(double)c_w/100,(double)c_h/100);
		}
		
		for (int i=0;i<m_w;i++){
			for (int j=0;j<m_h;j++){
				
				// walls
				if (m_env.isWall(i, j)){
					g2d.setColor(m_env.m_blocks[i][j].seeBlock());
					g2d.fillRect(i*c_w, h-(j+1)*c_h, c_w+1, c_h+1);
				}
				
				// fish
				if (m_env.isFood(i, j)){
					
					AffineTransform centerCell = new AffineTransform();
					centerCell.translate(i*c_w+c_w/2, h-(j+1)*c_h+c_h/2);
					centerCell.scale((double) c_w / 100, (double) c_h / 100); 
					g2d.transform(centerCell);
					g2d.setColor(m_env.m_blocks[i][j].seeBlock());
					g2d.fill(m_fish);
					g2d.setTransform(boardReference);
				}
				
				// leaf
				if (m_env.isAlga(i, j)){
					AffineTransform centerCell = new AffineTransform();
					centerCell.translate(i*c_w+c_w/2, h-(j+1)*c_h+c_h/2);
					centerCell.scale((double) c_w / 100, (double) c_h / 100); 
					g2d.transform(centerCell);

					g2d.setColor(m_env.m_blocks[i][j].seeBlock());
					
					g2d.fill(m_leaf);
					g2d.setTransform(boardReference);
				}
				
			}
		}
		
		// draw informations
		
		drawInformation((Graphics2D)g.create());
		
		// draw dream square
		//m_model.paintDream((Graphics2D)g.create(),c_w*(m_w-2)-c_w/2,c_h/2, (double)c_w/100, (double)c_h/100);
		//g2d.setColor(Color.black);
		//g.drawRect(c_w*(m_w-3), 0, c_w, c_h);
		
	}


	@Override
	public void mouseClicked(MouseEvent e){

		int m_w = m_env.getWidth();
		int m_h = m_env.getHeight();

		int h=this.getHeight();
		int w=this.getWidth();

		m_clickX= (e.getX() / (int)( (float)w/(float)m_w ));
		m_clickY= (e.getY() / (int)( (float)h/(float)m_h ));
		
		m_FclickX=(float)e.getX() / ((float)w/(float)m_w );
		m_FclickY=(float)e.getY() / ((float)h/(float)m_h );
		
		if (e.getButton() == MouseEvent.BUTTON1)
			if (e.isShiftDown()) m_clicked = 4;
			else m_clicked = CLICK_AGENT;
		if (e.getButton() == MouseEvent.BUTTON2)
				if (e.isShiftDown()) m_clicked = CLICK_MOVING_TARGET;
				else m_clicked = CLICK_TARGET;
		if (e.getButton() == MouseEvent.BUTTON3)
		{
			if (e.isShiftDown()) 
			{
				if(e.isControlDown())
					m_clicked = CLICK_BRICK;
				else	
					m_clicked = CLICK_ALGA;
			}
			else m_clicked = CLICK_WALL;
		}
	}
	
	public int getClicked()
	{
		int c = m_clicked;
		m_clicked = 0;
		return c;
	}


	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Draw the information square.
	 */
	private void drawInformation(Graphics2D g2d){
		
		int c_w = this.getWidth() /m_env.getWidth();
		int c_h = this.getHeight() /m_env.getHeight();
		
		String counter ="0";
		
		//if (m_modelList.size()>0) counter=m_modelList.get(0).getCounter() + ""; 
		counter = m_env.getCounter() + "";
		
		//Font font = new Font("Dialog", Font.BOLD, 10);
		Font font = new Font("Dialog", Font.BOLD, this.getWidth() /m_env.getWidth() /3);
		g2d.setFont(font);
		
		FontMetrics fm = getFontMetrics(font);

		int width = fm.stringWidth(counter);
		
		//g2d.setColor(new Color(200, 255, 200));		
		g2d.setColor(new Color(0xC0C0C0));		
		g2d.drawString(counter, this.getWidth() - c_w*1.2f - width, c_h*1.6f + 5);	
	}
	
}
