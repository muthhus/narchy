/*
 * Parameters.java
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
package nars;


import com.gs.collections.impl.map.mutable.UnifiedMap;
import com.gs.collections.impl.set.mutable.UnifiedSet;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.util.Util;
import nars.util.data.list.FasterList;
import nars.util.data.map.UnifriedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Global NAR operating parameters (static scope)
 * Contains many static values which will eventually be migrated to
 * dynamic parameters specific to a component (allowing independent control).
 * (They began here for developmentconvenience)
 *
 */
public enum Global {
    ;


    //TODO use 'I' for SELf, it is 3 characters shorter
    public static final Atom DEFAULT_SELF = $.the("I");


    public static int DEFAULT_NAL_LEVEL = 8;


    public static boolean EXIT_ON_EXCEPTION = true;


    /** use this for advanced error checking, at the expense of lower performance.
        it is enabled for unit tests automatically regardless of the value here.    */
    public static boolean DEBUG;

    ///** extra debugging checks */
    //public static final boolean DEBUG_PARANOID = false;

    //public static final boolean DEBUG_BAG_MASS = false;
    //public static boolean DEBUG_TRACE_EVENTS = false; //shows all emitted events
    //public static boolean DEBUG_DERIVATION_STACKTRACES; //includes stack trace in task's derivation rule string
    //public static boolean DEBUG_INVALID_SENTENCES = true;
    //public static boolean DEBUG_NONETERNAL_QUESTIONS = false;
    public static boolean DEBUG_TASK_LOG = true; //false disables task history completely
    //public static boolean PRINT_DUPLICATE_DERIVATIONS = false;
    //public static final boolean DEBUG_DERIVATION_GRAPH = false;
    //public static final boolean DEBUG_REMOVED_CYCLIC_DERIVATIONS = false;
    //public static final boolean DEBUG_REMOVED_INSUFFICIENT_BUDGET_DERIVATIONS = false;
    //public static boolean DEBUG_DETECT_DUPLICATE_RULES;
    //public static final boolean DEBUG_NON_INPUT_ANSWERED_QUESTIONS = false;


    //static public final float maxForgetPeriod = 200f; //TODO calculate based on budget epsilon etc
    //static public final float minForgetPeriod = 0.75f; //TODO calculate based on budget epsilon etc



    /** Evidential Horizon, the amount of future evidence to be considered (during revision).
     * Must be >=1.0, usually 1 .. 2
     */
    public static final float HORIZON = 1f;

    public static final float TRUTH_EPSILON = 0.01f;
    public static final int TRUTH_DISCRETION = (int)(1f/Global.TRUTH_EPSILON);

    /** how precise unit test results must match expected values to pass */
    public static final float TESTS_TRUTH_ERROR_TOLERANCE = TRUTH_EPSILON;



    public static final int MAX_VARIABLE_CACHED_PER_TYPE = 16;

    
    /* ---------- avoiding repeated reasoning ---------- */
        /** Maximum length of the evidental base of the Stamp, a power of 2 */
    public static final int STAMP_MAX_EVIDENCE = 10;



    /** hard upper-bound limit on Compound term complexity;
     * if this is exceeded it may indicate a recursively
     * malformed term due to a serious inference bug */
    public static final int compoundVolumeMax = 192;


    /**
     * maximum changes logged in deriver's stack
     */
    public final static int UnificationStackMax = 72;

    /**
     * max # of chained termutes which can be active
     */
    public final static int UnificationTermutesMax = 16;


    /** lower limit for # of termutations derived, determined by premise's priority */
    public static float matchTermutationsMin = 1;

    /** upper limit for # of termutations derived, determined by premise's priority */
    public static float matchTermutationsMax = 3;

    public static int QUERY_ANSWERS_PER_MATCH = 1;


    /** smallest non-zero sub-cycle time measurement; # of cycles per frame should not exceed 1 / SUBFRAME_EPSILON */
    public static float SUBFRAME_EPSILON = 0.0001f;


    /** permute certain rules backward to questions (experimental, generates a lot of questions) */
    public static boolean BACKWARD_QUESTIONS = true;



    /** minimum difference necessary to indicate a significant modification in budget float number components */
    public static final float BUDGET_EPSILON = 0.0005f;




    /** minimum durability and quality necessary for a derivation to form */
    public static final float DERIVATION_DURABILITY_THRESHOLD = BUDGET_EPSILON*2f;

    /** relates time and evidence */
    public static final float DEFAULT_TEMPORAL_HISTORY_FACTOR = 2f;

    public static boolean REDUCE_TRUTH_BY_TEMPORAL_DISTANCE = false;

    public static int AUTO_CONCEPTUALIZE_DURING_LINKING_COMPLEXITY_THRESHOLD = 32;

    /** for large caches this should be false */
    public static boolean TERMLINKS_LINK_TO_CONCEPTS_IF_POSSIBLE = false;


    @NotNull
    public static <K,V> Map<K, V> newHashMap() {
        return newHashMap(0);
    }

