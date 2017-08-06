package com.github.disc99.injector;

import java.util.HashMap;
import java.util.Map;

public class ClassDependencies {
    private final Map<Class<?>, Class<?>> mapping = new HashMap<>();

    public ClassDependencies bind(Class<?> key, Class<?> value) {
        mapping.put(key, value);
        return this;
    }

    public boolean isBound(Class<?> clazz) {
        return mapping.containsKey(clazz);
    }

    public Class<?> get(Class<?> clazz) {
        return mapping.get(clazz);
    }
}
