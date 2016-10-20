package nars.web;


import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import nars.util.FSWatch;
import nars.util.Util;
import org.apache.commons.io.IOUtils;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.cache.DiskCachedClassHolderSource;
import org.teavm.cache.DiskProgramCache;
import org.teavm.cache.DiskRegularMethodNodeCache;
import org.teavm.cache.FileSymbolTable;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationBuilder;
import org.teavm.diagnostics.Problem;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.model.*;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.tooling.*;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMEntryPoint;

/**
 * see https://github.com/konsoletyper/teavm/blob/master/tools/maven/plugin/src/main/java/org/teavm/maven/TeaVMCompileMojo.java
 */
public class Java2Javascript {

    public static void main(String[] vsdkjf) throws Exception {

        String srcRoot = "/home/me/opennars/web/src/main/java";
        //String dir = srcRoot + "/nars/web/ui";
        String mainClass = "nars.web.ui.TestTeaVM";

        Executor exe = Executors.newSingleThreadExecutor(); //Executors.newFixedThreadPool(1);

        String targetDir = "/home/me/opennars/web/src/main/resources/_compiled";

        autocompile(srcRoot, mainClass, exe, targetDir);

    }

    //    public static Class compile(String classBody, String... args) throws Exception {
    public static Class compile(String classBody) throws Exception {

//
//        // Get arguments.
//        String[] arguments = new String[ args.length ];
//        System.arraycopy(args, 0, arguments, 0, arguments.length);

        // Compile the class body.
        IClassBodyEvaluator cbe = CompilerFactoryFactory.getDefaultCompilerFactory().newClassBodyEvaluator();
        cbe.cook(classBody);
        return cbe.getClazz();

//        // Invoke the "public static main(String[])" method.
//        Method m = c.getMethod("main", String[].class);
//        Object returnValue = m.invoke(null, (Object) arguments);
//
//        // If non-VOID, print the return value.
//        if (m.getReturnType() != void.class) {
//            System.out.println(
//                    returnValue instanceof Object[]
//                            ? Arrays.toString((Object[]) returnValue)
//                            : String.valueOf(returnValue)
//            );
//        }
    }


    public static FSWatch autocompile(String srcRoot, String mainClass, Executor exe, String targetDir) throws IOException {
        return autocompile(srcRoot, mainClass, exe, targetDir, simpleName(mainClass));
    }

    public static FSWatch autocompile(String srcRoot, String mainClass, Executor exe, String targetDir, String targetFile) throws IOException {

        logger.info("autocompile: {}/{}.java", srcRoot, targetFile);

        String fileDir = srcRoot + "/" + mainClass.substring(0, mainClass.lastIndexOf('.')).replace('.', '/');
        Consumer<Path> method = (p) -> {

//            Java2Javascript.compile(
//                    new File(srcRoot),
//                    mainClass,
//                    new File(targetDir), targetFile + ".js"
//            );

            try {
                Runtime.getRuntime().exec("mvn teavm:compile");
                Runtime.getRuntime().exec("cp target/javascript/* src/resources/_compiled");
            } catch (IOException e) {
                e.printStackTrace();
            }


        };

        FSWatch f = new FSWatch(fileDir, exe, method);

        method.accept(null); //initial call

        return f;
    }

    static final Logger logger = LoggerFactory.getLogger(Java2Javascript.class);

//    private static final File cacheDir = new File(
//            Util.tempDir(), Java2Javascript.class.getSimpleName()
//    );
//    static {
//        if (!cacheDir.exists() && !cacheDir.mkdir())
//            throw new RuntimeException("unable to create temporary directory");
//    }

    //private static final Set<String> busy = Collections.synchronizedSet(new HashSet());

    static String simpleName(String c) {
        int lastPackagePeriod = c.lastIndexOf('.');
        if (lastPackagePeriod == -1)
            return c; //root package
        else
            return c.substring(lastPackagePeriod + 1, c.length());
    }


