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
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.concept.BooleanConcept;
import nars.concept.Concept;
import nars.guifx.chart.MatrixImage;
import nars.guifx.util.ColorArray;
import nars.index.Indexes;
import nars.learn.Agent;
import nars.nal.Tense;
import nars.nar.Default;
import nars.nar.Multi;
import nars.nar.util.Answerer;
import nars.nar.util.OperationAnswerer;
import nars.op.mental.Abbreviation2;
import nars.op.time.MySTMClustered;
import nars.term.Compound;
import nars.term.Term;
import nars.time.FrameClock;
import nars.truth.Truth;
import nars.util.FX;
import nars.util.NAgent;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.experiment.Environment;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import static nars.$.*;

public class PongEnvironment extends Player implements Environment {

	int actions = 3;

	final int width = 20;
	final int height = 20;
	final int pixels = width * height;
	final int scaleY = 20;
	final int scaleX = 20;
	final int ticksPerFrame = 1; //framerate divisor
	private final PongModel pong;
	private final MatrixImage priMatrix;

	float bias = 0f; //pain of boredom
	private NAgent nagent;

	public static void main (String[] args) {
		//Global.TRUTH_EPSILON = 0.2f;
		PongEnvironment e = new PongEnvironment();

		XorShift128PlusRandom rng = new XorShift128PlusRandom(1);
		Multi nar = new Multi(4,
		//Default nar = new Default(
				512, 12, 1, 3, rng,
				//new CaffeineIndex(Terms.terms, new DefaultConceptBuilder(rng))
				//new InfinispanIndex(Terms.terms, new DefaultConceptBuilder(rng))
				new Indexes.WeakTermIndex(128 * 1024, rng)
				//new Indexes.SoftTermIndex(128 * 1024, rng)
				//new Indexes.DefaultTermIndex(128 *1024, rng)
				,new FrameClock());
		nar.conceptActivation.setValue(0.3f);
		nar.beliefConfidence(0.99f);
		nar.goalConfidence(0.99f); //must be slightly higher than epsilon's eternal otherwise it overrides
		nar.DEFAULT_BELIEF_PRIORITY = 0.2f;
		nar.DEFAULT_GOAL_PRIORITY = 0.7f;
		nar.DEFAULT_QUESTION_PRIORITY = 0.6f;
		nar.DEFAULT_QUEST_PRIORITY = 0.6f;
		nar.cyclesPerFrame.set(256);


		NAgent a = new NAgent(nar);
		//a.epsilon = 0.6f;
		a.epsilonRandomMin = 0.05f;

		new Abbreviation2(nar, "_");
		new MySTMClustered(nar, 16, '.');
		new HappySad(nar, 8);

		//DQN a = new DQN();
		//HaiQAgent a = new HaiQAgent();

		e.run(a, 1024*8);

		NAR.printTasks(nar, true);
		NAR.printTasks(nar, false);
		a.printActions();
		nar.forEachConcept(c -> {
			System.out.println(c);
		});

		//nar.forEachConcept(System.out::println);
	}


