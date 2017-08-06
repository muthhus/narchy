package com.github.disc99.injector.util;

import java.util.function.Function;

public final class Throwables {
    private Throwables() {
    }

    public static <T, R> Function<T, R> uncheck(UncheckFunction<T, R> func) {
        return func;
    }
}
