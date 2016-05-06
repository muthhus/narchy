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
import nars.util.Agent;
import nars.util.DQN;
import nars.util.NAgent;
import nars.util.experiment.Environment;

import static nars.util.NAgent.printTasks;

/**
 * the java application class of pacman 
 */
public class PacmanEnvironment extends cpcman implements Environment {

	final int visionRadius = 2;
	final int itemTypes = 3;

	final int inputs = (int)Math.pow(visionRadius * 2 +1, 2) * itemTypes;


	public static void main (String[] args) 	{
		NAgent a = new NAgent(new Default()
				//.logSummaryGT(System.out, 0.01f)
				);
		a.nar.cyclesPerFrame.set(200);

		new PacmanEnvironment().run(
				//new DQN(),
				a,

				150);

		printTasks(a.nar, true);
		printTasks(a.nar, false);
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

	protected void initInput() {
		//managed by this class
	}

	float lastScore = 0;

	@Override
	public float cycle(int t, int action, float[] ins, Agent a) {

		int p = 0;
		if (maze!=null && pac!=null) {
			int[][] m = maze.iMaze;
			for (int i = -visionRadius; i <= +visionRadius; i++) {
				for (int j = -visionRadius; j <= +visionRadius; j++) {
					int px = pac.iX / 16 + i;
					int py = pac.iY / 16 + j;

					if (px >= 0 && py >= 0 && px < m.length && py < m[0].length) {
						int v = m[px][py];
						ins[p++] = (v == cmaze.WALL) ? 1f : 0f;

						float dotness = 0;
						switch (v) {
							case cmaze.DOT:
								dotness = 0.75f;
							case cmaze.POWER_DOT:
								dotness = 1f;
						}
						ins[p++] = dotness;


						boolean ghost = false;
						for (cghost g : ghosts) {
							int ix = g.iX / 16;
							int iy = g.iY / 16;
							if (ix == px && iy == py) {
								ghost = true;
								break;
							}
						}

						//if (ghost)
						//System.out.println("ghost at " + i + ", " + j);

						ins[p++] = ghost ? 1f : 0f; //TODO attenuate distance
					}
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

		cycle();

		/*static final int BLANK=0;
		static final int WALL=1;
		static final int DOOR=2;
		static final int DOT=4;
		static final int POWER_DOT=8;*/


		float bias = -0.15f; //pain of boredom

		//delta score from pacman game
		float ds = score - lastScore;
		this.lastScore = score;

		ds/=10f;
		if (ds > 1f) ds = 1f;
		if (ds < -1f) ds = -1f;

		ds += bias;

		ds += interScore;
		interScore = 0;

		System.out.println(ds);


		return ds;
	}

	float interScore = 0;
	@Override
	public void killedByGhost() {
		super.killedByGhost();
		interScore -= 1f;
	}

	@Override
	public void killGhost() {
		super.killGhost();
		interScore += 1f;
	}
}


