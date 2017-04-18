package alice.tuprolog;

import org.jetbrains.annotations.Nullable;

import java.util.List;


public interface ClauseIndex extends  /*Map<String,FamilyClausesList>,*/ Iterable<ClauseInfo> {

    FamilyClausesList get(String key);
    FamilyClausesList remove(String key);

    void add(String key, ClauseInfo d, boolean first);
    @Nullable List<ClauseInfo> getPredicates(Struct headt);

    void clear();
}
