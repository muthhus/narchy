/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package nars.perf.nars.nar.perf;

import nars.Narsese;
import nars.nar.Default;
import nars.term.Compound;
import nars.test.DeductiveMeshTest;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import static nars.$.$;
import static nars.perf.Main.perf;

@State(Scope.Benchmark)
public class NARBenchmark {

    Default n;

    @Setup
    public void prepare() {
        n = new Default();
    }



    @Benchmark
    @BenchmarkMode(value = Mode.AverageTime)
    public void deductiveChainTest1() {

        //n.inputActivation.setValue(0.5f);
        //n.derivedActivation.setValue(0.5f);
        //n.nal(4);

        n.core.conceptsFiredPerCycle.setValue(64);


        new DeductiveMeshTest(n, new int[]{16, 16});
        //new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));

        n.run(1000);

        System.err.println(n.concepts.summary());

    }

//    @Benchmark
//    @BenchmarkMode(value = Mode.AverageTime)
//    public void nal1Deduction() throws Narsese.NarseseException {
//        n.nal(1);
//        Compound a = $("<a-->b>");
//        Compound b = $("<b-->c>");
//
//        n.believe(a);
//        n.believe(b);
//        n.run(10000);
//    }
//
//    @Benchmark
//    @BenchmarkMode(value = Mode.AverageTime)
//    public void nal1DeductionInNAL8() throws Narsese.NarseseException {
//        n.nal(8);
//        Compound a = $("<a-->b>");
//        Compound b = $("<b-->c>");
//
//        n.believe(a);
//        n.believe(b);
//        n.run(10000);
//    }


    public static void main(String[] args) throws RunnerException {
        perf(NARBenchmark.class, 2, 2);
    }

}
