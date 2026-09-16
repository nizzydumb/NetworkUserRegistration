package storage;

/** Locks a selected disk's volumes and opens the disk read/write without writing any bytes. */
public final class PhysicalDriveAccessVerification {
    private PhysicalDriveAccessVerification() {}

    public static void main(String[] args) {
        if (args.length != 3 || !"--check".equals(args[0])) {
            System.out.println("No access check performed. Syntax:");
            System.out.println("  --check <driveNumber> CHECK-PHYSICALDRIVE-<driveNumber>");
            return;
        }
        int driveNumber = Integer.parseInt(args[1]);
        String requiredToken = "CHECK-PHYSICALDRIVE-" + driveNumber;
        if (!requiredToken.equals(args[2])) {
            throw new IllegalArgumentException("Confirmation must exactly equal " + requiredToken);
        }
        JnaPhysicalDriveService service = new JnaPhysicalDriveService();
        if (!service.isElevated()) throw new IllegalStateException("Run from an elevated process.");
        PhysicalDriveInfo drive = service.queryPhysicalDrive(driveNumber);
        if (drive == null) throw new IllegalArgumentException("PhysicalDrive" + driveNumber + " was not detected.");
        System.out.println("Checking without writing: " + drive);
        service.checkPhysicalWriteAccess(driveNumber);
        System.out.println("Write-access preflight passed; zero bytes were written.");
    }
}
