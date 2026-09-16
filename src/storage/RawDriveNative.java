package storage;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import java.nio.file.Path;

interface RawDriveNative extends Library {
    Path DLL = resolveLibrary();
    RawDriveNative INSTANCE = Native.load(DLL.toAbsolutePath().toString(), RawDriveNative.class);

    private static Path resolveLibrary() {
        String packagedPath = System.getProperty("rawdrive.library.path");
        if (packagedPath != null && !packagedPath.isBlank()) return Path.of(packagedPath);
        return Path.of(System.getProperty("user.dir"), "lib", "native", "rawdrive", "rawdrive.dll");
    }
    int rd_is_elevated();
    int rd_query_drive(int driveNumber, LongByReference sizeBytes, IntByReference offline,
                       IntByReference mounted, IntByReference systemDisk, IntByReference busType,
                       Pointer model, int modelCapacity);
    int rd_check_physical_write_access(int driveNumber);
    int rd_write_physical(int driveNumber, long offset, byte[] data, int length);
    int rd_write_image(WString path, long offset, byte[] data, int length);
}
