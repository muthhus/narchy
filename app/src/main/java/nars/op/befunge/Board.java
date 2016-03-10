package nars.op.befunge;

import org.jetbrains.annotations.NotNull;

/**
 * Created by didrik on 30.12.2014.
 */
public class Board {

	@NotNull
	private final char[][] board;

	Board() {
		board = new char[25][80];
	}

	char get(int y, int x) {
		return board[y][x];
	}

	void put(int y, int x, char c) {
		board[y][x] = c;
	}
}
