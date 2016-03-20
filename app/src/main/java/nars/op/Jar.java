package nars.op;

import ch.qos.logback.classic.Level;
import com.google.common.base.Joiner;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xeustechnologies.jcl.DelegateProxyClassLoader;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;

import java.io.File;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * https://github.com/kamranzafar/JCL/
 */
public class Jar extends JarClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(Jar.class);
    static {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Reflections.class);
        root.setLevel(Level.INFO);
    }

    final static JclObjectFactory factory = JclObjectFactory.getInstance();


    @Override
    protected void addDefaultLoader() {
        synchronized (loaders) {
            loaders.add(getSystemLoader());
            //loaders.add(parentLoader);
            loaders.add(getCurrentLoader());
            loaders.add(getThreadLoader());
            Collections.sort(loaders);
        }
    }

    public Jar(Object... sources) {
        super();

        //classpathResources.setCollisionAllowed(true);
        classpathResources.setIgnoreMissingResources(false);

        logger.info("loading {}", Arrays.toString(sources));
        addAll(sources);

        //addDefaultLoader();

//        add(new URL("http://myserver.com/myjar.jar"));
//        add(new FileInputStream("myotherjar.jar"));
//        add("myclassfolder/");
//        add("myjarlib/"); //Recursively load all jar files in the folder/sub-folder(s)



//        getLoadedClasses().forEach((k,v)->{
//            System.out.println(k + " " + v);
//        });

    }

//    public <C> C getNew(String className, Object... args) {
//        return (C) factory.create(this, className, args);
//    }

    public Class<?> getClass(String name) {
        try {
            Class c = loadClass(name);

            //make all fields, methods, and constructors accessible
            AccessibleObject.setAccessible(c.getDeclaredFields(), true);
            AccessibleObject.setAccessible(c.getMethods(), true);
            AccessibleObject.setAccessible(c.getConstructors(), true);

            return c;
        } catch (ClassNotFoundException e) {
            logger.error("{} {}", name, e);
            return null;
        }
    }

    public void forEach(Consumer<String> eachClassName) {

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addClassLoader(this)
                .setUrls(ClasspathHelper.forClassLoader(this))
                /*.filterInputsBy(i -> {
                    return i.endsWith(".class");
                })*/
                //.forPackages("org.terasology.engine")


                //.addClassLoaders(loader)
                //.forPackages()

                .setScanners(
                    new SubTypesScanner(false)
                    //new TypeElementsScanner()
                )

                        //new TypeAnnotationsScanner().filterResultsBy(optionalFilter)),
        ) {
            @Override
            protected void scan(URL url) {
                try {
                    super.scan(url);
                } catch (ReflectionsException e) {
                    //logger.warn("{}",e);
                }
            }
        };
        reflections.getAllTypes().forEach(eachClassName);

        //reflections.getSubTypesOf(Object.class).stream().map(c -> c.getName()).forEach(eachClassName);

////or using ConfigurationBuilder

                //.setUrls(ClasspathHelper.forPackage("my.project.prefix"))
//                .setScanners(new SubTypesScanner()
//                        //new TypeAnnotationsScanner().filterResultsBy(optionalFilter), ...),
//                        //.filterInputsBy(new FilterBuilder().includePackage("my.project.prefix"))
//                ));

    }


    public Object invoke(String className, String staticMethod, Class<?>[] params, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        return getClass(className).getMethod(staticMethod, params).invoke(null, args);
    }

    public Object invokeMain(String className, String cwd, String... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        cwd(cwd);

        logger.info("invoking {} in {}", className, new File("./").getAbsolutePath());

        return invoke(className,  "main",
                new Class<?>[] { String[].class },
                new Object[] { args } );
    }

    public void cwd(String cwd) {
        System.setProperty("user.dir", cwd);
    }

    private Object invoke(String className, String staticMethodWithZeroArgs) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return invoke(className, staticMethodWithZeroArgs, new Class[0], new Object[0]);
    }


    public static void main(String[] args) throws Exception {

        Jar j = new Jar(
            "/home/me/terasology/libs/",
            "/home/me/terasology/natives/"
            //"/home/me/terasology/modules/"
        );



        j.invokeMain("org.terasology.engine.Terasology", "/home/me/terasology" /*, "-homedir=/tmp"*/ );

//        System.out.println(
//                Joiner.on("\n").join(j.getClass("org.terasology.editor.TeraEd").getMethods() ));
//
//        System.out.println( j.getNew("org.terasology.engine.Terasology").toString() );

//        j.forEach(cname -> {
//            //if (cname.contains("erasology"))
//                System.out.println(cname);
//        } );

//        Class clazz = j.getClass("");
//
//        Class<? extends Runnable> runClass = clazz.asSubclass(Runnable.class);
//// Avoid Class.newInstance, for it is evil.
//        Constructor<? extends Runnable> ctor = runClass.getConstructor();
//        Runnable doRun = ctor.newInstance();
//        doRun.run();



    }



}
