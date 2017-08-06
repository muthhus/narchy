package com.github.disc99.injector.util;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.github.disc99.injector.util.Throwables.uncheck;
import static java.util.stream.Collectors.toList;

public final class ClassScanner {
    private final ClassLoader classLoader;

    public ClassScanner() {
        classLoader = Thread.currentThread().getContextClassLoader();
    }

    public ClassScanner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private String fileNameToClassName(String name) {
        return name.substring(0, name.length() - ".class".length());
    }

    private String resourceNameToClassName(String resourceName) {
        return fileNameToClassName(resourceName).replace('/', '.');
    }

    private boolean isClassFile(String fileName) {
        return fileName.endsWith(".class");
    }

    private String packageNameToResourceName(String packageName) {
        return packageName.replace('.', '/');
    }

    public Collection<Class<?>> scan(String rootPackageName) {
        String resourceName = packageNameToResourceName(rootPackageName);
        URL url = classLoader.getResource(resourceName);

        if (url == null) {
            return new ArrayList<Class<?>>();
        }

        String protocol = url.getProtocol();
        try {
            if ("file".equals(protocol)) {
                return findClassesWithFile(rootPackageName, new File(url.getFile()));
            } else if ("jar".equals(protocol)) {
                return findClassesWithJarFile(rootPackageName, url);
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
        throw new RuntimeException("Unsupported Class Load Protodol[" + protocol + "]");

    }

    private List<Class<?>> findClassesWithFile(String packageName, File dir) throws Exception {
        List<Class<?>> classes = new ArrayList<Class<?>>();

        for (String path : dir.list()) {
            File entry = new File(dir, path);
            if (entry.isFile() && isClassFile(entry.getName())) {
                classes.add(classLoader.loadClass(createClassName(packageName, fileNameToClassName(entry.getName()))));
            } else if (entry.isDirectory()) {
                classes.addAll(findClassesWithFile(createClassName(packageName, entry.getName()), entry));
            }
        }

        return classes;
    }

    private String createClassName(String packageName, String className) {
        return "".equals(packageName) ? className : packageName + "." + className;
    }

    private List<Class<?>> findClassesWithJarFile(String rootPackageName, URL jarFileUrl) throws Exception {

        JarURLConnection jarUrlConnection = (JarURLConnection) jarFileUrl.openConnection();
        try (JarFile jarFile = jarUrlConnection.getJarFile()) {

            Enumeration<JarEntry> jarEntry = jarFile.entries();
            return Collections.list(jarEntry).stream()
                    .filter(jar -> jar.getName().startsWith(packageNameToResourceName(rootPackageName)))
                    .filter(jar -> isClassFile(jar.getName()))
                    .map(jar -> resourceNameToClassName(jar.getName()))
                    .map(uncheck(classLoader::loadClass))
                    .collect(toList());
        }
    }
}
