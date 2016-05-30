/**
 * Copyright (C) 1997-2010 Junyang Gu <mikejyg@gmail.com>
 * 
 * This file is part of javaiPacman.
 *
 * javaiPacman is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * javaiPacman is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with javaiPacman.  If not, see <http://www.gnu.org/licenses/>.
 */

package mikejyg.javaipacman.pacman;

import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.$;
import nars.NAR;
import nars.index.Indexes;
import nars.learn.Agent;
import nars.nar.Default;
import nars.op.mental.Abbreviation2;
import nars.op.time.MySTMClustered;
import nars.task.DerivedTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.time.FrameClock;
import nars.util.NAgent;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.experiment.Environment;

import java.util.Random;

/**
 * the java application class of pacman 
 */
public class PacmanEnvironment extends cpcman implements Environment {

	final int visionRadius = 1;
	final int itemTypes = 3;

	final int inputs = (int)Math.pow(visionRadius * 2 +1, 2) * itemTypes;
	private final int pacmanCyclesPerFrame = 8;
	float bias = -0.15f; //pain of boredom

	public PacmanEnvironment(int ghosts) {
		super(ghosts);
	}


	public static void main (String[] args) 	{
		Random rng = new XorShift128PlusRandom(1);

		Default nar = new Default(
				1024, 8, 1, 2, rng,
				//new CaffeineIndex(Terms.terms, new DefaultConceptBuilder(rng))
				//new InfinispanIndex(Terms.terms, new DefaultConceptBuilder(rng))
				//new Indexes.WeakTermIndex(128 * 1024, rng)
				//new Indexes.SoftTermIndex(128 * 1024, rng)
				new Indexes.DefaultTermIndex(128 *1024, rng)
				,new FrameClock());
		//nar.premiser.confMin.setValue(0.03f);
		//nar.conceptActivation.setValue(0.01f);

		nar.beliefConfidence(0.65f);
		nar.goalConfidence(0.65f); //must be slightly higher than epsilon's eternal otherwise it overrides
		nar.DEFAULT_BELIEF_PRIORITY = 0.1f;
		nar.DEFAULT_GOAL_PRIORITY = 0.5f;
		nar.DEFAULT_QUESTION_PRIORITY = 0.4f;
		nar.DEFAULT_QUEST_PRIORITY = 0.4f;
		nar.cyclesPerFrame.set(512);
//		nar.conceptRemembering.setValue(1f);
//		nar.termLinkRemembering.setValue(3f);
//		nar.taskLinkRemembering.setValue(1f);

		//nar.inputAt(100,"$1.0;0.8;1.0$ ( ( ((#x,?r)-->#a) && ((#x,?s)-->#b) ) ==> col:(#x,#a,#b) ). %1.0;1.0%");
		//nar.inputAt(100,"$1.0;0.8;1.0$ col:(?c,?x,?y)?");

		//nar.inputAt(20,"$1.0;0.8;1.0$ rightOf:((0,#x),(1,#x)). %1.0;1.0%");
		//nar.inputAt(20,"$1.0;0.8;1.0$ rightOf:((1,#x),(2,#x)). %1.0;1.0%");
		//nar.inputAt(20,"$1.0;0.8;1.0$ ( ( (($x,$y)-->$a) && (($x,$y)-->$b) ) ==> samePlace:(($x,$y),$a,$b) ). %1.0;1.0%");
		//nar.inputAt(100,"$1.0;0.8;1.0$ samePlace:(#x,#y,#a,#b)?");

		//nar.log();
		//nar.logSummaryGT(System.out, 0.1f);
		nar.log(System.err, v -> {
			if (v instanceof Task) {
				Task t = (Task)v;
				if (t instanceof DerivedTask && t.punc() == '!')
					return true;
			}
			return false;
		});

		new Abbreviation2(nar, "_");
		new MySTMClustered(nar, 16, '.');
		//new MySTMClustered(nar, 8, '!');

		NAgent n = new NAgent(nar);

		new PacmanEnvironment(1 /* ghosts  */).run(
				//new DQN(),
				//new DPG(),
				//new HaiQAgent(),
				n,
				555450);

		//nar.index.print(System.out);
		NAR.printTasks(nar, true);
		NAR.printTasks(nar, false);
		n.printActions();
		nar.core.active.print();
//		nar.index.forEach(t -> {
//			if (t instanceof Concept) {
//				Concept c = (Concept)t;
//				if (c.hasQuestions()) {
//					System.out.println(c.questions().iterator().next());
//				}
//			}
//		});
	}


