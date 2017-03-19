package nars.experiment;

import com.google.common.collect.Lists;
import jcog.bag.PLink;
import jcog.data.MutableIntRange;
import jcog.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.control.DefaultConceptBagControl;
import nars.derive.DefaultDeriver;
import nars.nar.Default;
import nars.premise.Premise;
import nars.task.DerivedTask;
import nars.test.agent.Line1DSimplest;
import org.apache.lucene.search.FieldComparator;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nars.NAgentX.runRT;

/**
 * Created by me on 3/15/17.
 */
public class Line1D {

    public static void main(String[] args) {
        //Param.DEBUG = true;


        NAR nar = runRT((NAR n) -> {

            //n.setControl(new InteractiveFirer(n));

            //n.log();

            n.onTask(x -> {
                if (x.isGoal() && !x.isInput()) {
                    System.err.println(x.proof());
                }
            });

            n.termVolumeMax.setValue(24);

            Line1DSimplest a = new Line1DSimplest(n);
            n.onCycle(() -> {
                a.i.setValue( 0.5f * (Math.sin(n.time()/350f) + 1f) );
            });
            return a;

        }, 32, 8, -1);

    }

    private static class InteractiveFirer extends DefaultConceptBagControl.DirectConceptBagControl {

        private Premise premise;

        public InteractiveFirer(NAR n) {
            super(n, ((Default) n).newConceptBag(1024), ((Default) n).newPremiseBuilder());
        }

        final Set<Task> derived = new HashSet(1024);

        @Override
        protected synchronized void cycle() {

            new PremiseMatrix(1, 1, new MutableIntRange(1,1)).accept(nar);

            if (!derived.isEmpty()) {
                System.out.println(premise);

                List<Task> l = new FasterList(derived);
                l.sort((a, b)->{
                   int x = Float.compare(b.budget().pri(), a.pri());
                   if (x == 0)
                       return 1;
                   else
                       return x;
                });

                derived.clear();

                for (Task x : l) {
                    System.out.println("\t" + x);
                }
                try {
                    System.in.read();
                } catch (IOException e) {

                }
            }
        }



        @Override
        public void accept(DerivedTask derivedTask) {
            //nar.input(derivedTask);
            premise = derivedTask.premise;
            derived.add(derivedTask);
        }
    }

}
