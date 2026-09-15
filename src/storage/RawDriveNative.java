package storage;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import java.nio.file.Path;

interface RawDriveNative extends Library {
    Path DLL = Path.of(System.getProperty("user.dir"), "lib", "native", "rawdrive", "rawdrive.dll");
    RawDriveNative INSTANCE = Native.load(DLL.toAbsolutePath().toString(), RawDriveNative.class);
    int rd_is_elevated();
    int rd_query_drive(int driveNumber, LongByReference sizeBytes, IntByReference offline,
                       IntByReference systemDisk, IntByReference busType, Pointer model, int modelCapacity);
    int rd_write_physical(int driveNumber, long offset, byte[] data, int length);
    int rd_write_image(WString path, long offset, byte[] data, int length);
}
