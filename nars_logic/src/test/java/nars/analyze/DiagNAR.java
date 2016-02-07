package nars.analyze;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.bag.BLink;
import nars.concept.Concept;
import nars.concept.DefaultConceptProcess;
import nars.nal.Deriver;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;

/**
 * A NAR for collecting statistics and measuring qualities
 * about reasoner activity
 */
public class DiagNAR extends Default {

    private final PrintStream out;

    public DiagNAR() throws FileNotFoundException {
        out = new PrintStream(new FileOutputStream("/tmp/t.csv"));

    }

    @Override
    public DefaultPremiseGenerator newPremiseGenerator() {
        return new DefaultPremiseGenerator(this, Deriver.getDefaultDeriver(), Global.newArrayList()) {

            @Override @NotNull
            protected DefaultConceptProcess newPremise(BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief) {
                return new DiagConceptProcess(nar, concept, taskLink, termLink, belief, sharedResultBuffer);
            }
        };
    }

    class DiagConceptProcess extends DefaultConceptProcess {

        public DiagConceptProcess(NAR nar, BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief, Collection<Task> sharedResultBuffer) {
            super(nar, concept, taskLink, termLink, belief, sharedResultBuffer);
        }

        @Override
        protected void commit() {
            onPremiseComplete(this, results);
            super.commit();
        }
    }

    final MutableTask nullTask = new MutableTask($.$("(null)"), '.');
    final MutableTask nothing = new MutableTask($.$("(nothing)"), '.');

    private void onPremiseComplete(DiagConceptProcess p, Collection<Task> c) {
        Task task = p.task();
        Task belief = p.belief();

        Term beliefTerm = p.beliefTerm().term();

        if (belief == null)
            belief = nullTask;

        if (c.isEmpty()) {
            derive(task, beliefTerm, belief, nothing);
        }
        else {
            final Task finalBelief = belief;
            c.forEach(t -> {
                derive(task, beliefTerm, finalBelief, t);
            });
        }

        //1. associate the premise with the rules that produced its results
        //2. analyze any duplicate or redundant tasks, and what rules may be overlapping
        //3. record budget input vs. generated, confidence input vs. generated, etc
        //      compute budget inflation and confidence loss/gain
    }

    private void derive(Task task, Term beliefTerm, Task belief, Task c) {
        String line = task + "\t" + beliefTerm + "\t" + belief + "\t" + c;
        out.println(line);
        System.out.println(line);
    }

    public static void main(String[] args) throws Exception {
        DiagNAR d = new DiagNAR();
        d.input("a:b.");
        d.input("b:c.");
        d.run(1000);

    }
}
