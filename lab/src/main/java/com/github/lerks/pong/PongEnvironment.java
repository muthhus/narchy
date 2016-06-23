package com.github.lerks.pong;/*
 *  Copyright (C) 2010  Luca Wehrstedt
 *
 *  This file is released under the GPLv2
 *  Read the file 'COPYING' for more information
 */

import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.$;
import nars.NAR;
import nars.gui.BeliefTableChart;
import nars.gui.graph.GraphSpace;
import nars.guifx.chart.MatrixImage;
import nars.index.Indexes;
import nars.learn.Agent;
import nars.nal.Tense;
import nars.nar.Default;
import nars.op.time.MySTMClustered;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.time.FrameClock;
import nars.agent.NAgent;
import nars.vision.SwingCamera;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.experiment.Environment;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import javax.swing.*;
import java.util.function.Consumer;

import static nars.$.*;

public class PongEnvironment extends Player implements Environment {

	int actions = 3;


	final int width = 16;
	final int height = 16;
	final int pixels = width * height;
	final int scaleX = (int)(24f*20/width);
	final int scaleY = (int)(24f*16/width);
	final int ticksPerFrame = 1; //framerate divisor
	private final PongModel pong;
	private final MatrixImage priMatrix;

	float bias; //pain of boredom
	private NAgent nagent;
	final SwingCamera swingCamera;



	public static void main (String[] args) {
		//Global.TRUTH_EPSILON = 0.2f;

		XorShift128PlusRandom rng = new XorShift128PlusRandom(1);
		//Multi nar = new Multi(3,
		Default nar = new Default(
				1024, 4, 2, 3, rng,
				//new CaffeineIndex(new DefaultConceptBuilder(rng))
				//new InfinispanIndex(Terms.terms, new DefaultConceptBuilder(rng))
				new Indexes.WeakTermIndex(256 * 1024, rng)
				//new Indexes.SoftTermIndex(128 * 1024, rng)
				//new Indexes.DefaultTermIndex(128 *1024, rng)
				,new FrameClock());
		//nar.conceptActivation.setValue(0.5f);
		nar.beliefConfidence(0.8f);
		nar.goalConfidence(0.8f); //must be slightly higher than epsilon's eternal otherwise it overrides
		nar.DEFAULT_BELIEF_PRIORITY = 0.2f;
		nar.DEFAULT_GOAL_PRIORITY = 0.8f;
		nar.DEFAULT_QUESTION_PRIORITY = 0.6f;
		nar.DEFAULT_QUEST_PRIORITY = 0.6f;
		nar.cyclesPerFrame.set(128);
		nar.confMin.setValue(0.01f);

		nar.conceptCold.termlinksCapacityMin.setValue(8);
		nar.conceptCold.termlinksCapacityMax.setValue(16);
		nar.conceptWarm.termlinksCapacityMin.setValue(16);
		nar.conceptWarm.termlinksCapacityMax.setValue(64);
		nar.conceptCold.taskLinksCapacity.setValue(16);
		nar.conceptWarm.taskLinksCapacity.setValue(64);
		
		NAgent a = new NAgent(nar) {
			@Override
			public void start(int inputs, int ac) {
				super.start(inputs, ac);
				new BeliefTableChart(nar, actions).show(400, 100);
			}
		};
		//a.epsilon = 0.6f;
		//a.gamma /= 4f;

		//new Abbreviation2(nar, "_");
		new MySTMClustered(nar, 16, '.');
		new HappySad(nar, 8);

		//DQN a = new DQN();
		//HaiQAgent a = new HaiQAgent();


		PongEnvironment e = new PongEnvironment();


		new GraphSpace(new GraphSpace.ConceptsSource(nar, 256)).show(800, 500);

		e.run(a, 256*8);

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
		this.position = (height*scaleY)/2;
		pong.acceleration = false;
		j.getContentPane ().add (pong);

		j.addMouseListener (pong);
		j.addKeyListener (pong);

		j.setVisible(true);
		j.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);


		this.swingCamera = new SwingCamera(pong,width,height);

		priMatrix = new MatrixImage();

//		FX.run(()->{
//			BorderPane priMatrixPane = new BorderPane(priMatrix);
//
//			FX.newWindow("Visual", new BorderPane(priMatrixPane), 400, 400);
//
//			priMatrix.fit(priMatrixPane);
//		});


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


	final Compound[][] pCache = new Compound[width][height];

	public final Compound p(int x, int y) {
		Compound e = pCache[x][y];
		if (e == null) {
			pCache[x][y] = e = _p(x, y);
		}
		return e;
	}

	public Compound _p(int x, int y) {
		//return $.p(the(x), the(y));

//		int d = log2(Math.max(width, height));
//		Compound n = $.inh($.p(binaryp(x, d), binaryp(y, d)), $.the("w"));
//		System.out.println(" (" + x + "," + y + ") " + n);
//		return n;

		//int d = log2(Math.max(width, height));
		Compound q = (Compound) quadp(0, x, y, width, height);
		//Compound n = $.inh(q, $.the("w"));
		Compound n = q;

		System.out.println(" (" + x + "," + y + ") " + n);
		return n;


		//return inh(c, the("w"));
		//return inst(c, the("w"));
	}

