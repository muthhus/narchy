/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.util.signal;

import java.util.function.DoubleSupplier;

/**
 *
 * @author me
 */
@FunctionalInterface
public interface FloatSupplier extends DoubleSupplier {

    float asFloat();
    
    default double getAsDouble() { return asFloat(); }

}
