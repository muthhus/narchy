package nars.budget;

import nars.link.BLink;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created by me on 9/4/16.
 */
public final class Forget implements Consumer<BLink> {
    public final float r;

    static final float maxEffectiveDurability = 1f;

    public Forget(float r) {
        this.r = r;
    }

    @Override
    public void accept(@NotNull BLink bLink) {
        float p = bLink.pri();
        if (p == p) {
            float d = bLink.dur();
            //float d = or(bLink.dur(), bLink.qua());
            //float d = Math.max(bLink.dur(), bLink.qua());
            bLink.setPriority(p * (1f - (r * (1f - d * maxEffectiveDurability))));
        }
    }

//        public Consumer<BLink> set(float r) {
//            this.r = r;
//            return this;
//        }

}
