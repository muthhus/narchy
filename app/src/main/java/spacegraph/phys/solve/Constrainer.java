/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package spacegraph.phys.solve;

import spacegraph.phys.Collidable;
import spacegraph.phys.collision.broad.Intersecter;
import spacegraph.phys.collision.narrow.PersistentManifold;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.util.OArrayList;

/**
 * Abstract class for constraint solvers.
 * 
 * @author jezek2
 */
public abstract class Constrainer {
	
	//protected final BulletStack stack = BulletStack.get();

	public void prepareSolve (int numBodies, int numManifolds) {}

	/**
	 * Solve a group of constraints.
	 */
	public abstract float solveGroup(OArrayList<Collidable> bodies, int numBodies, OArrayList<PersistentManifold> manifold, int manifold_offset, int numManifolds, OArrayList<TypedConstraint> constraints, int constraints_offset, int numConstraints, ContactSolverInfo info/*, btStackAlloc* stackAlloc*/, Intersecter intersecter);

	public void allSolved(ContactSolverInfo info /*, btStackAlloc* stackAlloc*/) {}

	/**
	 * Clear internal cached data and reset random seed.
	 */
	public abstract void reset();
	
}
