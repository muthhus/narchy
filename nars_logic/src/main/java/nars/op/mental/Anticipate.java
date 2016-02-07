/*
 * Believe.java
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


package nars.op.mental;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.task.MutableTask;
import nars.task.Task;
import nars.task.Temporal;
import nars.term.Compound;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 something expected did not happen
 anticipation will generate a negative event as consequence
 */
public final class Anticipate {

    public float DEFAULT_CONFIRMATION_EXPECTATION = 0.51f;

    /** max differnce in frequency value to be considered a match */
    final static float FREQ_TOLERANCE = 0.25f;

    final Multimap<Compound,Task> anticipations = LinkedHashMultimap.create();

    @NotNull
    private final NAR nar;

//    private static final boolean debug = false;
    //private long nextUpdateTime = -1;

    /** called each cycle to update calculations of anticipations */
    int happeneds = 0, didnts = 0;

//    public static boolean testing = false;
//    public static String teststring = "";


    final List<Task> toRemove = Global.newArrayList();


    public Anticipate(@NotNull NAR nar) {
        this.nar = nar;

        nar.memory.eventFrameStart.on(c -> updateAnticipations());
        nar.memory.eventTaskProcess.on(this::onInput);
    }

    public void onInput(@NotNull Task t) {
        if (((Temporal)t).isAnticipated()) {
            anticipate(t);
            if (t.isInput())
                mayHaveHappenedAsExpected(t);
        }
    }

    public void anticipate(@NotNull Task t) {

        if (t.expectation() < DEFAULT_CONFIRMATION_EXPECTATION) {
            return;
        }

        Compound tt = t.term();
        if(tt.op().isConjunctive()) { //not observable, TODO probably revise
            return;
        }

        long now = nar.time();

        if (now > t.occurrence()) //its about the past
            return;

//        if (debug)
//            System.err.println("Anticipating " + tt + " in " + (t.getOccurrenceTime() - now));

        //        if(testing) {
//            String s = "anticipating: "+taskTime.task.getTerm().toString();
//            System.out.println(s);
//            teststring += s + "\n";
//        }

        anticipations.put(tt, t);

    }

    protected void deriveDidntHappen(@NotNull Compound prediction, @NotNull Task tt) {

//        if(testing) {
//            String s = "did not happen: " + prediction.toString();
//            System.out.println(s);
//            teststring += s + "\n";
//        }

        long expectedOccurrenceTime = tt.occurrence();

        //it did not happen, so the time of when it did not
        //happen is exactly the time it was expected

//        if (debug)
//            System.err.println("Anticipation Negated " + tt.task);

        nar.input(new MutableTask($.neg(prediction))
                .belief()
                .truth(nar.memory.getTruthDefault(Symbols.BELIEF))
                .time(nar.time(), expectedOccurrenceTime)
                //.parent(tt, null)
                .because("Absent Anticipated Event"));
    }



    protected void mayHaveHappenedAsExpected(@NotNull Task c) {

        if(!c.isInput() || c.isEternal()) {
            return; //it's not a input task, the system is not allowed to convince itself about the state of affairs ^^
        }

        float freq = c.freq();
        long cOccurr = c.occurrence();

        final List<Task> toRemove = this.toRemove;

        int tolerance = nar.memory.duration();

        Multimap<Compound, Task> a = this.anticipations;
        Compound ct = c.term();

        a.get(ct).stream().filter(tt -> inTime(freq, tt, cOccurr, tolerance)).forEach(tt -> {
            toRemove.add(tt);
            happeneds++;
        });

        toRemove.forEach(tt -> a.remove(ct,tt));
        toRemove.clear();

    }

    protected void updateAnticipations() {

        if (anticipations.isEmpty()) return;

        long now = nar.memory.time();

        Iterator<Map.Entry<Compound, Task>> it = anticipations.entries().iterator();

        int halfDur = nar.memory.duration()/2;
        while (it.hasNext()) {

            Map.Entry<Compound, Task> t = it.next();
            Compound term = t.getKey();
            Task tt = t.getValue();

            if (tooLate(tt, now, halfDur)) {
                deriveDidntHappen(term, tt);
                it.remove();
                didnts++;
            }
        }

//        if (debug)
//            System.err.println(now + ": Anticipations: pending=" + anticipations.size() + " happened=" + happeneds + " , didnts=" + didnts);
    }


    public static boolean tooLate(Task t, long occur, int TOLERANCE_DIV) {
        return occur > t.occurrence() + TOLERANCE_DIV;
    }

    public static boolean inTime(float incomingFreq, Task existing, long incomingOccurr, int TOLERANCE_DIV) {
        long occurrTime= existing.occurrence();
        return
                Util.equal(incomingFreq, existing.freq(), FREQ_TOLERANCE) &&
                //within time tolerance
                (incomingOccurr > occurrTime - TOLERANCE_DIV && incomingOccurr < occurrTime + TOLERANCE_DIV);
    }

//    /** Prediction point vector / centroid of a group of Tasks
//     *      time a prediction is made (creationTime), and
//     *      tme it is expected (ocurrenceTime) */
//    public static final class TaskTime {
//
//        /** all data is from task */
//        @NotNull
//        public final Task task;
//
//        /** cached locally, same value as in task */
//        public final long occurrTime;
//        public final long creationTime;
//
//        /** cached locally, same value as in task */
//        private final int hash;
//        //public float tolerance = 0;
//
//        public TaskTime(@NotNull Task task) {
//            this.task = task;
//            long cre = this.creationTime = task.creation();
//            long occ = this.occurrTime = task.occurrence();
//            hash = (int)(31 * cre + occ);
//            //expiredate in relation how long we predicted forward
//            //long prediction_time = occ - cre;
//            //tolerance = prediction_time/TOLERANCE_DIV;
//        }
//
//
//        public float getPriority() { return task.pri(); }
//
//        @Override
//        public int hashCode() {
//            return hash;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (obj == this) return true;
//            TaskTime t = (TaskTime)obj;
//            return creationTime == t.creationTime  &&  occurrTime == t.occurrTime;
//        }
//
//
//    }
}
