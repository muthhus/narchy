package nars.web;


import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import nars.util.FSWatch;
import nars.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.tooling.*;
import org.teavm.tooling.sources.DirectorySourceFileProvider;

/**
 * see https://github.com/konsoletyper/teavm/blob/master/tools/maven/plugin/src/main/java/org/teavm/maven/TeaVMCompileMojo.java
 */
public class Java2Javascript {

    public static void main(String[] vsdkjf) throws Exception {

        String dir = "/home/me/opennars/web/src/main/java/nars/web/ui";
        String mainClass = "nars.web.ui.TestTeaVM";
        Executor exe = ForkJoinPool.commonPool();
        String targetDir = "/home/me/opennars/web/src/main/resources/_compiled";

        autocompile(dir, mainClass, exe, targetDir);

    }

    public static FSWatch autocompile(String dir, String mainClass, Executor exe, String targetDir) throws IOException {
        return autocompile(dir, mainClass, exe, targetDir, simpleName(mainClass) + ".js");
    }

    public static FSWatch autocompile(String dir, String mainClass, Executor exe, String targetDir, String targetFile) throws IOException {

        logger.info("autocompile: {}/{}", dir, mainClass);

        return new FSWatch(dir, exe, (p) -> {

            try {
                Java2Javascript.compile(
                        new File(dir),
                        mainClass,
                        new File(targetDir), targetFile
                );
            } catch (Exception e) {
                logger.error("compile changed file: {}", e);
            }

        });
    }

    static final Logger logger = LoggerFactory.getLogger(Java2Javascript.class);

    private static final File cacheDir = new File(
            Util.tempDir(), Java2Javascript.class.getSimpleName()
    );

    static {
        if (!cacheDir.exists() && !cacheDir.mkdir())
            throw new RuntimeException("unable to create temporary directory");
    }

    static final Set<String> busy = Collections.synchronizedSet(new HashSet());




    static String simpleName(String c) {
        int lastPackagePeriod = c.lastIndexOf('.');
        if (lastPackagePeriod==-1)
            return c; //root package
        else
            return c.substring(lastPackagePeriod+1, c.length());
    }

    public static void compile(File srcDir, String mainClass, File targetDir, String targetFileName) throws Exception {

        String pathMainClass = srcDir + "/" + mainClass;

        synchronized (busy) {
            if (!busy.add(pathMainClass)) {
                logger.info("already compiling {}", mainClass);
                return; //already busy
            }
        }

        Util.time(logger, "js://" + pathMainClass + ".java", () -> {

            final TeaVMTool tool = new TeaVMTool();

            ClassLoader cl = ClassLoader.getSystemClassLoader();
            tool.setClassLoader(cl);

            tool.addSourceFileProvider(new DirectorySourceFileProvider(srcDir));

            tool.setMinifying(true);
            tool.setDebugInformationGenerated(false);
            tool.setSourceMapsFileGenerated(false);
            tool.setSourceFilesCopied(false);
            tool.setMainClass(mainClass);
            tool.setMainPageIncluded(false);
            //tool.setIncremental(true); //<=- not working yet
            tool.setRuntime(RuntimeCopyOperation.NONE);
            tool.setTargetDirectory(targetDir);
            tool.setTargetFileName(targetFileName);
            tool.setCacheDirectory(cacheDir); //shared by all invocations

            try {
                tool.generate();
            } catch (TeaVMToolException e) {
                throw new RuntimeException( e );
            }

            if (!tool.getProblemProvider().getSevereProblems().isEmpty()) {
                throw new RuntimeException(
                        tool.getProblemProvider().getSevereProblems().toString()
                );
            }

        });

        busy.remove(pathMainClass);

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