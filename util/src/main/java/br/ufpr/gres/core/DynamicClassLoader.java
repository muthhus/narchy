package br.ufpr.gres.core;

public class DynamicClassLoader extends ClassLoader {

    Class load(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }

}
