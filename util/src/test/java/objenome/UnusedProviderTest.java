/*
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

import java.util.Set;

import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class UnusedProviderTest {

    @Test
    public void unusedProvidesMethod_whenModuleLibrary_passes() throws Exception {
        class EntryPoint {
        }
        class BagOfMoney {
        }
        @the(in = EntryPoint.class, library = true)
        class TestModule {
            @out
            BagOfMoney providesMoney() {
                return new BagOfMoney();
            }
        }

        O graph = O.of(new TestModule());
        graph.validate();
    }

    @Test
    public void unusedProviderMethod_whenNotLibraryModule_fails() throws Exception {
        class EntryPoint {
        }
        class BagOfMoney {
        }

        @the(in = EntryPoint.class)
        class TestModule {
            @out
            BagOfMoney providesMoney() {
                return new BagOfMoney();
            }
        }

        try {
            O graph = O.of(new TestModule());
            graph.validate();
            fail("Validation should have exploded!");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void whenLibraryModulePlussedToNecessaryModule_shouldNotFailOnUnusedLibraryModule()
            throws Exception {
        class EntryPoint {
        }
        class BagOfMoney {
        }

        @the(in = EntryPoint.class, library = true)
        class ExampleLibraryModule {
            @out
            BagOfMoney providesMoney() {
                return new BagOfMoney();
            }
        }

        @the(in = EntryPoint.class)
        class TestModule {
        }

        O graph = O.of(new TestModule());
        graph = graph.plus(new ExampleLibraryModule());
        graph.validate();
    }

    @Test
    public void unusedSetBinding() throws Exception {
        @the
        class TestModule {
            @out(type = out.Type.SET)
            String provideA() {
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
    public void unusedSetValuesBinding() throws Exception {
        @the
        class TestModule {
            @out(type = out.Type.SET_VALUES)
            Set<String> provideA() {
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
}
