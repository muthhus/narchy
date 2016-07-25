package spacegraph;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.Quaternion;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import spacegraph.math.*;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.Dynamics;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.Transform;
import spacegraph.phys.shape.BoxShape;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.render.Draw;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * volumetric subspace.
 * an atom (base unit) of spacegraph physics-simulated virtual matter
 */
public abstract class Spatial<O> implements BiConsumer<GL2, Dynamic> {


    public final O key;
    public final int hash;


    /**
     * the draw order if being drawn
     * order = -1: inactive
     * order > =0: live
     */
    transient public short order;



    public Spatial() {
        this(null);
    }

    public Spatial(O k) {
        this.key = k!=null ? k : (O) this;
        this.hash = k!=null ? k.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {

        return key + "<" +
                //(body!=null ? body.shape() : "shapeless")  +
                ">";
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || key.equals(((Spatial) obj).key);
    }

    @Override
    public final int hashCode() {
        return hash;
    }


    public boolean preactive;


    /** returns true if this is an initialization cycle, or false if it is a subsequent one (already initialized) */
    public final void update(SpaceGraph<O> s) {
        preactive = true;
    }

    public final boolean active(short nextOrder) {
        if (active()) {
            this.order = nextOrder;
            return true;
        } else {
            return false;
        }
    }

    public final boolean active() {
        return preactive || order > -1;
    }


    public final void preactivate(boolean b) {
        this.preactive = b;
    }


    public boolean collidable() {
        return true;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onTouch(Collidable body, ClosestRay hitPoint, short[] buttons) {
        return false;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onKey(Collidable body, v3 hitPoint, char charCode) {
        return false;
    }


    public void stop(Dynamics s) {
        order = -1;
        preactive = false;
    }

    /** schedules this node for removal from the engine, where it will call stop(s) to complete the removal */
    public void stop() {
        order = -1;
    }

    abstract public List<Collidable> bodies();
    abstract public List<TypedConstraint> constraints();

}