	@Override
	public Twin<Integer> start() {

		init();

		return Tuples.twin(inputs, 4);
	}

	@Override
	public void run() {
		//managed by this class
	}

	@Override
	protected void initInput() {
		//managed by this class
	}

	float lastScore;


	final Atom PILL = $.the("pill");
	final Atom WALL = $.the("wall");
	final Atom GHOST = $.the("ghost");


	@Override
	public void preStart(Agent a) {
		if (a instanceof NAgent) {
			//provide custom sensor input names for the nars agent

			int visionDiameter = 2 * visionRadius + 1;

			NAgent nar = (NAgent) a;


			nar.setSensorNamer((i) -> {
				int cell = i / 3;
				int type = i % 3;
				Atom typeTerm;
				switch (type) {
					case 0: typeTerm = WALL; break;
					case 1: typeTerm = PILL; break;
					case 2: typeTerm = GHOST; break;
					default:
						throw new RuntimeException();
				}

				int dx = cell % visionDiameter;
				int dy = cell / visionDiameter;
				Term squareTerm = $.p($.the(dx), $.the(dy));
				//return $.p(squareTerm, typeTerm);
				return $.prop(squareTerm, typeTerm);
				//return (Compound)$.inh($.the(square), typeTerm);
			});
		}
	}


	@Override
	public float pre(int t, float[] ins) {

		int p = 0;

		if (maze!=null && pac!=null) {
			int[][] m = maze.iMaze;
//			int pix = pac.iX / 16;
//			int piy = pac.iY / 16;
			int pix = Math.round(pac.iX / 16f);
			int piy = Math.round(pac.iY / 16f);
			for (int i = -visionRadius; i <= +visionRadius; i++) {
				for (int j = -visionRadius; j <= +visionRadius; j++) {
					int px = pix + i;
					int py = piy + j;

					float dotness = 0;
					boolean ghost = false;
					int v = cmaze.WALL;

					if (px >= 0 && py >= 0 && py < m.length && px < m[0].length) {
						v = m[py][px];

						switch (v) {
							case cmaze.DOT:
								dotness = 0.85f;
								break;
							case cmaze.POWER_DOT:
								dotness = 1f;
								break;
						}


						for (cghost g : ghosts) {
//							int ix = g.iX / 16;
//							int iy = g.iY / 16;
							int ix = Math.round(g.iX / 16f);
							int iy = Math.round(g.iY / 16f);
							if (ix == px && iy == py) {
								ghost = true;
								break;
							}
						}

						/*if (ghost)
							System.out.println("ghost at " + i + ", " + j);*/

					}

					ins[p++] = (v == cmaze.WALL) ? 1f : 0f;
					ins[p++] = dotness;
					ins[p++] = ghost ? 1f : 0f; //TODO attenuate distance
				}


			}
			//System.out.println(Arrays.toString(ins));

//			//noise
//			if (Math.random() < 0.1) {
//				float noiselevel = 0.1f;
//				for (int i = 0; i < ins.length; i++) {
//					ins[i] = Util.clamp(ins[i] + (float) ((Math.random() - 0.5) * 2) * noiselevel);
//				}
//			}
		}



		//delta score from pacman game
		float ds = score - lastScore;
		this.lastScore = score;

		ds/=10f;

		ds += bias;

		ds += interScore;
		interScore *= 0.97f;
		//interScore = 0;

		if (ds > 1f) ds = 1f;
		if (ds < -1f) ds = -1f;


		return ds;

	}

	@Override
	public void post(int t, int action, float[] ins, Agent a) {


		switch (action) {
			case 0: pacKeyDir = ctables.RIGHT; break;
			case 1: pacKeyDir = ctables.UP; break;
			case 2: pacKeyDir = ctables.LEFT; break;
			case 3: pacKeyDir = ctables.DOWN; break;
		}


		if (interScore < bias*2f) {
			//too much pain
			pacKeyDir = -1;
		}

		cycle(pacmanCyclesPerFrame);


		System.out.println( a.summary());

		/*static final int BLANK=0;
		static final int WALL=1;
		static final int DOOR=2;
		static final int DOT=4;
		static final int POWER_DOT=8;*/

	}

	float interScore;
	@Override
	public void killedByGhost() {
		super.killedByGhost();
		interScore -= 1f;
	}

	@Override
	public void killGhost() {
		super.killGhost();
		//interScore += 1f; //DISABLED FOR NOW TO NOT CONFUSE IT
	}

}


