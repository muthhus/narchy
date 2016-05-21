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

package mikejyg.javaipacman.wrapper;

import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import mikejyg.javaipacman.pacman.cghost;
import mikejyg.javaipacman.pacman.cmaze;
import mikejyg.javaipacman.pacman.cpcman;
import mikejyg.javaipacman.pacman.ctables;
import nars.nar.Default;
import nars.op.time.STMClustered;
import nars.time.FrameClock;
import nars.util.Agent;
import nars.util.NAgent;
import nars.util.data.MutableInteger;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.experiment.Environment;

import java.util.Random;

import static nars.util.NAgent.printTasks;

/**
 * the java application class of pacman 
 */
public class PacmanEnvironment extends cpcman implements Environment {

	final int visionRadius = 2;
	final int itemTypes = 3;

	final int inputs = (int)Math.pow(visionRadius * 2 +1, 2) * itemTypes;
	private final int pacmanCyclesPerFrame = 4;

	public PacmanEnvironment(int ghosts) {
		super(ghosts);
	}


	public static void main (String[] args) 	{
		Random rng = new XorShift128PlusRandom(1);

		Default nar = new Default(
				1024, 4, 2, 2, rng,
				new Default.WeakTermIndex(128 * 1024, rng),
				//new Default.SoftTermIndex(128 * 1024, rng),
				//new Default.DefaultTermIndex(128 *1024, rng),
				new FrameClock());
		nar.beliefConfidence(0.51f);
		nar.conceptActivation.setValue(0.2f);
		nar.cyclesPerFrame.set(32);
//		nar.conceptRemembering.setValue(1f);
//		nar.termLinkRemembering.setValue(3f);
//		nar.taskLinkRemembering.setValue(1f);
		//.logSummaryGT(System.out, 0.01f)

		new STMClustered(nar, new MutableInteger(32)) {

		};

		new PacmanEnvironment(1 /* ghosts  */).run(
				//new DQN(),
				new NAgent(nar),
				100);

		printTasks(nar, true);
		printTasks(nar, false);
		nar.index.print(System.out);
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

	@Override
	public float cycle(int t, int action, float[] ins, Agent a) {

		int p = 0;
		if (maze!=null && pac!=null) {
			int[][] m = maze.iMaze;
			int pix = pac.iX / 16;
			int piy = pac.iY / 16;
//			int pix = Math.round(pac.iX / 16f);
//			int piy = Math.round(pac.iY / 16f);
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
							int ix = g.iX / 16;
							int iy = g.iY / 16;
							//int ix = Math.round(g.iX / 16f);
							//int iy = Math.round(g.iY / 16f);
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
		}

		switch (action) {
			case 0: pacKeyDir = ctables.RIGHT; break;
			case 1: pacKeyDir = ctables.UP; break;
			case 2: pacKeyDir = ctables.LEFT; break;
			case 3: pacKeyDir = ctables.DOWN; break;
		}

		float bias = -0.15f; //pain of boredom

		if (interScore < bias*2f) {
			//too much pain
			pacKeyDir = -1;
		}


		cycle(pacmanCyclesPerFrame);


		/*static final int BLANK=0;
		static final int WALL=1;
		static final int DOOR=2;
		static final int DOT=4;
		static final int POWER_DOT=8;*/



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

		System.out.println(ds + "\t" + a.summary());
		return ds;
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


