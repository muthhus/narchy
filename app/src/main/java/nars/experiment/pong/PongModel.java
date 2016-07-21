package nars.experiment.pong;/*
 *  Copyright (C) 2010  Luca Wehrstedt
 *
 *  This file is released under the GPLv2
 *  Read the file 'COPYING' for more information
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PongModel extends JPanel implements ActionListener, MouseListener, KeyListener {
	// Proprietà della palla
	private static final int RADIUS = 15; // Raggio
	private static final int START_SPEED = 4; // Velocità iniziale
	private static final int ACCELERATION = 110; // Ogni quanti frame aumenta di 1 pixel la velocità

	// Proprietà dei carrelli
	public static final int SPEED = 15; // Velocità dei carrelli
	public static final int PADDLE_HEIGHT = 40; // SEMI-altezza del carrello
	public static final int WIDTH = 15;
	private static final int TOLERANCE = 5;
	private static final int PADDING = 0;

	public final Player player1;
	public final Player player2;
	
	private boolean new_game = true;
	
	public int ball_x;
	int ball_y;
	private double ball_x_speed;
	private double ball_y_speed;
	
	public boolean acceleration;
	private int ball_acceleration_count;
	
	private boolean mouse_inside;
	private boolean key_up;
	private boolean key_down;
	private final double minBallYSpeed = 0.1f;

	// Constructor
	public PongModel(Player player1, Player player2) {
		super ();
		setBackground (new Color (0, 0, 0));
		
		this.player1 = player1;
		this.player2 = player2;
	}
	
	// Compute destination of the ball
	private void computeDestination (Player player) {
		if (ball_x_speed > 0)
			player.destination = ball_y + (getWidth() - PADDING - WIDTH - RADIUS - ball_x) * (int)(ball_y_speed) / (int)(ball_x_speed);
		else
			player.destination = ball_y - (ball_x - PADDING - WIDTH - RADIUS) * (int)(ball_y_speed) / (int)(ball_x_speed);
		
		if (player.destination <= RADIUS)
			player.destination = 2 * PADDING - player.destination;
		
		if (player.destination > getHeight() - 10) {
			player.destination -= RADIUS;
			if ((player.destination / (getHeight() - 2 * RADIUS)) % 2 == 0)
				player.destination = player.destination % (getHeight () - 2 * RADIUS);
			else
				player.destination = getHeight() - 2 * RADIUS - player.destination % (getHeight () - 2 * RADIUS);
			player.destination += RADIUS;
		}
	}
	
	// Set new position of the player
	public void movePlayer(Player player, int destination) {
		int distance = Math.abs (player.position - destination);
		
		if (distance != 0) {
			int direction = - (player.position - destination) / distance;
			
			if (distance > SPEED)
				distance = SPEED;
			
			player.position += direction * distance;
			
			if (player.position - PADDLE_HEIGHT < 0)
				player.position = PADDLE_HEIGHT;
			if (player.position + PADDLE_HEIGHT > getHeight())
				player.position = getHeight() - PADDLE_HEIGHT;
		}
	}
	

	float bgFlash;

	// Draw
	@Override
	public void paintComponent (Graphics g) {

		
		// Prepara il campo di gioco
		if (new_game) {
			bgFlash = 1f;
			ball_x = getWidth () / 2;
			ball_y = getHeight () / 2;
			
			double phase = Math.random () * Math.PI / 2 - Math.PI / 4;
			ball_x_speed = (int)(Math.cos (phase) * START_SPEED);
			ball_y_speed = (int)(Math.sin (phase) * START_SPEED);
			
			ball_acceleration_count = 0;
			
			/*if (player1.getType() == Player.CPU_HARD || player1.getType() == Player.CPU_EASY)*/
			computeDestination (player1);
			/*if (player2.getType() == Player.CPU_HARD || player2.getType() == Player.CPU_EASY)*/
			computeDestination (player2);

			new_game = false;
		}

		bgFlash *= 0.95f;
		//setBackground(new Color(bgFlash,bgFlash,bgFlash));

		super.paintComponent (g);


		// Calcola la posizione del primo giocatore
		//if (player1.getType() == Player.MOUSE || player1.getType() == Player.KEYBOARD || ball_x_speed < 0)
			player1.computePosition(this);
		
		// Calcola la posizione del secondo giocatore
		//if (player2.getType() == Player.MOUSE || player2.getType() == Player.KEYBOARD || ball_x_speed > 0)
			player2.computePosition(this);
		
		// Calcola la posizione della pallina
		ball_x += ball_x_speed;
		ball_y += ball_y_speed;
//		if (ball_y_speed < 0) // Hack to fix double-to-int conversion
//			ball_y ++;

