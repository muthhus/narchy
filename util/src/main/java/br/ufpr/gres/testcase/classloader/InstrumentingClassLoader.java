/*
 * Copyright 2017 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.ufpr.gres.testcase.classloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public abstract class InstrumentingClassLoader extends ClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentingClassLoader.class);

    private String testClassName;

    public InstrumentingClassLoader(String testClassName) {
        super(bootClassLoader());
        this.testClassName = testClassName;
    }

    public static ClassLoader bootClassLoader() {
    return Object.class.getClassLoader();
  }

    /**
     * Before a new class is defined, we need to create a package definition for it
     *
     * @param className
     */
    private void definePackage(String className) {
        int i = className.lastIndexOf('.');
        if (i != -1) {
            String pkgname = className.substring(0, i);

            // Check if package already loaded.
            if (this.getPackage(pkgname) == null) {
                definePackage(pkgname, null, null, null, null, null, null, null);
            }
        }
    }

    public Class<?> defineClassForName(String name, byte[] data) {
        return this.defineClass(name, data, 0, data.length);
    }

    protected static String getName(String fileName) {
        return fileName.replace('.', File.separatorChar).concat(".class");
    }

    /**
     * Overrides getResource (String) to get non-class files including resource bundles from
     * property files
     *
     * @return
     */
//    @Override
//    public URL getResource(String name) {
//        URL url = null;
//        File resource = new File(MutationSystem.CLASS_PATH, name);
//        if (resource.exists()) {
//            try {
//                return resource.toURI().toURL();
//            } catch (MalformedURLException e) {
//                logger.error("Error in get the binary file in 'classes' path ", e);
//            }
//        }
//        return url;
//    }

    public void setTestClassName(String testSetName) {
        this.testClassName = testSetName;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // See if type has already been loaded by this class loader
        Class result = findLoadedClass(name);

        if (result != null) {
            // Return an already-loaded class
            return result;
        }

        return findSystemClass(name);
    }

    public Class<?> loadClassFromFile(String fileName, String directory) throws ClassNotFoundException {
        try {
            String filename = getName(fileName);

            //definePackage(fileName);
            // Get all bytes from file (with class extension) in session/testset/filename        
            byte[] byteBuffer = null;

            if (Files.exists(Paths.get(directory, filename))) {
                byteBuffer = Files.readAllBytes(Paths.get(directory, filename));
            } else {
                int i = fileName.lastIndexOf('.');
                String name = i >= 0 ? fileName.substring(i + 1, fileName.length()) : fileName;
                byteBuffer = Files.readAllBytes(Paths.get(directory, getName(name)));
            }

            //logger.info("Loaded class '" + fileName + "' from path '" + directory + "'");
            return defineClassForName(fileName, byteBuffer);
        } catch (IOException e) {
            //logger.error("Error while loading class " + fileName);
            throw new ClassNotFoundException();
        }
    }

//    public Class<?> loadOriginalClass(String name) throws ClassNotFoundException {
//        String filename = getName(name);
//
//        if (Files.exists(Paths.get(MutationSystem.CLASS_PATH, filename))) {
//            return loadClassFromFile(name, MutationSystem.CLASS_PATH);
//        } else {
//            return loadClassFromFile(name, MutationSystem.TESTSET_PATH);
//        }
//    }
//
//    public Class<?> loadTestClass() throws ClassNotFoundException {
//        return loadClassFromFile(this.testClassName, MutationSystem.TESTSET_PATH);
//    }
}
