package com.github.lerks.pong;/*
 *  Copyright (C) 2010  Luca Wehrstedt
 *
 *  This file is released under the GPLv2
 *  Read the file 'COPYING' for more information
 */

abstract public class Player {

	abstract public void computePosition(PongModel pong);

	// Tipi di giocatore
//	public static final int CPU_EASY = 0;
//	public static final int CPU_HARD = 1;
//	public static final int MOUSE = 2;
//	public static final int KEYBOARD = 3;

//	// Compute player position
//	private void computePosition (Player player) {
//
//		// MOUSE
//		if (player.getType() == Player.MOUSE) {
//			if (mouse_inside) {
//				int cursor = getMousePosition().y;
//				movePlayer (player, cursor);
//			}
//		}
//		// KEYBOARD
//		else if (player.getType() == Player.MOUSE) {
//			if (key_up && !key_down) {
//				movePlayer (player, player.position - SPEED);
//			}
//			else if (key_down && !key_up) {
//				movePlayer (player, player.position + SPEED);
//			}
//		}

//	}

	public static final class CPU_EASY extends Player {

		@Override
		public void computePosition(PongModel pong) {
			pong.movePlayer(this, pong.ball_y);
		}
	}
	public static final class CPU_HARD extends Player {

		@Override
		public void computePosition(PongModel pong) {
			pong.movePlayer(this, destination);
		}
	}

	public int position = 0;
	public int destination = 0;
	public int points = 0;
	

}
