package com.github.lerks.pong;/*
 *  Copyright (C) 2010  Luca Wehrstedt
 *
 *  This file is released under the GPLv2
 *  Read the file 'COPYING' for more information
 */

import com.github.sarxos.webcam.util.ImageUtils;
import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.*;
import nars.learn.Agent;
import nars.util.experiment.Environment;
import org.encog.util.ImageSize;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.Image;
import java.awt.image.BufferedImage;

import static java.awt.Image.SCALE_SMOOTH;

public class PongEnvironment implements Environment {

	final int width = 64;
	final int height = 48;
	final int pixels = width * height;
	final int scale = 10;

	public PongEnvironment() throws AWTException {
		super ();

		JFrame j = new JFrame();
		j.setTitle ("Pong");
		j.setSize (width * scale, height * scale);
		j.setResizable(false);

		
		PongModel content = new PongModel(new Player.CPU_HARD(), new Player.CPU_EASY());
		content.acceleration = true;
		j.getContentPane ().add (content);
		
		j.addMouseListener (content);
		j.addKeyListener (content);

		j.setVisible(true);
		j.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

		Timer timer = new Timer (20, content);
		timer.start();

		BufferedImage big =
				ScreenImage.createImage((JComponent) j.getContentPane());

		BufferedImage small = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
		Graphics2D tGraphics2D = small.createGraphics(); //create a graphics object to paint to

		tGraphics2D.fillRect( 0, 0, width, height );
		tGraphics2D.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
		tGraphics2D.drawImage( big, 0, 0, width, height, null ); //draw the image scaled


		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int p = small.getRGB(x, y);
				int r = (p & 0x00ff0000) >> 16;
				String c = r > 0 ? "XX" : "..";
				System.out.print(c);
			}
			System.out.println();
		}

	}

	public static void main (String[] args) throws AWTException {
		new PongEnvironment();
	}

	@Override
	public Twin<Integer> start() {
		return Tuples.twin(pixels, 3);
	}

	@Override
	public float pre(int t, float[] ins) {
		return 0;
	}

	@Override
	public void post(int t, int action, float[] ins, Agent a) {

	}
}
