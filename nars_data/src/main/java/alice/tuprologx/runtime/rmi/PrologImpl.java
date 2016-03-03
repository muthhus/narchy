package alice.tuprologx.runtime.rmi;
import alice.tuprolog.*;
import  java.io.*;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

@SuppressWarnings("serial")
public class PrologImpl extends UnicastRemoteObject
    implements alice.tuprologx.runtime.rmi.Prolog, Serializable {

   private alice.tuprolog.Prolog imp;

    public PrologImpl() throws RemoteException {
        try {
            imp=new alice.tuprolog.Prolog();
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void clearTheory() throws RemoteException {
        imp.clearTheory();
    }

    @Override
    public Theory getTheory() throws RemoteException{
        return imp.getTheory();
    }

    @Override
    public void setTheory(Theory theory) throws InvalidTheoryException, RemoteException {
        imp.setTheory(theory);
    }

    @Override
    public void addTheory(Theory theory) throws InvalidTheoryException, RemoteException {
        imp.addTheory(theory);
    }


    @Override
    public SolveInfo   solve(Term g) throws RemoteException {
        return imp.solve(g);
    }

    @Override
    public SolveInfo   solve(String g) throws MalformedGoalException, RemoteException{
        return imp.solve(g);
    }

    @Override
    public boolean hasOpenAlternatives() throws java.rmi.RemoteException {
        return imp.hasOpenAlternatives();
    }

    @Override
    public SolveInfo   solveNext() throws NoMoreSolutionException, RemoteException {
        return imp.solveNext();
    }

    @Override
    public void solveHalt() throws RemoteException {
        imp.solveHalt();
    }

    @Override
    public void solveEnd() throws RemoteException{
        imp.solveEnd();
    }


    @Override
    public void loadLibrary(String className) throws InvalidLibraryException, RemoteException {
        imp.loadLibrary(className);
    }

    @Override
    public void unloadLibrary(String className) throws InvalidLibraryException, RemoteException {
        imp.unloadLibrary(className);
    }

}
