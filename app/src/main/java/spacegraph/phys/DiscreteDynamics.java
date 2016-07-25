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

package spacegraph.phys;

import com.gs.collections.api.block.procedure.primitive.IntObjectProcedure;
import spacegraph.Spatial;
import spacegraph.math.v3;
import spacegraph.phys.collision.CollisionConfiguration;
import spacegraph.phys.collision.Islands;
import spacegraph.phys.collision.broad.*;
import spacegraph.phys.collision.narrow.ManifoldPoint;
import spacegraph.phys.collision.narrow.PersistentManifold;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.dynamics.ActionInterface;
import spacegraph.phys.dynamics.InternalTickCallback;
import spacegraph.phys.dynamics.vehicle.RaycastVehicle;
import spacegraph.phys.math.*;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.SphereShape;
import spacegraph.phys.solve.Constrainer;
import spacegraph.phys.solve.ContactSolverInfo;
import spacegraph.phys.solve.SequentialImpulseConstrainer;
import spacegraph.phys.util.OArrayList;

import java.util.Comparator;
import java.util.function.Consumer;

import static spacegraph.phys.Dynamic.ifDynamic;

/**
 * DiscreteDynamicsWorld provides discrete rigid body simulation.
 *
 * @author jezek2
 */
abstract public class DiscreteDynamics<X> extends Dynamics<X> {

    protected final Constrainer constrainer;
    protected final Islands islands;
    protected final OArrayList<TypedConstraint> constraints = new OArrayList<TypedConstraint>();
    protected final v3 gravity = new v3(0f, -10f, 0f);

    //for variable timesteps
    protected float localTime = 1f / 60f;
    //for variable timesteps

    protected boolean ownsIslandManager;
    protected boolean ownsConstrainer;

    protected OArrayList<RaycastVehicle> vehicles = new OArrayList<RaycastVehicle>();

    protected OArrayList<ActionInterface> actions = new OArrayList<ActionInterface>();

    protected int profileTimings = 0;

    protected InternalTickCallback preTickCallback;
    private float dt;

    public DiscreteDynamics(Intersecter intersecter, Broadphase pairCache, Constrainer constrainer, CollisionConfiguration collisionConfiguration) {
        super(intersecter, pairCache, collisionConfiguration);

        if (constrainer == null) {
            this.constrainer = new SequentialImpulseConstrainer();
            this.ownsConstrainer = true;
        } else {
            this.constrainer = constrainer;
            this.ownsConstrainer = false;
        }

        islands = new Islands();
        ownsIslandManager = true;
    }


    private int nextID;

    protected final void updateObjects() {
        nextID = 0;
        collidable.clear(); //populate in 'saveKinematicState'
        forEachIntSpatial((i, s) -> {

            if (s.active((short) nextID, this)) {
                nextID++;
                reactivate(s);
                return false; //dont remove
            } else {
                inactivate(s);
                return true; //remove
            }

        });

    }

    //hold sthe current list of active bodies
    private final OArrayList<Collidable<X>> collidable = new OArrayList<>();

    @Override
    public final void forEachCollidable(IntObjectProcedure<Collidable<X>> each) {

        OArrayList<Collidable<X>> o = this.collidable;
        int s = o.size();
        Collidable[] cc = o.array;
        for (int i = 0; i < s; i++) {
            each.value(i, cc[i]);
            //if (!each.(i, o.get(i)))
              //  break;
        }

    }

    /**
     * re-activates the spatial's components for this cycle
     */
    private void reactivate(Spatial<X> s) {

        s.bodies().forEach(this::on);
    }

    protected final void inactivate(Spatial<X> s) {
        s.constraints().forEach(b -> removeConstraint(b));
        s.bodies().forEach(b -> removeBody(b));
        s.stop(this);
    }

    @Override
    public OArrayList<Collidable<X>> collidables() {
        return collidable;
    }

