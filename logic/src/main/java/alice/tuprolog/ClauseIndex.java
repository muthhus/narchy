package alice.tuprolog;

import org.jetbrains.annotations.Nullable;

import java.util.Deque;


public interface ClauseIndex extends  /*Map<String,FamilyClausesList>,*/ Iterable<ClauseInfo> {

    FamilyClausesList get(String key);
    FamilyClausesList remove(String key);

    void add(String key, ClauseInfo d, boolean first);
    @Nullable Deque<ClauseInfo> getPredicates(Struct headt);

    void clear();
}
