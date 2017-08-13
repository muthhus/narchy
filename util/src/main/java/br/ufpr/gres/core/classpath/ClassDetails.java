/*
 * Copyright 2016 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
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
package br.ufpr.gres.core.classpath;

import br.ufpr.gres.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
abstract public class ClassDetails {

    private final Logger logger = LoggerFactory.getLogger(ClassDetails.class);

    //private final ClassInfo classInfo;
    private final ClassName className;


    public ClassDetails(ClassInfo classInfo) {

        this.className = new ClassName(classInfo.getName());
    }

    public static String path(Class c) {
        return c.getName().replace('.', '/') + ".class";
    }

    public static byte[] bytes(Class c) {
        return bytes(path(c));
    }

    public static byte[] bytes(String path) {
        byte[] bytes = null;
        try {
            bytes = ClassLoader.getSystemResourceAsStream(path).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bytes;
    }

    abstract public byte[] getBytes();


    /**
     * Class name without package
     *
     * @return
     */
    public ClassName getClassName() {
        return this.className;
    }


    /**
     * Examine if class <i>c</i> is an applet class
     */
    private static boolean isApplet(Class c) {
        while (c != null) {
            if (c.getName().indexOf("java.applet") == 0) {
                return true;
            }

            c = c.getSuperclass();
            if (c.getName().indexOf("java.lang") == 0) {
                return false;
            }
        }
        return false;
    }

    /**
     * Examine if class <i>c</i> is a GUI class
     */
    private static boolean isGUI(Class c) {
        while (c != null) {
            if ((c.getName().indexOf("java.awt") == 0)
                    || (c.getName().indexOf("javax.swing") == 0)) {
                return true;
            }

            c = c.getSuperclass();
            if (c.getName().indexOf("java.lang") == 0) {
                return false;
            }
        }
        return false;
    }

//    public Class getClassInstance() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//        // initialization of the test set class
//        if (clazz.getConstructor().newInstance() == null) {
//            throw new InstantiationException();
//        }
//        return clazz;
//    }

//    /**
//     * Verify if a class is testable
//     *
//     * @return Return true if a class is testable; otherwise false
//     * @throws ClassNotFoundException
//     * @throws IOException
//     */
//    public boolean isTestable() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//        Class c = getClassInstance();
//
//        if (c.isInterface()) {
//            logger.error("Can't apply mutation because {} is 'interface'", className);
//            return false;
//        }
//
//        if (Modifier.isAbstract(c.getModifiers())) {
//            logger.error("Can't apply mutation because {} is 'abstract' class", className);
//            return false;
//        }
//
//        if (isGUI(c)) {
//            logger.error("Can't apply mutation because {} is 'GUI' class", className);
//            return false;
//        }
//        if (isApplet(c)) {
//            logger.error("Can't apply mutation because {} is 'applet' class", className);
//            return false;
//        }
//
//        return true;
//    }

    @Override
    public String toString() {
        return this.className.toString();
    }
}
