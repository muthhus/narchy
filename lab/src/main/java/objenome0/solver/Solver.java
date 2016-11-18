/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package objenome0.solver;

import objenome0.Multitainer;
import objenome0.problem.Problem;

import java.util.Map;

/**
 *
 * @author me
 */
public interface Solver {

    void solve(Multitainer g, Map<Problem, Solution> p, Object[] targets);
    
}
