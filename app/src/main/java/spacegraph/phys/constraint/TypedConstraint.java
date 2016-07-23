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

package spacegraph.phys.constraint;

import spacegraph.math.v3;
import spacegraph.phys.Dynamic;
import spacegraph.phys.solve.ContactSolverInfo;

/**
 * TypedConstraint is the base class for Bullet constraints and vehicles.
 * 
 * @author jezek2
 */
public abstract class TypedConstraint {
	
	//protected final BulletStack stack = BulletStack.get();
	
	// TODO: stack allocation
	private static final Dynamic s_fixed = new Dynamic(0, null, null);
	
	@Deprecated protected static /*synchronized*/ Dynamic getFixed() {
//		if (s_fixed == null) {
//			s_fixed = new RigidBody(0, null, null);
//		}
		return s_fixed;
	}

	private int userConstraintType = -1;
	private int userConstraintId = -1;

	protected final TypedConstraintType constraintType;
	
	protected final Dynamic rbA;
	protected final Dynamic rbB;
	protected float appliedImpulse;

	public TypedConstraint(TypedConstraintType type) {
		this(type, getFixed(), getFixed());
	}
	
	public TypedConstraint(TypedConstraintType type, Dynamic rbA) {
		this(type, rbA, getFixed());
	}
	
	public TypedConstraint(TypedConstraintType type, Dynamic rbA, Dynamic rbB) {
		this.constraintType = type;
		this.rbA = rbA;
		this.rbB = rbB;
		getFixed().setMassProps(0f, new v3(0f, 0f, 0f));
	}
	
	public abstract void buildJacobian();

	public abstract void solveConstraint(float timeStep);
	
	public Dynamic getRigidBodyA() {
		return rbA;
	}

	public Dynamic getRigidBodyB() {
		return rbB;
	}

	public int getUserConstraintType() {
		return userConstraintType;
	}
	
	public void setUserConstraintType(int userConstraintType) {
		this.userConstraintType = userConstraintType;
	}

	public int getUserConstraintId() {
		return userConstraintId;
	}

	public int getUid() {
		return userConstraintId;
	}

	public void setUserConstraintId(int userConstraintId) {
		this.userConstraintId = userConstraintId;
	}

	public float getAppliedImpulse() {
		return appliedImpulse;
	}

	public TypedConstraintType getConstraintType() {
		return constraintType;
	}


        // added to Java port for the Generic6DofSpringConstraint
        // use same name as latest version of Bullet, for consistency, 
        // even though the name doesn't properly reflect function here
        public void getInfo2(ContactSolverInfo infoGlobal) {
        }

	/**
     * Typed constraint type.
     *
     * @author jezek2
     */
    public enum TypedConstraintType {
        POINT2POINT_CONSTRAINT_TYPE,
        HINGE_CONSTRAINT_TYPE,
        CONETWIST_CONSTRAINT_TYPE,
        D6_CONSTRAINT_TYPE,
        VEHICLE_CONSTRAINT_TYPE,
        SLIDER_CONSTRAINT_TYPE,
            D6_SPRING_CONSTRAINT_TYPE
    }
}
