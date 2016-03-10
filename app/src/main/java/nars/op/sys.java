package nars.op;

import nars.$;
import nars.nal.Tense;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.atom.Atom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;

/**
 * Execute a sequence of commands in system shell. DANGEROUS
 */
public class sys extends TermFunction {

    final static Logger logger = LoggerFactory.getLogger(sys.class);

    @Override
    public Object function(Compound args, TermIndex i) {
        for (Term x : args.terms()) {
            if (x instanceof Atom) {
                String cmd = ((Atom) x).toStringUnquoted();

                long pid = -1;
                try {
                    Process p = Runtime.getRuntime().exec(cmd);
//                    final long pidCopy = pid =
//                            p.hashCode(); //HACK temporary
//                            //p.getPid();
                    nar.runAsync(()-> {
                        try {

                            Scanner s = new Scanner(p.getInputStream());
                            while (s.hasNextLine()) {
                                String l = s.nextLine();
                                //logger.info("{}: {}", cmd, l);
                                onInput(cmd, l);
                            }
                        /*while (s.hasNext()) {
                            String l = s.next();
                        }*/

                            int result = p.waitFor();
                        } catch (Exception e) {
                            error(cmd, e);
                        }
                    });
                } catch (IOException e) {
                    error(cmd, e);
                }

                if (pid!=-1)
                    return pid;
                else
                    return false;
            }
        }
        return null;
    }

    public void error(String cmd, Exception e) {
        logger.error("exec \"{}\": {}", cmd, e);
    }


    protected void onInput(String cmd, String line) {
        nar.believe(
            $.inst($.quote(line), $.quote(cmd)), Tense.Present,
                1f, 0.9f
                );
    }
}
