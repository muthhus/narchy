package nars.control;

import nars.NAR;
import nars.Param;
import nars.derive.PrediTerm;
import nars.derive.PrediTrie;
import nars.derive.instrument.DebugDerivationPredicate;
import nars.derive.rule.PremiseRuleSet;
import nars.index.term.PatternIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * utility class for working witih Deriver's
 */
public class Derivers {

    public static Set<String> defaultRules(int level, String... otherFiles) {
        Set<String> files = new TreeSet();
        switch (level) {
            case 8:
                //files.add("motivation.nal");
            case 7:
                //TODO move temporal induction to a separate file
                //fallthru
            case 6:
                files.add("nal6.nal");
                files.add("nal6.guess.nal");

                files.add("induction.nal");  //TODO nal6 only needs general induction, not the temporal parts

                files.add("misc.nal"); //TODO split this up
                //files.add("list.nal");  //experimental
                //fallthru
            case 5:
            case 4:
            case 3:
            case 2:
                files.add("nal3.nal");
                //files.add("nal3.guess.nal");
                files.add("nal2.nal");
                files.add("nal2.guess.nal");
                //fallthru
            case 1:
                files.add("nal1.nal");
                files.add("nal1.guess.nal");
                break;
            default:
                throw new UnsupportedOperationException();
        }

        Collections.addAll(files, otherFiles);

        return files;
    }
}
