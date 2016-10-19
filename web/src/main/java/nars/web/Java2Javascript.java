package nars.web;


import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import nars.util.Util;
import org.teavm.tooling.*;
import org.teavm.tooling.sources.DirectorySourceFileProvider;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * see https://github.com/konsoletyper/teavm/blob/master/tools/maven/plugin/src/main/java/org/teavm/maven/TeaVMCompileMojo.java
 */
public class Java2Javascript {

    private static final File cacheDir = new File(
        Util.tempDir(), Java2Javascript.class.getSimpleName()
    );
    static {
        if (!cacheDir.exists() && !cacheDir.mkdir())
            throw new RuntimeException("unable to create temporary directory");
    }

    public static class Auto implements Runnable {
        final WatchService watchService;
        private final WatchKey watchKey;
        private final Thread thread;

        public Auto(String dir) throws IOException {

            watchService = FileSystems.getDefault().newWatchService();

            Path path = Paths.get(dir);

            watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            System.out.println("Watch service registered dir: " + path.toString());

            this.thread = new Thread(this, "directoryWatch:" + dir);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        boolean running;

        public void stop() {
            running = false;
        }

        @Override
        public void run() {

            if (running)
                throw new UnsupportedOperationException("already running");

            running = true;

            while (running) {

                WatchKey key;

                try {
                    System.out.println("Waiting for key to be signalled...");
                    key = watchService.take();
                } catch (InterruptedException ex) {
                    System.out.println("Interrupted Exception");
                    return;
                }

                List<WatchEvent<?>> eventList = key.pollEvents();
                System.out.println("Process the pending events for the key: " + eventList.size());

                for (WatchEvent<?> genericEvent : eventList) {

                    WatchEvent.Kind<?> eventKind = genericEvent.kind();
                    System.out.println("Event kind: " + eventKind);

                    if (eventKind == OVERFLOW) {

                        continue; // pending events for loop
                    }

                    WatchEvent pathEvent = genericEvent;
                    Path file = (Path) pathEvent.context();
                    System.out.println("File name: " + file.toString());
                }

                boolean validKey = key.reset();
                System.out.println("Key reset");
                System.out.println("");

                if (!validKey) {
                    System.out.println("Invalid key");
                    break; // infinite for loop
                }

            }
        }
    }

    public static void main(String[] vsdkjf) throws Exception {

        String dir = "/home/me/opennars/web/src/main/java/nars/web/ui";

        new Auto(dir);

        Java2Javascript.compile(
            new File(
                    dir
            ),
            "nars.web.ui.TestTeaVM",

            new File("/tmp"), "run.js"
        );
    }

    public static void compile(File srcDir, String mainClass, File targetDir, String targetFileName) throws Exception {

        System.out.println("compiling " + srcDir + " / " + mainClass);

        final TeaVMTool tool = new TeaVMTool();

        //Log log = getLog();
        //log.info("Preparing classpath for JavaScript generation");
//        List<URL> urls = new ArrayList<>();
//        StringBuilder classpath = new StringBuilder();
//        return new JavaDynamicClassLoader(
//                files.stream().map(f -> {
//                    try {
//                        return f.toURL();
//                    } catch (MalformedURLException e) {
//                        e.printStackTrace();
//                        return null;
//                    }
//                }).toArray(URL[]::new),
//                Java2Javascript.class.getClassLoader()
//        );

//            for (File file : files) {
//                if (classpath.length() > 0) {
//                    classpath.append(':');
//                }
//                classpath.append(file.getPath());
//                urls.add(file.toURI().toURL());
//            }
//            if (classpath.length() > 0) {
//                classpath.append(':');
//            }
//            classpath.append(classFiles.getPath());
//            urls.add(classFiles.toURI().toURL());
//            for (File additionalEntry : getAdditionalClassPath()) {
//                classpath.append(':').append(additionalEntry.getPath());
//                urls.add(additionalEntry.toURI().toURL());
//            }
//            //log.info("Using the following classpath for JavaScript generation: " + classpath);
//            classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
//                    getClass().getClassLoader());
//            return classLoader;
//        } catch (MalformedURLException e) {
//            throw new RuntimeException("Error gathering classpath information", e);
//        }
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        tool.setClassLoader(cl);

        //tool.getTransformers().addAll(instantiateTransformers(cl));
//            if (true) {

        tool.addSourceFileProvider(new DirectorySourceFileProvider(srcDir));

//                for (SourceFileProvider provider : getSourceFileProviders()) {
//                    tool.addSourceFileProvider(provider);
//                }
//            }
//            if (properties != null) {
//                tool.getProperties().putAll(properties);
//            }
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


        //tool.setOptimizationLevel(optimizationLevel);
//            if (classAliases != null) {
//                tool.getClassAliases().addAll(Arrays.asList(classAliases));
//            }
//            if (methodAliases != null) {
//                tool.getMethodAliases().addAll(Arrays.asList(methodAliases));
//            }
        //tool.setTargetType(targetType);

        tool.generate();

        if (!tool.getProblemProvider().getSevereProblems().isEmpty()) {
            throw new RuntimeException(
                tool.getProblemProvider().getSevereProblems().toString()
            );
        }
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