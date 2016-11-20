/*
 * Copyright (C) 2010 Google Inc.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public final class InjectionTest {
    @Test
    public void basicInjection() {
        class TestEntryPoint {
            @in
            Provider<G> gProvider;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            E provideE(F f) {
                return new E(f);
            }

            @out
            F provideF() {
                return new F();
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);
        G g = entryPoint.gProvider.get();
        assertThat(g.a).isNotNull();
        assertThat(g.b).isNotNull();
        assertThat(g.c).isNotNull();
        assertThat(g.d).isNotNull();
        assertThat(g.e).isNotNull();
        assertThat(g.e.f).isNotNull();
    }

    static class A {
        @in
        A() {
        }
    }

    static class B {
        @in
        B() {
        }
    }

    @Singleton
    static class C {
        @in
        C() {
        }
    }

    @Singleton
    static class D {
        @in
        D() {
        }
    }

    static class E {
        F f;

        E(F f) {
            this.f = f;
        }
    }

    static class F {
    }

    static class G {
        @in
        A a;
        @in
        B b;
        C c;
        D d;
        @in
        E e;

        @in
        G(C c, D d) {
            this.c = c;
            this.d = d;
        }
    }

    @Test
    public void providerInjection() {
        class TestEntryPoint {
            @in
            Provider<A> aProvider;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);

        assertThat(entryPoint.aProvider.get()).isNotNull();
        assertThat(entryPoint.aProvider.get()).isNotNull();
        assertThat(entryPoint.aProvider.get()).isNotSameAs(entryPoint.aProvider.get());
    }


    @Test
    public void singletons() {
        class TestEntryPoint {
            @in
            Provider<F> fProvider;
            @in
            Provider<I> iProvider;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            @Singleton
            F provideF() {
                return new F();
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);
        assertThat(entryPoint.fProvider.get()).isSameAs(entryPoint.fProvider.get());
        assertThat(entryPoint.iProvider.get()).isSameAs(entryPoint.iProvider.get());
    }

    @Singleton
    static class I {
        @in
        I() {
        }
    }

    @Test
    public void bindingAnnotations() {
        final A one = new A();
        final A two = new A();

        class TestEntryPoint {
            @in
            A a;
            @in
            @Named("one")
            A aOne;
            @in
            @Named("two")
            A aTwo;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            @Named("one")
            A getOne() {
                return one;
            }

            @out
            @Named("two")
            A getTwo() {
                return two;
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);
        assertThat(entryPoint.a).isNotNull();
        assertThat(one).isSameAs(entryPoint.aOne);
        assertThat(two).isSameAs(entryPoint.aTwo);
    }

    @Test
    public void singletonBindingAnnotationAndProvider() {
        class TestEntryPoint {
            @in
            Provider<L> lProvider;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            A a1;
            A a2;

            @out
            @Singleton
            @Named("one")
            F provideF(Provider<A> aProvider) {
                a1 = aProvider.get();
                a2 = aProvider.get();
                return new F();
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        TestModule module = new TestModule();
        O.of(module).with(entryPoint);
        entryPoint.lProvider.get();

        assertThat(module.a1).isNotNull();
        assertThat(module.a2).isNotNull();
        assertThat(module.a1).isNotSameAs(module.a2);
        assertThat(entryPoint.lProvider.get()).isSameAs(entryPoint.lProvider.get());
    }

    @Singleton
    public static class L {
        @in
        @Named("one")
        F f;
        @in
        Provider<L> lProvider;
    }

    @Test
    public void singletonInGraph() {
        class TestEntryPoint {
            @in
            N n1;
            @in
            N n2;
            @in
            F f1;
            @in
            F f2;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            @Singleton
            F provideF() {
                return new F();
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);

        assertThat(entryPoint.f1).isSameAs(entryPoint.f2);
        assertThat(entryPoint.f1).isSameAs(entryPoint.n1.f1);
        assertThat(entryPoint.f1).isSameAs(entryPoint.n1.f2);
        assertThat(entryPoint.f1).isSameAs(entryPoint.n2.f1);
        assertThat(entryPoint.f1).isSameAs(entryPoint.n2.f2);
        assertThat(entryPoint.f1).isSameAs(entryPoint.n1.fProvider.get());
        assertThat(entryPoint.f1).isSameAs(entryPoint.n2.fProvider.get());
    }

    public static class N {
        @in
        F f1;
        @in
        F f2;
        @in
        Provider<F> fProvider;
    }

    @Test
    public void noJitBindingsForAnnotations() {
        class TestEntryPoint {
            @in
            @Named("a")
            A a;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
        }

        O graph = O.of(new TestModule());
        try {
            graph.validate();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void injectableSupertypes() {
        class TestEntryPoint {
            @in
            Q q;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            F provideF() {
                return new F();
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);
        assertThat(entryPoint.q.f).isNotNull();
    }

    @Test
    public void uninjectableSupertypes() {
        class TestEntryPoint {
            @in
            T t;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);
        assertThat(entryPoint.t).isNotNull();
    }

    public static class P {
        @in
        F f;
    }

    public static class Q extends P {
        @in
        Q() {
        }
    }

    static class S {
    }

    public static class T extends S {
        @in
        T() {
        }
    }

    @Test
    public void singletonsAreNotEager() {
        class TestEntryPoint {
            @in
            Provider<A> aProvider;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            boolean sInjected = false;

            @out
            F provideF(R r) {
                return new F();
            }

            @out
            @Singleton
            S provideS() {
                sInjected = true;
                return new S();
            }
        }

        R.injected = false;
        TestEntryPoint entryPoint = new TestEntryPoint();
        TestModule module = new TestModule();
        O.of(module).with(entryPoint);

        assertThat(R.injected).isFalse();
        assertThat(module.sInjected).isFalse();
    }

    @Singleton
    static class R {
        static boolean injected = false;

        @in
        R() {
            injected = true;
        }
    }

    @Test
    public void providesSet() {
        final Set<A> set = Collections.emptySet();

        class TestEntryPoint {
            @in
            Set<A> set;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            Set<A> provideSet() {
                return set;
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        TestModule module = new TestModule();
        O.of(module).with(entryPoint);

        assertThat(entryPoint.set).isSameAs(set);
    }

    @Test
    public void providesSetValues() {
        final Set<A> set = Collections.emptySet();

        class TestEntryPoint {
            @in
            Set<A> set;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out(type = out.Type.SET_VALUES)
            Set<A> provideSet() {
                return set;
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        TestModule module = new TestModule();
        O.of(module).with(entryPoint);

        // copies into immutable collection
        assertThat(entryPoint.set).isNotSameAs(set);
        assertThat(entryPoint.set).isEqualTo(set);
    }

    @Test
    public void providerMethodsConflict() {
        @the
        class TestModule {
            @out
            A provideA1() {
                throw new AssertionError();
            }

            @out
            A provideA2() {
                throw new AssertionError();
            }
        }

        try {
            O.of(new TestModule());
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void providesSetConflictsWithProvidesTypeSet() {
        @the
        class TestModule {
            @out(type = out.Type.SET)
            A provideSetElement() {
                throw new AssertionError();
            }

            @out
            Set<A> provideSet() {
                throw new AssertionError();
            }
        }

        try {
            O.of(new TestModule());
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void providesSetConflictsWithProvidesTypeSetValues() {
        @the
        class TestModule {
            @out(type = out.Type.SET_VALUES)
            Set<A> provideSetContribution() {
                throw new AssertionError();
            }

            @out
            Set<A> provideSet() {
                throw new AssertionError();
            }
        }

        try {
            O.of(new TestModule());
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void providesSetOfProvidersIsDifferentThanProvidesTypeSetValues() {
        final Set<A> set = Collections.emptySet();
        final Set<Provider<A>> providers = Collections.emptySet();

        class TestEntryPoint {
            @in
            Set<A> set;
            @in
            Set<Provider<A>> providers;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out(type = out.Type.SET_VALUES)
            Set<A> provideSetContribution() {
                return set;
            }

            @out
            Set<Provider<A>> provideProviders() {
                return providers;
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        TestModule module = new TestModule();
        O.of(module).with(entryPoint);

        // copies into immutable collection
        assertThat(entryPoint.set).isNotSameAs(set);
        assertThat(entryPoint.set).isEqualTo(set);
        assertThat(entryPoint.providers).isSameAs(providers);
    }

    @Test
    public void singletonsInjectedOnlyIntoProviders() {
        class TestEntryPoint {
            @in
            Provider<A> aProvider;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            @Singleton
            A provideA() {
                return new A();
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);
        assertThat(entryPoint.aProvider.get()).isSameAs(entryPoint.aProvider.get());
    }

    @Test
    public void moduleOverrides() {
        class TestEntryPoint {
            @in
            Provider<E> eProvider;
        }

        @the(in = TestEntryPoint.class)
        class BaseModule {
            @out
            F provideF() {
                throw new AssertionError();
            }

            @out
            E provideE(F f) {
                return new E(f);
            }
        }

        @the(override = true)
        class OverridesModule {
            @out
            F provideF() {
                return new F();
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new BaseModule(), new OverridesModule()).with(entryPoint);
        E e = entryPoint.eProvider.get();
        assertThat(e).isNotNull();
        assertThat(e.f).isNotNull();
    }

    @Test
    public void noJitBindingsForInterfaces() {
        class TestEntryPoint {
            @in
            RandomAccess randomAccess;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
        }

        O graph = O.of(new TestModule());
        try {
            graph.validate();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void objectGraphGetInterface() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
            }
        };

        @the(in = Runnable.class)
        class TestModule {
            @out
            Runnable provideRunnable() {
                return runnable;
            }
        }

        O graph = O.of(new TestModule());
        graph.validate();
        assertThat(graph.a(Runnable.class)).isSameAs(runnable);
    }

    @Test
    public void noProvideBindingsForAbstractClasses() {
        class TestEntryPoint {
            @in
            AbstractList abstractList;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
        }

        O graph = O.of(new TestModule());
        try {
            graph.validate();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    static class ExtendsParameterizedType extends AbstractList<Integer> {
        @in
        String string;

        @Override
        public Integer get(int i) {
            throw new AssertionError();
        }

        @Override
        public int size() {
            throw new AssertionError();
        }
    }

    /**
     * We've had bugs where we look for the wrong keys when a class extends a
     * parameterized class. Explicitly test that we can inject such classes.
     */
    @Test
    public void extendsParameterizedType() {
        class TestEntryPoint {
            @in
            ExtendsParameterizedType extendsParameterizedType;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            String provideString() {
                return "injected";
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);
        assertThat(entryPoint.extendsParameterizedType.string).isEqualTo("injected");
    }

    @Test
    public void injectParameterizedType() {
        class TestEntryPoint {
            @in
            List<String> listOfStrings;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            List<String> provideList() {
                return Arrays.asList("a", "b");
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);
        assertThat(entryPoint.listOfStrings).isEqualTo(Arrays.asList("a", "b"));
    }

    @Test
    public void injectWildcardType() {
        class TestEntryPoint {
            @in
            List<? extends Number> listOfNumbers;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            List<? extends Number> provideList() {
                return Arrays.asList(1, 2);
            }
        }

        try {
            O.of(new TestModule());
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }

    static class Parameterized<T> {
        @in
        String string;
    }

    @Test
    public void noConstructorInjectionsForClassesWithTypeParameters() {

        class TestEntryPoint {
            @in
            Parameterized<Long> parameterized;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            String provideString() {
                return "injected";
            }
        }

        O graph = O.of(new TestModule());
        try {
            graph.validate();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void moduleWithNoProvidesMethods() {
        @the
        class TestModule {
        }

        O.of(new TestModule());
    }

    @Test
    public void getInstance() {
        final AtomicInteger next = new AtomicInteger(0);

        @the(in = Integer.class)
        class TestModule {
            @out
            Integer provideInteger() {
                return next.getAndIncrement();
            }
        }

        O graph = O.of(new TestModule());
        assertThat(graph.a(Integer.class)).isEqualTo(0);
        assertThat(graph.a(Integer.class)).isEqualTo(1);
    }

    @Test
    public void getInstanceRequiresEntryPoint() {
        @the
        class TestModule {
            @out
            Integer provideInteger() {
                throw new AssertionError();
            }
        }

        O graph = O.of(new TestModule());
        try {
            graph.a(Integer.class);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void getInstanceOfPrimitive() {
        @the(in = int.class)
        class TestModule {
            @out
            int provideInt() {
                return 1;
            }
        }

        O graph = O.of(new TestModule());
        assertEquals(1, (int) graph.a(int.class));
    }

    @Test
    public void getInstanceOfArray() {
        @the(in = int[].class)
        class TestModule {
            @out
            int[] provideIntArray() {
                return new int[]{1, 2, 3};
            }
        }

        O graph = O.of(new TestModule());
        assertEquals("[1, 2, 3]", Arrays.toString(graph.a(int[].class)));
    }

    @Test
    public void getInstanceAndInjectMembersUseDifferentKeys() {
        class BoundTwoWays {
            @in
            String s;
        }

        @the(in = BoundTwoWays.class)
        class TestModule {
            @out
            BoundTwoWays provideBoundTwoWays() {
                BoundTwoWays result = new BoundTwoWays();
                result.s = "Pepsi";
                return result;
            }

            @out
            String provideString() {
                return "Coke";
            }
        }

        O graph = O.of(new TestModule());
        BoundTwoWays provided = graph.a(BoundTwoWays.class);
        assertEquals("Pepsi", provided.s);

        BoundTwoWays membersInjected = new BoundTwoWays();
        graph.with(membersInjected);
        assertEquals("Coke", membersInjected.s);
    }

    static class NoInjections {
        NoInjections(Void noDefaultConstructorEither) {
        }
    }

    @Test
    public void entryPointNeedsNoInjectAnnotation() {
        @the(in = NoInjections.class)
        class TestModule {
        }

        O.of(new TestModule()).validate();
    }

    static class InjectMembersOnly {
        InjectMembersOnly(Void noInjectableConstructor) {
        }

        @in
        String string;
    }

    @Test
    public void cannotGetOnMembersOnlyInjectionPoint() {
        @the(in = InjectMembersOnly.class)
        class TestModule {
            @out
            String provideString() {
                return "injected";
            }
        }

        O graph = O.of(new TestModule());
        try {
            graph.a(InjectMembersOnly.class);
            fail();
        } catch (IllegalStateException expected) {
        }

        InjectMembersOnly instance = new InjectMembersOnly(null);
        graph.with(instance);
        assertThat(instance.string).isEqualTo("injected");
    }

    @Test
    public void nonEntryPointNeedsInjectAnnotation() {
        @the
        class TestModule {
            @out
            String provideString(NoInjections noInjections) {
                throw new AssertionError();
            }
        }

        O graph = O.of(new TestModule());
        try {
            graph.validate();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    static class TwoAtInjectConstructors {
        @in
        TwoAtInjectConstructors() {
        }

        @in
        TwoAtInjectConstructors(String s) {
        }
    }

    @Test
    public void twoAtInjectConstructorsIsRejected() {
        @the(in = TwoAtInjectConstructors.class)
        class TestModule {
            @out
            String provideString() {
                throw new AssertionError();
            }
        }

        O graph = O.of(new TestModule());
        try {
            graph.validate();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void runtimeProvidesMethodsExceptionsAreNotWrapped() {
        class TestEntryPoint {
            @in
            String string;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            String provideString() {
                throw new ClassCastException("foo");
            }
        }

        try {
            O.of(new TestModule()).with(new TestEntryPoint());
            fail();
        } catch (ClassCastException e) {
            assertThat(e.getMessage()).isEqualTo("foo");
        }
    }

    static class ThrowsOnConstruction {
        @in
        ThrowsOnConstruction() {
            throw new ClassCastException("foo");
        }
    }

    @Test
    public void runtimeConstructorExceptionsAreNotWrapped() {
        @the(in = ThrowsOnConstruction.class)
        class TestModule {
        }

        try {
            O.of(new TestModule()).a(ThrowsOnConstruction.class);
            fail();
        } catch (ClassCastException e) {
            assertThat(e.getMessage()).isEqualTo("foo");
        }
    }

    static class SingletonLinkedFromExtension {
        @in
        C c; // Singleton.
    }

    @the(complete = false, in = C.class)
    static class RootModule {
    }

    @the(addsTo = RootModule.class, in = SingletonLinkedFromExtension.class)
    static class ExtensionModule {
    }

    @Test
    public void testSingletonLinkingThroughExtensionGraph() {
        O root = O.of(new RootModule());
        // DO NOT CALL root.get(C.class)) HERE to get forced-linking behaviour from plus();
        O extension = root.plus(new ExtensionModule());
        assertThat(extension.a(SingletonLinkedFromExtension.class).c).isSameAs(root.a(C.class));
    }

    @Test
    public void privateFieldsFail() {
        class Test {
            @in
            private Object nope;
        }

        @the(in = Test.class)
        class TestModule {
            @out
            Object provideObject() {
                return null;
            }
        }

        try {
            O.of(new TestModule()).with(new Test());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("Can't inject private field: ");
        }
    }

    @Test
    public void privateConstructorsFail() {
        class Test {
            @in
            private Test() {
            }
        }

        @the(in = Test.class)
        class TestModule {
        }

        try {
            O.of(new TestModule()).a(Test.class);
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("Can't inject private constructor: ");
        }
    }

    /**
     * https://github.com/square/dagger/issues/231
     */
    @Test
    public void atInjectAlwaysRequiredForConstruction() {
        @the(in = ArrayList.class)
        class TestModule {
        }

        O objectGraph = O.of(new TestModule());
        objectGraph.validate();
        try {
            objectGraph.a(ArrayList.class);
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("Unable to create binding for java.util.ArrayList");
        }
    }
}
