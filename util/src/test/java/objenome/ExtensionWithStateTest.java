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

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public final class ExtensionWithStateTest {
  static class A { }

  static class B {
    @in
    A a;
  }

  @the(
      in = A.class, // for testing
      complete = false
  )
  static class RootModule {
    final A a;
    RootModule(A a) {
      this.a = a;
    }
    @out
    A provideA() { return a; }
  }

  @the(addsTo = RootModule.class, in = { B.class })
  static class ExtensionModule { }

  @Test public void basicInjectionWithExtension() {
    A a = new A();
    O root = O.via(new DynamicLoader(), new RootModule(a));
    assertThat(root.a(A.class)).isSameAs(a);

    // Extension graph behaves as the root graph would for root-ish things.
    O extension = root.plus(new ExtensionModule());
    assertThat(extension.a(A.class)).isSameAs(a);
    assertThat(extension.a(B.class).a).isSameAs(a);
  }

}