    @NotNull
    public static <K, V> Map<K,V> newHashMap(int capacity) {
        //return new UnifiedMap(capacity);
        return new UnifriedMap(capacity /*, loadFactor */);

        //return new FasterHashMap(capacity);
        //return new FastMap<>(); //javolution http://javolution.org/apidocs/javolution/util/FastMap.html
        //return new HashMap<>(capacity); //doesn't work here, possiblye due to null value policy
        //return new LinkedHashMap(capacity);
    }

    /** copy */
    @NotNull
    public static <X,Y> Map<X, Y> newHashMap(Map<X, Y> xy) {
        //return new UnifriedMap(xy);
        return new UnifiedMap(xy);
        //return new HashMap(xy);
    }

    @NotNull
    public static <X> List<X> newArrayList() {
        return new FasterList<>(); //GS
        //return new ArrayList();
    }

    @NotNull
    public static <X> List<X> newArrayList(int capacity) {
        return new FasterList(capacity);
        //return new ArrayList(capacity);
    }

    @NotNull
    public static <X> Set<X> newHashSet(int capacity) {
        if (capacity < 2) {
            return new UnifiedSet(0);
        } else {
            //return new UnifiedSet(capacity);
            //return new SimpleHashSet(capacity);
            return new HashSet(capacity);
            //return new LinkedHashSet(capacity);
        }
    }

    @NotNull
    public static <X> Set<X> newHashSet(@NotNull Collection<X> values) {
        Set<X> s = newHashSet(values.size());
        s.addAll(values);
        return s;
    }


    @Nullable
    public static <C> Reference<C> reference(@Nullable C s) {
        return s == null ? null :
                //new SoftReference<>(s);
                new WeakReference<>(s);
                //Global.DEBUG ? new SoftReference<>(s) : new WeakReference<>(s);
    }


    @Nullable
    public static <C> Reference<C>[] reference(@Nullable C[] s) {
        int l = Util.lastNonNull(s);
        if (l > -1) {
            l++;
            Reference<C>[] rr = new Reference[l];
            for (int i = 0; i < l; i++) {
                rr[i] = reference(s[i]);
            }
            return rr;
        }
        return null;
    }

    public static void dereference(@NotNull Reference[] p) {
        for (int i = 0; i < p.length; i++) {
            Reference x = p[i];
            if (x != null)
                x.clear();
            p[i] = null;
        }
    }

    @Nullable
    public static <C> C dereference(@Nullable Reference<C> s) {
        return s == null ? null : s.get();
    }

    @Nullable
    public static <C> C dereference(@Nullable Reference<C>[] s, int index) {
        if (s == null || index >= s.length) return null;
        return dereference(s[index]);
    }





    //TODO eventually sort out in case that a parameter is not needed anymore
//
//    public static float CURIOSITY_BUSINESS_THRESHOLD=0.15f; //dont be curious if business is above
//    public static float CURIOSITY_PRIORITY_THRESHOLD=0.3f; //0.3f in 1.6.3
//    public static float CURIOSITY_CONFIDENCE_THRESHOLD=0.8f;
//    public static float CURIOSITY_DESIRE_CONFIDENCE_MUL=0.1f; //how much risk is the system allowed to take just to fullfill its hunger for knowledge?
//    public static float CURIOSITY_DESIRE_PRIORITY_MUL=0.1f; //how much priority should curiosity have?
//    public static float CURIOSITY_DESIRE_DURABILITY_MUL=0.3f; //how much durability should curiosity have?
//    public static boolean CURIOSITY_FOR_OPERATOR_ONLY=false; //for Peis concern that it may be overkill to allow it for all <a =/> b> statement, so that a has to be an operator
//    public static boolean CURIOSITY_ALSO_ON_LOW_CONFIDENT_HIGH_PRIORITY_BELIEF=true;
//
//    //public static float HAPPY_EVENT_HIGHER_THRESHOLD=0.75f;
//    public static float HAPPY_EVENT_CHANGE_THRESHOLD =0.01f;
//    //public static float BUSY_EVENT_HIGHER_THRESHOLD=0.9f; //1.6.4, step by step^, there is already enough new things ^^
//    public static float BUSY_EVENT_CHANGE_THRESHOLD =0.5f;
//    public static boolean REFLECT_META_HAPPY_GOAL = false;
//    public static boolean REFLECT_META_BUSY_BELIEF = false;
//    public static boolean CONSIDER_REMIND=true;

//
//    public static boolean QUESTION_GENERATION_ON_DECISION_MAKING=true;
//    public static boolean HOW_QUESTION_GENERATION_ON_DECISION_MAKING=true;
//
//    public static float ANTICIPATION_CONFIDENCE=0.95f;



    public static boolean ensureValidVolume(@NotNull Term derived) {

        //HARD VOLUME LIMIT
        boolean valid = derived.volume() <= Global.compoundVolumeMax;
        if (!valid && Global.DEBUG) {
            //$.logger.error("Term volume overflow");
                /*c.forEach(x -> {
                    Terms.printRecursive(x, (String line) ->$.logger.error(line) );
                });*/

            $.logger.warn("Derivation explosion: {}", derived/*, rule*/);

            //System.err.println(m.premise.task().explanation());
            //System.err.println( (m.premise.belief()!=null) ? m.premise.belief().explanation() : "belief: null");
            //System.exit(1);
            //throw new RuntimeException(message);
            return false;
        }

        return valid;

    }



}

