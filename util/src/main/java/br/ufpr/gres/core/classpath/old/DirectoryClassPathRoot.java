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
package br.ufpr.gres.core.classpath.old;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class DirectoryClassPathRoot implements ClassPathRoot {

    private final File root;

    public DirectoryClassPathRoot(final File root) {
        this.root = root;
    }

    @Override
    public InputStream getData(final String classname) throws java.io.FileNotFoundException {
        final String filename = classname.replace('.', File.separatorChar).concat(
                ".class");
        final File file = new File(this.root, filename);
        return file.canRead() ? new FileInputStream(file) : null;
    }

    @Override
    public URL getResource(final String name) throws MalformedURLException {
        final File f = new File(this.root, name);
        return f.canRead() ? f.toURI().toURL() : null;
    }

    @Override
    public Collection<String> classNames() {
        return classNames(this.root);
    }

    private Collection<String> classNames(final File file) {
        final List<String> classNames = new LinkedList<>();
        for (final File f : file.listFiles()) {
            if (f.isDirectory()) {
                classNames.addAll(classNames(f));
            } else if (f.getName().endsWith(".class")) {
                classNames.add(fileToClassName(f));
            }
        }
        return classNames;
    }

    private String fileToClassName(final File f) {
        return f
                .getAbsolutePath()
                .substring(this.root.getAbsolutePath().length() + 1,
                        (f.getAbsolutePath().length() - ".class".length()))
                .replace(File.separatorChar, '.');
    }

    @Override
    public Optional<String> cacheLocation() {
        return Optional.of(this.root.getAbsolutePath());
    }
}
