package storage;

/** Captured result from the guarded PowerShell raw-write process. */
public record RawWriteResult(int exitCode, String output) {
    public boolean successful() {
        return exitCode == 0;
    }
}
