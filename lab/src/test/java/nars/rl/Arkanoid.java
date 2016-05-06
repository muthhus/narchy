package nars.rl;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;

/** https://gist.github.com/Miretz/f10b18df01f9f9ebfad5 */
public class Arkanoid extends JFrame implements KeyListener {

	private static final long serialVersionUID = 1L;

	/* CONSTANTS */

	public static final int SCREEN_WIDTH = 800;
	public static final int SCREEN_HEIGHT = 600;

	public static final double BALL_RADIUS = 10.0;
	public static final double BALL_VELOCITY = 0.7;

	public static final double PADDLE_WIDTH = 120.0;
	public static final double PADDLE_HEIGHT = 20.0;
	public static final double PADDLE_VELOCITY = 0.6;

	public static final double BLOCK_WIDTH = 60.0;
	public static final double BLOCK_HEIGHT = 20.0;

	public static final int COUNT_BLOCKS_X = 11;
	public static final int COUNT_BLOCKS_Y = 4;

	public static final int PLAYER_LIVES = 5;

	public static final double FT_SLICE = 1.0;
	public static final double FT_STEP = 1.0;

	private static final String FONT = "Monospace";

	/* GAME VARIABLES */

	private boolean tryAgain = false;
	private boolean running = false;

	private Paddle paddle = new Paddle(SCREEN_WIDTH / 2, SCREEN_HEIGHT - 50);
	private Ball ball = new Ball(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
	private List<Brick> bricks = new ArrayList<Arkanoid.Brick>();
	private ScoreBoard scoreboard = new ScoreBoard();

	private double lastFt;
	private double currentSlice;

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

		double left() {
			return x - sizeX / 2.0;
		}

		double right() {
			return x + sizeX / 2.0;
		}

		double top() {
			return y - sizeY / 2.0;
		}

		double bottom() {
			return y + sizeY / 2.0;
		}

	}

	class ScoreBoard {

		int score = 0;
		int lives = PLAYER_LIVES;
		boolean win = false;
		boolean gameOver = false;
		String text = "";

		Font font;

		ScoreBoard() {
			font = new Font(FONT, Font.PLAIN, 12);
			text = "Welcome to Arkanoid Java version";
		}

		void increaseScore() {
			score++;
			if (score == (COUNT_BLOCKS_X * COUNT_BLOCKS_Y)) {
				win = true;
				text = "You have won! \nYour score was: " + score
						+ "\n\nPress Enter to restart";
			} else {
				updateScoreboard();
			}
		}

		void die() {
			lives--;
			if (lives == 0) {
				gameOver = true;
				text = "You have lost! \nYour score was: " + score
						+ "\n\nPress Enter to restart";
			} else {
				updateScoreboard();
			}
		}

		void updateScoreboard() {
			text = "Score: " + score + "  Lives: " + lives;
		}

		void draw(Graphics g) {
			if (win || gameOver) {
				font = font.deriveFont(50f);
				FontMetrics fontMetrics = g.getFontMetrics(font);
				g.setColor(Color.WHITE);
				g.setFont(font);
				int titleHeight = fontMetrics.getHeight();
				int lineNumber = 1;
				for (String line : text.split("\n")) {
					int titleLen = fontMetrics.stringWidth(line);
					g.drawString(line, (SCREEN_WIDTH / 2) - (titleLen / 2),
							(SCREEN_HEIGHT / 4) + (titleHeight * lineNumber));
					lineNumber++;

				}
			} else {
				font = font.deriveFont(34f);
				FontMetrics fontMetrics = g.getFontMetrics(font);
				g.setColor(Color.WHITE);
				g.setFont(font);
				int titleLen = fontMetrics.stringWidth(text);
				int titleHeight = fontMetrics.getHeight();
				g.drawString(text, (SCREEN_WIDTH / 2) - (titleLen / 2),
						titleHeight + 5);

			}
		}

	}

	class Paddle extends Rectangle {

		double velocity = 0.0;

		public Paddle(double x, double y) {
			this.x = x;
			this.y = y;
			this.sizeX = PADDLE_WIDTH;
			this.sizeY = PADDLE_HEIGHT;
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
			g.setColor(Color.RED);
			g.fillRect((int) (left()), (int) (top()), (int) sizeX, (int) sizeY);
		}

	}

