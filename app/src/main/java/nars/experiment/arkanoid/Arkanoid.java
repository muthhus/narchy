package nars.experiment.arkanoid;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** https://gist.github.com/Miretz/f10b18df01f9f9ebfad5 */
public class Arkanoid extends JFrame implements KeyListener {

	int score;

	public static final int SCREEN_WIDTH = 420;
	public static final int SCREEN_HEIGHT = 250;

	public static final int BLOCK_LEFT_MARGIN = 10;
	public static final int BLOCK_TOP_MARGIN = 15;

	public static final double BALL_RADIUS = 10.0;
	public static final double BALL_VELOCITY = 1.5;

	public static final double PADDLE_WIDTH = 60.0;
	public static final double PADDLE_HEIGHT = 20.0;
	public static final double PADDLE_VELOCITY = 0.6;

	public static final double BLOCK_WIDTH = 40.0;
	public static final double BLOCK_HEIGHT = 15.0;

	public static final int COUNT_BLOCKS_X = 8;
	public static final int COUNT_BLOCKS_Y = 2;

	public static final double FT_STEP = 1.0;


	/* GAME VARIABLES */


	private boolean running;

	public final Paddle paddle = new Paddle(SCREEN_WIDTH / 2, SCREEN_HEIGHT - PADDLE_HEIGHT);
	private final Ball ball = new Ball(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
	private final List<Brick> bricks = new ArrayList<Arkanoid.Brick>();

	//private double lastFt;
	//private double currentSlice;

	abstract class GameObject {
		abstract double left();

		abstract double right();

		abstract double top();

		abstract double bottom();
	}

	class Rectangle extends GameObject {

		double x, y;
		double sizeX;
		double sizeY;

		@Override
		double left() {
			return x - sizeX / 2.0;
		}

		@Override
		double right() {
			return x + sizeX / 2.0;
		}

		@Override
		double top() {
			return y - sizeY / 2.0;
		}

		@Override
		double bottom() {
			return y + sizeY / 2.0;
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

		double velocity;

		public Paddle(double x, double y) {
			this.x = x;
			this.y = y;
			this.sizeX = PADDLE_WIDTH;
			this.sizeY = PADDLE_HEIGHT;
		}

		public void move(float dx) {
			x += dx;
			x = Math.max(x, sizeX);
			x = Math.min(x, SCREEN_WIDTH - sizeX);
		}

		void update() {
			x += velocity * FT_STEP;
		}

		void stopMove() {
			velocity = 0.0;
		}

		void moveLeft() {
			if (left() > 0.0) {
				velocity = -PADDLE_VELOCITY;
			} else {
				velocity = 0.0;
			}
		}

		void moveRight() {
			if (right() < SCREEN_WIDTH) {
				velocity = PADDLE_VELOCITY;
			} else {
				velocity = 0.0;
			}
		}

		void draw(Graphics g) {
			g.setColor(Color.WHITE);
			g.fillRect((int) (left()), (int) (top()), (int) sizeX, (int) sizeY);
		}

	}

	class Brick extends Rectangle {

		boolean destroyed;

		Brick(double x, double y) {
			this.x = x;
			this.y = y;
			this.sizeX = BLOCK_WIDTH;
			this.sizeY = BLOCK_HEIGHT;
		}

		void draw(Graphics g) {
			g.setColor(Color.WHITE);
			g.fillRect((int) left(), (int) top(), (int) sizeX, (int) sizeY);
		}
	}

	class Ball extends GameObject {

		double x, y;
		double radius = BALL_RADIUS;
		double velocityX = BALL_VELOCITY;
		double velocityY = BALL_VELOCITY;

		Ball(int x, int y) {
			this.x = x;
			this.y = y;
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
				die();
			}

		}

		@Override
		double left() {
			return x - radius;
		}

		@Override
		double right() {
			return x + radius;
		}

		@Override
		double top() {
			return y - radius;
		}

		@Override
		double bottom() {
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

		double overlapLeft = mBall.right() - mBrick.left();
		double overlapRight = mBrick.right() - mBall.left();
		double overlapTop = mBall.bottom() - mBrick.top();
		double overlapBottom = mBrick.bottom() - mBall.top();

		boolean ballFromLeft = overlapLeft < overlapRight;
		boolean ballFromTop = overlapTop < overlapBottom;

		double minOverlapX = ballFromLeft ? overlapLeft : overlapRight;
		double minOverlapY = ballFromTop ? overlapTop : overlapBottom;

		if (minOverlapX < minOverlapY) {
			mBall.velocityX = ballFromLeft ? -BALL_VELOCITY : BALL_VELOCITY;
		} else {
			mBall.velocityY = ballFromTop ? -BALL_VELOCITY : BALL_VELOCITY;
		}
	}

	void initializeBricks(List<Brick> bricks) {
		// deallocate old bricks
		synchronized(bricks) {
			bricks.clear();

			for (int iX = 0; iX < COUNT_BLOCKS_X; ++iX) {
				for (int iY = 0; iY < COUNT_BLOCKS_Y; ++iY) {
					bricks.add(new Brick((iX + 1) * (BLOCK_WIDTH + 3) + BLOCK_LEFT_MARGIN,
							(iY + 2) * (BLOCK_HEIGHT + 3) + BLOCK_TOP_MARGIN));
				}
			}
		}
	}

	public Arkanoid() {

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setUndecorated(false);
		this.setResizable(false);
		this.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
		this.setVisible(true);
		this.addKeyListener(this);
		this.setLocationRelativeTo(null);
		setIgnoreRepaint(true);

		//this.createBufferStrategy(2);


		reset();

	}

	public void reset() {
		initializeBricks(bricks);
		score = 0;
		ball.x = SCREEN_WIDTH / 2;
		ball.y = SCREEN_HEIGHT / 2;
		paddle.x = SCREEN_WIDTH / 2;

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

		synchronized (bricks) {
			Iterator<Brick> it = bricks.iterator();
			while (it.hasNext()) {
				Brick brick = it.next();
				testCollision(brick, ball);
				if (brick.destroyed) {
					it.remove();
				}
			}
		}

		//}


		SwingUtilities.invokeLater(this::repaint);

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