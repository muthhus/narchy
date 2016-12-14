/*
 * Copyright (C) 2013 Square, Inc.
 * Copyright (C) 2013 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package objenome.impl.loaders;


import objenome.impl.*;
import objenome.lazy;
import objenome.out;
import objenome.the;
import org.jetbrains.annotations.NotNull;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Collections;
import java.util.Set;

/**
 * A dynamic-only loader that only uses reflection
 */
public final class DynamicLoader extends Loader {

  @Override public <T> ModuleAdapter<T> getModuleAdapter(Class<T> type) {
    ModuleAdapter<T> adapter = DynamicModuleAdapter.create(type);
    return adapter;
  }

  @Override public Binding<?> getAtInjectBinding(String key, String className, ClassLoader ignored,
                                                 boolean mustHaveInjections) {
     try {
      Class<?> type = getClass().getClassLoader().loadClass(className);
      if (type.isInterface()) {
        return null; // Short-circuit since we can't build reflective bindings for interfaces.
      }
      return ReflectiveAtInjectBinding.create(type, mustHaveInjections);
    } catch (ClassNotFoundException e) {
      throw new TypeNotPresentException(
          String.format("Could not find %s needed for binding %s", className, key), e);
    }
  }

  @Override public StaticInjection getStaticInjection(Class<?> injectedClass) {
    return ReflectiveStaticInjection.create(injectedClass);
  }

    //TODO: Reduce the complexity of this and/or replace with a mock or fake.
    public static class DynamicModuleAdapter<M> extends ModuleAdapter<M> {

      private static final boolean STRICT = true;

      protected DynamicModuleAdapter(@NotNull Class<M> moduleClass, @NotNull the annotation) {
        super(
            moduleClass,
            injectableTypesToKeys(annotation.in()),
            annotation.the(),
            annotation.override(),
            annotation.extend(),
            annotation.complete(),
            annotation.library());
      }

      private static String[] injectableTypesToKeys(Class<?>[] injectableTypes) {
        String[] result = new String[injectableTypes.length];
        for (int i = 0; i < injectableTypes.length; i++) {
          Class<?> injectableType = injectableTypes[i];
          result[i] = injectableType.isInterface()
              ? Keys.get(injectableType)
              : Keys.getMembersKey(injectableType);
        }
        return result;
      }

      @Override public void getBindings(BindingsGroup bindings, M module) {
        for (Class<?> c = moduleClass; !c.equals(Object.class); c = c.getSuperclass()) {
          for (Method method : c.getDeclaredMethods()) {
            out out = method.getAnnotation(objenome.out.class);
            if (out != null) {
              Type genericReturnType = method.getGenericReturnType();

              Type typeToCheck = genericReturnType;
              if (genericReturnType instanceof ParameterizedType) {
                typeToCheck = ((ParameterizedType) genericReturnType).getRawType();
              }
              if (Provider.class.equals(typeToCheck)) {
                throw new IllegalStateException("@Provides method must not return Provider directly: "
                    + c.getName()
                    + '.'
                    + method.getName());
              }
              if (lazy.class.equals(typeToCheck)) {
                throw new IllegalStateException("@Provides method must not return Lazy directly: "
                    + c.getName()
                    + '.'
                    + method.getName());
              }

              String key = Keys.get(genericReturnType, method.getAnnotations(), method);
              switch (out.type()) {
                case UNIQUE:
                  handleBindings(bindings, module, method, key, library);
                  break;
                case SET:
                  String setKey = Keys.getSetKey(method.getGenericReturnType(),
                      method.getAnnotations(), method);
                  handleSetBindings(bindings, module, method, setKey, key, library);
                  break;
                case SET_VALUES:
                  handleSetBindings(bindings, module, method, key, key, library);
                  break;
                default:
                  throw new AssertionError("Unknown @out type " + out.type());
              }
            }
          }
        }
      }

      private void handleBindings(BindingsGroup bindings, M module, Method method, String key,
          boolean library) {
        bindings.contributeProvidesBinding(key,
            new ReflectiveProvidesBinding<M>(method, key, moduleClass.getName(), module, library));
      }

      private void handleSetBindings(BindingsGroup bindings, M module, Method method,
          String setKey, String providerKey, boolean library) {
        SetBinding.<M>add(bindings, setKey,
            new ReflectiveProvidesBinding<M>(
                method, providerKey, moduleClass.getName(), module, library));
      }

      @Override public M newModule() {
        try {
          Constructor<?> constructor = moduleClass.getDeclaredConstructor();
          constructor.setAccessible(true);
          return (M)constructor.newInstance();
        } catch (InvocationTargetException e) {
          throw new IllegalArgumentException(e.getCause());
        } catch (NoSuchMethodException e) {
          throw new IllegalArgumentException("Could not construct " + moduleClass.getName()
              + " as it lacks an accessible no-args constructor. This module must be passed"
              + " in as an instance, or an accessible no-args constructor must be added.", e);
        } catch (InstantiationException e) {
          throw new IllegalArgumentException("Failed to construct " + moduleClass.getName(), e);
        } catch (IllegalAccessException e) {
          throw new AssertionError();
        }
      }

      @Override public String toString() {
        return "DynamicModuleAdapter[" + this.moduleClass.getName() + ']';
      }

      /**
       * Creates a TestingModuleAdapter or throws an {@code IllegalArgumentException}.
       */
      public static <M> ModuleAdapter<M> create(Class<M> moduleClass) {
        the annotation = moduleClass.getAnnotation(the.class);
        if (annotation == null && STRICT) {
          throw new IllegalArgumentException("No @the on " + moduleClass.getName());
        }
        if (!moduleClass.getSuperclass().equals(Object.class)) {
          throw new IllegalArgumentException(
              "Modules must not extend from other classes: " + moduleClass.getName());
        }
        return new DynamicModuleAdapter<>(moduleClass, annotation);
      }

      /**
       * Invokes a method to provide a value. The method's parameters are injected.
       */
      private static final class ReflectiveProvidesBinding<T> extends ProvidesBinding<T> {
        private Binding<?>[] parameters;
        private final Method method;
        private final Object instance;

        public ReflectiveProvidesBinding(Method method, String key, String moduleClass,
            Object instance, boolean library) {
          super(key, method.isAnnotationPresent(Singleton.class), moduleClass, method.getName());
          this.method = method;
          this.instance = instance;
          method.setAccessible(true);
          setLibrary(library);
        }

        @Override public void attach(Linker linker) {
          Type[] types = method.getGenericParameterTypes();
          Annotation[][] annotations = method.getParameterAnnotations();
          parameters = new Binding[types.length];
          for (int i = 0; i < parameters.length; i++) {
            String key = Keys.get(types[i], annotations[i], method + " parameter " + i);
            parameters[i] = linker.requestBinding(key, method, instance.getClass().getClassLoader());
          }
        }

        @Override public T get() {
          Object[] args = new Object[parameters.length];
          for (int i = 0; i < parameters.length; i++) {
            args[i] = parameters[i].get();
          }
          try {
            return (T) method.invoke(instance, args);
          } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw cause instanceof RuntimeException
                ? (RuntimeException) cause
                : new RuntimeException(cause);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }

        @Override public void getDependencies(Set<Binding<?>> get, Set<Binding<?>> injectMembers) {
          Collections.addAll(get, parameters);
        }

        @Override public void injectMembers(T t) {
          throw new AssertionError("Provides method bindings are not MembersInjectors");
        }
      }
    }
}
