package storage;

import java.util.HexFormat;

/**
 * Explicit destructive integration test. It never chooses a drive or writes by default.
 * Required syntax is printed when the complete execution gate is absent.
 */
public final class PhysicalDriveWriteManualTest {
    private PhysicalDriveWriteManualTest() {}

    public static void main(String[] args) {
        if (args.length != 5 || !"--execute".equals(args[0])) {
            usage();
            return;
        }

        int driveNumber = Integer.parseInt(args[1]);
        long offset = parseOffset(args[2]);
        byte[] bytes = parseHex(args[3]);
        String requiredToken = "WRITE-PHYSICALDRIVE-" + driveNumber;
        if (!requiredToken.equals(args[4])) {
            throw new IllegalArgumentException("Confirmation must exactly equal " + requiredToken);
        }

        JnaPhysicalDriveService service = new JnaPhysicalDriveService();
        if (!service.isElevated()) throw new IllegalStateException("Run the test from an elevated IntelliJ instance.");
        PhysicalDriveInfo drive = service.listPhysicalDrives().stream()
                .filter(candidate -> candidate.number() == driveNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("PhysicalDrive" + driveNumber + " was not detected."));
        if (drive.systemDisk()) {
            throw new IllegalStateException("Refusing target: " + drive);
        }

        System.out.println("Writing to explicitly selected target: " + drive);
        service.writePhysical(new RawByteWriteRequest(driveNumber, offset, bytes));
        System.out.println("Write and native read-back verification passed.");
    }

    private static long parseOffset(String value) {
        long offset = value.startsWith("0x") || value.startsWith("0X")
                ? Long.parseUnsignedLong(value.substring(2), 16) : Long.parseLong(value);
        if (offset < 0) throw new IllegalArgumentException("Offset cannot be negative.");
        return offset;
    }

    private static byte[] parseHex(String value) {
        String normalized = value.replaceAll("(?i)0x", "").replaceAll("[\\s,;:_-]", "");
        if (normalized.isEmpty() || (normalized.length() & 1) != 0)
            throw new IllegalArgumentException("Hex data must contain complete bytes.");
        return HexFormat.of().parseHex(normalized);
    }

    private static void usage() {
        System.out.println("No write performed. Explicit destructive-test syntax:");
        System.out.println("  --execute <driveNumber> <offset> <hexBytes> WRITE-PHYSICALDRIVE-<driveNumber>");
        System.out.println("Example: --execute 2 0x100000 DEADBEEF WRITE-PHYSICALDRIVE-2");
    }
}
