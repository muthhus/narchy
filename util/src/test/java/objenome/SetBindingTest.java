/*
 * Copyright (C) 2012 Google Inc.
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

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static objenome.out.Type.SET;
import static objenome.out.Type.SET_VALUES;
import static java.util.Collections.emptySet;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public final class SetBindingTest {
  @Test public void multiValueBindings_SingleModule() {
    class TestEntryPoint {
      @in
      Set<String> strings;
    }

    @the(in = TestEntryPoint.class)
    class TestModule {
      @out(type=SET) String provideFirstString() { return "string1"; }
      @out(type=SET) String provideSecondString() { return "string2"; }
    }

    TestEntryPoint ep = injectWithModule(new TestEntryPoint(), new TestModule());
    assertEquals(set("string1", "string2"), ep.strings);
  }

  @Test public void multiValueBindings_MultiModule() {
    class TestEntryPoint {
      @in
      Set<String> strings;
    }

    @the
    class TestIncludesModule {
      @out(type=SET) String provideSecondString() { return "string2"; }
    }

    @the(in = TestEntryPoint.class, extend = TestIncludesModule.class)
    class TestModule {
      @out(type=SET) String provideFirstString() { return "string1"; }

      @out(type=SET_VALUES) Set<String> provideDefaultStrings() {
        return emptySet();
      }
    }

    TestEntryPoint ep = injectWithModule(new TestEntryPoint(),
        new TestModule(), new TestIncludesModule());
    assertEquals(set("string1", "string2"), ep.strings);
  }

  @Test public void multiValueBindings_MultiModule_NestedSet() {
    class TestEntryPoint {
      @in
      Set<Set<String>> stringses;
    }

    @the
    class TestIncludesModule {
      @out(type=SET) Set<String> provideSecondStrings() { return set("string2"); }
    }

    @the(in = TestEntryPoint.class, extend = TestIncludesModule.class)
    class TestModule {
      @out(type=SET) Set<String> provideFirstStrings() { return set("string1"); }

      @out(type=SET_VALUES) Set<Set<String>> provideDefaultStringeses() {
        return set(set("string3"));
      }
    }

    TestEntryPoint ep = injectWithModule(new TestEntryPoint(),
        new TestModule(), new TestIncludesModule());
    assertEquals(set(set("string1"),set("string2"), set("string3")), ep.stringses);
  }

  @Test public void multiValueBindings_WithSingletonAndDefaultValues() {
    final AtomicInteger singletonCounter = new AtomicInteger(100);
    final AtomicInteger defaultCounter = new AtomicInteger(200);
    class TestEntryPoint {
      @in
      Set<Integer> objects1;
      @in
      Set<Integer> objects2;
    }

    @the(in = TestEntryPoint.class)
    class TestModule {
      @out(type=SET) @Singleton Integer a() { return singletonCounter.getAndIncrement(); }
      @out(type=SET) Integer b() { return defaultCounter.getAndIncrement(); }
    }

    TestEntryPoint ep = injectWithModule(new TestEntryPoint(), new TestModule());
    assertEquals(set(100, 200), ep.objects1);
    assertEquals(set(100, 201), ep.objects2);
  }

  @Test public void multiValueBindings_WithSingletonsAcrossMultipleInjectableTypes() {
    final AtomicInteger singletonCounter = new AtomicInteger(100);
    final AtomicInteger defaultCounter = new AtomicInteger(200);
    class TestEntryPoint1 {
      @in
      Set<Integer> objects1;
    }
    class TestEntryPoint2 {
      @in
      Set<Integer> objects2;
    }

    @the(in = { TestEntryPoint1.class, TestEntryPoint2.class })
    class TestModule {
      @out(type=SET) @Singleton Integer a() { return singletonCounter.getAndIncrement(); }
      @out(type=SET) Integer b() { return defaultCounter.getAndIncrement(); }
    }

    O graph = O.via(new DynamicLoader(), new TestModule());
    TestEntryPoint1 ep1 = graph.with(new TestEntryPoint1());
    TestEntryPoint2 ep2 = graph.with(new TestEntryPoint2());
    assertEquals(set(100, 200), ep1.objects1);
    assertEquals(set(100, 201), ep2.objects2);

 }

  @Test public void multiValueBindings_WithQualifiers() {
    class TestEntryPoint {
      @in
      Set<String> strings;
      @in
      @Named("foo") Set<String> fooStrings;
    }

    @the(in = TestEntryPoint.class)
    class TestModule {
      @out(type=SET_VALUES) Set<String> provideString1() {
        return set("string1");
      }
      @out(type=SET) String provideString2() { return "string2"; }
      @out(type=SET) @Named("foo") String provideString3() { return "string3"; }
      @out(type=SET_VALUES) @Named("foo") Set<String> provideString4() {
        return set("string4");
      }
    }

    TestEntryPoint ep = injectWithModule(new TestEntryPoint(), new TestModule());
    assertEquals(set("string1", "string2"), ep.strings);
    assertEquals(set("string4", "string3"), ep.fooStrings);
  }

  // TODO(cgruber): Move this into an example project.
  @Test public void sampleMultiBindingLogger() {
    class TestEntryPoint {
      @in
      Logger logger;
      public void doStuff() {
        Throwable t = new NullPointerException("Naughty Naughty");
        this.logger.log("Logging an error", t);
      }
    }

    final AtomicReference<String> logoutput = new AtomicReference<String>();
    @the
    class LogModule {
      @out(type=SET) LogSink outputtingLogSink() {
        return new LogSink() {
          @Override public void log(LogMessage message) {
            StringWriter sw = new StringWriter();
            message.error.printStackTrace(new PrintWriter(sw));
            logoutput.set(message.message + '\n' + sw.getBuffer());
          }
        };
      }
    }
    @the(in = TestEntryPoint.class)
    class TestModule {
      @out(type=SET) LogSink nullLogger() {
        return new LogSink() { @Override public void log(LogMessage message) {} };
      }
    }

    TestEntryPoint ep = injectWithModule(new TestEntryPoint(),new TestModule(), new LogModule());
    assertNull(logoutput.get());
    ep.doStuff();
    assertNotNull(logoutput.get());
    assertThat(logoutput.get()).contains("Naughty Naughty");
    assertThat(logoutput.get()).contains("NullPointerException");
  }

  @Test public void duplicateValuesContributed() {
    class TestEntryPoint {
      @in
      Set<String> strings;
    }

    @the(in = TestEntryPoint.class)
    class TestModule {
      @out(type=SET) String provideString1() { return "a"; }
      @out(type=SET) String provideString2() { return "a"; }
      @out(type=SET) String provideString3() { return "b"; }
    }

    TestEntryPoint ep = injectWithModule(new TestEntryPoint(), new TestModule());
    assertThat(ep.strings).containsExactly("a", "b");
  }

  @Test public void validateSetBinding() {
    class TestEntryPoint {
      @in
      Set<String> strings;
    }

    @the(in = TestEntryPoint.class)
    class TestModule {
      @out(type=SET) String provideString1() { return "string1"; }
      @out(type=SET) String provideString2() { return "string2"; }
    }

    O graph = O.via(new DynamicLoader(), new TestModule());
    graph.validate();
  }

  @Test public void validateEmptySetBinding() {
    class TestEntryPoint {
      @in
      Set<String> strings;
    }

    @the(in = TestEntryPoint.class)
    class TestModule {
      @out(type=SET_VALUES) Set<String> provideDefault() {
        return emptySet();
      }
    }

    O graph = O.via(new DynamicLoader(), new TestModule());
    graph.validate();
  }

  @Test public void validateLibraryModules() {
    class TestEntryPoint {}

    @the(library = true)
    class SetModule {
      @out(type = SET)
      public String provideString() {
        return "";
      }
    }

    @the(in = TestEntryPoint.class, extend = SetModule.class)
    class TestModule {}

    O graph = O.via(new DynamicLoader(),
        new TestModule(), new SetModule());
    graph.validate();
  }

  @Test public void validateLibraryModules_nonLibraryContributors() {
    class TestEntryPoint {}

    @the(library = true)
    class SetModule1 {
      @out(type = SET)
      public String provideString() {
        return "a";
      }
    }

    @the
    class SetModule2 {
      @out(type = SET)
      public String provideString() {
        return "b";
      }
    }

    @the(in = TestEntryPoint.class, extend = { SetModule1.class, SetModule2.class })
    class TestModule {}

    O graph = O.via(new DynamicLoader(),
        new TestModule(), new SetModule1(), new SetModule2());
    try {
      graph.validate();
      fail();
    } catch (IllegalStateException expected) {}
  }

  static class Logger {
    @in
    Set<LogSink> loggers;
    public void log(String text, Throwable error) {
      LogMessage m = new LogMessage(text, error);
      for (LogSink sink : loggers) {
        sink.log(m);
      }
    }
  }

  static class LogMessage {
    public final String message;
    public final Throwable error;
    public LogMessage (String message, Throwable error) {
      this.message = message;
      this.error = error;
    }
  }

  interface LogSink {
    void log(LogMessage message);
  }

  private static <T> T injectWithModule(T ep, Object... modules) {
    return O.via(new DynamicLoader(), modules).with(ep);
  }

  private static <T> Set<T> set(T... ts) {
    return new LinkedHashSet<T>(Arrays.asList(ts));
  }

}
