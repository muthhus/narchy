package nars.nar;

import jcog.data.random.XorShift128PlusRandom;
import nars.NAR;
import nars.concept.Concept;
import nars.index.term.TermIndex;
import nars.link.BLink;
import nars.nal.Deriver;
import nars.reason.ConceptBagReasoner;
import nars.reason.DefaultDeriver;
import nars.term.Termed;
import nars.time.FrameTime;
import nars.time.Time;
import nars.util.exe.Executioner;
import nars.util.exe.SynchronousExecutor;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.NotNull;

import java.util.Random;


public class Classic extends NAR {

    public static class NARBuilder {
//
//        Classic classic;
//
        public Classic build() {
            return new Classic(getRandom(),getIndex(), getTime(), getExec());
        }
//
//        public Atom getSelf() {
//            return (Atom) $.the("I");
//        }

        public Executioner getExec() {
            return new SynchronousExecutor();
        }

        public Time getTime() {
            return new FrameTime();
        }

        public Deriver getDeriver() {
            return new DefaultDeriver();
        }

        public TermIndex getIndex() {
            return new Default.DefaultTermTermIndex(1024);
        }

        public Random getRandom() {
            return new XorShift128PlusRandom(1);
        }

//        public NAR build(Classic c) {
//            return c;
//        }
    }

//    public static void main(String[] args) {
//        DaggerObjectGraph n = (DaggerObjectGraph) O.of(NARBuilder.class); //of(NARBuilder.class);
//
//        System.out.println(n);
//        System.out.println(n.linker.bindings);
//        System.out.println(n.linker.linkedBindings);
//        System.out.println(n.linker.pending);
//        //n.validate();
//
//        System.out.println(n.a(Classic.class));
//
//    }






    public Classic(@NotNull Random random, @NotNull TermIndex index, @NotNull Time time, @NotNull Executioner exe) {
        super(time, index, random, exe);


        /*
        int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept,
        core.active.capacity(activeConcepts);
        core.termlinksFiredPerFiredConcept.set(1, termLinksPerConcept);
        core.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);
        core.conceptsFiredPerCycle.set(conceptsFirePerCycle);
        */



    }



}
