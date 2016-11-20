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

import objenome.impl.MembersInjector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Provider;
import javax.inject.Singleton;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests MembersInjector injection, and how object graph features interact with
 * types unconstructable types (types that support members injection only).
 */
@RunWith(JUnit4.class)
public final class MembersInjectorTest {
    @Test
    public void injectMembers() {
        class TestEntryPoint {
            @in
            MembersInjector<Injectable> membersInjector;
        }

        @the(in = TestEntryPoint.class)
        class StringModule {
            @out
            String provideString() {
                return "injected";
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new StringModule()).with(entryPoint);
        Injectable injectable = new Injectable();
        entryPoint.membersInjector.injectMembers(injectable);
        assertThat(injectable.injected).isEqualTo("injected");
    }

    static class Injectable {
        @in
        String injected;
    }

    static class Unconstructable {
        final String constructor;
        @in
        String injected;

        Unconstructable(String constructor) {
            this.constructor = constructor;
        }
    }

    @Test
    public void membersInjectorOfUnconstructableIsOkay() {
        class TestEntryPoint {
            @in
            MembersInjector<Unconstructable> membersInjector;
        }

        @the(in = TestEntryPoint.class)
        class StringModule {
            @out
            String provideString() {
                return "injected";
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new StringModule()).with(entryPoint);
        Unconstructable object = new Unconstructable("constructor");
        entryPoint.membersInjector.injectMembers(object);
        assertThat(object.constructor).isEqualTo("constructor");
        assertThat(object.injected).isEqualTo("injected");
    }


    @Test
    public void injectionOfUnconstructableFails() {
        class TestEntryPoint {
            @in
            Unconstructable unconstructable;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
        }

        O graph = O.of(new TestModule());
        try {
            graph.a(TestEntryPoint.class);
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void instanceInjectionOfMembersOnlyType() {
        class TestEntryPoint {
            @in
            Provider<Unconstructable> provider;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
        }

        O graph = O.of(new TestModule());
        try {
            graph.a(TestEntryPoint.class);
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void rejectUnconstructableSingleton() {
        class TestEntryPoint {
            @in
            MembersInjector<UnconstructableSingleton> membersInjector;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
        }

        O graph = O.of(new TestModule());
        try {
            graph.a(TestEntryPoint.class);
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Singleton
    static class UnconstructableSingleton {
        final String constructor;
        @in
        String injected;

        UnconstructableSingleton(String constructor) {
            this.constructor = constructor;
        }
    }

    static class NonStaticInner {
        @in
        String injected;
    }

    @Test
    public void membersInjectorOfNonStaticInnerIsOkay() {
        class TestEntryPoint {
            @in
            MembersInjector<NonStaticInner> membersInjector;
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
        NonStaticInner nonStaticInner = new NonStaticInner();
        entryPoint.membersInjector.injectMembers(nonStaticInner);
        assertThat(nonStaticInner.injected).isEqualTo("injected");
    }

    @Test
    public void instanceInjectionOfNonStaticInnerFailsEarly() {
        class TestEntryPoint {
            @in
            NonStaticInner nonStaticInner;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
        }

        O graph = O.of(new TestModule());
        try {
            graph.a(TestEntryPoint.class);
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void providesMethodsAndMembersInjectionDoNotConflict() {
        class InjectsString {
            @in
            String value;
        }

        class TestEntryPoint {
            @in
            Provider<InjectsString> provider;
            @in
            MembersInjector<InjectsString> membersInjector;
        }

        @the(in = TestEntryPoint.class)
        class TestModule {
            @out
            InjectsString provideInjectsString() {
                InjectsString result = new InjectsString();
                result.value = "provides";
                return result;
            }

            @out
            String provideString() {
                return "members";
            }
        }

        TestEntryPoint entryPoint = new TestEntryPoint();
        O.of(new TestModule()).with(entryPoint);

        InjectsString provided = entryPoint.provider.get();
        assertThat(provided.value).isEqualTo("provides");

        InjectsString membersInjected = new InjectsString();
        entryPoint.membersInjector.injectMembers(membersInjected);
        assertThat(membersInjected.value).isEqualTo("members");
    }
}
