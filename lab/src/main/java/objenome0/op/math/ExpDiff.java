/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package objenome0.op.math;

import objenome0.op.DiffableFunction;
import objenome0.op.Node;
import objenome0.op.Scalar;

/**
 * TODO
 * @author thorsten
 */
public class ExpDiff extends Exp implements DiffableFunction{

    private final DiffableFunction x;

    public ExpDiff(Node x) {
        super(x);
        this.x = (DiffableFunction) x;
    }
    
    @Override
    public double value() {
        return Math.exp(x.value());
    }

    @Override
    public double partialDerive(Scalar parameter) {
        return x.partialDerive(parameter) * Math.exp(x.value());
    }
    
}
