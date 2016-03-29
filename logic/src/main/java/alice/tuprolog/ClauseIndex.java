package alice.tuprolog;

import java.util.List;


public interface ClauseIndex extends  /*Map<String,FamilyClausesList>,*/ Iterable<ClauseInfo> {

    FamilyClausesList get(String key);
    FamilyClausesList remove(String key);

    void add(String key, ClauseInfo d, boolean first);
    List<ClauseInfo> getPredicates(Term headt);

    void clear();
}
