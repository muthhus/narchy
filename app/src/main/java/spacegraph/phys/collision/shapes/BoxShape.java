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

package spacegraph.phys.collision.shapes;

import spacegraph.phys.collision.broadphase.BroadphaseNativeType;
import spacegraph.phys.collision.dispatch.Collidable;
import spacegraph.phys.dynamics.RigidBody;
import spacegraph.phys.linearmath.AabbUtil2;
import spacegraph.phys.linearmath.ScalarUtil;
import spacegraph.phys.linearmath.Transform;
import spacegraph.phys.linearmath.VectorUtil;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import static javax.vecmath.Vector3f.v;

/**
 * BoxShape is a box primitive around the origin, its sides axis aligned with length
 * specified by half extents, in local shape coordinates. When used as part of a
 * {@link Collidable} or {@link RigidBody} it will be an oriented box in world space.
 *
 * @author jezek2
 */
public class BoxShape extends PolyhedralConvexShape {

	public BoxShape(Vector3f boxHalfExtents) {
		this(boxHalfExtents.x*2f, boxHalfExtents.y*2f, boxHalfExtents.z*2f);
	}

	public BoxShape(float w, float h, float d) {
		super();

		//VectorUtil.mul(implicitShapeDimensions, boxHalfExtents, localScaling);
		implicitShapeDimensions.set(w/2f, h/2f, d/2f); //localscaling is by default 1,1,1 anyway

		float m = getMargin();

		implicitShapeDimensions.add(-m, -m, -m);
	}

	public void size(float x, float y, float z) {
		implicitShapeDimensions.set(x/2f, y/2f, z/2f);
	}

	public Vector3f getHalfExtentsWithMargin(Vector3f out) {
		Vector3f halfExtents = getHalfExtentsWithoutMargin(out);

		float m = getMargin();
		if (m!=0) {
			halfExtents.add(m, m, m);
		}

		return halfExtents;
	}

	public final Vector3f getHalfExtentsWithoutMargin(Vector3f out) {
		out.set(implicitShapeDimensions); // changed in Bullet 2.63: assume the scaling and margin are included
		return out;
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.BOX_SHAPE_PROXYTYPE;
	}

	@Override
	public Vector3f localGetSupportingVertex(Vector3f vec, Vector3f out) {
		Vector3f halfExtents = getHalfExtentsWithoutMargin(out);
		
		float margin = getMargin();
		halfExtents.x += margin;
		halfExtents.y += margin;
		halfExtents.z += margin;

		out.set(
				ScalarUtil.fsel(vec.x, halfExtents.x, -halfExtents.x),
				ScalarUtil.fsel(vec.y, halfExtents.y, -halfExtents.y),
				ScalarUtil.fsel(vec.z, halfExtents.z, -halfExtents.z));
		return out;
	}