    public void debugDrawWorld(IDebugDraw debugDrawer) {

        if (debugDrawer != null && (debugDrawer.getDebugMode() & DebugDrawModes.DRAW_CONTACT_POINTS) != 0) {
            int numManifolds = intersecter.getNumManifolds();
            v3 color = new v3();
            color.set(0f, 0f, 0f);
            for (int i = 0; i < numManifolds; i++) {
                PersistentManifold contactManifold = intersecter.getManifoldByIndexInternal(i);
                //btCollisionObject* obA = static_cast<btCollisionObject*>(contactManifold->getBody0());
                //btCollisionObject* obB = static_cast<btCollisionObject*>(contactManifold->getBody1());

                int numContacts = contactManifold.getNumContacts();
                for (int j = 0; j < numContacts; j++) {
                    ManifoldPoint cp = contactManifold.getContactPoint(j);
                    debugDrawer.drawContactPoint(cp.positionWorldOnB, cp.normalWorldOnB, cp.getDistance(), cp.getLifeTime(), color);
                }
            }
        }

        if (debugDrawer != null && (debugDrawer.getDebugMode() & (DebugDrawModes.DRAW_WIREFRAME | DebugDrawModes.DRAW_AABB)) != 0) {
            int i;

            Transform tmpTrans = new Transform();
            v3 minAabb = new v3();
            v3 maxAabb = new v3();
            v3 colorvec = new v3();

            // todo: iterate over awake simulation islands!
            for (i = 0; i < collidable.size(); i++) {
                //return array[index];
                Collidable colObj = collidable.get(i);
                if (debugDrawer != null && (debugDrawer.getDebugMode() & DebugDrawModes.DRAW_WIREFRAME) != 0) {
                    v3 color = new v3();
                    color.set(255f, 255f, 255f);
                    switch (colObj.getActivationState()) {
                        case Collidable.ACTIVE_TAG:
                            color.set(255f, 255f, 255f);
                            break;
                        case Collidable.ISLAND_SLEEPING:
                            color.set(0f, 255f, 0f);
                            break;
                        case Collidable.WANTS_DEACTIVATION:
                            color.set(0f, 255f, 255f);
                            break;
                        case Collidable.DISABLE_DEACTIVATION:
                            color.set(255f, 0f, 0f);
                            break;
                        case Collidable.DISABLE_SIMULATION:
                            color.set(255f, 255f, 0f);
                            break;
                        default: {
                            color.set(255f, 0f, 0f);
                        }
                    }

                    debugDrawObject(debugDrawer, colObj.getWorldTransform(tmpTrans), colObj.shape(), color);
                }
                if (debugDrawer != null && (debugDrawer.getDebugMode() & DebugDrawModes.DRAW_AABB) != 0) {
                    colorvec.set(1f, 0f, 0f);
                    colObj.shape().getAabb(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb);
                    debugDrawer.drawAabb(minAabb, maxAabb, colorvec);
                }
            }

            v3 wheelColor = new v3();
            v3 wheelPosWS = new v3();
            v3 axle = new v3();
            v3 tmp = new v3();

            for (i = 0; i < vehicles.size(); i++) {
                //return array[index];
                for (int v = 0; v < vehicles.get(i).getNumWheels(); v++) {
                    wheelColor.set(0, 255, 255);
                    //return array[index];
                    if (vehicles.get(i).getWheelInfo(v).raycastInfo.isInContact) {
                        wheelColor.set(0, 0, 255);
                    } else {
                        wheelColor.set(255, 0, 255);
                    }

                    //return array[index];
                    wheelPosWS.set(vehicles.get(i).getWheelInfo(v).worldTransform);

                    //return array[index];
                    //return array[index];
                    //return array[index];
                    //return array[index];
                    //return array[index];
                    //return array[index];
                    axle.set(
                            vehicles.get(i).getWheelInfo(v).worldTransform.basis.get(0, vehicles.get(i).getRightAxis()),
                            vehicles.get(i).getWheelInfo(v).worldTransform.basis.get(1, vehicles.get(i).getRightAxis()),
                            vehicles.get(i).getWheelInfo(v).worldTransform.basis.get(2, vehicles.get(i).getRightAxis()));


                    //m_vehicles[i]->getWheelInfo(v).m_raycastInfo.m_wheelAxleWS
                    //debug wheels (cylinders)
                    tmp.add(wheelPosWS, axle);
                    debugDrawer.drawLine(wheelPosWS, tmp, wheelColor);
                    //return array[index];
                    debugDrawer.drawLine(wheelPosWS, vehicles.get(i).getWheelInfo(v).raycastInfo.contactPointWS, wheelColor);
                }
            }

            if (debugDrawer != null && debugDrawer.getDebugMode() != 0) {
                for (i = 0; i < actions.size(); i++) {
                    //return array[index];
                    actions.get(i).debugDraw(debugDrawer);
                }
            }
        }
    }


    protected void synchronizeMotionStates(boolean clear) {
        Transform interpolatedTransform = new Transform();

        Transform tmpTrans = new Transform();
        v3 tmpLinVel = new v3();
        v3 tmpAngVel = new v3();

        // todo: iterate over awake simulation islands!
        OArrayList<Collidable<X>> colliding = this.collidable;
        for (int i = 0, collidingSize = colliding.size(); i < collidingSize; i++) {
            Dynamic body = ifDynamic(colliding.get(i));

            if (body == null) {
                continue;
            }

            if (body.getMotionState() != null && !body.isStaticOrKinematicObject()) {
                // we need to call the update at least once, even for sleeping objects
                // otherwise the 'graphics' transform never updates properly
                // so todo: add 'dirty' flag
                //if (body->getActivationState() != ISLAND_SLEEPING)
                TransformUtil.integrateTransform(
                        body.getInterpolationWorldTransform(tmpTrans),
                        body.getInterpolationLinearVelocity(tmpLinVel),
                        body.getInterpolationAngularVelocity(tmpAngVel),
                        localTime * body.getHitFraction(), interpolatedTransform);
                body.getMotionState().setWorldTransform(interpolatedTransform);
            }

            if (clear) {
                body.clearForces();
            }
        }


    }

