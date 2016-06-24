package nars.gui.test.bullet;

import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;


public class Motion extends MotionState {

    public final Transform graphicsWorldTrans = new Transform();

    public final Transform centerOfMassOffset;

    public Motion() {
        this.graphicsWorldTrans.setIdentity();
        this.centerOfMassOffset = null;
    }

    public Motion(Transform startTrans) {
        this.graphicsWorldTrans.set(startTrans);
        this.centerOfMassOffset = null;
    }

    public Motion(Transform startTrans, Transform centerOfMassOffset) {
        this.graphicsWorldTrans.set(startTrans);
        this.centerOfMassOffset = new Transform();
        this.centerOfMassOffset.set(centerOfMassOffset);
    }

    public Transform getWorldTransform(Transform out) {
        Transform w = this.graphicsWorldTrans;
        Transform c = this.centerOfMassOffset;
        if (c !=null) {
            out.inverse(c);
            out.mul(w);
        } else {
            out.set(w);
        }
        return out;
    }

    public void setWorldTransform(Transform centerOfMassWorldTrans) {
        Transform w = this.graphicsWorldTrans;
        w.set(centerOfMassWorldTrans);
        Transform c = this.centerOfMassOffset;
        if (c != null) {
            w.mul(c);
        }
    }
}