	class Brick extends Rectangle {

		boolean destroyed = false;

		Brick(double x, double y) {
			this.x = x;
			this.y = y;
			this.sizeX = BLOCK_WIDTH;
			this.sizeY = BLOCK_HEIGHT;
		}

		void draw(Graphics g) {
			g.setColor(Color.YELLOW);
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
			g.setColor(Color.RED);
			g.fillOval((int) left(), (int) top(), (int) radius * 2,
					(int) radius * 2);
		}

		void update(ScoreBoard scoreBoard, Paddle paddle) {
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
				scoreBoard.die();
			}

		}

		double left() {
			return x - radius;
		}

		double right() {
			return x + radius;
		}

		double top() {
			return y - radius;
		}

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

	void testCollision(Brick mBrick, Ball mBall, ScoreBoard scoreboard) {
		if (!isIntersecting(mBrick, mBall))
			return;

		mBrick.destroyed = true;

		scoreboard.increaseScore();

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
		bricks.clear();

		for (int iX = 0; iX < COUNT_BLOCKS_X; ++iX) {
			for (int iY = 0; iY < COUNT_BLOCKS_Y; ++iY) {
				bricks.add(new Brick((iX + 1) * (BLOCK_WIDTH + 3) + 22,
						(iY + 2) * (BLOCK_HEIGHT + 3) + 20));
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

		this.createBufferStrategy(2);

		initializeBricks(bricks);

	}

	void run() {

		BufferStrategy bf = this.getBufferStrategy();
		Graphics g = bf.getDrawGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());

		running = true;

		while (running) {

			long time1 = System.currentTimeMillis();

			if (!scoreboard.gameOver && !scoreboard.win) {
				tryAgain = false;
				update();
				drawScene(ball, bricks, scoreboard);

				// to simulate low FPS
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			} else {
				if (tryAgain) {
					tryAgain = false;
					initializeBricks(bricks);
					scoreboard.lives = PLAYER_LIVES;
					scoreboard.score = 0;
					scoreboard.win = false;
					scoreboard.gameOver = false;
					scoreboard.updateScoreboard();
					ball.x = SCREEN_WIDTH / 2;
					ball.y = SCREEN_HEIGHT / 2;
					paddle.x = SCREEN_WIDTH / 2;
				}
			}

			long time2 = System.currentTimeMillis();
			double elapsedTime = time2 - time1;

			lastFt = elapsedTime;

			double seconds = elapsedTime / 1000.0;
			if (seconds > 0.0) {
				double fps = 1.0 / seconds;
				this.setTitle("FPS: " + fps);
			}

		}

		this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));

	}

	private void update() {

		currentSlice += lastFt;

		for (; currentSlice >= FT_SLICE; currentSlice -= FT_SLICE) {

			ball.update(scoreboard, paddle);
			paddle.update();
			testCollision(paddle, ball);

			Iterator<Brick> it = bricks.iterator();
			while (it.hasNext()) {
				Brick brick = it.next();
				testCollision(brick, ball, scoreboard);
				if (brick.destroyed) {
					it.remove();
				}
			}

		}
	}

	private void drawScene(Ball ball, List<Brick> bricks, ScoreBoard scoreboard) {
		// Code for the drawing goes here.
		BufferStrategy bf = this.getBufferStrategy();
		Graphics g = null;

		try {

			g = bf.getDrawGraphics();

			g.setColor(Color.black);
			g.fillRect(0, 0, getWidth(), getHeight());

			ball.draw(g);
			paddle.draw(g);
			for (Brick brick : bricks) {
				brick.draw(g);
			}
			scoreboard.draw(g);

		} finally {
			g.dispose();
		}

		bf.show();

		Toolkit.getDefaultToolkit().sync();

	}

	@Override
	public void keyPressed(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
			running = false;
		}
		if (event.getKeyCode() == KeyEvent.VK_ENTER) {
			tryAgain = true;
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

	public static void main(String[] args) {
		new Arkanoid().run();
	}

}