    @Override
    public int stepSimulation(float timeStep, int maxSubSteps, float fixedTimeStep) {
        startProfiling(timeStep);

        long t0 = System.nanoTime();

        BulletStats.pushProfile("stepSimulation");
        try {
            int numSimulationSubSteps = 0;

            if (maxSubSteps != 0) {
                // fixed timestep with interpolation
                localTime += timeStep;
                if (localTime >= fixedTimeStep) {
                    numSimulationSubSteps = (int) (localTime / fixedTimeStep);
                    localTime -= numSimulationSubSteps * fixedTimeStep;
                }
            } else {
                //variable timestep
                fixedTimeStep = timeStep;
                localTime = timeStep;
                if (ScalarUtil.fuzzyZero(timeStep)) {
                    numSimulationSubSteps = 0;
                    maxSubSteps = 0;
                } else {
                    numSimulationSubSteps = 1;
                    maxSubSteps = 1;
                }
            }

            this.dt = fixedTimeStep;

            if (numSimulationSubSteps != 0) {

                updateObjects();

                // clamp the number of substeps, to prevent simulation grinding spiralling down to a halt
                int clampedSimulationSteps = (numSimulationSubSteps > maxSubSteps) ? maxSubSteps : numSimulationSubSteps;

                for (int i = 0; i < clampedSimulationSteps; i++) {
                    internalSingleStepSimulation(fixedTimeStep);
                    synchronizeMotionStates(false);
                }
            }

            synchronizeMotionStates(true);

            CProfileManager.incrementFrameCounter();

            return numSimulationSubSteps;
        } finally {
            BulletStats.popProfile();

            BulletStats.stepSimulationTime = (System.nanoTime() - t0) / 1000000;
        }
    }

//    private final void addPending() {
//        FasterList<Spatial<X>> p = this.pendingAdd;
//        if (p.isEmpty())
//            return;
//
//        this.pendingAdd = $.newArrayList(); //new copy, thread-safe
//
//        objects().addAll(p);
//    }

    protected void internalSingleStepSimulation(float timeStep) {
        BulletStats.pushProfile("internalSingleStepSimulation");
        try {
            if (preTickCallback != null) {
                preTickCallback.internalTick(this, timeStep);
            }

            // apply gravity, predict motion
            predictUnconstraintMotion(timeStep);

            DispatcherInfo dispatchInfo = getDispatchInfo();

            dispatchInfo.timeStep = timeStep;
            dispatchInfo.stepCount = 0;


            // perform collision detection
            performDiscreteCollisionDetection();

            calculateSimulationIslands();

            solverInfo.timeStep = timeStep;

            // solve contact and other joint constraints
            solveConstraints(solverInfo);

            //CallbackTriggers();

            // integrate transforms
            integrateTransforms(timeStep);

            // update vehicle simulation
            updateActions(timeStep);

            // update vehicle simulation
            updateVehicles(timeStep);

            updateActivationState(timeStep);

            if (internalTickCallback != null) {
                internalTickCallback.internalTick(this, timeStep);
            }
        } finally {
            BulletStats.popProfile();
        }
    }

    @Override
    public void setGravity(v3 gravity) {
        this.gravity.set(gravity);
    }


    /**
     * enable/register the body in the engine
     */
    protected final void on(Collidable c) {
        collidable.add(c);

        Dynamic d = ifDynamic(c);
        if (d != null) {
            if (d.getActivationState() != Collidable.ISLAND_SLEEPING)
                d.saveKinematicState(dt); // to calculate velocities next frame

            if (d.isActive())
                d.applyGravity();
        }

        if (d.shape() != null) {
            super.on(d);
        }

        if (!d.isStaticOrKinematicObject()) {
            d.setGravity(gravity);
        }
    }

    public void updateActions(float timeStep) {
        BulletStats.pushProfile("updateActions");
        try {
            for (int i = 0; i < actions.size(); i++) {
                //return array[index];
                actions.get(i).updateAction(this, timeStep);
            }
        } finally {
            BulletStats.popProfile();
        }
    }

    protected void updateVehicles(float timeStep) {
        BulletStats.pushProfile("updateVehicles");
        try {
            for (int i = 0; i < vehicles.size(); i++) {
                //return array[index];
                RaycastVehicle vehicle = vehicles.get(i);
                vehicle.updateVehicle(timeStep);
            }
        } finally {
            BulletStats.popProfile();
        }
    }

