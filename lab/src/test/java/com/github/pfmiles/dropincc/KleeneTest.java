/*******************************************************************************
 * Copyright (c) 2012 pf_miles.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     pf_miles - initial API and implementation
 ******************************************************************************/
package com.github.pfmiles.dropincc;

import junit.framework.TestCase;

import com.github.pfmiles.dropincc.testhelper.IccActionManager;

/**
 * Complex kleene nodes situation tests.
 * 
 * @author pf-miles
 * 
 */
public class KleeneTest extends TestCase {

    // NBK - NBK
    /**
     * <pre>
     * S ::= A $
     * A ::= (a (b d)+ c)*
     *     | (e (g h)? f)+
     * </pre>
     */
    public void test1() {
        IccActionManager mgr = new IccActionManager();
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();

        lang.when(A, CC.EOF).then(mgr.newCheck(1, 2));
        A.when(CC.ks("a", CC.kc("b", "d")), "c").then(mgr.newCheck(0, 0)).orWhen(CC.kc("e", CC.op("g", "h"), "f")).then(mgr.newCheck(1, 2));

        Exe exe = lang.compile();
        exe.eval("efeghf");

        mgr.checkFinalCounts();
    }

    // NBK - BK
    /**
     * <pre>
     * S ::= A $
     * A ::= (a (b C e)+ b C d)*
     * C ::= f
     *     | g C h
     * </pre>
     */
    public void test2() {
        IccActionManager mgr = new IccActionManager();
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();
        Grule C = lang.rule();

        lang.when(A, CC.EOF).then(mgr.newCheck(1, 2));
        A.when(CC.ks("a", CC.kc("b", C, "e"), "b", C, "d")).then(mgr.newCheck(1, 2));
        C.when("f").then(mgr.newCheck(7, -1)).orWhen("g", C, "h").then(mgr.newCheck(11, 3));

        Exe exe = lang.compile();
        exe.eval("abfebgfhebfdabggfhhebgggfhhhebggggfhhhhebgfhd");

        mgr.checkFinalCounts();
    }

    // BK - NBK
    /**
     * <pre>
     * S ::= A $
     * A ::= (B (e f)+ d)* B c
     * B ::= g
     *     | h B i
     * </pre>
     */
    public void test3() {
        IccActionManager mgr = new IccActionManager();
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();
        Grule B = lang.rule();

        lang.when(A, CC.EOF).then(mgr.newCheck(1, 2));

        A.when(CC.ks(B, CC.kc("e", "f"), "d"), B, "c").then(mgr.newCheck(1, 3));

        B.when("g").then(mgr.newCheck(4, -1)).orWhen("h", B, "i").then(mgr.newCheck(7, 3));

        Exe exe = lang.compile();

        exe.eval("hgiefdhhgiiefefdhhhgiiiefefefdhgic");
        mgr.checkFinalCounts();
    }

    // BK-BK
    /**
     * <pre>
     * S ::= A $
     * A ::= (B (B c)? B d)+ B c
     * B ::= e
     *     | f B g
     * </pre>
     */
    public void test4() {
        IccActionManager mgr = new IccActionManager();
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();
        Grule B = lang.rule();

        lang.when(A, CC.EOF).then(mgr.newCheck(1, 2));
        A.when(CC.kc(B, CC.op(B, "c"), B, "d"), B, "c").then(mgr.newCheck(1, 3));
        B.when("e").then(mgr.newCheck(6, -1)).orWhen("f", B, "g").then(mgr.newCheck(15, 3));

        Exe exe = lang.compile();
        exe.eval("efegdffeggfffegggcffffeggggdfffffegggggc");

        mgr.checkFinalCounts();
    }

    /**
     * <pre>
     * S ::= A $
     * A ::= (B (B c)? B d)+ B c
     * B ::= e
     *     | f B g
     * </pre>
     */
    public void test4b1() {
        IccActionManager mgr = new IccActionManager();
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();
        Grule B = lang.rule();

        lang.when(A, CC.EOF).then(mgr.newCheck(1, 2));
        A.when(CC.kc(B, CC.op(B, "c"), B, "d"), B, "c").then(mgr.newCheck(1, 3));
        B.when("e").then(mgr.newCheck(3, -1)).orWhen("f", B, "g").then(mgr.newCheck(1, 3));

        Exe exe = lang.compile();
        exe.eval("eedfegc");

        mgr.checkFinalCounts();
    }

    /**
     * <pre>
     * S ::= A $
     * A ::= (a b)*
     * </pre>
     */
    public void testKsNoMatch() {
        IccActionManager mgr = new IccActionManager();
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();

        lang.when(A, CC.EOF).then(mgr.newCheck(1, 2));
        A.when(CC.ks("a", "b")).then(mgr.newCheck(1, 0));

        Exe exe = lang.compile();
        exe.eval("");

        mgr.checkFinalCounts();
    }

    /**
     * <pre>
     * S ::= A $
     * A ::= (a b)?
     * </pre>
     */
    public void testOpNoMatch() {
        IccActionManager mgr = new IccActionManager();
        Grammar lang = new Grammar("Test");
        Grule A = lang.rule();
        lang.when(A, CC.EOF).then(mgr.newCheck(1, 2));
        A.when(CC.op("a", "b")).then(mgr.newCheck(1, -2));
    }
}
