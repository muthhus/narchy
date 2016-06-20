package nars.rover.physics.gl;

import com.jogamp.newt.event.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.gl2.GLUgl2;
import nars.rover.Sim;
import nars.rover.physics.Display;
import nars.util.AbstractJoglPanel;
import org.jbox2d.dynamics.World2D;

import java.awt.*;
import java.nio.FloatBuffer;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

/* generic box2d physics-executing panel */
public class Box2DJoglPanel extends AbstractJoglPanel implements Display {


	//final Space2D sg = new DemoTextButton().getSpace();
	public final JoglDraw draw;
	public final World2D world;
	Game game;
	private Point center;
	private int w = 1024, h = 768;

	public Box2DJoglPanel(final Sim sim) {
		this(sim.world);
	}

	public Box2DJoglPanel(final World2D world) {
		super();
		this.world = world;
		this.draw = new JoglDraw(this);

	}

	private int forward = KeyEvent.VK_W;
	private int backward = KeyEvent.VK_S;
	private int strafeL = KeyEvent.VK_A;
	private int strafeR = KeyEvent.VK_D;
	private int shoot = InputEvent.BUTTON1_MASK;
	private int use = InputEvent.BUTTON3_MASK;

	final boolean mousePressed[] = new boolean[6];

	public void init(GLAutoDrawable drawable) {



		GL2 gl = drawable.getGL().getGL2();

		initEffects(gl);

		game = new Game();
		game.z = -300;

		game.setMouseCenter(new Point());

		GLU glu = new GLUgl2();

		gl.glViewport(0, 0, w, h);

		gl.glMatrixMode(GL_PROJECTION);

		glu.gluPerspective(25.0f, (((float) w) / h), 0.01f, 100f);

		gl.glMatrixMode(GL_MODELVIEW);

		gl.glShadeModel(gl.GL_SMOOTH);

		//gl.glClearColor(0.0f, 0.0f, 0.0f, 0.9f);
		gl.glClearAccum(0,0,0,0.9f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_ACCUM_BUFFER_BIT);

		gl.glClearDepth(1.0f);

		gl.glEnable(gl.GL_DEPTH_TEST);

		gl.glDepthFunc(gl.GL_LEQUAL);

		gl.glHint(gl.GL_PERSPECTIVE_CORRECTION_HINT, gl.GL_FASTEST);



		this.addKeyListener(new KeyAdapter() {

			@Override

			public void keyPressed(KeyEvent e) {

				int k = e.getKeyCode();
				if (k == KeyEvent.VK_ESCAPE) {

					new Thread(new Runnable() {

						public void run() {

							System.exit(0);

						}

					}).start();

				}

				//TODO switch/table
				if (k == forward) game.forward(+1f);

				else if (k == backward) game.forward(-1);

				else if (k == strafeL) game.strafe(+1);

				else if (k == strafeR) game.strafe(-1);

			}

			@Override

			public void keyReleased(KeyEvent e) {

				int k = e.getKeyCode();
				if (k == forward || k == backward) game.forward(0);
				else if (k == strafeL || k == strafeR) game.strafe(0);

			}

		});

		this.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				int bb = e.getButton();
				if (bb < mousePressed.length)
					mousePressed[bb] = true;
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				int bb = e.getButton();
				if (bb < mousePressed.length)
					mousePressed[bb] = false;
			}

			@Override
			public void mouseClicked(MouseEvent e) {

				if ((e.getModifiers() & shoot) != 0)

					game.click();

//                if((e.getModifiers() & use)!=0)
//
//                    game.setUse();

			}

		});

	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

//        GL2 gl = (GL2) drawable.getGL();

