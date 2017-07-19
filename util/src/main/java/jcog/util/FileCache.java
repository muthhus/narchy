package jcog.util;

import jcog.Util;
import jcog.list.FasterList;
import org.slf4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FileCache {

    public static <X> Stream<X> fileCache(URL u, String baseName, Supplier<Stream<X>> o,
                                          BiConsumer<Stream<X>, DataOutput> encoder,
                                          Function<DataInput, X> decoder,
                                          Logger logger
    ) throws IOException, URISyntaxException {
        return fileCache(new File(u.toURI()), baseName, o, encoder, decoder, logger);
    }

    public static <X> Stream<X> fileCache(Path p, String baseName, Supplier<Stream<X>> o,
                                          BiConsumer<Stream<X>, DataOutput> encoder,
                                          Function<DataInput, X> decoder,
                                          Logger logger
    ) throws IOException {
        return fileCache(p.toFile(), baseName, o, encoder, decoder, logger);
    }

    public static <X> Stream<X> fileCache(File f, String baseName, Supplier<Stream<X>> o,
                                          BiConsumer<Stream<X>, DataOutput> encoder,
                                          Function<DataInput, X> decoder,
                                          Logger logger
    ) throws IOException, FileNotFoundException {

        long lastModified = f.lastModified();
        long size = f.length();
        String suffix = '_' + f.getName() + '_' + lastModified + '_' + size;

        List<X> buffer = new FasterList(1024 /* estimate */);

        String tempDir = Util.tempDir();

        File cached = new File(tempDir, baseName + suffix);
        if (cached.exists()) {
            //try read
            try {

                FileInputStream ff = new FileInputStream(cached);
                DataInputStream din = new DataInputStream(new BufferedInputStream(ff));
                while (din.available() > 0) {
                    buffer.add(decoder.apply(din));
                }
                din.close();

                logger.warn("cache loaded {}: ({} bytes, from {})", cached.getAbsolutePath(), cached.length(), new Date(cached.lastModified()));

                return buffer.stream();
            } catch (Exception e) {
                logger.warn("{}, regenerating..", e);
                //continue below
            }
        }

        //save
        buffer.clear();

        Stream<X> instanced = o.get();

        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cached.getAbsolutePath())));
        encoder.accept(instanced.peek(buffer::add), dout);
        dout.close();
        logger.warn("cache saved {}: ({} bytes)", cached.getAbsolutePath(), dout.size());

        return buffer.stream();


    }
}
