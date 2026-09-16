package storage;

public record PhysicalDriveInfo(int number, long sizeBytes, boolean offline, boolean mounted,
                                boolean systemDisk, int busType, String model) {
    public String devicePath() { return "\\\\.\\PhysicalDrive" + number; }
    public boolean writableByPolicy() { return !systemDisk; }
    public String sizeLabel() { return String.format("%.1f GiB", sizeBytes / 1073741824.0); }
    @Override public String toString() {
        String state = systemDisk ? "SYSTEM — blocked" : (mounted ? "mounted" : "not mounted");
        return "PhysicalDrive" + number + " · " + sizeLabel() + " · " + model + " · " + state;
    }
}
