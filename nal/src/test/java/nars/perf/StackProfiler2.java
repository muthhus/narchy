package nars.perf;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.data.sorted.SortedArray;
import jcog.list.FasterList;
import joptsimple.OptionException;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.*;
import org.openjdk.jmh.util.HashMultiset;
import org.openjdk.jmh.util.Multiset;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/**
 * Very basic and naive stack profiler.
 */
public class StackProfiler2 implements InternalProfiler {

    /**
     * Threads to ignore (known system and harness threads)
     */
    private static final String[] IGNORED_THREADS = {
            "Finalizer",
            "Signal Dispatcher",
            "Reference Handler",
            "main",
            "Sampling Thread",
            "Attach Listener"
    };

    private final int stackLines;
    private final int topStacks;
    private final int periodMsec;
    private final boolean sampleLine;
    private final Set<String> excludePackageNames;

    public StackProfiler2() throws ProfilerException {
//        OptionParser parser = new OptionParser();
//        //parser.formatHelpWith(new ProfilerOptionFormatter(org.openjdk.jmh.profile.StackProfiler.class.getCanonicalName()));

//        OptionSpec<Integer> optStackLines = parser.accepts("lines", "Number of stack lines to save in each stack trace. " +
//                "Larger values provide more insight into who is calling the top stack method, as the expense " +
//                "of more stack trace shapes to collect.")
//                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.POSITIVE).describedAs("int").defaultsTo(1);
//
//        OptionSpec<Integer> optTopStacks = parser.accepts("top", "Number of top stacks to show in the profiling results. " +
//                "Larger values may catch some stack traces that linger in the distribution tail.")
//                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.POSITIVE).describedAs("int").defaultsTo(10);
//
//        OptionSpec<Integer> optSamplePeriod = parser.accepts("period", "Sampling period, in milliseconds. " +
//                "Smaller values improve accuracy, at the expense of more profiling overhead.")
//                .withRequiredArg().withValuesConvertedBy(IntegerValueConverter.POSITIVE).describedAs("int").defaultsTo(10);
//
//        OptionSpec<Boolean> optDetailLine = parser.accepts("detailLine", "Record detailed source line info. " +
//                "This adds the line numbers to the recorded stack traces.")
//                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);
//
//        OptionSpec<Boolean> optExclude = parser.accepts("excludePackages", "Enable package filtering. " +
//                "Use excludePackages option to control what packages are filtered")
//                .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(false);
//
//        OptionSpec<String> optExcludeClasses = parser.accepts("excludePackageNames", "Filter there packages. " +
//                "This is expected to be a comma-separated list\n" +
//                "of the fully qualified package names to be excluded. Every stack line that starts with the provided\n" +
//                "patterns will be excluded.")
//                .withRequiredArg().withValuesSeparatedBy(",").ofType(String.class).describedAs("package+")
//                .defaultsTo("java.", "javax.", "sun.", "sunw.", "com.sun.", "org.openjdk.jmh.");
//
//         //
//        //Map<String, AbstractOptionSpec<?>> m;
//        OptionSet set = parseInitLine(initLine, parser);


        try {
            sampleLine = true; //set.valueOf(optDetailLine);
            periodMsec = 0; //set.valueOf(optSamplePeriod);
            topStacks = 10; //set.valueOf(optTopStacks);
            stackLines = 10; //set.valueOf(optStackLines);

            //boolean excludePackages = true; //set.valueOf(optExclude);
            excludePackageNames =
                    Sets.mutable.of("java.", "jdk.", "javax.", "sun.",
                            "sunw.", "com.sun.", "org.openjdk.jmh.");

                    /*excludePackages ?
                    new HashSet<>(set.valuesOf(optExcludeClasses)) :
                    Collections.<String>emptySet();*/
        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
    }

    private volatile SamplingTask samplingTask;

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        samplingTask = new SamplingTask();
        samplingTask.start();
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result) {
        samplingTask.stop();
        return Collections.singleton(new StackResult(samplingTask.stacks, topStacks));
    }

    @Override
    public String getDescription() {
        return "Simple and naive Java stack profiler++";
    }

    public class SamplingTask implements Runnable {

        private final Thread thread;
        private final Map<Thread.State, Multiset<StackRecord>> stacks;

