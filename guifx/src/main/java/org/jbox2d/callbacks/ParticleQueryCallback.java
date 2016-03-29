package org.jbox2d.callbacks;

import org.jbox2d.dynamics.World2D;

/**
 * Callback class for AABB queries. See
 * {@link World2D#queryAABB(QueryCallback, org.jbox2d.collision.AABB)}.
 * 
 * @author dmurph
 * 
 */
public interface ParticleQueryCallback {
  /**
   * Called for each particle found in the query AABB.
   * 
   * @return false to terminate the query.
   */
  boolean reportParticle(int index);
}
