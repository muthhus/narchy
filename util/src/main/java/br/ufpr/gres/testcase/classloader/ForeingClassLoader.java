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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class ForeingClassLoader {

    // Create a File object on the root of the directory containing the class file
    private final File rootPath;

    public ForeingClassLoader(File file) {
        rootPath = file;
    }

    public ClassLoader getLoader() {

        ClassLoader loader = null;

        try {
            // Create a new class loader with the directory
            loader = new URLClassLoader(new URL[]{rootPath.toURI().toURL()});

            // Load in the class; MyClass.class should be located in
            // the directory file:/c:/myclasses/com/mycompany
            //Class cls = loader.loadClass("com.mycompany.MyClass");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL: " + rootPath.getAbsolutePath(), e);
        }

        return loader;
    }
}
