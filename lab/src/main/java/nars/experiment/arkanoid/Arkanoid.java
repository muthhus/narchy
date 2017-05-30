package nars.experiment.arkanoid;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** https://gist.github.com/Miretz/f10b18df01f9f9ebfad5 */
public class Arkanoid extends JFrame implements KeyListener {

	int score;

	public static final int SCREEN_WIDTH = 360;
	public static final int SCREEN_HEIGHT = 250;

	public static final int BLOCK_LEFT_MARGIN = 10;
	public static final int BLOCK_TOP_MARGIN = 15;

	public static final float BALL_RADIUS = 10.0f;
	public float BALL_VELOCITY = 1f;

	public static final float PADDLE_WIDTH = 30.0f;
	public static final float PADDLE_HEIGHT = 20.0f;
	public static final float PADDLE_VELOCITY = 0.6f;

	public static final float BLOCK_WIDTH = 40.0f;
	public static final float BLOCK_HEIGHT = 15.0f;

	public static final int COUNT_BLOCKS_X = 7;
	public static final int COUNT_BLOCKS_Y = 3;

	public static final float FT_STEP = 2.0f;


	/* GAME VARIABLES */


	private boolean running;

	public final Paddle paddle = new Paddle(SCREEN_WIDTH / 2, SCREEN_HEIGHT - PADDLE_HEIGHT);
	public final Ball ball = new Ball(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
	public final Collection<Brick> bricks = Collections.newSetFromMap(new ConcurrentHashMap());

	//private float lastFt;
	//private float currentSlice;

	abstract class GameObject {
		abstract float left();

		abstract float right();

		abstract float top();

		abstract float bottom();
	}

	class Rectangle extends GameObject {

		public float x, y;
		public float sizeX;
		public float sizeY;

		@Override
		float left() {
			return x - sizeX / 2.0f;
		}

		@Override
		float right() {
			return x + sizeX / 2.0f;
		}

		@Override
		float top() {
			return y - sizeY / 2.0f;
		}

		@Override
		float bottom() {
			return y + sizeY / 2.0f;
		}

	}



	void increaseScore() {
		score++;
		if (score == (COUNT_BLOCKS_X * COUNT_BLOCKS_Y)) {
			win();
		}
	}

	protected void win() {
		reset();
	}
	protected void die() {
		reset();
	}

	class Paddle extends Rectangle {

		public float velocity;

		public Paddle(float x, float y) {
			this.x = x;
			this.y = y;
			this.sizeX = PADDLE_WIDTH;
			this.sizeY = PADDLE_HEIGHT;
		}

		/** returns percent of movement accomplished */
		public void move(float dx) {
			x += dx;
			x = Math.max(x, sizeX);
			x = Math.min(x, SCREEN_WIDTH - sizeX);
		}

		void update() {
			x += velocity * FT_STEP;
		}

		void stopMove() {
			velocity = 0.0f;
		}

		void moveLeft() {
			if (left() > 0.0) {
				velocity = -PADDLE_VELOCITY;
			} else {
				velocity = 0.0f;
			}
		}

		void moveRight() {
			if (right() < SCREEN_WIDTH) {
				velocity = PADDLE_VELOCITY;
			} else {
				velocity = 0.0f;
			}
		}

		void draw(Graphics g) {
			g.setColor(Color.WHITE);
			g.fillRect((int) (left()), (int) (top()), (int) sizeX, (int) sizeY);
		}

		public void set(float freq) {
			x = freq * SCREEN_WIDTH;
		}

		public float moveTo(float target, float paddleSpeed) {
			target *= SCREEN_WIDTH;

			if (Math.abs(target-x) <= paddleSpeed) {
				x = target;
			} else if (target < x) {
				x -= paddleSpeed;
			} else {
				x += paddleSpeed;
			}

			x = Math.min(x, SCREEN_WIDTH-1);
			x = Math.max(x, 0);

			return (float)(x/SCREEN_WIDTH);
		}
	}


	static final AtomicInteger brickSerial = new AtomicInteger(0);

	class Brick extends Rectangle implements Comparable<Brick> {

		int id;
		boolean destroyed;

		Brick(float x, float y) {
			this.x = x;
			this.y = y;
			this.sizeX = BLOCK_WIDTH;
			this.sizeY = BLOCK_HEIGHT;
			this.id = brickSerial.incrementAndGet();
		}

		void draw(Graphics g) {
			g.setColor(Color.WHITE);
			g.fillRect((int) left(), (int) top(), (int) sizeX, (int) sizeY);
		}

		@Override
		public int compareTo(Brick o) {
			return Integer.compare(id, o.id);
		}
	}

	class Ball extends GameObject {

		public float x, y;
		float radius = BALL_RADIUS;

		//45 degree angle initially
		public float velocityX;
		public float velocityY;

		Ball(int x, int y) {
			this.x = x;
			this.y = y;
			setVelocityRandom();
		}

		public void setVelocityRandom() {
			this.setVelocity(BALL_VELOCITY, (float)(Math.random() * -Math.PI*(2/3f) + -Math.PI - Math.PI/6)); //angled downward
		}

		public void setVelocity(float speed, float angle) {
			this.velocityX = (float)Math.cos(angle) * speed;
			this.velocityY = (float)Math.sin(angle) * speed;
		}

		void draw(Graphics g) {
			g.setColor(Color.WHITE);
			g.fillOval((int) left(), (int) top(), (int) radius * 2,
					(int) radius * 2);
		}

		void update(Paddle paddle) {
			x += velocityX * FT_STEP;
			y += velocityY * FT_STEP;

			if (left() < 0)
				velocityX = BALL_VELOCITY;
			else if (right() > SCREEN_WIDTH)
				velocityX = -BALL_VELOCITY;
			if (top() < 0) {
				velocityY = BALL_VELOCITY;
			} else if (bottom() > SCREEN_HEIGHT) {
				velocityY = -BALL_VELOCITY;
				x = paddle.x;
				y = paddle.y - 50;
				score--;
				die();
			}

		}

		@Override
		float left() {
			return x - radius;
		}

		@Override
		float right() {
			return x + radius;
		}

		@Override
		float top() {
			return y - radius;
		}

		@Override
		float bottom() {
			return y + radius;
		}

	}

	boolean isIntersecting(GameObject mA, GameObject mB) {
		return mA.right() >= mB.left() && mA.left() <= mB.right()
				&& mA.bottom() >= mB.top() && mA.top() <= mB.bottom();
	}

	void testCollision(Paddle mPaddle, Ball mBall) {
		if (!isIntersecting(mPaddle, mBall))
			return;
		mBall.velocityY = -BALL_VELOCITY;
		if (mBall.x < mPaddle.x)
			mBall.velocityX = -BALL_VELOCITY;
		else
			mBall.velocityX = BALL_VELOCITY;
	}

	void testCollision(Brick mBrick, Ball mBall) {
		if (!isIntersecting(mBrick, mBall))
			return;

		mBrick.destroyed = true;

		increaseScore();

		float overlapLeft = mBall.right() - mBrick.left();
		float overlapRight = mBrick.right() - mBall.left();
		float overlapTop = mBall.bottom() - mBrick.top();
		float overlapBottom = mBrick.bottom() - mBall.top();

		boolean ballFromLeft = overlapLeft < overlapRight;
		boolean ballFromTop = overlapTop < overlapBottom;

		float minOverlapX = ballFromLeft ? overlapLeft : overlapRight;
		float minOverlapY = ballFromTop ? overlapTop : overlapBottom;

		if (minOverlapX < minOverlapY) {
			mBall.velocityX = ballFromLeft ? -BALL_VELOCITY : BALL_VELOCITY;
		} else {
			mBall.velocityY = ballFromTop ? -BALL_VELOCITY : BALL_VELOCITY;
		}
	}

	void initializeBricks(Collection<Brick> bricks) {
		// deallocate old bricks
		//synchronized(bricks) {
			bricks.clear();

			for (int iX = 0; iX < COUNT_BLOCKS_X; ++iX) {
				for (int iY = 0; iY < COUNT_BLOCKS_Y; ++iY) {
					bricks.add(new Brick((iX + 1) * (BLOCK_WIDTH + 3) + BLOCK_LEFT_MARGIN,
							(iY + 2) * (BLOCK_HEIGHT + 3) + BLOCK_TOP_MARGIN));
				}
			}
		//}
	}

	public Arkanoid(boolean visible) {

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setUndecorated(false);
		this.setResizable(false);
		this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
		this.addKeyListener(this);
		this.setLocationRelativeTo(null);
		setIgnoreRepaint(true);

		if (visible)
			this.setVisible(true);

		//this.createBufferStrategy(2);

		paddle.x = SCREEN_WIDTH / 2;

		reset();

	}

	public void reset() {
		initializeBricks(bricks);
		ball.x = SCREEN_WIDTH / 2;
		ball.y = SCREEN_HEIGHT / 2;
		ball.setVelocityRandom();
	}

//	void run() {
//
//
//		running = true;
//
//		reset();
//
//		while (running) {
//
//			next();
//
//		}
//
//		this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
//
//	}

	public float next() {

		//currentSlice += lastFt;

		//for (; currentSlice >= FT_SLICE; currentSlice -= FT_SLICE) {

		ball.update(paddle);
		paddle.update();
		testCollision(paddle, ball);

		//synchronized (bricks) {
			Iterator<Brick> it = bricks.iterator();
			while (it.hasNext()) {
				Brick brick = it.next();
				testCollision(brick, ball);
				if (brick.destroyed) {
					it.remove();
				}
			}
		//}

		//}


		SwingUtilities.invokeLater(this::repaint);
		//repaint();

        return score;
	}


	@Override
	public void paint(Graphics g) {
		// Code for the drawing goes here.
		//BufferStrategy bf = this.getBufferStrategy();

		try {



			g.setColor(Color.black);
			g.fillRect(0, 0, getWidth(), getHeight());

			ball.draw(g);
			paddle.draw(g);
			for (Brick brick : bricks) {
				brick.draw(g);
			}

		} finally {
			g.dispose();
		}


		//Toolkit.getDefaultToolkit().sync();

	}

	@Override
	public void keyPressed(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
			running = false;
		}

		switch (event.getKeyCode()) {
		case KeyEvent.VK_LEFT:
			paddle.moveLeft();
			break;
		case KeyEvent.VK_RIGHT:
			paddle.moveRight();
			break;
		default:
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent event) {
		switch (event.getKeyCode()) {
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_RIGHT:
			paddle.stopMove();
			break;
		default:
			break;
		}
	}

	@Override
	public void keyTyped(KeyEvent arg0) {

	}


}