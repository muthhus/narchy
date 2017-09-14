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

import br.ufpr.gres.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipException;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class ClassPath {

    private static final Logger logger = LoggerFactory.getLogger(ClassPath.class);

    private final CompoundClassPathRoot root;

    public ClassPath() {
        this(ClassPath.getClassPathElementsAsFiles());
    }

    /**
     * Chama aqui
     *
     * @param roots
     */
    public ClassPath(final ClassPathRoot... roots) {
        this(Arrays.asList(roots));
    }

    public ClassPath(List<ClassPathRoot> roots) {
        this.root = new CompoundClassPathRoot(roots);
    }

    public ClassPath(final Collection<File> files) {
        this(createRoots(files.stream().filter(a -> a.exists() && a.canRead()).collect(Collectors.toList())));
    }

    public Collection<String> classNames() {
        return this.root.classNames();
    }

    // fixme should not be determining type here
    private static List<ClassPathRoot> createRoots(final Collection<File> files) {
        final List<ClassPathRoot> rs = new ArrayList<>();
        File lastFile = null;
        try {
            for (final File f : files) {
                lastFile = f;
                if (f.isDirectory()) {
                    rs.add(new DirectoryClassPathRoot(f));
                } else {
                    handleArchive(rs, f);
                }
            }
            return rs;
        } catch (final IOException ex) {
            logger.error("Error handling file " + lastFile, ex);
        }

        return rs;
    }

    private static void handleArchive(final List<ClassPathRoot> rs, final File f) throws IOException {
        try {
            if (!f.canRead()) {
                throw new IOException("Can't read the file " + f);
            }
            rs.add(new ArchiveClassPathRoot(f));
        } catch (final ZipException ex) {
            logger.warn("Can't open the archive " + f);
        }
    }

    public byte[] getClassData(final String classname) throws IOException {
        InputStream is = this.root.getData(classname);
        if (is != null) {
            try {
                return StreamUtils.streamToByteArray(is);
            } finally {
                is.close();
            }
        }
        return null;
    }

    public URL findResource(final String name) {
        return this.root.getResource(name);
    }

    public static Collection<String> getClassPathElementsAsPaths() {
        return getClassPathElementsAsFiles().stream().map(File::getPath).collect(Collectors.toList());
    }

    public static Collection<File> getClassPathElementsAsFiles() {
        Set<File> us = new LinkedHashSet<>();
        try {
            for (String m : getClassPathElementsAsAre()) {
                us.add(new File(m).getCanonicalFile());
            }
        } catch (IOException e) {
            logger.error("Error in to get the File", e);
        }
        return us;
    }

    public String getLocalClassPath() {
        return this.root.cacheLocation().get();
    }

    /**
     * FIXME move somewhere common
     */
    private static List<String> getClassPathElementsAsAre() {
        final String classPath = System.getProperty("java.class.path");
        if (classPath != null) {
            final String separator = File.pathSeparator;
            return Arrays.asList(classPath.split(separator));
        } else {
            return new ArrayList<>();
        }

    }
}