//        GLU glu = new GLU();
//
//        if (height <= 0) {
//            height = 1;
//        }
//
//        final float h = (float) width / (float) height;
//
//        gl.glViewport(0, 0, width, height);
//
//        gl.glMatrixMode(GL_PROJECTION);
//
//        gl.glLoadIdentity();
//
//        glu.gluPerspective(45.0f, h, 1.0, 20.0);
//
//        gl.glMatrixMode(GL_MODELVIEW);
//
//        gl.glLoadIdentity();

		center = new Point(x + width / 2, y + height / 2);

		game.setMouseCenter(center);

	}
	//TODO rename this as 'Camera'
	public class Game {


		private int dx, dy;

		private Point mouseCenter = new Point();
		private float ford, strafe;

		public Game() {
			matrix.put(mat);

		}

		public void setMouseCenter(Point center) {
			if (center != null)
				this.mouseCenter.setLocation(center);
		}

		float linSpeed = 75f;
		float angleSpeed = 0.00005f;

		public void forward(float v) {
			ford = v;
		}

		public void strafe(float v) {
			strafe = v;
		}

		public void click() {

		}

        /*

 * If polled events are true, we will instruct the Player class to act upon

 * them.

 */

		private final float _90 = (float) Math.toRadians(90);

		private final float _maxPitch = (float) Math.toRadians(90);

		private float heading = 0.0f;

		private float pitch = 0.0f;


 /*

 * cosa/sina deterine pitch. cosb/sinb determine strafe vector. cosz/sinz are

 * 90 degrees off cosb/sinb and determine forward/back. cosc/sinc determine

 * roll (lean) which we are not altering so we set identity values for them *

 */

		private float cosa, cosb, cosz, sina, sinb, sinz;

		private float cosc = 1.0f;

		private float sinc = 0.0f;

		private float x;
		private float y;
		public float z;//player position

 /*

 * OpenGL matrices are coloumn major. To envisage this take the normal

 * row major matrix and imagine rotating it 90 degrees, now mirror it about the y

axis.

 * Being an identity matrix, this one will still appear the same, except

 * that the idices 0, 1, 2, for example, will accees the first three

 * values down the first coloumn. Read Songho's article to see what I mean.

 */

		private float[] mat = {
				1, 0, 0, 0,

				0, 1, 0, 0,

				0, 0, 1, 0,

				0, 0, 0, 1};

		private FloatBuffer matrix = FloatBuffer.allocate(mat.length);



 /*

 * Heading is an angle. Given that 0 deg is East cos and sin values of

 * heading are calculated relative to East and are therefore relevant

 * for determining strafe movements. We need to add 90 deg to heading

 * to get cos and sin values relevant for moving the player forward or back

 */

		public void updateHeading(float amount) {

			//amount = (float)Math.log(amount)/5f; //prefilter curve the amount actually turning

			if (mousePressed[MouseEvent.BUTTON1])
				heading -= amount * angleSpeed;

			cosb = (float) Math.toRadians(Math.cos(heading));

			sinb = (float) Math.toRadians(Math.sin(heading));

			cosz = (float) Math.toRadians(Math.cos(heading + _90));

			sinz = (float) Math.toRadians(Math.sin(heading + _90));

		}

 /*

 * We don't want the player doing summersalts so we limit the

 * angles through which they can pitch

 */

		public void updatePitch(float amount) {


			//amount = (float)Math.log(amount)/5f; //prefilter curve the amount actually turning

			if (mousePressed[MouseEvent.BUTTON1])
				pitch -= amount * angleSpeed;

			if (pitch > _maxPitch) pitch = _maxPitch;

			if (pitch < -_maxPitch) pitch = -_maxPitch;

			cosa = (float) Math.cos(pitch);

			sina = (float) Math.sin(pitch);

		}

 /*

 * what we need to do is construct a vector that we can add to the player's

 * current position to give us the players new position. cosz/sinz give us

 * the direction of the vector, amount gives the scaling factor.

 */


		public void updateStrafe(float v) {
			x += cosb * v * linSpeed;
			z += sinb * v * linSpeed;
		}

		public void updateForward(float v) {
			x += cosz * v * linSpeed;
			z += sinz * v * linSpeed;
		}

 /*

 * Here is where the matrix for each loop is constructed. This calculation

 * is specific for z, x, y order rotations. See Songho for explanation.

 * Basically, any rotation about an axis alters all other axes due the

 * fact that all axes must remain orthogonal (at right angles to each other),

 * thus different orders will give different results.

 */

        /*

 * Before loading the new matrix we draw a quad which represents a weapon.

 * Because it is drawn before matrix is loaded the vertices are not altered

 * by the matrix. The Floor, which is drawn after the matrix is loaded will

 * alter every matrix change, the weapon will not.

 */


		public void camera(GL2 gl) {

            /*

            * Polled keyboard events are either true of false. However, we want to

            * send the Player class an amount to move. Sending through a static

            * amount like 0.5 eg would not be a good idea, because fast machines

            * would update the player's position 0.5 every loop and so to would

            * slow machines... the speed of the machine would affect the speed of

            * the player. So what we do is gab the amount from the nano timer.

            * This means that fast machines send out smaller amounts more often,

            * and slower machines bigger amounts less often. We also 'multiply' the

            * lastTime to nowTime by a suitable factor so we do not require

            * a further 'multiplication' later.

            */

			//long now=System.nanoTime();

			//float period=(float)((now-lastTime)*0.000005);
			float period = 1f;

			//lastTime=now;


			Point l = MouseInfo.getPointerInfo().getLocation();
			dx = l.x;
			dy = l.y;

			//System.out.println(x + " " + y + " " + z);


             /*

 * At each loop the Player class creates the correct ModelViewMatrix that

defines

 * where the player should be, and in what direction they should be heading.

 * This matrix is later loaded into GL instead of the identity matrix.

 * The following methods activate the player to setup relevant parts of the

 * matrix.

 */
			float head = mouseCenter.x - dx;


			if (head != 0) updateHeading(head);

			float pit = mouseCenter.y - dy;

			if (pit != 0) updatePitch(pit);
//
			if (ford != 0) updateForward(ford * (float) period);
			if (strafe != 0) updateStrafe(strafe * (float) period);

			FloatBuffer m = this.matrix;

			//TODO cache some of these components
			m.put(0, cosc * cosb - sinc * sina * sinb);

			m.put(1, sinc * cosb + cosc * sina * sinb);

			m.put(2, -cosa * sinb);

			m.put(4, -sinc * cosa);

			m.put(5, cosc * cosa);

			m.put(6, sina);

			m.put(8, cosc * sinb + sinc * sina * cosb);

			m.put(9, sinc * sinb - cosc * sina * cosb);

			m.put(10, cosa * cosb);

			m.put(12, m.get(0) * x + m.get(4) * y + m.get(8) * z);

			m.put(13, m.get(1) * x + m.get(5) * y + m.get(9) * z);

			m.put(14, m.get(2) * x + m.get(6) * y + m.get(10) * z);

			//if(robot!=null)

			//robot.mouseMove(mouseCenter.x, mouseCenter.y);

			//shoot=0;

			//use=0;

 /*

 * Everything drawn after the ModelViewMatrix has been sent to GL will

 * be effected by that matrix. As we will see in the Player class, weapons

 * that move with the player are drawn before the matrix is loaded,

 * but the floor is not.

 */

//            player.draw(gl);
//
//            floor.draw(gl);

			//gl.glLoadIdentity();

			gl.glLoadMatrixf(matrix);

			gl.glScalef(3, 0.05f, 1); //why is this restoring the balance of dmensions

//            for (int a = -8; a < 8; a++) {
//                for (int b = -8; b < 4; b++) {
//                    for (int c = -4; c < 4; c++) {
//                        gl.glPushMatrix();
//
//                        gl.glTranslatef(
//                                a * 10f, b *10f, c * 10f
//                        );
//
//                        gl.glColor4f(100f + (float) Math.sin(a * 3f) * 75f, 100 + 0.5f, ((a ^ b) + c) % 2 == 0 ? 0.9f : 0.1f, 0.7f);
//
//                        gl.glBegin(gl.GL_QUADS);
//
//                        gl.glVertex3f(1.0f, 1.0f, -1.0f);
//
//                        gl.glVertex3f(1.0f, 1.0f, 1.0f);
//
//                        gl.glVertex3f(-1.0f, 1.0f, 1.0f);
//
//                        gl.glVertex3f(-1.0f, 1.0f, -1f);
//
//                        gl.glEnd();
//
//                        gl.glPopMatrix();
//                    }
//                }
//            }



		}


	}


