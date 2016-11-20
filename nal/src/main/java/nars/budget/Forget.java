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
    public static Forget forget(float pressure, float existingMass, int size, float expectedAvgMass) {

        float r = pressure > 0 ? -((size * expectedAvgMass) - pressure - existingMass) / existingMass : 0;

        //float pressurePlusOversize = pressure + Math.max(0, expectedAvgMass * size - existingMass);
        //float r = (pressurePlusOversize) / (pressurePlusOversize + existingMass*4f /* momentum*/);

        //System.out.println(pressure + " " + existingMass + "\t" + r);

        Forget f;
        if (r >= Param.BUDGET_EPSILON)
            f = new Forget(Util.unitize(r));
        else
            f = null;
        return f;
    }

    @Nullable
    public static Forget forget(float pressure, float existingMass, int siz) {
        return forget(pressure, existingMass, siz, Param.BAG_THRESHOLD);
    }

    @Override
    public void accept(@NotNull BLink bLink) {
        float p = bLink.pri();
        if (p == p) {
            float d = bLink.qua();
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
