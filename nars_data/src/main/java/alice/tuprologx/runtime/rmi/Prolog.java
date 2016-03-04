package alice.tuprologx.runtime.rmi;
import alice.tuprolog.*;

/**
 * @author  ale
 */
public interface Prolog extends java.rmi.Remote {


    void clearTheory() throws java.rmi.RemoteException;

   Theory getTheory() throws java.rmi.RemoteException;

    /**
	 * @param theory
	 * @throws InvalidTheoryException
	 * @throws java.rmi.RemoteException
	 */
    void setTheory(Theory theory) throws InvalidTheoryException, java.rmi.RemoteException;

    void addTheory(Theory theory) throws InvalidTheoryException, java.rmi.RemoteException;


    SolveInfo   solve(PTerm g) throws java.rmi.RemoteException;

    SolveInfo   solve(String g) throws MalformedGoalException, java.rmi.RemoteException;

    boolean   hasOpenAlternatives() throws java.rmi.RemoteException;

    SolveInfo   solveNext() throws NoMoreSolutionException, java.rmi.RemoteException;

    void solveHalt() throws java.rmi.RemoteException;

    void solveEnd() throws java.rmi.RemoteException;


    void loadLibrary(String className) throws InvalidLibraryException, java.rmi.RemoteException;

    void unloadLibrary(String className) throws InvalidLibraryException, java.rmi.RemoteException;

}
