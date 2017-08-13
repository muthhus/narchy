/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufpr.gres.core.premutation;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class PremutationClassInfo {

    private final Set<Integer> linesToAvoid = new HashSet<>();

    public void registerLineToAvoid(final int lineNumber) {
        this.linesToAvoid.add(lineNumber);

    }

    public boolean isLineToAvoid(final int line) {
        return this.linesToAvoid.contains(line);
    }
}