    protected void updateActivationState(float timeStep) {
        BulletStats.pushProfile("updateActivationState");
        try {
            v3 tmp = new v3();

            for (int i = 0; i < collidable.size(); i++) {
                //return array[index];
                Collidable colObj = collidable.get(i);
                Dynamic body = ifDynamic(colObj);
                if (body != null) {
                    body.updateDeactivation(timeStep);

                    if (body.wantsSleeping()) {
                        if (body.isStaticOrKinematicObject()) {
                            body.setActivationState(Collidable.ISLAND_SLEEPING);
                        } else {
                            if (body.getActivationState() == Collidable.ACTIVE_TAG) {
                                body.setActivationState(Collidable.WANTS_DEACTIVATION);
                            }
                            if (body.getActivationState() == Collidable.ISLAND_SLEEPING) {
                                tmp.set(0f, 0f, 0f);
                                body.setAngularVelocity(tmp);
                                body.setLinearVelocity(tmp);
                            }
                        }
                    } else {
                        if (body.getActivationState() != Collidable.DISABLE_DEACTIVATION) {
                            body.setActivationState(Collidable.ACTIVE_TAG);
                        }
                    }
                }
            }
        } finally {
            BulletStats.popProfile();
        }
    }

    @Override
    public void addConstraint(TypedConstraint constraint, boolean disableCollisionsBetweenLinkedBodies) {
        constraints.add(constraint);
        if (disableCollisionsBetweenLinkedBodies) {
            constraint.getRigidBodyA().addConstraintRef(constraint);
            constraint.getRigidBodyB().addConstraintRef(constraint);
        }
    }

    @Override
    public void removeConstraint(TypedConstraint constraint) {
        constraints.remove(constraint);
        constraint.getRigidBodyA().removeConstraintRef(constraint);
        constraint.getRigidBodyB().removeConstraintRef(constraint);
    }

    @Override
    public void addAction(ActionInterface action) {
        actions.add(action);
    }

    @Override
    public void removeAction(ActionInterface action) {
        actions.remove(action);
    }

    @Override
    public void addVehicle(RaycastVehicle vehicle) {
        vehicles.add(vehicle);
    }

    @Override
    public void removeVehicle(RaycastVehicle vehicle) {
        vehicles.remove(vehicle);
    }

    private static int getConstraintIslandId(TypedConstraint lhs) {
        int islandId;

        Collidable rcolObj0 = lhs.getRigidBodyA();
        Collidable rcolObj1 = lhs.getRigidBodyB();
        islandId = rcolObj0.getIslandTag() >= 0 ? rcolObj0.getIslandTag() : rcolObj1.getIslandTag();
        return islandId;
    }

    private static class InplaceSolverIslandCallback extends Islands.IslandCallback {
        public ContactSolverInfo solverInfo;
        public Constrainer solver;
        public OArrayList<TypedConstraint> sortedConstraints;
        public int numConstraints;
        //public StackAlloc* m_stackAlloc;
        public Intersecter intersecter;

        public void init(ContactSolverInfo solverInfo, Constrainer solver, OArrayList<TypedConstraint> sortedConstraints, int numConstraints, Intersecter intersecter) {
            this.solverInfo = solverInfo;
            this.solver = solver;
            this.sortedConstraints = sortedConstraints;
            this.numConstraints = numConstraints;

            this.intersecter = intersecter;
        }

        @Override
        public void processIsland(OArrayList<Collidable> bodies, int numBodies, OArrayList<PersistentManifold> manifolds, int manifolds_offset, int numManifolds, int islandId) {
            if (islandId < 0) {
                // we don't split islands, so all constraints/contact manifolds/bodies are passed into the solver regardless the island id
                solver.solveGroup(bodies, numBodies, manifolds, manifolds_offset, numManifolds, sortedConstraints, 0, numConstraints, solverInfo/*,m_stackAlloc*/, intersecter);
            } else {
                // also add all non-contact constraints/joints for this island
                //ObjectArrayList<TypedConstraint> startConstraint = null;
                int startConstraint_idx = -1;
                int numCurConstraints = 0;
                int i;

                // find the first constraint for this island
                for (i = 0; i < numConstraints; i++) {
                    //return array[index];
                    if (getConstraintIslandId(sortedConstraints.get(i)) == islandId) {
                        //startConstraint = &m_sortedConstraints[i];
                        //startConstraint = sortedConstraints.subList(i, sortedConstraints.size());
                        startConstraint_idx = i;
                        break;
                    }
                }
                // count the number of constraints in this island
                for (; i < numConstraints; i++) {
                    //return array[index];
                    if (getConstraintIslandId(sortedConstraints.get(i)) == islandId) {
                        numCurConstraints++;
                    }
                }

                // only call solveGroup if there is some work: avoid virtual function call, its overhead can be excessive
                if ((numManifolds + numCurConstraints) > 0) {
                    solver.solveGroup(bodies, numBodies, manifolds, manifolds_offset, numManifolds, sortedConstraints, startConstraint_idx, numCurConstraints, solverInfo/*,m_stackAlloc*/, intersecter);
                }
            }
        }
    }


    private final OArrayList<TypedConstraint> sortedConstraints = new OArrayList<TypedConstraint>();
    private final InplaceSolverIslandCallback solverCallback = new InplaceSolverIslandCallback();

