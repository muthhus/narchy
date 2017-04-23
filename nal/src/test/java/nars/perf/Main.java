package nars.perf;

import org.openjdk.jmh.profile.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.Collection;

/**
 * Created by me on 12/11/15.
 */
public enum Main {
	;

	public static void perf(Class c, int iterations, int batchSize) throws RunnerException {
		perf(c.getSimpleName(), iterations, batchSize);
	}

	public static void perf(String include, int iterations, int batchSize) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(include)
				.shouldDoGC(true)
				.warmupIterations(0)
				.measurementIterations(iterations)
				.measurementBatchSize(batchSize)
				.threads(1)
				.forks(1)
				.resultFormat(ResultFormatType.TEXT)
				.verbosity(VerboseMode.EXTRA) //VERBOSE OUTPUT

			    .addProfiler(StackProfiler2.class)

//			    .addProfiler(PausesProfiler.class, "period=10" /*uS */)
//        		.addProfiler(SafepointsProfiler.class)

//				.addProfiler(StackProfiler.class,
//			 "lines=10;top=10;period=3;detailLine=true;excludePackages=true" +
//					";excludePackageNames=java., jdk., javax., sun., " +
//					 "sunw., com.sun., org.openjdk.jmh."
//				)
				 //.addProfiler(GCProfiler.class)

				//.addProfiler(HotspotRuntimeProfiler.class)
				//.addProfiler(HotspotMemoryProfiler.class)
				//.addProfiler(HotspotThreadProfiler.class)

				//.addProfiler(HotspotCompilationProfiler.class)
				// .addProfiler(HotspotClassloadingProfiler.class)

				// sudo sysctl kernel.perf_event_paranoid=0
//				.addProfiler(LinuxPerfProfiler.class)
//			    .addProfiler(LinuxPerfAsmProfiler.class)
//				.addProfiler(LinuxPerfNormProfiler.class)

				//.addProfiler(CompilerProfiler.class)


				.timeout(TimeValue.seconds(100))

				.build();

		Collection<RunResult> result = new Runner(opt).run();
		result.forEach(r -> {
			r.getSecondaryResults().forEach((k,v)->{
				if (v instanceof StackProfiler2.StackResult) {

				}
//				if (v instanceof StackProfiler.StackResult) {
//					StackProfiler.StackResult s = (StackProfiler.StackResult)v;
//					s.getStack(Thread.State.RUNNABLE)
//				}
			});
			System.out.println(r.getPrimaryResult().getClass());
//			System.out.println(r.getBenchmarkResults().forEach );
//			System.out.println(r.getAggregatedResult());
		});


	}

}
