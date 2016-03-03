package alice.tuprolog;

public interface ISolution<Q,S,T> {
    
    <Z extends T> Z agetVarValue(String varName) throws alice.tuprolog.NoSolutionException;

    <Z extends T> Z getTerm(String varName) throws alice.tuprolog.NoSolutionException, UnknownVarException ;

    boolean isSuccess();

    boolean isHalted();

    boolean hasOpenAlternatives();

    S getSolution() throws NoSolutionException;

    Q getQuery();

    java.util.List<? extends T> getBindingVars() throws alice.tuprolog.NoSolutionException;
}