	@Override
	public Vector3f localGetSupportingVertexWithoutMargin(Vector3f vec, Vector3f out) {
		Vector3f halfExtents = getHalfExtentsWithoutMargin(out);

		out.set(
				ScalarUtil.fsel(vec.x, halfExtents.x, -halfExtents.x),
				ScalarUtil.fsel(vec.y, halfExtents.y, -halfExtents.y),
				ScalarUtil.fsel(vec.z, halfExtents.z, -halfExtents.z));
		return out;
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(Vector3f[] vectors, Vector3f[] supportVerticesOut, int numVectors) {
		Vector3f halfExtents = getHalfExtentsWithoutMargin(new Vector3f());

		for (int i = 0; i < numVectors; i++) {
			Vector3f vec = vectors[i];
			supportVerticesOut[i].set(ScalarUtil.fsel(vec.x, halfExtents.x, -halfExtents.x),
					ScalarUtil.fsel(vec.y, halfExtents.y, -halfExtents.y),
					ScalarUtil.fsel(vec.z, halfExtents.z, -halfExtents.z));
		}
	}

	@Override
	public void setMargin(float margin) {
		// correct the implicitShapeDimensions for the margin
		float m = getMargin();
		Vector3f oldMargin = new Vector3f(m, m,m);

		Vector3f implicitShapeDimensionsWithMargin = new Vector3f();
		implicitShapeDimensionsWithMargin.add(implicitShapeDimensions, oldMargin);

		super.setMargin(margin);

		float n = getMargin();
		Vector3f newMargin = new Vector3f(n, n, n);
		implicitShapeDimensions.sub(implicitShapeDimensionsWithMargin, newMargin);
	}

	@Override
	public void setLocalScaling(Vector3f scaling) {

		float m = getMargin();
		Vector3f oldMargin = new Vector3f(m, m, m);

		Vector3f implicitShapeDimensionsWithMargin = new Vector3f();
		implicitShapeDimensionsWithMargin.add(implicitShapeDimensions, oldMargin);
		Vector3f unScaledImplicitShapeDimensionsWithMargin = new Vector3f();
		VectorUtil.div(unScaledImplicitShapeDimensionsWithMargin, implicitShapeDimensionsWithMargin, localScaling);

		super.setLocalScaling(scaling);

		VectorUtil.mul(implicitShapeDimensions, unScaledImplicitShapeDimensionsWithMargin, localScaling);
		implicitShapeDimensions.sub(oldMargin);
	}

	@Override
	public void getAabb(Transform t, Vector3f aabbMin, Vector3f aabbMax) {
		AabbUtil2.transformAabb(getHalfExtentsWithoutMargin(), getMargin(), t, aabbMin, aabbMax);
	}

	public final Vector3f getHalfExtentsWithoutMargin() {
		return getHalfExtentsWithoutMargin(new Vector3f());
	}

	@Override
	public void calculateLocalInertia(float mass, Vector3f inertia) {

		Vector3f halfExtents = getHalfExtentsWithMargin(new Vector3f());

		float lx = 2f * halfExtents.x;
		float ly = 2f * halfExtents.y;
		float lz = 2f * halfExtents.z;

		inertia.set(
				mass / 12f * (ly * ly + lz * lz),
				mass / 12f * (lx * lx + lz * lz),
				mass / 12f * (lx * lx + ly * ly));
	}

	@Override
	public void getPlane(Vector3f planeNormal, Vector3f planeSupport, int i) {
		// this plane might not be aligned...
		Vector4f plane = new Vector4f();
		Vector3f tmp = new Vector3f();
		getPlaneEquation(plane, i, tmp);
		planeNormal.set(plane.x, plane.y, plane.z);

		tmp.negate(planeNormal);
		localGetSupportingVertex(tmp, planeSupport);
	}

	@Override
	public int getNumPlanes() {
		return 6;
	}

	@Override
	public int getNumVertices() {
		return 8;
	}

	@Override
	public int getNumEdges() {
		return 12;
	}

	@Override
	public void getVertex(int i, Vector3f vtx) {
		Vector3f halfExtents = getHalfExtentsWithoutMargin(new Vector3f());

		vtx.set(halfExtents.x * (1 - (i & 1)) - halfExtents.x * (i & 1),
				halfExtents.y * (1 - ((i & 2) >> 1)) - halfExtents.y * ((i & 2) >> 1),
				halfExtents.z * (1 - ((i & 4) >> 2)) - halfExtents.z * ((i & 4) >> 2));
	}
	
	public void getPlaneEquation(Vector4f plane, int i, Vector3f tmp) {
		Vector3f halfExtents = getHalfExtentsWithoutMargin(tmp);

		switch (i) {
			case 0:
				plane.set(1f, 0f, 0f, -halfExtents.x);
				break;
			case 1:
				plane.set(-1f, 0f, 0f, -halfExtents.x);
				break;
			case 2:
				plane.set(0f, 1f, 0f, -halfExtents.y);
				break;
			case 3:
				plane.set(0f, -1f, 0f, -halfExtents.y);
				break;
			case 4:
				plane.set(0f, 0f, 1f, -halfExtents.z);
				break;
			case 5:
				plane.set(0f, 0f, -1f, -halfExtents.z);
				break;
			default:
				assert (false);
		}
	}

	@Override
	public void getEdge(int i, Vector3f pa, Vector3f pb) {
		int edgeVert0 = 0;
		int edgeVert1 = 0;

		switch (i) {
			case 0:
				edgeVert0 = 0;
				edgeVert1 = 1;
				break;
			case 1:
				edgeVert0 = 0;
				edgeVert1 = 2;
				break;
			case 2:
				edgeVert0 = 1;
				edgeVert1 = 3;

				break;
			case 3:
				edgeVert0 = 2;
				edgeVert1 = 3;
				break;
			case 4:
				edgeVert0 = 0;
				edgeVert1 = 4;
				break;
			case 5:
				edgeVert0 = 1;
				edgeVert1 = 5;

				break;
			case 6:
				edgeVert0 = 2;
				edgeVert1 = 6;
				break;
			case 7:
				edgeVert0 = 3;
				edgeVert1 = 7;
				break;
			case 8:
				edgeVert0 = 4;
				edgeVert1 = 5;
				break;
			case 9:
				edgeVert0 = 4;
				edgeVert1 = 6;
				break;
			case 10:
				edgeVert0 = 5;
				edgeVert1 = 7;
				break;
			case 11:
				edgeVert0 = 6;
				edgeVert1 = 7;
				break;
			default:
				assert (false);
		}

		getVertex(edgeVert0, pa);
		getVertex(edgeVert1, pb);
	}

	@Override
	public final boolean isInside(Vector3f pt, float tolerance) {
		Vector3f halfExtents = getHalfExtentsWithoutMargin();

		float px = pt.x;
		float hx = halfExtents.x;
		if (px <= (hx + tolerance)) {
			if (px >= (-hx - tolerance)) {
				float py = pt.y;
				float hy = halfExtents.y;
				if (py <= (hy + tolerance)) {
					if (py >= (-hy - tolerance)) {
						float pz = pt.z;
						float hz = halfExtents.z;
						if (pz <= (hz + tolerance)) {
							if (pz >= (-hz - tolerance)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return "Box";
	}

	@Override
	public int getNumPreferredPenetrationDirections() {
		return 6;
	}

	@Override
	public void getPreferredPenetrationDirection(int index, Vector3f penetrationVector) {
		switch (index) {
			case 0:
				penetrationVector.set(1f, 0f, 0f);
				break;
			case 1:
				penetrationVector.set(-1f, 0f, 0f);
				break;
			case 2:
				penetrationVector.set(0f, 1f, 0f);
				break;
			case 3:
				penetrationVector.set(0f, -1f, 0f);
				break;
			case 4:
				penetrationVector.set(0f, 0f, 1f);
				break;
			case 5:
				penetrationVector.set(0f, 0f, -1f);
				break;
			default:
				assert (false);
		}
	}

	public final float x() {
		return implicitShapeDimensions.x;
	}

	public final float y() {
		return implicitShapeDimensions.y;
	}

	public final float z() {
		return implicitShapeDimensions.z;
	}
}