        public SamplingTask() {
            stacks = new EnumMap<>(Thread.State.class);
            for (Thread.State s : Thread.State.values()) {
                stacks.put(s, new HashMultiset<StackRecord>());
            }
            thread = new Thread(this);
            thread.setName("Sampling Thread");
        }

        @Override
        public void run() {

            while (!Thread.interrupted()) {
                ThreadInfo[] infos = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false);

                info:
                for (ThreadInfo info : infos) {

                    // filter out ignored threads TODO is lowercase compare necessary?
                    switch (info.getThreadName()) {
                        case "Finalizer":
                        case "Signal Dispatcher":
                        case "Reference Handler":
                        case "main":
                        case "Sampling Thread":
                        case "Attach Listener":
                            continue info; //ignore

                    }


                    //   - Discard everything that matches excluded patterns from the top of the stack
                    //   - Get the remaining number of stack lines and build the stack record


                    StackRecord lines = new StackRecord();
                            Stream.of(info.getStackTrace())
                                    .filter(f -> !isExcluded(f.getClassName()))
                                    .limit(stackLines)
                                    .forEach(l -> lines.add( Tuples.pair(
                                            l.getClassName(),
                                            PrimitiveTuples.pair(l.getLineNumber(), l.getMethodName()) )
                                    ))
                                    ;

//                    for (StackTraceElement l : stack) {
//                        String className = l.getClassName();
//                        if (!isExcluded(className)) { //TODO use a trie
//
//                            lines.add(className + '.' + l.getMethodName()
//                                    + (sampleLine ? ":" + l.getLineNumber() : ""));
//
//                            if (lines.size() >= stackLines) {
//                                break;
//                            }
//                        }
//                    }

                    if (!lines.isEmpty()) {
                        lines.commit();
                        stacks.get(info.getThreadState()).add(lines);
                    }
                }


                try {
                    TimeUnit.MILLISECONDS.sleep(periodMsec);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        public void start() {
            thread.start();
        }

        public void stop() {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private boolean isExcluded(String className) {
            for (String p : excludePackageNames) {
                if (className.startsWith(p)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class StackRecord extends FasterList<Pair<String, IntObjectPair<String>>> {
        private static final long serialVersionUID = -1829626661894754733L;

        private int hash;

        public StackRecord() {
            super();
        }

        public void commit() {
            this.hash = super.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass() || hash != o.hashCode()) return false;
            return super.equals(((StackRecord) o));
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static class StackResult extends Result<StackResult> {
        private static final long serialVersionUID = 2609170863630346073L;

        private final Map<Thread.State, Multiset<StackRecord>> stacks;
        private final int topStacks;

        public StackResult(Map<Thread.State, Multiset<StackRecord>> stacks, int topStacks) {
            super(ResultRole.SECONDARY, Defaults.PREFIX + "stack", of(Double.NaN), "---", AggregationPolicy.AVG);
            this.stacks = stacks;
            this.topStacks = topStacks;
        }

        @Override
        protected Aggregator<StackResult> getThreadAggregator() {
            return new StackResultAggregator();
        }

        @Override
        protected Aggregator<StackResult> getIterationAggregator() {
            return new StackResultAggregator();
        }

        @Override
        public String toString() {
            return "<delayed till summary>";
        }

        @Override
        public String extendedInfo() {
            return getStack(stacks);
        }

        public String getStack(final Map<Thread.State, Multiset<StackRecord>> stacks) {
            SortedArray<LongObjectPair<Pair<Thread.State, StackRecord>>> r = new SortedArray<>() {

                @Override
                protected LongObjectPair<Pair<Thread.State, StackRecord>>[] newArray(int oldSize) {
                    return new LongObjectPair[grow(oldSize)];
                }
            };

            stacks.entrySet().forEach( e -> {
                    e.getValue().entrySet().forEach(x -> {
                        LongObjectPair<Pair<Thread.State, StackRecord>> i = PrimitiveTuples.pair(x.getValue(), Tuples.pair(e.getKey(), x.getKey()));
                        r.add(i, z -> -z.getOne());
                    });
            });

            return Joiner.on("\n").join(r);

//            List<Thread.State> sortedStates = new ArrayList<>(stacks.keySet());
//            Collections.sort(sortedStates, new Comparator<Thread.State>() {
//
//                private long stateSize(Thread.State state) {
//                    Multiset<StackRecord> set = stacks.get(state);
//                    return (set == null) ? 0 : set.size();
//                }
//
//                @Override
//                public int compare(Thread.State s1, Thread.State s2) {
//                    return Long.valueOf(stateSize(s2)).compareTo(stateSize(s1));
//                }
//
//            });
//
//            long totalSize = getTotalSize(stacks);
//
//            StringBuilder builder = new StringBuilder();
//            builder.append("Stack profiler:\n\n");
//
//            builder.append(dottedLine("Thread state distributions"));
//            for (Thread.State state : sortedStates) {
//                if (isSignificant(stacks.get(state).size(), totalSize)) {
//                    builder.append(String.format("%5.1f%% %7s %s%n", stacks.get(state).size() * 100.0 / totalSize, "", state));
//                }
//            }
//            builder.append("\n");
//
//            for (Thread.State state : sortedStates) {
//                Multiset<StackRecord> stateStacks = stacks.get(state);
//                if (isSignificant(stateStacks.size(), totalSize)) {
//                    builder.append(dottedLine("Thread state: " + state.toString()));
//
//                    int totalDisplayed = 0;
//                    for (StackRecord s : Multisets.countHighest(stateStacks, topStacks)) {
//                        List<String> lines = s.lines;
//                        if (!lines.isEmpty()) {
//                            totalDisplayed += stateStacks.count(s);
//                            builder.append(String.format("%5.1f%% %5.1f%% %s%n",
//                                    stateStacks.count(s) * 100.0 / totalSize,
//                                    stateStacks.count(s) * 100.0 / stateStacks.size(),
//                                    lines.get(0)));
//                            if (lines.size() > 1) {
//                                for (int i = 1; i < lines.size(); i++) {
//                                    builder.append(String.format("%13s %s%n", "", lines.get(i)));
//                                }
//                                builder.append("\n");
//                            }
//                        }
//                    }
//                    if (isSignificant((stateStacks.size() - totalDisplayed), stateStacks.size())) {
//                        builder.append(String.format("%5.1f%% %5.1f%% %s%n",
//                                (stateStacks.size() - totalDisplayed) * 100.0 / totalSize,
//                                (stateStacks.size() - totalDisplayed) * 100.0 / stateStacks.size(),
//                                "<other>"));
//                    }
//
//                    builder.append("\n");
//                }
//            }
//            return builder.toString();
        }

        // returns true, if part is >0.1% of total
        private boolean isSignificant(long part, long total) {
            // returns true if part*100.0/total is greater or equals to 0.1
            return part * 1000 >= total;
        }

        private long getTotalSize(Map<Thread.State, Multiset<StackRecord>> stacks) {
            long sum = 0;
            for (Multiset<StackRecord> set : stacks.values()) {
                sum += set.size();
            }
            return sum;
        }
    }

    static String dottedLine(String header) {
        final int HEADER_WIDTH = 100;

        StringBuilder sb = new StringBuilder();
        sb.append("....");
        if (header != null) {
            header = "[" + header + "]";
            sb.append(header);
        } else {
            header = "";
        }

        for (int c = 0; c < HEADER_WIDTH - 4 - header.length(); c++) {
            sb.append(".");
        }
        sb.append("\n");
        return sb.toString();
    }

    public static class StackResultAggregator implements Aggregator<StackResult> {
        @Override
        public StackResult aggregate(Collection<StackResult> results) {
            int topStacks = 0;
            Map<Thread.State, Multiset<StackRecord>> sum = new EnumMap<>(Thread.State.class);
            for (StackResult r : results) {
                for (Map.Entry<Thread.State, Multiset<StackRecord>> entry : r.stacks.entrySet()) {
                    if (!sum.containsKey(entry.getKey())) {
                        sum.put(entry.getKey(), new HashMultiset<StackRecord>());
                    }
                    Multiset<StackRecord> sumSet = sum.get(entry.getKey());
                    for (StackRecord rec : entry.getValue().keys()) {
                        sumSet.add(rec, entry.getValue().count(rec));
                    }
                }
                topStacks = r.topStacks;
            }
            return new StackResult(sum, topStacks);
        }
    }

}
