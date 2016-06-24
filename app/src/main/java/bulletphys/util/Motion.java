package bulletphys.util;

import bulletphys.linearmath.MotionState;
import bulletphys.linearmath.Transform;

import javax.vecmath.Vector3f;


public class Motion extends MotionState {

    public final Transform t = new Transform();

    public final Transform centerOfMassOffset;

    public Motion() {
        this.t.setIdentity();
        this.centerOfMassOffset = null;
    }

    public Motion(Transform startTrans) {
        this.t.set(startTrans);
        this.centerOfMassOffset = null;
    }

    public Motion(Transform startTrans, Transform centerOfMassOffset) {
        this.t.set(startTrans);
        this.centerOfMassOffset = new Transform();
        this.centerOfMassOffset.set(centerOfMassOffset);
    }

    @Override
    public Transform getWorldTransform(Transform out) {
        Transform w = this.t;
        Transform c = this.centerOfMassOffset;
        if (c !=null) {
            out.inverse(c);
            out.mul(w);
        } else {
            out.set(w);
        }
        return out;
    }

    @Override
    public void setWorldTransform(Transform centerOfMassWorldTrans) {
        Transform w = this.t;
        w.set(centerOfMassWorldTrans);
        Transform c = this.centerOfMassOffset;
        if (c != null) {
            w.mul(c);
        }
    }

    public void center(Vector3f v) {
        t.origin.set(v);
    }
}
