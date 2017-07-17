package alice.tuprolog;

public interface ISolution<Q,S,T> {
    
    <Z extends T> Z agetVarValue(String varName);

    <Z extends T> Z getTerm(String varName);

    boolean isSuccess();

    boolean isHalted();

    boolean hasOpenAlternatives();

    S getSolution();

    Q getQuery();

    java.util.List<? extends T> getBindingVars();
}
