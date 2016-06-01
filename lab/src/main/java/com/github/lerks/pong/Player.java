package com.github.lerks.pong;/*
 *  Copyright (C) 2010  Luca Wehrstedt
 *
 *  This file is released under the GPLv2
 *  Read the file 'COPYING' for more information
 */

abstract public class Player {

	abstract public void computePosition(PongModel pong);

	public void bounce() {
		points++;
	}

	public void score() {
		points++;
	}

	public void ouch() {
		points--;
	}


	public void moveTo(int y, PongModel pong) {
		position = y;
		clip(pong);
	}
	public void move(int dy, PongModel pong) {
		position += dy;
		clip(pong);
	}

	public void clip(PongModel pong) {
		int maxPos = pong.getHeight() - pong.HEIGHT;
		int minPos = pong.HEIGHT;
		if (position > maxPos)
			position = maxPos;
		if (position < minPos)
			position = minPos;
	}
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

		float noise = 10f;
		@Override
		public void computePosition(PongModel pong) {

			pong.movePlayer(this, pong.ball_y + (int)(Math.random() * noise));
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
	public float points = 0;
	

}
