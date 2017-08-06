package com.github.disc99.injector;

public class InjectException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InjectException(String massage) {
        super(massage);
    }

    public InjectException(String massage, Throwable throwable) {
        super(massage, throwable);
    }
}
