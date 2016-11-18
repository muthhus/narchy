package nars.experiment.pacman;

class Organism {
	int x;
	int y;
	
	public enum Direction {
		UP,
		DOWN,
		LEFT,
		RIGHT,
		NONE
	}
	
	Direction dir, dirY;
	
	public Organism(int x, int y) {
		this.x = x;
		this.y = y;
		this.dir = Direction.NONE;
	}
	
	// return true if there is a wall on the board at screen position (x, y)
	public static boolean isWallAtPoint(PacmanGame.Things board[][], int x, int y)
	{
		int i = y / PacmanGame.SQUARE_SIZE;
		int j = x / PacmanGame.SQUARE_SIZE;
		
		if (board[i][j] == PacmanGame.Things.WALL)
			return true;
		else
			return false;
	}

	public synchronized int move(PacmanGame.Things board[][], int dx, int dy) {
		int newX = x, newY = y;


		int i;
		for (i = 0;  (dx!=0 || dy!=0);) {


			if (dx != 0) {
				int ddx = dx > 0 ? 1 : -1;
				if (!isWallAtPoint(board, newX + ddx, newY) &&
						!isWallAtPoint(board, newX + ddx + PacmanGame.SQUARE_SIZE/2 - 1, newY + PacmanGame.SQUARE_SIZE/2 - 1)) {
					newX = newX + ddx;
					i++;
				}
			}

			if (dy != 0) {
				int ddy = dy > 0 ? 1 : -1;
				if (!isWallAtPoint(board, newX, newY + ddy) &&
						!isWallAtPoint(board, newX + PacmanGame.SQUARE_SIZE/2 - 1, newY + ddy + PacmanGame.SQUARE_SIZE/2 - 1)) {
					newY = newY + ddy;
					i++;
				}
			}

			if (dx > 0) dx--;
			if (dy > 0) dy--;
			if (dx < 0) dx++;
			if (dy < 0) dy++;

			moved(board, newX, newY);
		}

		x = newX;
		y = newY;

		return i;
	}

	protected void moved(PacmanGame.Things board[][], int newX, int newY) {

	}

	public void move(PacmanGame.Things board[][], int speed) {
		int newX = x, newY = y;

		if(dir == Direction.RIGHT)
			newX += speed;
		if(dir == Direction.LEFT)
			newX -= speed;
		if(dir == Direction.UP)
			newY -= speed;
		if(dir == Direction.DOWN)
			newY += speed;
		
		if (isWallAtPoint(board, newX, newY) ||
			isWallAtPoint(board, newX + PacmanGame.SQUARE_SIZE - 1, newY + PacmanGame.SQUARE_SIZE - 1))
		{
			// we will run into a wall if we keep moving, so stop moving
			dir = Direction.NONE;
		} else {
			x = newX;
			y = newY;
		}
	}
}
