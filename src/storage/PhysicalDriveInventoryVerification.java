package storage;

/** Read-only smoke test for the native physical-drive inventory. */
public final class PhysicalDriveInventoryVerification {
    private PhysicalDriveInventoryVerification() {}

    public static void main(String[] args) {
        JnaPhysicalDriveService service = new JnaPhysicalDriveService();
        System.out.println("Elevated: " + service.isElevated());
        var drives = service.listPhysicalDrives();
        if (drives.isEmpty()) throw new AssertionError("No physical drives were detected.");
        drives.forEach(System.out::println);
        System.out.println("Read-only inventory verification passed for " + drives.size() + " drive(s).");
    }
}
