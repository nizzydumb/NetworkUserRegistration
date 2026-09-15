package storage;

public final class RawDriveException extends RuntimeException {
    private final int errorCode;
    public RawDriveException(String message, int errorCode) { super(message); this.errorCode = errorCode; }
    public int errorCode() { return errorCode; }
}
