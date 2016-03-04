/*
 * SolveInfo.java
 *
 * Created on 13 marzo 2007, 12.00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.engine;

import alice.tuprolog.PTerm;
import alice.tuprolog.Solution;
import alice.tuprologx.pj.model.*;
import alice.tuprolog.UnknownVarException;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author Maurizio
 */
public class PrologSolution<Q extends Term<?>, S extends Term<?>> /*implements ISolution<Q,S,Term<?>>*/ {
    
    private final Solution _solution;
    
    /** Creates a new instance of SolveInfo */
    public PrologSolution(Solution si) {
        _solution = si;
    }

    public <Z extends Term<?>> Z getVarValue(String varName) throws alice.tuprolog.NoSolutionException {
        PTerm retValue;
        retValue = _solution.getVarValue(varName);
        return Term.unmarshal(retValue);
    }

    public <Z extends Term<?>> Z getTerm(String varName) throws alice.tuprolog.NoSolutionException, UnknownVarException {
        PTerm retValue;
        retValue = _solution.getTerm(varName);
        return Term.unmarshal(retValue);
    }

    public boolean isSuccess() {        
        return _solution.isSuccess();
    }

    public boolean isHalted() {        
        return _solution.isHalted();
    }

    public boolean hasOpenAlternatives() {        
        return _solution.hasOpenAlternatives();
    }

    public S getSolution() throws alice.tuprolog.NoSolutionException {
        PTerm retValue;
        retValue = _solution.getSolution();
        return Term.unmarshal(retValue);
    }

    public Q getQuery() {
        PTerm retValue;
        retValue = _solution.getQuery();
        return Term.unmarshal(retValue);
    }

    public List<Term<?>> getBindingVars() throws alice.tuprolog.NoSolutionException {
        List<alice.tuprolog.Var> retValue;        
        retValue = _solution.getBindingVars();
        Vector<Term<?>> bindings = new Vector<Term<?>>();
        for (PTerm t : retValue) {
            bindings.add(Term.unmarshal(t));
        }
        return bindings;
    }
}
