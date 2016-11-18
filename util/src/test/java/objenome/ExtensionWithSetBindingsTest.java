/*
 * Copyright (C) 2013 Google Inc.
 * Copyright (C) 2013 Square Inc.
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

import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;
import static objenome.out.Type.SET;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public final class ExtensionWithSetBindingsTest {
  private static final AtomicInteger counter = new AtomicInteger(0);

  @Singleton
  static class RealSingleton {
    @in
    Set<Integer> ints;
  }

  @Singleton
  static class Main {
    @in
    Set<Integer> ints;
  }

  @the(in = RealSingleton.class)
  static class RootModule {
    @out(type=SET) @Singleton
    static Integer provideA() { return counter.getAndIncrement(); }
    @out(type=SET) @Singleton
    static Integer provideB() { return counter.getAndIncrement(); }
  }

  @the(addsTo = RootModule.class, in = Main.class )
  static class ExtensionModule {
    @out(type=SET) @Singleton
    static Integer provideC() { return counter.getAndIncrement(); }
    @out(type=SET) @Singleton
    static Integer provideD() { return counter.getAndIncrement(); }
  }

  @the
  static class EmptyModule {
  }

  @the(library = true)
  static class DuplicateModule {
    @out
    @Singleton
    static String provideFoo() { return "foo"; }
    @out
    @Singleton
    static String provideBar() { return "bar"; }
  }

  @Test public void basicInjectionWithExtension() {
    O root = O.via(new DynamicLoader(), new RootModule());
    RealSingleton rs = root.a(RealSingleton.class);
    assertThat(rs.ints).containsExactly(0, 1);

    O extension = root.plus(new ExtensionModule());
    Main main = extension.a(Main.class);
    assertThat(main.ints).containsExactly(0, 1, 2, 3);

    // Second time around.
    O extension2 = root.plus(new ExtensionModule());
    Main main2 = extension2.a(Main.class);
    assertThat(main2.ints).containsExactly(0, 1, 4, 5);
  }

  @the(extend = ExtensionModule.class, override = true)
  static class TestModule {
    @out(type=SET) @Singleton
    static Integer provide9999() { return 9999; }
  }

  @Test public void basicInjectionWithExtensionAndOverrides() {
    try {
      O.via(new DynamicLoader(), new RootModule()).plus(new TestModule());
      fail("Should throw exception.");
    } catch (IllegalArgumentException e) {
      assertEquals("TestModule: Module overrides cannot contribute set bindings.", e.getMessage());
    }
  }

  @Test public void duplicateBindingsInSecondaryModule() {
    try {
      O.via(new DynamicLoader(), new EmptyModule(), new DuplicateModule());
      fail("Should throw exception.");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().startsWith("DuplicateModule: Duplicate"));
    }
  }
}
