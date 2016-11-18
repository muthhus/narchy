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

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class LazyInjectionTest {
  @Test public void getLazyDoesNotCauseInjectedTypesToBeLoaded() {
    @the(in = LazyEntryPoint.class)
    class TestModule {
    }

    O.via(new DynamicLoader(), new TestModule());
    assertThat(lazyEntryPointLoaded).isFalse();
  }

  private static boolean lazyEntryPointLoaded = false;
  static class LazyEntryPoint {
    static {
      lazyEntryPointLoaded = true;
    }
  }

  @Test public void getLazyDoesNotCauseProvidesParametersToBeLoaded() {
    @the
    class TestModule {
      @out
      Object provideObject(LazyProvidesParameter parameter) {
        throw new AssertionError();
      }
    }

    O.via(new DynamicLoader(), new TestModule());
    assertThat(lazyProvidesParameterLoaded).isFalse();
  }

  private static boolean lazyProvidesParameterLoaded = false;
  static class LazyProvidesParameter {
    static {
      lazyProvidesParameterLoaded = true;
    }
  }

  @Test public void getLazyDoesNotCauseProvidesResultToBeLoaded() {
    @the
    class TestModule {
      @out
      LazyProvidesResult provideLazy() {
        throw new AssertionError();
      }
    }

    O.via(new DynamicLoader(), new TestModule());
    assertThat(lazyProvidesResultLoaded).isFalse();
  }

  private static boolean lazyProvidesResultLoaded = false;
  static class LazyProvidesResult {
    static {
      lazyProvidesResultLoaded = true;
    }
  }

  @Test public void getLazyDoesNotCauseStaticsToBeLoaded() {
    @the(The = LazyInjectStatics.class)
    class TestModule {
    }

    O.via(new DynamicLoader(), new TestModule());
    assertThat(LazyInjectStaticsLoaded).isFalse();
  }

  private static boolean LazyInjectStaticsLoaded = false;
  static class LazyInjectStatics {
    static {
      LazyInjectStaticsLoaded = true;
    }
  }

  @Test public void lazyInjectionRequiresProvidesMethod() {
    class TestEntryPoint {
      @in
      String injected;
    }

    @the(in = TestEntryPoint.class)
    class TestModule {
      @out
      String provideString(Integer integer) {
        return integer.toString();
      }
      @out
      Integer provideInteger() {
        return 5;
      }
    }

    O objectGraph = O.via(new DynamicLoader(), new TestModule());
    TestEntryPoint entryPoint = new TestEntryPoint();
    objectGraph.with(entryPoint);
    assertThat(entryPoint.injected).isEqualTo("5");
  }
}
