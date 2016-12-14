/*
 * Copyright (C) 2012 Square Inc.
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
package objenome;

import objenome.impl.loaders.DynamicLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Provider;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

//TODO: Migrate to compiler.

@RunWith(JUnit4.class)
public final class ModuleTest {
    static class TestEntryPoint {
        @in
        String s;
    }

    @the(in = TestEntryPoint.class)
    static class ModuleWithEntryPoint {
    }

    @Test
    public void childModuleWithEntryPoint() {
        @the(extend = ModuleWithEntryPoint.class)
        class TestModule {
            @out
            String provideString() {
                return "injected";
            }
        }

        O objectGraph = O.via(new DynamicLoader(), new TestModule());
        TestEntryPoint entryPoint = objectGraph.a(TestEntryPoint.class);
        assertThat(entryPoint.s).isEqualTo("injected");
    }

    static class TestStaticInjection {
        @in
        static String s;
    }

    @the(the = TestStaticInjection.class)
    static class ModuleWithStaticInjection {
    }

    @Test
    public void childModuleWithStaticInjection() {
        @the(extend = ModuleWithStaticInjection.class)
        class TestModule {
            @out
            String provideString() {
                return "injected";
            }
        }

        O objectGraph = O.via(new DynamicLoader(), new TestModule());
        TestStaticInjection.s = null;
        objectGraph.injectStatics();
        assertThat(TestStaticInjection.s).isEqualTo("injected");
    }

    @the
    static class ModuleWithBinding {
        @out
        static String provideString() {
            return "injected";
        }
    }

    @Test
    public void childModuleWithBinding() {

        @the(
                in = TestEntryPoint.class,
                extend = ModuleWithBinding.class
        )
        class TestModule {
        }

        O objectGraph = O.via(new DynamicLoader(), new TestModule());
        TestEntryPoint entryPoint = new TestEntryPoint();
        objectGraph.with(entryPoint);
        assertThat(entryPoint.s).isEqualTo("injected");
    }

    @the(extend = ModuleWithBinding.class)
    static class ModuleWithChildModule {
    }

    @Test
    public void childModuleWithChildModule() {

        @the(
                in = TestEntryPoint.class,
                extend = ModuleWithChildModule.class
        )
        class TestModule {
        }

        O objectGraph = O.via(new DynamicLoader(), new TestModule());
        TestEntryPoint entryPoint = new TestEntryPoint();
        objectGraph.with(entryPoint);
        assertThat(entryPoint.s).isEqualTo("injected");
    }

    @the
    static class ModuleWithConstructor {
        private final String value;

        ModuleWithConstructor(String value) {
            this.value = value;
        }

        @out
        String provideString() {
            return value;
        }
    }

    @Test
    public void childModuleMissingManualConstruction() {
        @the(extend = ModuleWithConstructor.class)
        class TestModule {
        }

        try {
            O.via(new DynamicLoader(), new TestModule());
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void childModuleWithManualConstruction() {

        @the(
                in = TestEntryPoint.class,
                extend = ModuleWithConstructor.class
        )
        class TestModule {
        }

        O objectGraph = O.via(new DynamicLoader(), new ModuleWithConstructor("a"), new TestModule());
        TestEntryPoint entryPoint = new TestEntryPoint();
        objectGraph.with(entryPoint);
        assertThat(entryPoint.s).isEqualTo("a");
    }

    static class A {
    }

    static class B {
        @in
        A a;
    }

    @the(in = A.class)
    public static class TestModuleA {
        @out
        static A a() {
            return new A();
        }
    }

    @the(extend = TestModuleA.class, in = B.class)
    public static class TestModuleB {
    }

    @Test
    public void autoInstantiationOfModules() {
        // Have to make these non-method-scoped or instantiation errors occur.
        O objectGraph = O.via(new DynamicLoader(), TestModuleA.class);
        assertThat(objectGraph.a(A.class)).isNotNull();
    }

    @Test
    public void autoInstantiationOfIncludedModules() {
        // Have to make these non-method-scoped or instantiation errors occur.
        O objectGraph = O.via(new DynamicLoader(), new TestModuleB()); // TestModuleA auto-created.
        assertThat(objectGraph.a(A.class)).isNotNull();
        assertThat(objectGraph.a(B.class).a).isNotNull();
    }

    static class ModuleMissingModuleAnnotation {
    }

    @the(extend = ModuleMissingModuleAnnotation.class)
    static class ChildModuleMissingModuleAnnotation {
    }

    @Test
    public void childModuleMissingModuleAnnotation() {
        try {
            O.via(new DynamicLoader(), new ChildModuleMissingModuleAnnotation());
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage())
                    .contains("No @the on objenome.ModuleTest$ModuleMissingModuleAnnotation");
        }
    }

    @the
    static class ThreadModule extends Thread {
    }

    @Test
    public void moduleExtendingClassThrowsException() {
        try {
            O.via(new DynamicLoader(), new ThreadModule());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith("Modules must not extend from other classes: ");
        }
    }

    @Test
    public void provideProviderFails() {
        @the
        class ProvidesProviderModule {
            @out
            Provider<Object> provideObject() {
                return null;
            }
        }
        try {
            O.via(new DynamicLoader(), new ProvidesProviderModule());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).startsWith("@Provides method must not return Provider directly: ");
            assertThat(e.getMessage()).endsWith("ProvidesProviderModule.provideObject");
        }
    }

    @Test
    public void provideRawProviderFails() {
        @the
        class ProvidesRawProviderModule {
            @out
            Provider provideObject() {
                return null;
            }
        }
        try {
            O.via(new DynamicLoader(), new ProvidesRawProviderModule());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).startsWith("@Provides method must not return Provider directly: ");
            assertThat(e.getMessage()).endsWith("ProvidesRawProviderModule.provideObject");
        }
    }

    @Test
    public void provideLazyFails() {
        @the
        class ProvidesLazyModule {
            @out
            lazy<Object> provideObject() {
                return null;
            }
        }
        try {
            O.via(new DynamicLoader(), new ProvidesLazyModule());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).startsWith("@Provides method must not return Lazy directly: ");
            assertThat(e.getMessage()).endsWith("ProvidesLazyModule.provideObject");
        }
    }

    @Test
    public void provideRawLazyFails() {
        @the
        class ProvidesRawLazyModule {
            @out
            lazy provideObject() {
                return null;
            }
        }
        try {
            O.via(new DynamicLoader(), new ProvidesRawLazyModule());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).startsWith("@Provides method must not return Lazy directly: ");
            assertThat(e.getMessage()).endsWith("ProvidesRawLazyModule.provideObject");
        }
    }
}
