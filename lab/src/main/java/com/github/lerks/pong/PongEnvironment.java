package com.github.lerks.pong;/*
 *  Copyright (C) 2010  Luca Wehrstedt
 *
 *  This file is released under the GPLv2
 *  Read the file 'COPYING' for more information
 */

import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import javafx.scene.layout.BorderPane;
import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.guifx.chart.MatrixImage;
import nars.guifx.util.ColorArray;
import nars.learn.Agent;
import nars.nar.Default;
import nars.op.mental.Abbreviation2;
import nars.op.time.MySTMClustered;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.util.FX;
import nars.util.NAgent;
import nars.util.experiment.Environment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static nars.$.*;

public class PongEnvironment extends Player implements Environment {

	final int width = 36;
	final int height = 20;
	final int pixels = width * height;
	final int scale = 20;
	final int ticksPerFrame = 1; //framerate divisor
	private final PongModel pong;
	private final MatrixImage priMatrix;
	float bias = 0f; //pain of boredom
	private NAgent nar;

	public static void main (String[] args) throws AWTException {
		PongEnvironment e = new PongEnvironment();

		Default nar = new Default();
		nar.core.conceptsFiredPerCycle.set(8);
		nar.beliefConfidence(0.85f);
		nar.goalConfidence(0.65f); //must be slightly higher than epsilon's eternal otherwise it overrides
		nar.DEFAULT_BELIEF_PRIORITY = 0.6f;
		nar.DEFAULT_GOAL_PRIORITY = 0.6f;
		nar.DEFAULT_QUESTION_PRIORITY = 0.4f;
		nar.DEFAULT_QUEST_PRIORITY = 0.4f;
		nar.cyclesPerFrame.set(512);


		NAgent a = new NAgent(nar);
		new Abbreviation2(nar, "_");
		new MySTMClustered(nar, 16, '.');

		//DQN a = new DQN();
		//HaiQAgent a = new HaiQAgent();

		e.run(a, 8192);

		NAR.printTasks(nar, true);
		NAR.printTasks(nar, false);
		a.printActions();
		nar.forEachConcept(System.out::println);
	}


	public PongEnvironment() {
		super();

		JFrame j = new JFrame();
		j.setTitle ("Pong");
		j.setSize (width * scale, height * scale);
		j.setResizable(false);


		pong = new PongModel(this, new Player.CPU_EASY());
		pong.acceleration = false;
		j.getContentPane ().add (pong);

		j.addMouseListener (pong);
		j.addKeyListener (pong);

		j.setVisible(true);
		j.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

		priMatrix = new MatrixImage();
		FX.run(()->{
			BorderPane priMatrixPane = new BorderPane(priMatrix);
			FX.newWindow("Priority", priMatrixPane);
			priMatrix.fitWidthProperty().bind(priMatrixPane.widthProperty());
			priMatrix.fitHeightProperty().bind(priMatrixPane.heightProperty());
		});


	}


	@Override
	public void preStart(Agent a) {
		if (a instanceof NAgent) {
			//provide custom sensor input names for the nars agent

			nar = (NAgent) a;

			for (int i = 1; i < Math.max(width,height); i++) {
				nar.nar.believe("(" + (i-1) + " <-> " + i + ")", 0.75f, 1f);
			}

			nar.setSensorNamer((i) -> {
//				int cell = i;
//				int type = i % 3;
//				Atom typeTerm;
//				switch (type) {
//					case 0: typeTerm = WALL; break;
//					case 1: typeTerm = PILL; break;
//					case 2: typeTerm = GHOST; break;
//					default:
//						throw new RuntimeException();
//				}

				int ax = i % width;
				int ay = i / width;

				//Term squareTerm = $.p($.the(ax), $.the(ay));

//				int dx = (visionRadius  ) - ax;
//				int dy = (visionRadius  ) - ay;
//				Atom dirX, dirY;
//				if (dx == 0) dirX = $.the("v"); //vertical
//				else if (dx > 0) dirX = $.the("r"); //right
//				else /*if (dx < 0)*/ dirX = $.the("l"); //left
//				if (dy == 0) dirY = $.the("h"); //horizontal
//				else if (dy > 0) dirY = $.the("u"); //up
//				else /*if (dy < 0)*/ dirY = $.the("d"); //down
//				Term squareTerm = $.p(
//						//$.p(dirX, $.the(Math.abs(dx))),
//						$.inh($.the(Math.abs(dx)), dirX),
//						//$.p(dirY, $.the(Math.abs(dy)))
//						$.inh($.the(Math.abs(dy)), dirY)
//				);
//				System.out.println(dx + " " + dy + " " + squareTerm);

				return p(ax, ay);
			});
		}
	}

	public static Compound p(int x, int y) {
		@NotNull Compound c = $.p(the(x), the(y));
		//System.out.println(i + " (" + ax + "," + ay + ") " + c);
		//return c;
		return inh(c, the("w"));
		//return inst(c, the("w"));
	}

	@Override
	public Twin<Integer> start() {
		return Tuples.twin(pixels, 3);
	}

	float lastScore = 0;

	@Override
	public float pre(int t, float[] ins) {

		for (int i = 0; i < ticksPerFrame; i++)
			pong.actionPerformed(null);

		BufferedImage big =
				ScreenImage.createImage(pong);

		BufferedImage small = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
		Graphics2D tGraphics2D = small.createGraphics(); //create a graphics object to paint to

		tGraphics2D.fillRect( 0, 0, width, height );
		tGraphics2D.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
		tGraphics2D.drawImage( big, 0, 0, width, height, null ); //draw the image scaled


		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int p = small.getRGB(x, y);
				int r = (p & 0x00ff0000) >> 16;
				String c = r > 0 ? "XX" : "..";
				//System.out.print(c);
				ins[i++] = r/255f;
			}
			//System.out.println();
		}


		float score = points;
		float reward = score-lastScore - bias;
		lastScore = score;
		return reward;
	}

	@Override
	public void post(int t, int action, float[] ins, Agent a) {
		switch (action) {
			case 0:
				move(-PongModel.SPEED, pong);
				break;
			case 1: /* nothing */
				break;
			case 2:
				move(+PongModel.SPEED, pong);
				break;

		}

		long now = nar.nar.time();
		priMatrix.set(width,height,(x,y)->{

			@Nullable Concept c = nar.nar.concept(p(x, y));
			float p = nar.nar.conceptPriority(p(x, y));
			float e = c.beliefs().truth(now).expectation();
			return ColorArray.rgba(e, p, 0, 1f);

		});

		System.out.println( a.summary());

	}



	@Override
	public void computePosition(PongModel pong) {

	}
}
