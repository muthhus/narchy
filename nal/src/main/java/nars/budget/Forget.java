package nars.budget;

import nars.Param;
import nars.link.BLink;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /**
     existingMass - (existingMass * forgetRate) + pressure <= (capacity * avgMass)
        avgMass = 0.5 (estimate)

     forgetRate ~= -((capacity * avgMass) - pressure - existingMass) / existingMass
     */
    @Nullable
    public static Forget forget(float pressure, float existingMass, int cap, float expectedAvgMass) {

        float r = -((cap * expectedAvgMass) - pressure - existingMass) / existingMass;

        Forget f;
        if (r >= Param.BUDGET_EPSILON)
            f = new Forget(Util.unitize(r));
        else
            f = null;
        return f;
    }

    @Nullable
    public static Forget forget(float pressure, float existingMass, int cap) {
        return forget(pressure, existingMass, cap, Param.BAG_THRESHOLD);
    }

    @Override
    public void accept(@NotNull BLink bLink) {
        float p = bLink.pri();
        if (p == p) {
            float d = bLink.dur();
            bLink.setPriority(p * (1f - (r * (1f - d * maxEffectiveDurability))));
            //float d = or(bLink.dur(), bLink.qua());
            //float d = Math.max(bLink.dur(), bLink.qua());
        }
    }

//        public Consumer<BLink> set(float r) {
//            this.r = r;
//            return this;
//        }

}