//    public void init2D(GLAutoDrawable arg0) {
//        GL gl2 = getGL();
//
//
//        //getGL().getGL2().glClearColor(0f, 0f, 0f, 1f);
//
//
//        new FPSAnimator(this, 25);
//    }

	public void initEffects(GL2 gl) {

		gl.glLineWidth(2f);

		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glEnable(GL.GL_LINE_WIDTH);

		// Enables Clearing Of The Depth Buffer
		gl.glClearDepth(1.0);

		gl.glEnable(GL.GL_DEPTH_TEST);                 // Enables Depth Testing
		//gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);    // Select The Type Of Blending
		//gl.glDepthFunc(GL.GL_LEQUAL);                  // The Type Of Depth Test To Do

		// Really Nice Perspective Calculations
		//gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		//gl.glEnable(GL.GL_TEXTURE_2D);      // Enable 2D Texture Mapping

		//gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		//gl.glEnable(GL.GL_ALPHA);
		//gl.glAlphaFunc(GL.GL_GREATER, .01f);




	}

	@Override
	public void display(GLAutoDrawable arg0) {

		GL2 gl = getGL().getGL2();

		// Clear the draw and depth buffers

		//gl.glClearAccum(0,0,0,-1f);
		float currentHeat = world.getHeat();
		gl.glClearColor(0f + currentHeat /0.25f, 0f, 0f, 1f);
		gl.glClear(/*GL2.GL_ACCUM_BUFFER_BIT | */GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		//gl.glClearAccum(0.0f, 0.0f, 0.0f, 1.0f);
		//gl.glClear(GL2.GL_ACCUM_BUFFER_BIT);
		//gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		//gl.glEnable(GL.GL_ALPHA);


		float time = world.time();//0.0f; // what does this?


		game.camera(gl);

//        {
//            //shake camera by heat
//            float rx = rng.nextFloat() * currentHeat * 5;
//            float ry = rng.nextFloat() * currentHeat * 5;
//            float rz = rng.nextFloat() * currentHeat * 5;
//            gl.glTranslatef(rx, ry, rz);
//        }


		draw(gl, time);

		game.matrix.rewind();


		//gl.glAccum( GL2.GL_MULT, 0.95f );
		//gl.glAccum( GL2.GL_ACCUM, 0.05f );
		//gl.glAccum( GL2.GL_RETURN, 1.0f );


		//https://www.opengl.org/sdk/docs/man2/xhtml/glAccum.xml


		//gl.glFlush();

		gl.glAccum( GL2.GL_MULT, 0.5f   );
		gl.glAccum( GL2.GL_ACCUM, 0.5f + currentHeat /4f);
		gl.glAccum(GL2.GL_RETURN, 1f);
		//swapBuffers();


		//i++;

		//if(i >= n) {
		//i = 0;
		//gl.glAccum(GL2.GL_RETURN, 0.75f);
		//swapBuffers();


		//swapBuffers();

		//repaint();


	}


	@Override
	public void grabFocus() {

	}

	@Override
	public boolean render() {
		return true;
	}

	@Override
	public void paintScreen() {
		//display();
	}



	protected void draw(GL2 gl, float dt) {

		if ((world!=null) && (draw!=null)) {
			gl.glPushMatrix();
			draw.draw(world, dt);
			gl.glPopMatrix();
		}

//        //ex. layer #2
//		gl.glPushMatrix();
//		gl.glScalef(8f,8f,8f); //HACK scale shouldnt be applied, keep them in flat scalespace
//		sg.draw(gl);
//		gl.glPopMatrix();
	}

//	@Override
//	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
//		super.reshape(drawable, x, y, width, height);
//	}

}
