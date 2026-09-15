package storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Invokes the guarded Windows PowerShell writer for raw byte-offset operations. */
public final class WindowsRawByteWriter {
    private final Path scriptPath;
    private final Path powershellExecutable;

    public WindowsRawByteWriter(Path scriptPath) {
        this.scriptPath = Objects.requireNonNull(scriptPath, "PowerShell script path is required.")
                .toAbsolutePath().normalize();
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null || systemRoot.isBlank()) {
            systemRoot = "C:\\Windows";
        }
        this.powershellExecutable = Path.of(
                systemRoot,
                "System32",
                "WindowsPowerShell",
                "v1.0",
                "powershell.exe"
        );
    }

    public RawWriteResult dryRun(RawByteWriteRequest request) throws IOException, InterruptedException {
        return runPhysical(request, false);
    }

    public RawWriteResult writePhysical(RawByteWriteRequest request, String confirmation)
            throws IOException, InterruptedException {
        String expected = confirmationToken(request.driveNumber());
        if (!expected.equals(confirmation)) {
            throw new IllegalArgumentException("Confirmation must exactly equal " + expected);
        }
        return run(commandForPhysical(request, true, confirmation));
    }

    public RawWriteResult writeImage(Path imagePath, long byteOffset, byte[] bytes)
            throws IOException, InterruptedException {
        RawByteWriteRequest request = new RawByteWriteRequest(0, byteOffset, bytes);
        List<String> command = baseCommand();
        command.add("-ImagePath");
        command.add(imagePath.toAbsolutePath().normalize().toString());
        command.add("-ByteOffset");
        command.add(Long.toString(byteOffset));
        command.add("-HexBytes");
        command.add(request.hexBytes());
        return run(command);
    }

    public static String confirmationToken(int driveNumber) {
        if (driveNumber < 0) {
            throw new IllegalArgumentException("Drive number cannot be negative.");
        }
        return "WRITE-PHYSICALDRIVE-" + driveNumber;
    }

    private RawWriteResult runPhysical(RawByteWriteRequest request, boolean execute)
            throws IOException, InterruptedException {
        return run(commandForPhysical(request, execute, null));
    }

    private List<String> commandForPhysical(RawByteWriteRequest request, boolean execute, String confirmation) {
        Objects.requireNonNull(request, "Write request is required.");
        List<String> command = baseCommand();
        command.add("-DriveNumber");
        command.add(Integer.toString(request.driveNumber()));
        command.add("-ByteOffset");
        command.add(Long.toString(request.byteOffset()));
        command.add("-HexBytes");
        command.add(request.hexBytes());
        if (execute) {
            command.add("-Execute");
            command.add("-Confirmation");
            command.add(confirmation);
        }
        return command;
    }

    private List<String> baseCommand() {
        List<String> command = new ArrayList<>();
        command.add(powershellExecutable.toString());
        command.add("-NoProfile");
        command.add("-NonInteractive");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(scriptPath.toString());
        return command;
    }

    private RawWriteResult run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output;
        try (var stream = process.getInputStream()) {
            output = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        int exitCode = process.waitFor();
        return new RawWriteResult(exitCode, output);
    }
}
