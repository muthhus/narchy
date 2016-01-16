package nars.task.in;

import com.google.common.io.Files;
import nars.NAR;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
    Given a filepath, loads the file as task input
 */
public class FileInput extends TextInput {


    public FileInput(@NotNull NAR p, @NotNull File input) throws IOException {
        super(p, load(input));
    }

    public static String load(@NotNull String path) throws IOException {
        return load(new File(path));
    }

    public static String load(@NotNull File file) throws IOException {
        return Files.toString(file, Charset.defaultCharset());
    }

    public static String load(@NotNull File file, Function<? super String, CharSequence> lineTransform) throws IOException {
        List<String> lines = Files.readLines(file, Charset.defaultCharset());
        return lines.stream().map(lineTransform).collect(Collectors.joining("\n"));
    }


}
