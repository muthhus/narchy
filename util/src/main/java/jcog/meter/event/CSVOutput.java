package jcog.meter.event;

import com.google.common.base.Joiner;

import java.io.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/** dead simple CSV logging */
public class CSVOutput extends PrintStream {

    //public String[] headers;

    public CSVOutput(File out, String... headers) throws FileNotFoundException {
        this(new FileOutputStream(out), headers);
    }
    public CSVOutput(OutputStream out, String... headers) {
        super(out);
        println( Joiner.on(',').join(headers) );
    }

//    public void out(float... row) {
//        println(Stream.of(row).map(String::valueOf).collect(Collectors.joining(",")));
//    }

    public void out(double... row) {
        println(DoubleStream.of(row).mapToObj(String::valueOf).collect(Collectors.joining(",")));
    }


}
