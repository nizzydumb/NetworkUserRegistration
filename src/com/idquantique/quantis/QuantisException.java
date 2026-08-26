package com.idquantique.quantis;

/** Exception reported by the native Quantis library. */
public class QuantisException extends Exception {
    public QuantisException() {
        super();
    }

    public QuantisException(String message) {
        super(message);
    }

    public QuantisException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuantisException(Throwable cause) {
        super(cause);
    }
}
