/*
 * Copyright (C) 2012 Square, Inc.
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

import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public final class ProblemDetectorTest {
  @Test public void atInjectCircularDependenciesDetected() {
    class TestEntryPoint {
      @in
      Rock rock;
    }

    @the(in = TestEntryPoint.class)
    class TestModule {
    }

    O graph = O.via(new DynamicLoader(), new TestModule());
    try {
      graph.validate();
      fail();
    } catch (RuntimeException expected) {
    }
  }

  @Test public void providesCircularDependenciesDetected() {
    @the
    class TestModule {
      @out
      Integer provideInteger(String s) {
        throw new AssertionError();
      }
      @out
      String provideString(Integer i) {
        throw new AssertionError();
      }
    }

    O graph = O.via(new DynamicLoader(), new TestModule());
    try {
      graph.validate();
      fail();
    } catch (RuntimeException expected) {
    }
  }

  @Test public void validateLazy() {
    @the(library = true)
    class TestModule {
      @out
      Integer dependOnLazy(lazy<String> lazyString) {
        throw new AssertionError();
      }
      @out
      String provideLazyValue() {
        throw new AssertionError();
      }
    }

    O graph = O.via(new DynamicLoader(), new TestModule());
    graph.validate();
  }

  static class Rock {
    @in
    Scissors scissors;
  }

  static class Scissors {
    @in
    Paper paper;
  }

  static class Paper {
    @in
    Rock rock;
  }
}
