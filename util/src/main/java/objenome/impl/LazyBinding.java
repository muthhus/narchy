/*
 * Copyright (C) 2012 Google, Inc.
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
package objenome.impl;

import objenome.lazy;

/**
 * Injects a Lazy wrapper for a type T
 */
final class LazyBinding<T> extends Binding<lazy<T>> {

    private final static Object NOT_PRESENT = new Object();

    private final String lazyKey;
    private final ClassLoader loader;
    volatile Binding<T> delegate;

    LazyBinding(String key, Object requiredBy, ClassLoader loader, String lazyKey) {
        super(key, null, false, requiredBy);
        this.loader = loader;
        this.lazyKey = lazyKey;
    }

    @SuppressWarnings("unchecked") // At runtime we know it's a Binding<Lazy<T>>.
    @Override
    public void attach(Linker linker) {
        delegate = (Binding<T>) linker.requestBinding(lazyKey, requiredBy, loader);
    }

    @Override
    public void injectMembers(lazy<T> t) {
        throw new UnsupportedOperationException(); // Injecting into a custom Lazy not supported.
    }

    @Override
    public lazy<T> get() {
        return new lazy<T>() {
            private volatile Object cacheValue = NOT_PRESENT;

            @SuppressWarnings("unchecked") // Delegate is of type T
            @Override
            public T get() {
                if (cacheValue == NOT_PRESENT) {
                    synchronized (this) {
                        if (cacheValue == NOT_PRESENT) {
                            cacheValue = delegate.get();
                        }
                    }
                }
                return (T) cacheValue;
            }
        };
    }

    // public void getDependencies() not overridden.
    // We don't add 'delegate' because it isn't actually used by get() or injectMembers().
}
