package nars.perf;

import org.openjdk.jmh.profile.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

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
				// .include(".*" + YourClass.class.getSimpleName() + ".*")

				.include(include)
				 //.include(//c.getName())
				//.include(".*" + c.getSimpleName() + ".*")
				.warmupIterations(2)
				.measurementIterations(iterations)
				.measurementBatchSize(batchSize)
				.threads(1)
				.forks(1)

				.resultFormat(ResultFormatType.TEXT)
				// .verbosity(VerboseMode.EXTRA) //VERBOSE OUTPUT

				.addProfiler(StackProfiler.class,
						"lines=12;top=50;period=1;detailLine=true")

				//.addProfiler(HotspotRuntimeProfiler.class)
				//.addProfiler(HotspotMemoryProfiler.class)
				// .addProfiler(HotspotThreadProfiler.class)

				//.addProfiler(HotspotCompilationProfiler.class)
				// .addProfiler(HotspotClassloadingProfiler.class)
				//.addProfiler(LinuxPerfProfiler.class)
				/*

				 * .addProfiler(LinuxPerfAsmProfiler.class)
				 * .addProfiler(LinuxPerfNormProfiler.class)
				 */
				//.addProfiler(CompilerProfiler.class)
				// .addProfiler(GCProfiler.class)

				.timeout(TimeValue.seconds(15)).build();

		new Runner(opt).run();


	}

}
