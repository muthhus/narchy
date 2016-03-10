package alice.tuprolog;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


public interface ClauseIndex extends  /*Map<String,FamilyClausesList>,*/ Iterable<ClauseInfo> {

    FamilyClausesList get(String key);
    FamilyClausesList remove(String key);

    void add(String key, ClauseInfo d, boolean first);
    List<ClauseInfo> getPredicates(Term headt);

    void clear();
}
