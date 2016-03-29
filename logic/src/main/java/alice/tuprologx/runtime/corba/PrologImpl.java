package alice.tuprologx.runtime.corba;

import alice.tuprolog.InvalidLibraryException;
import alice.tuprolog.Solution;
import alice.tuprolog.Theory;

import java.io.ByteArrayInputStream;


@SuppressWarnings("serial")
public class PrologImpl extends _PrologImplBase {

    alice.tuprolog.Prolog imp;

    /**
     * construction with default libraries: ISO + Meta
     */
    public PrologImpl(){
        try {
            imp=new alice.tuprolog.Prolog(new String[]{ "alice.tuprolog.lib.MetaLibrary","alice.tuprolog.lib.ISOLibrary" });
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * construction speciying (eventually) libraries
     */
    public PrologImpl(String[] libs) throws InvalidLibraryException {
        imp=new alice.tuprolog.Prolog(libs);
    }

    /**
     * managing theory theory
     */
    @Override
    public void clearTheory(){
        imp.clearTheory();
    }

    @Override
    public String getTheory(){
        Theory th=imp.getTheory();
        return th.toString();
    }

    @Override
    public void setTheory(String st) { //throws InvalidTheoryException {
        try {
            imp.setTheory(new Theory(new ByteArrayInputStream(st.getBytes())));
        } catch (Exception e){}
    }

    @Override
    public alice.tuprologx.runtime.corba.SolveInfo solve(String g) { //throws MalformedGoalException{
        try {
            return toSolveInfo(imp.solve(g));
        } catch (Exception e){
            return null;
        }
    }

    @Override
    public boolean hasOpenAlternatives(){
        //try {
            return imp.hasOpenAlternatives();
        //} catch (Exception e){
        //    return null;
        //}
    }

    @Override
    public alice.tuprologx.runtime.corba.SolveInfo   solveNext() { //throws NoMoreSolutionException{
        try {
            return toSolveInfo(imp.solveNext());
        } catch (Exception e){
            return null;
        }
    }

    @Override
    public void solveHalt(){
        imp.solveHalt();
    }

    @Override
    public void solveEnd(){
        imp.solveEnd();
    }

    @Override
    public void loadLibrary(String className) { //throws InvalidLibraryException {
        try {
            imp.loadLibrary(className);
    } catch (Exception e){}
    }

    @Override
    public void unloadLibrary(String className) { //throws InvalidLibraryException {
        try {
            imp.unloadLibrary(className);
    } catch (Exception e){}
    }


    static alice.tuprologx.runtime.corba.SolveInfo  toSolveInfo(Solution info){
        try {
            alice.tuprologx.runtime.corba.SolveInfo cinfo=
                new alice.tuprologx.runtime.corba.SolveInfo();
            cinfo.success=info.isSuccess();
            if (info.isSuccess()){
                cinfo.solution=info.getSolution().toString();
            }
            else {
                cinfo.solution="";
            }
            return cinfo;
        } catch (Exception ex){
            return null;
        }
    }

}