    public static void compile(File srcDir, String mainClass, File targetDir, String targetFileName) {

        String pathMainClass = srcDir + "/" + mainClass;


        String compileKey = "js://" + pathMainClass.replace('.', '/') + ".java";

//        ClassLoader cl = new ClassLoader(Java2Javascript.class.getClassLoader()) {
//            @Override
//            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
//                System.out.println("load: " + name + " " + resolve);
//                return super.loadClass(name, resolve);
//            }
//        };


        Util.time(logger, compileKey, () -> {


            final TeaVMTool tool = new TeaVMTool();


            ///tool.setClassLoader(cl);
            tool.addSourceFileProvider(new DirectorySourceFileProvider(srcDir));

            tool.setMinifying(true);
            tool.setDebugInformationGenerated(false);
            tool.setSourceMapsFileGenerated(false);
            tool.setSourceFilesCopied(true);
            tool.setMainClass(mainClass + "_");
            tool.setMainPageIncluded(false);
            tool.setIncremental(false);
            tool.setRuntime(RuntimeCopyOperation.NONE);
            tool.setTargetDirectory(targetDir);
            tool.setTargetFileName(targetFileName);
            tool.setCacheDirectory(Files.createTempDir().getAbsoluteFile());
            //tool.setBytecodeLogging(true);

            //delete any existing file
            //removeTarget(targetDir, targetFileName);

            try {
                tool.generate();
            } catch (Exception e) {


                logger.error("compile {}", e);

                //removeTarget(targetDir, targetFileName);
                //throw new RuntimeException( e );
            }

            System.out.println("used resources: " + tool.getUsedResources());


            if (!tool.getProblemProvider().getProblems().isEmpty()) {
                for (Problem p : tool.getProblemProvider().getProblems()) {
                    logger.warn("{} {}", p.getLocation(), p.getText());
                }
            }
            if (tool.wasCancelled()) {
                logger.error("fail");
            }

        });


    }

    public static void removeTarget(File targetDir, String targetFileName) {
        File r = new File(targetDir, targetFileName);
        r.delete();
    }


//    protected final List<SourceFileProvider> getSourceFileProviders() {
//        MavenSourceFileProviderLookup lookup = new MavenSourceFileProviderLookup();
//        lookup.setMavenProject(project);
//        lookup.setRepositorySystem(repositorySystem);
//        lookup.setLocalRepository(localRepository);
//        lookup.setRemoteRepositories(remoteRepositories);
//        lookup.setPluginDependencies(pluginArtifacts);
//        List<SourceFileProvider> providers = lookup.resolve();
//        addSourceProviders(providers);
//        return providers;
//    }

//    protected static final List<ClassHolderTransformer> instantiateTransformers(ClassLoader classLoader)  throws RuntimeException {
//        List<ClassHolderTransformer> transformerInstances = new ArrayList<>(transformers.length);
//        if (transformers == null) {
//            return transformerInstances;
//        }
//        for (String transformerName : transformers) {
//            Class<?> transformerRawType;
//            try {
//                transformerRawType = Class.forName(transformerName, true, classLoader);
//            } catch (ClassNotFoundException e) {
//                throw new RuntimeException("Transformer not found: " + transformerName, e);
//            }
//            if (!ClassHolderTransformer.class.isAssignableFrom(transformerRawType)) {
//                throw new RuntimeException("Transformer " + transformerName + " is not subtype of "
//                        + ClassHolderTransformer.class.getName());
//            }
//            Class<? extends ClassHolderTransformer> transformerType = transformerRawType.asSubclass(
//                    ClassHolderTransformer.class);
//            Constructor<? extends ClassHolderTransformer> ctor;
//            try {
//                ctor = transformerType.getConstructor();
//            } catch (NoSuchMethodException e) {
//                throw new RuntimeException("Transformer " + transformerName + " has no default constructor");
//            }
//            try {
//                ClassHolderTransformer transformer = ctor.newInstance();
//                transformerInstances.add(transformer);
//            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
//                throw new RuntimeException("Error instantiating transformer " + transformerName, e);
//            }
//        }
//        return transformerInstances;
//    }


}