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

    static final float maxEffectiveQuality = 1f;

    public Forget(float r) {
        this.r = r;
    }

    /**
     existingMass - (existingMass * forgetRate) + pressure <= (capacity * avgMass)
        avgMass = 0.5 (estimate)

     forgetRate ~= -((capacity * avgMass) - pressure - existingMass) / existingMass
     */
    @Nullable
    public static Forget forget(float pressure, float existingMass, int num, float expectedAvgMass) {

        float r = pressure > 0 ?
                -((num * expectedAvgMass) - pressure - existingMass) / existingMass :
                0;

        //float pressurePlusOversize = pressure + Math.max(0, expectedAvgMass * size - existingMass);
        //float r = (pressurePlusOversize) / (pressurePlusOversize + existingMass*4f /* momentum*/);

        //System.out.println(pressure + " " + existingMass + "\t" + r);
        return r >= Param.BUDGET_EPSILON ? new Forget(Util.unitize(r)) : null;
    }

    @Override
    public void accept(@NotNull BLink bLink) {
        float p = bLink.pri();
        if ((p == p) && p > 0) {
            float q = bLink.qua();
            //bLink.setPriority(p * (1f - (r * (1f - q * maxEffectiveQuality))));
            bLink.setPriority(p - (r * (1f - q * maxEffectiveQuality)));

        }
    }

//        public Consumer<BLink> set(float r) {
//            this.r = r;
//            return this;
//        }

}
