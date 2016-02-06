/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.op.in;

import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.Narsese;
import nars.task.Task;
import nars.task.flow.TaskQueue;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * Access to library of examples/unit tests
 * TODO use getClass().getResource ?
 */
public class LibraryInput extends TextInput {

    private String input;

    public static final String[] directories =
            {
                    "test1", "test2", "test3", "test4", "test4/depr", "test5", "test5/depr", "test6", "test7", "test8",
                    "other",
                    "app/testchamber", "app/pattern_matching1", "app/metacat"
            };


    public static Iterable<String> getUnitTestPaths() {
        return getUnitTests().values();
        //Collection<String> v = getUnitTests().values();
        /*return Iterables.transform(v, new Function<String, String>() {

            @Override
            public String apply(String f) {
                return (String)((Object[])f)[0];
            }
            
        });*/
    }


    protected LibraryInput(@NotNull NAR n, @NotNull String path) throws IOException {
        super(n, FileInput.load(path));
    }
    
    @NotNull
    public static LibraryInput get(@NotNull NAR n, @NotNull String id) throws Exception {
        if (!id.endsWith(".nal"))
            id = id + ".nal";

        String path = getExamplePath(id);
                
        return new LibraryInput(n, path);
    }

    static final String cwd;

    static {
        Path currentRelativePath = Paths.get("");
        cwd = currentRelativePath.toAbsolutePath().toString();
    }

    @NotNull
    public static String getExamplePath(@NotNull String path) {
        if (path.length() > 0 && path.charAt(0) == '/') return path; //dont modify, it's already absolute
        return cwd.endsWith("nars_logic") || cwd.endsWith("nars_lab") ? "../nal/" + path : "nal/" + path;
    }
    
//    public List<OutputCondition> enableConditions(NAR n, int similarResultsToSave) {
//        return OutputCondition.getConditions(n, input, similarResultsToSave);
//    }

    @NotNull
    public static Map<String,String> getUnitTests() {
        return getUnitTests("test", "Examples/DecisionMaking", "Examples/ClassicalConditioning");
    }


    @NotNull
    public static Map<String,String> getUnitTests(@NotNull String... directories) {
        Map<String,String> l = new TreeMap();

        for (String dir : directories ) {

            String se = getExamplePath(dir);
            File folder = new File(se);
            File[] files = folder.listFiles();
            if (files == null) {
                System.err.println(folder.getAbsoluteFile() + " is not a directory or does not exist");
                break;
            }

            File[] ff = folder.listFiles();
            if (ff == null)
                throw new RuntimeException(se + " not found");

            for (File file : ff) {
                if (file.isDirectory() || "README.txt".equals(file.getName()) || file.getName().contains(".png"))
                    continue;
                if(!("extra".equals(file.getName()))) {
                    l.put(file.getName(), file.getAbsolutePath() );
                }
            }
            
        }
        return l;
    }

    @Override
    protected int process(@NotNull NAR nar, String input) {
        int n = super.process(nar, input);
        this.input = input;
        return n;
    }


    public String getSource() {
        return input;
    }

    @NotNull
    protected static Map<String, String> examples = new HashMap(); //path -> script data

    static final Function<? super String, CharSequence> lineFilter = _line -> {
        String line = _line.trim();
        if (line.startsWith("IN:"))
            line = line.replace("IN:", "");
        if (line.startsWith("OUT:"))
            return "";
        return line;
    };

    public static String getExample(@NotNull String path) {
        try {
            String existing = examples.get(path);
            if (existing!=null)
                return existing;

            existing = FileInput.load(new File(path), lineFilter );

            examples.put(path, existing);
            return existing;
        } catch (Exception ex) {
            throw new RuntimeException("Example file not found: " + path + ": " + ex + ": " + " ./=" + new File(".").getAbsolutePath());
        }
    }

    @NotNull
    public static Collection<String> getPaths(String... directories) {
        Map<String, String> et = LibraryInput.getUnitTests(directories);
        Collection<String> t = et.values();
        return t;
    }

//    public static Collection getParams(String directories, NARSeed... builds) {
//        return getParams(new String[] { directories }, builds);
//    }
//
//    //@Parameterized.Parameters(name="{1} {0}")
//    public static Collection getParams(String[] directories, NARSeed... builds) {
//        Collection<String> t = getPaths(directories);
//
//        Collection<Object[]> params = new ArrayList(t.size() * builds.length);
//        for (String script : t) {
//            for (NARSeed b : builds) {
//                params.add(new Object[] { b, script });
//            }
//        }
//        return params;
//    }

    @NotNull
    public static List<Object[]> rawTasks(String script) {

        List<Object[]> rr = Global.newArrayList();

        Narsese.tasksRaw(script, rr::add);

        return rr;
    }

    @NotNull
    public static List<Task> getExample(@NotNull List<Object[]> raw, @NotNull Memory m) {

        List<Task> y = Global.newArrayList(raw.size());
        for (Object o : raw) {
            if (o instanceof Task) y.add((Task)o);
            else {
                Object[] z = (Object[])o;
                y.add(Narsese.decodeTask(m, z));
            }
        }
        return y;
    }

    @NotNull
    public static TaskQueue getExampleInput(@NotNull List raw, @NotNull Memory m) {
        return new TaskQueue(getExample(raw, m));
    }
}