	private Term quadp(int level, int x, int y, int width, int height) {

		if (width <= 1 || height <= 1) {
			return null; //dir; //$.p(dir);
		}

		int cx = width/2;
		int cy = height/2;

		boolean left = x < cx;
		boolean up = y < cy;


		char c1 = (left ? 'l' : 'r');
		char c2 = (up ? 'u' : 'd');
		if (!left)
			x -= cx;
		if (!up)
			y -= cy;
		int area = width * height;

		//Term d = $.the(c1 + "" + c2);
		//Term d = $.secti($.the(c1), $.the(c2));
		//Term d = $.seti($.the(c1), $.the(c2));
//		Term d = level > 0  ?
//					$.seti($.the(c1), $.the(c2), $.the(area)) :
//					$.seti($.the(c1), $.the(c2) );
		Term d = level > 0  ?
				$.secte($.the(c1), $.the(c2), $.the(area)) :
				$.secte($.the(c1), $.the(c2) );

		//Term dir = $.p(d,$.the(area));
		//Term dir = level == 0 ? d : $.p(d,$.the(area));
		//Term dir = level == 0 ? d : $.p(d,$.the(area));
		//Term dir = level == 0 ? d : $.inh(d,$.the(area));
		//Term dir = $.inh(d,$.the(area));

		//Term dir = $.inh(d,$.the(area));
		//Term dir = level == 0 ? d : $.inh(d,$.the(area));

		Term q = quadp(level+1, x, y, width / 2, height / 2);
		if (q!=null) {
			//return $.p(dir, q);
			//return $.image(0, false, dir, $.sete(q));

			//return $.inh(q, dir);
			//return $.inst(q, (level== 0 ? d : $.seti(d, $.the(area))));
			return $.inst(q, d);
			//return $.diffe(dir, q);

			//return $.sete(q, dir);
			//return $.inst(q, dir);
			//return $.instprop(q, dir);
			//return $.p(q, dir);
			//return $.image(0, false, dir, q);
		}
		else {
			return d;
			//return $.p(dir);
			//return $.inst($.varDep(0), dir);
		}
	}

	private Compound quadpFlat(int x, int y, int width, int height) {
		int cx = width/2;
		int cy = height/2;

		boolean left = x < cx;
		boolean up = y < cy;


		char c1 = (left ? 'l' : 'r');
		char c2 = (up ? 'u' : 'd');
		if (!left)
			x -= cx;
		if (!up)
			y -= cy;

		Atom dir = $.the(c1 + "" + c2);

		if (width>1 || height > 1) {
			Compound q = quadpFlat(x, y, width / 2, height / 2);
			if (q!=null)
				return $.p(Terms.concat(new Term[] { dir }, q.terms()));
			else
				return $.p(dir);
		} else {
			return null; //dir; //$.p(dir);
		}
	}

	public static int log2(int width) {
		return (int)Math.ceil(Math.log(width)/Math.log(2));
	}

	public Term binaryp(int x, int depth) {
		String s = Integer.toBinaryString(x);
		int i = s.length()-1;
		Term n = null;
		for (int d = 0; d < depth; d++, i--) {
			Atom next = $.the(i < 0 || s.charAt(i) == '0' ? 0 : 1);

			//next = $.the( ((char)(d+'a')) + "" + next.toString());

			if (n == null) {
				//n = next;
				n = $.p(next);
				//n = $.p(next, $.varDep(0));
			} else {
				n = $.p(next, n);
				//n = $.inh(n, next);
			}
		}
		return n;
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

		swingCamera.update((x,y,p)->{
			int i = width * y + x;
			int r = (p & 0x00ff0000) >> 16;
			ins[i++] = r/255f;
		});




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

//		priMatrix.set(width,height*2,(x,y)->{
//
//			if (y < height) {
//				@Nullable Concept c = nagent.nar.concept(p(x, y));
//				//float pA = 0.5f + 0.5f * p;
//				float b = c.beliefs().truth(now).expectation();
//				@NotNull Truth gg = c.goals().truth(now);
//				float gP = (gg.expectationPositive()-0.5f) * 2f;
//				float gN = (gg.expectationNegative()-0.5f) * 2f;
//				//System.out.println(c.beliefs().size() + "/" + c.beliefs().capacity() + " " + gg + " " + gP + " " + gN);
//				return ColorArray.rgba(b, gP, gN, 1f);
//			}else {
//				//FUTURE
//				y-=height;
//				float p = nagent.nar.conceptPriority(p(x, y));
//				//@Nullable Concept c = nagent.nar.concept(p(x, y));
//				//float b0 = c.beliefs().truth(now).expectation();
//				//float b = c.beliefs().truth(now+dt).expectation();
//				//float g = c.goals().truth(now+dt).expectation();
//				//return ColorArray.rgba(b-b0 > 0 ? 1f : 0f, b0-b > 0 ? 1f: 0f, 0, 1f);
//				return ColorArray.rgba(p, p, p, 1f);
//			}
//
//		});

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

	int vel;

	public void actRelativeVelocity(int action) {
		switch (action) {
			case 0:
				vel = -PongModel.SPEED*2;
				break;
			case 1:
				vel = 0;
				break;
			case 2:
				vel = +PongModel.SPEED*2;
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
