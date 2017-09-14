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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class CompoundClassPathRoot implements ClassPathRoot, Iterable<ClassPathRoot> {

    private final List<ClassPathRoot> roots = new ArrayList<>();

    public CompoundClassPathRoot(final List<ClassPathRoot> roots) {
        this.roots.addAll(roots);
    }

    @Override
    public InputStream getData(final String name) throws IOException {
        for (final ClassPathRoot each : this.roots) {
            final InputStream is = each.getData(name);
            if (is != null) {
                return is;
            }
        }
        return null;
    }

    @Override
    public Collection<String> classNames() {
        final List<String> arrayList = new ArrayList<>();
        for (final ClassPathRoot root : this.roots) {
            arrayList.addAll(root.classNames());
        }
        return arrayList;
    }

    @Override
    public URL getResource(String name) {
        try {
            return findRootForResource(name);
        } catch (final IOException exception) {
            return null;
        }
    }

    private URL findRootForResource(final String name) throws MalformedURLException {
        for (final ClassPathRoot root : this.roots) {
            final URL u = root.getResource(name);
            if (u != null) {
                return u;
            }
        }
        return null;
    }

    @Override
    public Optional<String> cacheLocation() {
        StringBuilder classpath = new StringBuilder();
        for (final ClassPathRoot each : this.roots) {
            final Optional<String> additional = each.cacheLocation();
            if (additional.isPresent()) {
                classpath = classpath.append(File.pathSeparator).append(additional.get());
            }
        }

        return Optional.of(classpath.toString());
    }

    @Override
    public Iterator<ClassPathRoot> iterator() {
        return this.roots.iterator();
    }

}
