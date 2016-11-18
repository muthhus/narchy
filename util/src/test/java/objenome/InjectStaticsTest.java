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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class InjectStaticsTest {
  @Before public void setUp() {
    InjectsOneField.staticField = null;
    InjectsStaticAndNonStatic.staticField = null;
  }

  public static class InjectsOneField {
    @in
    static String staticField;
  }

  public static class InjectsStaticAndNonStatic {
    @in
    Integer nonStaticField;
    @in
    static String staticField;
  }

  @Test public void injectStatics() {
    @the(The = InjectsOneField.class)
    class TestModule {
      @out
      String provideString() {
        return "static";
      }
    }

    O graph = O.via(new DynamicLoader(),new TestModule());
    assertThat(InjectsOneField.staticField).isNull();
    graph.injectStatics();
    assertThat(InjectsOneField.staticField).isEqualTo("static");
  }

  @Test public void instanceFieldsNotInjectedByInjectStatics() {
    @the(
        The = InjectsStaticAndNonStatic.class,
        in = InjectsStaticAndNonStatic.class)
    class TestModule {
      @out
      String provideString() {
        return "static";
      }
      @out
      Integer provideInteger() {
        throw new AssertionError();
      }
    }

    O graph = O.via(new DynamicLoader(), new TestModule());
    assertThat(InjectsStaticAndNonStatic.staticField).isNull();
    graph.injectStatics();
    assertThat(InjectsStaticAndNonStatic.staticField).isEqualTo("static");
  }

  @Test public void staticFieldsNotInjectedByInjectMembers() {
    @the(
        The = InjectsStaticAndNonStatic.class,
        in = InjectsStaticAndNonStatic.class)
    class TestModule {
      @out
      String provideString() {
        throw new AssertionError();
      }
      @out
      Integer provideInteger() {
        return 5;
      }
    }

    O graph = O.via(new DynamicLoader(), new TestModule());
    assertThat(InjectsStaticAndNonStatic.staticField).isNull();
    InjectsStaticAndNonStatic object = new InjectsStaticAndNonStatic();
    graph.with(object);
    assertThat(InjectsStaticAndNonStatic.staticField).isNull();
    assertThat(object.nonStaticField).isEqualTo(5);
  }
}
