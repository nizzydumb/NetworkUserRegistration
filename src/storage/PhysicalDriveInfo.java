package storage;

public record PhysicalDriveInfo(int number, long sizeBytes, boolean offline,
                                boolean systemDisk, int busType, String model) {
    public String devicePath() { return "\\\\.\\PhysicalDrive" + number; }
    public boolean writableByPolicy() { return offline && !systemDisk; }
    public String sizeLabel() { return String.format("%.1f GiB", sizeBytes / 1073741824.0); }
    @Override public String toString() {
        String state = systemDisk ? "SYSTEM — blocked" : (offline ? "offline" : "online — blocked");
        return "PhysicalDrive" + number + " · " + sizeLabel() + " · " + model + " · " + state;
    }
}
