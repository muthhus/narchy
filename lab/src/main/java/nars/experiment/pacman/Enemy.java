package nars.experiment.pacman;

import java.util.ArrayList;
import java.util.Random;

//import PacmanGame.Things;


class Enemy extends Organism {

	private Organism pacman;
	
	public Enemy(int x, int y, Organism pacman) {
		super(x,y);
		this.pacman = pacman;
	}

	@Override
	public void move(PacmanGame.Things board[][], int speed) {
		super.move(board, speed);
		
		ArrayList<Direction> possibleFollowingDirections = new ArrayList<Direction>();
		
		if (x % PacmanGame.SQUARE_SIZE == 0 &&
			y % PacmanGame.SQUARE_SIZE == 0) {
			if ((Math.abs(pacman.x-this.x)+Math.abs(pacman.y-this.y))<4*PacmanGame.SQUARE_SIZE){
				if (pacman.x>this.x && !isWallAtPoint(board, x + PacmanGame.SQUARE_SIZE, y)){
					possibleFollowingDirections.add(Direction.RIGHT);
				}
				if (pacman.x<this.x && !isWallAtPoint(board, x - PacmanGame.SQUARE_SIZE, y)){
					possibleFollowingDirections.add(Direction.LEFT);
				}
				if (pacman.y<this.y && !isWallAtPoint(board, x, y - PacmanGame.SQUARE_SIZE)){
					possibleFollowingDirections.add(Direction.UP);
				}
				if (pacman.y>this.y && !isWallAtPoint(board, x, y + PacmanGame.SQUARE_SIZE)){
					possibleFollowingDirections.add(Direction.DOWN);
				}
				Random r = new Random();
				// Get a random between 0 and ( possibleDirections.size() - 1 )
				if (possibleFollowingDirections.size()==1){
					dir = possibleFollowingDirections.get(0);
				}
				else if (possibleFollowingDirections.size()==0){
					random_direction(board);
				}
				else {
					int index = r.nextInt(possibleFollowingDirections.size());
					dir = possibleFollowingDirections.get(index);
				}
			}
			else{
				random_direction(board);	
			}
		}
	}
	
	public void random_direction(PacmanGame.Things board[][]){
		
			// pick a new direction from among the possible directions
			ArrayList<Direction> possibleDirections = new ArrayList<Direction>();

			if (!isWallAtPoint(board, x - PacmanGame.SQUARE_SIZE, y)) {
				possibleDirections.add(Direction.LEFT);
			}
			if (!isWallAtPoint(board, x + PacmanGame.SQUARE_SIZE, y)) {
				possibleDirections.add(Direction.RIGHT);
			}
			if (!isWallAtPoint(board, x, y - PacmanGame.SQUARE_SIZE)) {
				possibleDirections.add(Direction.UP);
			}
			if (!isWallAtPoint(board, x, y + PacmanGame.SQUARE_SIZE)) {
				possibleDirections.add(Direction.DOWN);
			}
			
			if (dir ==Direction.LEFT) {
				if (possibleDirections.size()!=1){
					possibleDirections.remove(Direction.RIGHT);
				}
			}
			if (dir ==Direction.RIGHT) {
				if (possibleDirections.size()!=1){
					possibleDirections.remove(Direction.LEFT);
				}
			}
			if (dir ==Direction.UP) {
				if (possibleDirections.size()!=1){
					possibleDirections.remove(Direction.DOWN);
				}
			}
			if (dir ==Direction.DOWN) {
				if (possibleDirections.size()!=1){
					possibleDirections.remove(Direction.UP);
				}
			}
		
			Random r = new Random();
			// Get a random between 0 and ( possibleDirections.size() - 1 )
			int index = r.nextInt(possibleDirections.size());
			dir = possibleDirections.get(index);
			
		
		
	}
}









