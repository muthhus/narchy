package com.github.lerks.pong;/*
 *  Copyright (C) 2010  Luca Wehrstedt
 *
 *  This file is released under the GPLv2
 *  Read the file 'COPYING' for more information
 */

import javax.swing.*;

public class Pong  {

	public Pong() {
		super ();

		JFrame j = new JFrame();
		j.setTitle ("Pong");
		j.setSize (640, 480);
		
		PongModel content = new PongModel(new Player.CPU_HARD(), new Player.CPU_EASY());
		content.acceleration = true;
		j.getContentPane ().add (content);
		
		j.addMouseListener (content);
		j.addKeyListener (content);

		j.setVisible(true);
		j.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

		Timer timer = new Timer (20, content);
		timer.start();
	}

	public static void main (String[] args) {
		new Pong();
	}
}
