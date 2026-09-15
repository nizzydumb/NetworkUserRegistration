package storage;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** JNA facade for rawdrive.dll. The native layer independently enforces the write policy. */
public final class JnaPhysicalDriveService {
    public static final int MAX_DRIVE_NUMBER = 63;
    public static final int MAX_WRITE_BYTES = 1024 * 1024;
    public boolean isElevated() { return RawDriveNative.INSTANCE.rd_is_elevated() != 0; }

    public List<PhysicalDriveInfo> listPhysicalDrives() {
        List<PhysicalDriveInfo> drives = new ArrayList<>();
        for (int number = 0; number <= MAX_DRIVE_NUMBER; number++) {
            PhysicalDriveInfo drive = query(number);
            if (drive != null) drives.add(drive);
        }
        return List.copyOf(drives);
    }

    private PhysicalDriveInfo query(int number) {
        LongByReference size = new LongByReference();
        IntByReference offline = new IntByReference();
        IntByReference system = new IntByReference();
        IntByReference bus = new IntByReference();
        int characters = 256;
        Memory model = new Memory((long) characters * Native.WCHAR_SIZE);
        model.clear();
        int result = RawDriveNative.INSTANCE.rd_query_drive(number, size, offline, system, bus, model, characters);
        if (result == 2 || result == 3 || result == 15) return null;
        requireSuccess(result, "Querying PhysicalDrive" + number);
        return new PhysicalDriveInfo(number, size.getValue(), offline.getValue() != 0,
                system.getValue() != 0, bus.getValue(), model.getWideString(0).strip());
    }

    public void writePhysical(RawByteWriteRequest request) {
        byte[] bytes = request.bytes();
        if (bytes.length > MAX_WRITE_BYTES) throw new IllegalArgumentException("A write cannot exceed 1 MiB.");
        int result = RawDriveNative.INSTANCE.rd_write_physical(
                request.driveNumber(), request.byteOffset(), bytes, bytes.length);
        requireSuccess(result, "Writing " + bytes.length + " bytes to PhysicalDrive" + request.driveNumber());
    }

    void writeImageForVerification(Path image, long offset, byte[] bytes) {
        int result = RawDriveNative.INSTANCE.rd_write_image(new WString(image.toAbsolutePath().toString()),
                offset, bytes, bytes.length);
        requireSuccess(result, "Writing verification image");
    }

    private static void requireSuccess(int result, String operation) {
        if (result == 0) return;
        String detail = switch (result) {
            case -1 -> "invalid argument";
            case -2 -> "the disk is online; take it offline first";
            case -3 -> "the disk contains the running Windows installation";
            case -4 -> "the write exceeds the device boundary";
            case -5 -> "read-back verification did not match";
            default -> {
                try { yield Kernel32Util.formatMessage(result).strip(); }
                catch (RuntimeException ignored) { yield "Windows error " + result; }
            }
        };
        throw new RawDriveException(operation + " failed: " + detail + " (code " + result + ")", result);
    }
}
