package no.birkett.kiwi;

/**
 * Created by alex on 30/01/16.
 */
public class KiwiException extends RuntimeException {
    public KiwiException() {

    }
    public KiwiException(String message) {
        super(message);
    }
}
