/*
 * BudgetValue.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.budget;

import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A triple of priority (current), durability (decay), and quality (long-term average).
 *
 * Mutable, unit-scaled (1.0 max) budget value
 *
 */
public class UnitBudget extends RawBudget {

    /**common instance for a 'Deleted budget'.*/
    public static final Budget Deleted = new ROBudget(Float.NaN, 0, 0);

    /** common instance for a 'full budget'.*/
    public static final Budget One = new ROBudget(1f,1f,1f);

    /** common instance for a 'half budget'.*/
    public static final Budget Half = new ROBudget(0.5f, 0.5f, 0.5f);

    /** common instance for a 'zero budget'.*/
    public static final Budget Zero = new ROBudget(0,0,0);




    public UnitBudget(float p, float d, @Nullable Truth qualityFromTruth) {
        this(p, d, qualityFromTruth !=
                null ? BudgetFunctions.truthToQuality(qualityFromTruth) : 1.0f);
    }


    /**
     * Constructor with initialization
     *
     * @param p Initial priority
     * @param d Initial durability
     * @param q Initial quality
     */
    public UnitBudget(float p, float d, float q) {
        super(p, d, q);
    }


    /**
     * begins with 0.0f for all components
     */
    public UnitBudget() {
    }



    /**
     * Cloning constructor
     *
     * @param v Budget value to be cloned
     */
    public UnitBudget(@Nullable Budget v) {
        this();
        if (v != null) {
            budget(v);
        }
    }





    /**
     * Cloning method
     * TODO give this a less amgiuous name to avoid conflict with subclasses that have clone methods
     */
    @NotNull
    @Override
    public final Budget clone() {
        return new UnitBudget(this);
    }



    //    /**
//     * Increase quality value by a percentage of the remaining range
//     *
//     * @param v The increasing percent
//     */
//    public void orQuality(float v) {
//        quality = or(quality, v);
//    }
//
//    /**
//     * Decrease quality value by a percentage of the remaining range
//     *
//     * @param v The decreasing percent
//     */
//    public void andQuality(float v) {
//        quality = and(quality, v);
//    }



    public void mulDurability(float factor) {
        setDurability(durability * factor);
    }


}