	public PongEnvironment() {
		super();

		JFrame j = new JFrame();
		j.setTitle ("Pong");
		j.setSize (width * scaleX, height * scaleY);
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

			FX.newWindow("Visual", new BorderPane(priMatrixPane), 400, 400);

			priMatrix.fit(priMatrixPane);
		});


	}





	@Override
	public void preStart(Agent a) {
		if (a instanceof NAgent) {
			//provide custom sensor input names for the nars agent

			nagent = (NAgent) a;
			NAR nar = nagent.nar;

//			for (int i = 1; i < Math.max(width,height); i++) {
//				nar.believe("(" + (i-1) + " <-> " + i + ")", 0.85f, 1f);
//			}

//			ArrayList<Concept> convolution = new ArrayList();
//
//			//convolution concepts
//			for (int x = 0; x+1 < width; x+=2) {
//				for (int y = 0; y+1 < height; y+=2) {
//					@NotNull BooleanConcept b = BooleanConcept.Or(nar,
//						p(x, y), p(x + 1, y), p(x, y + 1), p(x + 1, y + 1)
//					);
//					convolution.add(b);
//					nar.ask(b);
//				}
//			}
//			nar.onFrame(f -> { //HACK some other way than forcing an update each frame
//				long t = nagent.nar.time();
//				for (Concept concept : convolution) {
//					Truth b = concept.beliefs().truth(t);
//					Truth g = concept.goals().truth(t);
//					//System.out.println(concept + " " + t);
//				}
//			});

			nagent.setSensorNamer((i) -> {
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
			//nar.ask($("( ((#1,#2)-->w) && (R) )"), Symbols.QUESTION);
			//nar.ask($("( ((#1,#2)-->w) ==> (R) )"), Symbols.QUESTION);
			//nar.ask($("( ((#1,#2)-->w) && (R) )"), Symbols.QUEST);
//			nar.ask($("( ((0,?y)-->w) && (R) )"), Symbols.QUESTION);
//			nar.ask($("( ((0,?y)-->w) && (R) )"), Symbols.QUEST);


//			new OperationAnswerer($.$("dist((%1,%2),(%3,%4),#d)"), nar) {
//
//				@Override
//				protected void onMatch(Term[] args) {
//					System.out.println(Arrays.toString(args));
//				}
//			};
//
//			//nar.log();
//			nar.input("((&&, ((#1,#2)-->w), ((#3,#4)-->w)) &&+0 dist((#1,#2),(#3,#4),#x)).");

		}
	}

	public static Compound p(int x, int y) {
		@NotNull Compound c = $.p(the(x), the(y));
		//System.out.println(i + " (" + ax + "," + ay + ") " + c);
		return c;
		//return inh(c, the("w"));
		//return inst(c, the("w"));
	}

	@Override
	public Twin<Integer> start() {
		return Tuples.twin(pixels, actions);
	}

	float lastScore;

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
				//String c = r > 0 ? "XX" : "..";
				//System.out.print(c);
				ins[i++] = r/255f;
			}
			//System.out.println();
		}


		float score = points;
		float reward = score-lastScore + bias;
		lastScore = score;
		return reward;
	}

	@Override
	public void post(int t, int action, float[] ins, Agent a) {
		//actRelative(action); //numActions = 3
		actRelativeVelocity(action); //numActions = 3
		//actAbsolute(action);

		long now = nagent.nar.time();
		int dt = 16;
		priMatrix.set(width,height*2,(x,y)->{

			if (y < height) {
				@Nullable Concept c = nagent.nar.concept(p(x, y));
				//float pA = 0.5f + 0.5f * p;
				float b = c.beliefs().truth(now).expectation();
				@NotNull Truth gg = c.goals().truth(now);
				float gP = (gg.expectationPositive()-0.5f) * 2f;
				float gN = (gg.expectationNegative()-0.5f) * 2f;
				return ColorArray.rgba(b, gP, gN, 1f);
			}else {
				//FUTURE
				y-=height;
				float p = nagent.nar.conceptPriority(p(x, y));
				//@Nullable Concept c = nagent.nar.concept(p(x, y));
				//float b0 = c.beliefs().truth(now).expectation();
				//float b = c.beliefs().truth(now+dt).expectation();
				//float g = c.goals().truth(now+dt).expectation();
				//return ColorArray.rgba(b-b0 > 0 ? 1f : 0f, b0-b > 0 ? 1f: 0f, 0, 1f);
				return ColorArray.rgba(p, p, p, 1f);
			}

		});

		System.out.println( a.summary());

	}

	private void actAbsolute(int action) {

		moveTo( (int)(((float)action) / (actions-1) * height * scaleY), pong );
	}

	public void actRelative(int action) {
		switch (action) {
			case 0:
				move(-PongModel.SPEED*ticksPerFrame, pong);
				break;
			case 1: /* nothing */
				break;
			case 2:
				move(+PongModel.SPEED*ticksPerFrame, pong);
				break;

		}
	}

	int vel = 0;

	public void actRelativeVelocity(int action) {
		switch (action) {
			case 0:
				vel = -PongModel.SPEED;
				break;
			case 1:
				vel = 0;
				break;
			case 2:
				vel = +PongModel.SPEED;
				break;

		}
		move(vel, pong);
	}


	@Override
	public void computePosition(PongModel pong) {

	}

	private static class HappySad implements Consumer<NAR> {

		private final DescriptiveStatistics happy;

		public HappySad(NAR nar, int windowSize) {
			happy = new DescriptiveStatistics();
			happy.setWindowSize(windowSize);

			nar.onFrame(this);

		}

		@Override
		public void accept(NAR nar) {
			float h = nar.emotion.happy();

			float dMean = (float)(h - happy.getMean());
			double varianceThresh = happy.getVariance();

			Compound e;
			if (dMean > varianceThresh) {
				e = happy(nar);
			} else if (dMean < -varianceThresh) {
				e = sad(nar);
			} else {
				e = null;
			}

			if (e!=null) {
				nar.believe(e, Tense.Present);
				logger.info("{}", e);
			}

			happy.addValue(h);
		}

		private Compound happy(NAR nar) {
			return $.prop(nar.self, $.the("happy"));
		}
		private Compound sad(NAR nar) {
			return $.prop(nar.self, $.the("sad"));

		}
	}
}
