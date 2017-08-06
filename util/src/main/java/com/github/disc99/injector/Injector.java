package com.github.disc99.injector;

import com.github.disc99.injector.util.ClassScanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Injector {

    private static final Class<? extends Annotation> INJECT = javax.inject.Inject.class;
    private static final Class<? extends Annotation> NAMED = javax.inject.Named.class;
    private static final List<Class<?>> namedClasses = new ClassScanner().scan("").stream()
            .filter(clz -> clz.isAnnotationPresent(NAMED))
            .collect(toList());

    private ClassDependencies dependencies = new ClassDependencies();

    public Injector() {
    }

    public Injector(ClassDependencies dependencies) {
        this.dependencies = dependencies;
    }

    public <T> T getInstance(Class<T> clazz) {
        try {
            T instance = clazz.newInstance();
            injectFields(instance);
            return instance;
        } catch (Exception e) {
            throw new InjectException("Fail get instance.", e);
        }
    }

    private <T> void injectFields(T instance) throws InstantiationException, IllegalAccessException {
        final Optional<List<Field>> injectTargetFields = findTargetFields(instance);
        if (injectTargetFields.isPresent()) {
            for (Field field : injectTargetFields.get()) {
                Object injectedInstance = findInjectClass(field).newInstance();
                field.setAccessible(true);
                field.set(instance, injectedInstance);
                injectFields(injectedInstance);
            }
        }
    }

    private Class<?> findInjectClass(Field field) {
        final Class<?> injectClass = field.getType();
        if (dependencies.isBound(injectClass)) {
            return dependencies.get(injectClass);
        }

        List<Class<?>> foundClasses = namedClasses.stream()
                .filter(cls -> injectClass.isAssignableFrom(cls))
                .collect(toList());

        if (foundClasses.size() == 1) {
            dependencies.bind(injectClass, foundClasses.get(0));
            return foundClasses.get(0);
        } else if (foundClasses.isEmpty()) {
            throw new InjectException("Injection class not found: " + injectClass);
        } else {
            throw new InjectException("Injection class there is more than one: " + injectClass);
        }
    }

    private <T> Optional<List<Field>> findTargetFields(T instance) {
        Field[] fields = instance.getClass().getDeclaredFields();

        return Optional.of(Stream.of(fields)
                .filter(field -> field.isAnnotationPresent(INJECT))
                .collect(toList()));
    }
}