    protected void solveConstraints(ContactSolverInfo solverInfo) {
        BulletStats.pushProfile("solveConstraints");
        try {
            // sorted version of all btTypedConstraint, based on islandId

            sortedConstraints.clear();
            constraints.forEach((TypedConstraint c) -> {
                sortedConstraints.add(c);
            });

            //Collections.sort(sortedConstraints, sortConstraintOnIslandPredicate);
            MiscUtil.quickSort(sortedConstraints, sortConstraintOnIslandPredicate);

            int num = sortedConstraints.size();
            solverCallback.init(solverInfo,
                    constrainer,
                    num != 0 ? sortedConstraints : null,
                    num, 
                    /*,m_stackAlloc*/ intersecter);

            constrainer.prepareSolve(getNumCollisionObjects(), ((Collisions) this).intersecter.getNumManifolds());

            // solve all the constraints for this island
            islands.buildAndProcessIslands(intersecter, collidable, solverCallback);

            constrainer.allSolved(solverInfo /*, m_stackAlloc*/);
        } finally {
            BulletStats.popProfile();
        }
    }

    protected void calculateSimulationIslands() {
        BulletStats.pushProfile("calculateSimulationIslands");
        try {

            islands.updateActivationState(this, intersecter);

            forEachConstraint((TypedConstraint constraint) -> {
                Dynamic colObj0 = constraint.getRigidBodyA();
                if (colObj0 == null || !colObj0.isActive() || colObj0.isStaticOrKinematicObject())
                    return;

                Dynamic colObj1 = constraint.getRigidBodyB();
                if (colObj1 == null || !colObj1.isActive() || colObj1.isStaticOrKinematicObject())
                    return;

                islands.find.unite(colObj0.getIslandTag(), colObj1.getIslandTag());
            });


            // Store the island id in each body
            islands.storeIslandActivationState(this);
        } finally {
            BulletStats.popProfile();
        }
    }

    public void forEachConstraint(Consumer<TypedConstraint> e) {
        constraints.forEach(e);
    }

    protected void integrateTransforms(float timeStep) {
        BulletStats.pushProfile("integrateTransforms");
        try {
            v3 tmp = new v3();
            Transform tmpTrans = new Transform();

            Transform predictedTrans = new Transform();
            for (int i = 0; i < collidable.size(); i++) {
                //return array[index];
                Collidable colObj = collidable.get(i);
                Dynamic body = ifDynamic(colObj);
                if (body != null) {
                    body.setHitFraction(1f);

                    if (body.isActive() && (!body.isStaticOrKinematicObject())) {
                        body.predictIntegratedTransform(timeStep, predictedTrans);

                        tmp.sub(predictedTrans, body.getWorldTransform(tmpTrans));
                        float squareMotion = tmp.lengthSquared();

                        if (body.getCcdSquareMotionThreshold() != 0f && body.getCcdSquareMotionThreshold() < squareMotion) {
                            BulletStats.pushProfile("CCD motion clamping");
                            try {
                                if (body.shape().isConvex()) {
                                    BulletStats.gNumClampedCcdMotions++;

                                    ClosestNotMeConvexResultCallback sweepResults = new ClosestNotMeConvexResultCallback(body, body.getWorldTransform(tmpTrans), predictedTrans, getBroadphase().getOverlappingPairCache(), intersecter);
                                    //ConvexShape convexShape = (ConvexShape)body.getCollisionShape();
                                    SphereShape tmpSphere = new SphereShape(body.getCcdSweptSphereRadius()); //btConvexShape* convexShape = static_cast<btConvexShape*>(body->getCollisionShape());

                                    sweepResults.collisionFilterGroup = body.broadphase().collisionFilterGroup;
                                    sweepResults.collisionFilterMask = body.broadphase().collisionFilterMask;

                                    convexSweepTest(tmpSphere, body.getWorldTransform(tmpTrans), predictedTrans, sweepResults);
                                    // JAVA NOTE: added closestHitFraction test to prevent objects being stuck
                                    if (sweepResults.hasHit() && (sweepResults.closestHitFraction > 0.0001f)) {
                                        body.setHitFraction(sweepResults.closestHitFraction);
                                        body.predictIntegratedTransform(timeStep * body.getHitFraction(), predictedTrans);
                                        body.setHitFraction(0f);
                                        //System.out.printf("clamped integration to hit fraction = %f\n", sweepResults.closestHitFraction);
                                    }
                                }
                            } finally {
                                BulletStats.popProfile();
                            }
                        }

                        body.proceedToTransform(predictedTrans);
                    }
                }
            }
        } finally {
            BulletStats.popProfile();
        }
    }

