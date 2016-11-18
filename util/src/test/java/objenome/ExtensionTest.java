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

import javax.inject.Singleton;
import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public final class ExtensionTest {
  @Singleton
  static class A {
    @in
    A() {}
  }

  static class B {
    @in
    A a;
  }

  @Singleton
  static class C {
    @in
    A a;
    @in
    B b;
  }

  static class D {
    @in
    A a;
    @in
    B b;
    @in
    C c;
  }

  @the(in = { A.class, B.class }) static class RootModule { }

  @the(addsTo = RootModule.class, in = { C.class, D.class })
  static class ExtensionModule { }

  @Test public void basicExtension() {
    assertNotNull(O.via(new DynamicLoader(), new RootModule())
        .plus(new ExtensionModule()));
  }

  @Test public void basicInjection() {
    O root = O.via(new DynamicLoader(), new RootModule());
    assertThat(root.a(A.class)).isNotNull();
    assertThat(root.a(A.class)).isSameAs(root.a(A.class)); // Present and Singleton.
    assertThat(root.a(B.class)).isNotSameAs(root.a(B.class)); // Not singleton.
    assertFailInjectNotRegistered(root, C.class); // Not declared in RootModule.
    assertFailInjectNotRegistered(root, D.class); // Not declared in RootModule.

    // Extension graph behaves as the root graph would for root-ish things.
    O extension = root.plus(new ExtensionModule());
    assertThat(root.a(A.class)).isSameAs(extension.a(A.class));
    assertThat(root.a(B.class)).isNotSameAs(extension.a(B.class));
    assertThat(root.a(B.class).a).isSameAs(extension.a(B.class).a);

    assertThat(extension.a(C.class).a).isNotNull();
    assertThat(extension.a(D.class).c).isNotNull();
  }

  @Test public void scopedGraphs() {
    O app = O.via(new DynamicLoader(), new RootModule());
    assertThat(app.a(A.class)).isNotNull();
    assertThat(app.a(A.class)).isSameAs(app.a(A.class));
    assertThat(app.a(B.class)).isNotSameAs(app.a(B.class));
    assertFailInjectNotRegistered(app, C.class);
    assertFailInjectNotRegistered(app, D.class);

    O request1 = app.plus(new ExtensionModule());
    O request2 = app.plus(new ExtensionModule());
    for (O request : Arrays.asList(request1, request2)) {
      assertThat(request.a(A.class)).isNotNull();
      assertThat(request.a(A.class)).isSameAs(request.a(A.class));
      assertThat(request.a(B.class)).isNotSameAs(request.a(B.class));
      assertThat(request.a(C.class)).isNotNull();
      assertThat(request.a(C.class)).isSameAs(request.a(C.class));
      assertThat(request.a(D.class)).isNotSameAs(request.a(D.class));
    }

    // Singletons are one-per-graph-instance where they are declared.
    assertThat(request1.a(C.class)).isNotSameAs(request2.a(C.class));
    // Singletons that come from common roots should be one-per-common-graph-instance.
    assertThat(request1.a(C.class).a).isSameAs(request2.a(C.class).a);
  }

  private static void assertFailInjectNotRegistered(O graph, Class<?> clazz) {
    try {
      assertThat(graph.a(clazz)).isNull();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).contains("No inject");
    }
  }
}
