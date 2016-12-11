package nars.experiment.pacman;

import java.awt.*;

class Pacman extends Organism {
	int mouthAngle=0;
	final int MAX_MOUTH_ANGLE=75;
	boolean isMouthOpening=true;
	public Pacman(int x, int y) {
		super(x, y);
		this.dir = Direction.LEFT;
	}

	@Override
	protected void moved(PacmanGame.Things board[][], int newX, int newY) {
		super.moved(board, newX, newY);

		int i = (y+PacmanGame.SQUARE_SIZE/2) / PacmanGame.SQUARE_SIZE;
		int j = (x+PacmanGame.SQUARE_SIZE/2) / PacmanGame.SQUARE_SIZE;
		if (isMouthOpening){
			if (mouthAngle==MAX_MOUTH_ANGLE){
				isMouthOpening=false;
			}
			else
				mouthAngle+= 5;
		}
		if (isMouthOpening==false){
			if (mouthAngle==0){
				isMouthOpening=true;
			}
			else
				mouthAngle-= 5;
		}
		if (board[i][j] == PacmanGame.Things.DOT)
		{
			board[i][j] = PacmanGame.Things.EMPTY;
			PacmanGame.score+=1;
		}
		else if (board[i][j] == PacmanGame.Things.CHERRY)
		{
			board[i][j] = PacmanGame.Things.EMPTY;
			PacmanGame.score+=5;
		}
	}

	@Override
	public void move(PacmanGame.Things board[][], int speed) {
		super.move(board,  speed);

		moved(board, x, y);
	}

	public void drawPacman(Graphics2D g){
		g.setColor(Color.WHITE);
		if (dir == Direction.RIGHT) {
			g.fillArc(x,y,PacmanGame.SQUARE_SIZE, PacmanGame.SQUARE_SIZE, mouthAngle/2, 360-mouthAngle);			
		} else if (dir == Direction.LEFT) {
			g.fillArc(x,y,PacmanGame.SQUARE_SIZE, PacmanGame.SQUARE_SIZE, 180+mouthAngle/2, 360-mouthAngle);	
		}else if (dir == Direction.UP) {
			g.fillArc(x,y,PacmanGame.SQUARE_SIZE, PacmanGame.SQUARE_SIZE, 90+mouthAngle/2, 360-mouthAngle);	
		}else if (dir == Direction.DOWN) {
			g.fillArc(x,y,PacmanGame.SQUARE_SIZE, PacmanGame.SQUARE_SIZE, 270+mouthAngle/2, 360-mouthAngle);	
		}else if (dir == Direction.NONE) {
			g.fillArc(x,y,PacmanGame.SQUARE_SIZE, PacmanGame.SQUARE_SIZE,0, 360);
		}
	}
}
		