    protected void predictUnconstraintMotion(float timeStep) {
        BulletStats.pushProfile("predictUnconstraintMotion");
        try {
            Transform tmpTrans = new Transform();

            for (int i = 0; i < collidable.size(); i++) {
                //return array[index];
                Collidable colObj = collidable.get(i);
                Dynamic body = ifDynamic(colObj);
                if (body != null) {
                    if (!body.isStaticOrKinematicObject()) {
                        if (body.isActive()) {
                            body.integrateVelocities(timeStep);
                            // damping
                            body.applyDamping(timeStep);

                            body.predictIntegratedTransform(timeStep, body.getInterpolationWorldTransform(tmpTrans));
                        }
                    }
                }
            }
        } finally {
            BulletStats.popProfile();
        }
    }

    protected static void startProfiling(float timeStep) {
        //#ifndef BT_NO_PROFILE
        CProfileManager.reset();
        //#endif //BT_NO_PROFILE
    }

    protected void debugDrawSphere(IDebugDraw debugDrawer, float radius, Transform transform, v3 color) {
        v3 start = new v3(transform);

        v3 xoffs = new v3();
        xoffs.set(radius, 0, 0);
        transform.basis.transform(xoffs);
        v3 yoffs = new v3();
        yoffs.set(0, radius, 0);
        transform.basis.transform(yoffs);
        v3 zoffs = new v3();
        zoffs.set(0, 0, radius);
        transform.basis.transform(zoffs);

        v3 tmp1 = new v3();
        v3 tmp2 = new v3();

        // XY
        tmp1.sub(start, xoffs);
        tmp2.add(start, yoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, yoffs);
        tmp2.add(start, xoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, xoffs);
        tmp2.sub(start, yoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.sub(start, yoffs);
        tmp2.sub(start, xoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);

        // XZ
        tmp1.sub(start, xoffs);
        tmp2.add(start, zoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, zoffs);
        tmp2.add(start, xoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, xoffs);
        tmp2.sub(start, zoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.sub(start, zoffs);
        tmp2.sub(start, xoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);

        // YZ
        tmp1.sub(start, yoffs);
        tmp2.add(start, zoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, zoffs);
        tmp2.add(start, yoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.add(start, yoffs);
        tmp2.sub(start, zoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
        tmp1.sub(start, zoffs);
        tmp2.sub(start, yoffs);
        debugDrawer.drawLine(tmp1, tmp2, color);
    }

    public void debugDrawObject(IDebugDraw debugDrawer, Transform worldTransform, CollisionShape shape, v3 color) {
        v3 tmp = new v3();
        v3 tmp2 = new v3();

        // Draw a small simplex at the center of the object
        v3 start = new v3(worldTransform);

        tmp.set(1f, 0f, 0f);
        worldTransform.basis.transform(tmp);
        tmp.add(start);
        tmp2.set(1f, 0f, 0f);
        debugDrawer.drawLine(start, tmp, tmp2);

        tmp.set(0f, 1f, 0f);
        worldTransform.basis.transform(tmp);
        tmp.add(start);
        tmp2.set(0f, 1f, 0f);
        debugDrawer.drawLine(start, tmp, tmp2);

        tmp.set(0f, 0f, 1f);
        worldTransform.basis.transform(tmp);
        tmp.add(start);
        tmp2.set(0f, 0f, 1f);
        debugDrawer.drawLine(start, tmp, tmp2);

        // JAVA TODO: debugDrawObject, note that this commented code is from old version, use actual version when implementing

//		if (shape->getShapeType() == COMPOUND_SHAPE_PROXYTYPE)
//		{
//			const btCompoundShape* compoundShape = static_cast<const btCompoundShape*>(shape);
//			for (int i=compoundShape->getNumChildShapes()-1;i>=0;i--)
//			{
//				btTransform childTrans = compoundShape->getChildTransform(i);
//				const btCollisionShape* colShape = compoundShape->getChildShape(i);
//				debugDrawObject(worldTransform*childTrans,colShape,color);
//			}
//
//		} else
//		{
//			switch (shape->getShapeType())
//			{
//
//			case SPHERE_SHAPE_PROXYTYPE:
//				{
//					const btSphereShape* sphereShape = static_cast<const btSphereShape*>(shape);
//					btScalar radius = sphereShape->getMargin();//radius doesn't include the margin, so draw with margin
//
//					debugDrawSphere(radius, worldTransform, color);
//					break;
//				}
//			case MULTI_SPHERE_SHAPE_PROXYTYPE:
//				{
//					const btMultiSphereShape* multiSphereShape = static_cast<const btMultiSphereShape*>(shape);
//
//					for (int i = multiSphereShape->getSphereCount()-1; i>=0;i--)
//					{
//						btTransform childTransform = worldTransform;
//						childTransform.getOrigin() += multiSphereShape->getSpherePosition(i);
//						debugDrawSphere(multiSphereShape->getSphereRadius(i), childTransform, color);
//					}
//
//					break;
//				}
//			case CAPSULE_SHAPE_PROXYTYPE:
//				{
//					const btCapsuleShape* capsuleShape = static_cast<const btCapsuleShape*>(shape);
//
//					btScalar radius = capsuleShape->getRadius();
//					btScalar halfHeight = capsuleShape->getHalfHeight();
//
//					// Draw the ends
//					{
//						btTransform childTransform = worldTransform;
//						childTransform.getOrigin() = worldTransform * btVector3(0,halfHeight,0);
//						debugDrawSphere(radius, childTransform, color);
//					}
//
//					{
//						btTransform childTransform = worldTransform;
//						childTransform.getOrigin() = worldTransform * btVector3(0,-halfHeight,0);
//						debugDrawSphere(radius, childTransform, color);
//					}
//
//					// Draw some additional lines
//					btVector3 start = worldTransform.getOrigin();
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * btVector3(-radius,halfHeight,0),start+worldTransform.getBasis() * btVector3(-radius,-halfHeight,0), color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * btVector3(radius,halfHeight,0),start+worldTransform.getBasis() * btVector3(radius,-halfHeight,0), color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * btVector3(0,halfHeight,-radius),start+worldTransform.getBasis() * btVector3(0,-halfHeight,-radius), color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * btVector3(0,halfHeight,radius),start+worldTransform.getBasis() * btVector3(0,-halfHeight,radius), color);
//
//					break;
//				}
//			case CONE_SHAPE_PROXYTYPE:
//				{
//					const btConeShape* coneShape = static_cast<const btConeShape*>(shape);
//					btScalar radius = coneShape->getRadius();//+coneShape->getMargin();
//					btScalar height = coneShape->getHeight();//+coneShape->getMargin();
//					btVector3 start = worldTransform.getOrigin();
//
//					int upAxis= coneShape->getConeUpIndex();
//
//
//					btVector3	offsetHeight(0,0,0);
//					offsetHeight[upAxis] = height * btScalar(0.5);
//					btVector3	offsetRadius(0,0,0);
//					offsetRadius[(upAxis+1)%3] = radius;
//					btVector3	offset2Radius(0,0,0);
//					offset2Radius[(upAxis+2)%3] = radius;
//
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight+offsetRadius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight-offsetRadius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight+offset2Radius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight-offset2Radius),color);
//
//
//
//					break;
//
//				}
//			case CYLINDER_SHAPE_PROXYTYPE:
//				{
//					const btCylinderShape* cylinder = static_cast<const btCylinderShape*>(shape);
//					int upAxis = cylinder->getUpAxis();
//					btScalar radius = cylinder->getRadius();
//					btScalar halfHeight = cylinder->getHalfExtentsWithMargin()[upAxis];
//					btVector3 start = worldTransform.getOrigin();
//					btVector3	offsetHeight(0,0,0);
//					offsetHeight[upAxis] = halfHeight;
//					btVector3	offsetRadius(0,0,0);
//					offsetRadius[(upAxis+1)%3] = radius;
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight+offsetRadius),start+worldTransform.getBasis() * (-offsetHeight+offsetRadius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight-offsetRadius),start+worldTransform.getBasis() * (-offsetHeight-offsetRadius),color);
//					break;
//				}
//			default:
//				{
//
//					if (shape->isConcave())
//					{
//						btConcaveShape* concaveMesh = (btConcaveShape*) shape;
//
//						//todo pass camera, for some culling
//						btVector3 aabbMax(btScalar(1e30),btScalar(1e30),btScalar(1e30));
//						btVector3 aabbMin(btScalar(-1e30),btScalar(-1e30),btScalar(-1e30));
//
//						DebugDrawcallback drawCallback(getDebugDrawer(),worldTransform,color);
//						concaveMesh->processAllTriangles(&drawCallback,aabbMin,aabbMax);
//
//					}
//
//					if (shape->getShapeType() == CONVEX_TRIANGLEMESH_SHAPE_PROXYTYPE)
//					{
//						btConvexTriangleMeshShape* convexMesh = (btConvexTriangleMeshShape*) shape;
//						//todo: pass camera for some culling			
//						btVector3 aabbMax(btScalar(1e30),btScalar(1e30),btScalar(1e30));
//						btVector3 aabbMin(btScalar(-1e30),btScalar(-1e30),btScalar(-1e30));
//						//DebugDrawcallback drawCallback;
//						DebugDrawcallback drawCallback(getDebugDrawer(),worldTransform,color);
//						convexMesh->getMeshInterface()->InternalProcessAllTriangles(&drawCallback,aabbMin,aabbMax);
//					}
//
//
//					/// for polyhedral shapes
//					if (shape->isPolyhedral())
//					{
//						btPolyhedralConvexShape* polyshape = (btPolyhedralConvexShape*) shape;
//
//						int i;
//						for (i=0;i<polyshape->getNumEdges();i++)
//						{
//							btPoint3 a,b;
//							polyshape->getEdge(i,a,b);
//							btVector3 wa = worldTransform * a;
//							btVector3 wb = worldTransform * b;
//							getDebugDrawer()->drawLine(wa,wb,color);
//
//						}
//
//
//					}
//				}
//			}
//		}
    }