//
		
		// Accelera la pallina
		if (acceleration) {
			ball_acceleration_count ++;
			if (ball_acceleration_count == ACCELERATION) {
				ball_x_speed = ball_x_speed + (int)ball_x_speed / Math.hypot ((int)ball_x_speed, (int)ball_y_speed) * 2;
				ball_y_speed = ball_y_speed + (int)ball_y_speed / Math.hypot ((int)ball_x_speed, (int)ball_y_speed) * 2;
				ball_acceleration_count = 0;
			}
		}
		
		// Border-collision LEFT
		int bounceLX = PADDING + WIDTH + RADIUS;
		int dieLX = RADIUS;

		if (ball_x <= bounceLX) {
			int collision_point = ball_y + (int)(ball_y_speed / ball_x_speed * (PADDING + WIDTH + RADIUS - ball_x));
			if (ball_x <= bounceLX && collision_point > player1.position - PADDLE_HEIGHT - TOLERANCE &&
			    collision_point < player1.position + PADDLE_HEIGHT + TOLERANCE) {
				ball_x = 2 * (PADDING + WIDTH + RADIUS) - ball_x;
				player1.bounce();
				ball_x_speed = Math.abs (ball_x_speed);
				ball_y_speed -= Math.sin ((double)(player1.position - ball_y) / PADDLE_HEIGHT * Math.PI / 4)
				                * Math.hypot (ball_x_speed, ball_y_speed);
				//if (player2.getType() == Player.CPU_HARD)
					computeDestination (player2);
			}
			else if (ball_x <= dieLX) {
				player2.score();
				player1.ouch();
				new_game = true;
			}
		}
		
		// Border-collision RIGHT
		int bounceRX = getWidth() - PADDING - WIDTH - RADIUS;
		int dieRX = getWidth() - RADIUS;
		if (ball_x >= bounceRX) {
			int collision_point = ball_y - (int)(ball_y_speed / ball_x_speed * (ball_x - getWidth() + PADDING + WIDTH + RADIUS));
			if (ball_x >= bounceRX && collision_point > player2.position - PADDLE_HEIGHT - TOLERANCE &&
			    collision_point < player2.position + PADDLE_HEIGHT + TOLERANCE) {
				ball_x = 2 * (bounceRX) - ball_x;
				ball_x_speed = -1 * Math.abs (ball_x_speed);
				player2.bounce();
				ball_y_speed -= Math.sin ((double)(player2.position - ball_y) / PADDLE_HEIGHT * Math.PI / 4)
				                * Math.hypot (ball_x_speed, ball_y_speed);
				//if (player1.getType() == Player.CPU_HARD)
					computeDestination (player1);
			}
			else if (ball_x >= dieRX) {
				player1.score();
				player2.ouch();
				new_game = true;
			}
		}


		// Border-collision TOP
		if (ball_y <= RADIUS) {
			ball_y_speed = Math.abs (ball_y_speed);
			ball_y = 2 * RADIUS - ball_y;
		}
		
		// Border-collision BOTTOM
		if (ball_y >= getHeight() - RADIUS) {
			ball_y_speed = -1 * Math.abs (ball_y_speed);
			ball_y = 2 * (getHeight() - RADIUS) - ball_y;
		}
		
		// Disegna i carrelli
		//g.setColor (Color.WHITE);
		g.setColor (new Color(1f-bgFlash, 1f-bgFlash, 1f-bgFlash));

		g.fillRect (PADDING, player1.position - PADDLE_HEIGHT, WIDTH, PADDLE_HEIGHT * 2);
		g.fillRect (getWidth() - PADDING - WIDTH, player2.position - PADDLE_HEIGHT, WIDTH, PADDLE_HEIGHT * 2);
		
		// Disegna la palla
		g.fillOval (ball_x - RADIUS, ball_y - RADIUS, RADIUS*2, RADIUS*2);
		
		// Disegna i punti
		g.drawString (player1.points+" ", getWidth() / 2 - 20, 20);
		g.drawString (player2.points+" ", getWidth() / 2 + 20, 20);


		//prevent horizontal minimum
//		if (Math.abs(ball_y_speed) < minBallYSpeed)
//			ball_y_speed = (Math.random()-0.5f) * minBallYSpeed*4;

	}
	
	// New frame
	@Override
	public void actionPerformed (ActionEvent e) {
		repaint ();
	}
	
	// Mouse inside
	@Override
	public void mouseEntered (MouseEvent e) {
		mouse_inside = true;
	}
	
	// Mouse outside
	@Override
	public void mouseExited (MouseEvent e) {
		mouse_inside = false;
	}
	
	// Mouse pressed
	@Override
	public void mousePressed (MouseEvent e) {}
	
	// Mouse released
	@Override
	public void mouseReleased (MouseEvent e) {}
		
	// Mouse clicked
	@Override
	public void mouseClicked (MouseEvent e) {}
	
	// Key pressed
	@Override
	public void keyPressed (KeyEvent e) {
//		System.out.println ("Pressed "+e.getKeyCode()+"   "+KeyEvent.VK_UP+" "+KeyEvent.VK_DOWN);
		if (e.getKeyCode() == KeyEvent.VK_UP)
			key_up = true;
		else if (e.getKeyCode() == KeyEvent.VK_DOWN)
			key_down = true;
	}
	
	// Key released
	@Override
	public void keyReleased (KeyEvent e) {
//		System.out.println ("Released "+e.getKeyCode());
		if (e.getKeyCode() == KeyEvent.VK_UP)
			key_up = false;
		else if (e.getKeyCode() == KeyEvent.VK_DOWN)
			key_down = false;
	}
	
	// Key released
	@Override
	public void keyTyped (KeyEvent e) {}
}