//    public void setConstrainer(Constrainer solver) {
//        if (ownsConstrainer) {
//            //btAlignedFree( m_constraintSolver);
//        }
//        ownsConstrainer = false;
//        constrainer = solver;
//    }


    // JAVA NOTE: not part of the original api
    @Override
    public int getNumActions() {
        return actions.size();
    }

    // JAVA NOTE: not part of the original api
    @Override
    public ActionInterface getAction(int index) {
        return actions.get(index);
        //return array[index];
    }

//	public SimulationIslandManager getSimulationIslandManager() {
//		return islandManager;
//	}


//	public void setNumTasks(int numTasks) {
//	}
//
//        public void setPreTickCallback(InternalTickCallback callback){
//                preTickCallback = callback;
//        }

    ////////////////////////////////////////////////////////////////////////////

    private static final Comparator<TypedConstraint> sortConstraintOnIslandPredicate = new Comparator<TypedConstraint>() {
        @Override
        public int compare(TypedConstraint lhs, TypedConstraint rhs) {
            int rIslandId0, lIslandId0;
            rIslandId0 = getConstraintIslandId(rhs);
            lIslandId0 = getConstraintIslandId(lhs);
            return lIslandId0 < rIslandId0 ? -1 : +1;
        }
    };

//	private static class DebugDrawcallback implements TriangleCallback, InternalTriangleIndexCallback {
//		private IDebugDraw debugDrawer;
//		private final Vector3f color = new Vector3f();
//		private final Transform worldTrans = new Transform();
//
//		public DebugDrawcallback(IDebugDraw debugDrawer, Transform worldTrans, Vector3f color) {
//			this.debugDrawer = debugDrawer;
//			this.worldTrans.set(worldTrans);
//			this.color.set(color);
//		}
//
//		public void internalProcessTriangleIndex(Vector3f[] triangle, int partId, int triangleIndex) {
//			processTriangle(triangle,partId,triangleIndex);
//		}
//
//		private final Vector3f wv0 = new Vector3f(),wv1 = new Vector3f(),wv2 = new Vector3f();
//
//		public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
//			wv0.set(triangle[0]);
//			worldTrans.transform(wv0);
//			wv1.set(triangle[1]);
//			worldTrans.transform(wv1);
//			wv2.set(triangle[2]);
//			worldTrans.transform(wv2);
//
//			debugDrawer.drawLine(wv0, wv1, color);
//			debugDrawer.drawLine(wv1, wv2, color);
//			debugDrawer.drawLine(wv2, wv0, color);
//		}
//	}

    private static class ClosestNotMeConvexResultCallback extends ClosestConvexResultCallback {
        private final Collidable me;
        private float allowedPenetration;
        private final OverlappingPairCache pairCache;
        private final Intersecter intersecter;

        public ClosestNotMeConvexResultCallback(Collidable me, v3 fromA, v3 toA, OverlappingPairCache pairCache, Intersecter intersecter) {
            super(fromA, toA);
            this.me = me;
            this.pairCache = pairCache;
            this.intersecter = intersecter;
        }

        @Override
        public float addSingleResult(LocalConvexResult convexResult, boolean normalInWorldSpace) {
            if (convexResult.hitCollidable == me) {
                return 1f;
            }

            v3 linVelA = new v3(), linVelB = new v3();
            linVelA.sub(convexToWorld, convexFromWorld);
            linVelB.set(0f, 0f, 0f);//toB.getOrigin()-fromB.getOrigin();

            v3 relativeVelocity = new v3();
            relativeVelocity.sub(linVelA, linVelB);
            // don't report time of impact for motion away from the contact normal (or causes minor penetration)
            if (convexResult.hitNormalLocal.dot(relativeVelocity) >= -allowedPenetration) {
                return 1f;
            }

            return super.addSingleResult(convexResult, normalInWorldSpace);
        }

        @Override
        public boolean needsCollision(Broadphasing proxy0) {
            // don't collide with itself
            if (proxy0.clientObject == me) {
                return false;
            }

            // don't do CCD when the collision filters are not matching
            if (!super.needsCollision(proxy0)) {
                return false;
            }

            Collidable otherObj = (Collidable) proxy0.clientObject;

            // call needsResponse, see http://code.google.com/p/bullet/issues/detail?id=179
            if (intersecter.needsResponse(me, otherObj)) {
                // don't do CCD when there are already contact points (touching contact/penetration)
                OArrayList<PersistentManifold> manifoldArray = new OArrayList<PersistentManifold>();
                BroadphasePair collisionPair = pairCache.findPair(me.broadphase(), proxy0);
                if (collisionPair != null) {
                    if (collisionPair.algorithm != null) {
                        //manifoldArray.resize(0);
                        collisionPair.algorithm.getAllContactManifolds(manifoldArray);
                        for (int j = 0; j < manifoldArray.size(); j++) {
                            //return array[index];
                            PersistentManifold manifold = manifoldArray.get(j);
                            if (manifold.getNumContacts() > 0) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

}
