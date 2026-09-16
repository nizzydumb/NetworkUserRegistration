#define WIN32_LEAN_AND_MEAN
#define RAWDRIVE_EXPORTS
#include "raw_drive.h"
#include <winioctl.h>
#include <ntddstor.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <wchar.h>

#define RD_MAX_WRITE (1024U * 1024U)

/* Present in current Windows SDKs, but missing from some MinGW header sets. */
#ifndef IOCTL_DISK_GET_DISK_ATTRIBUTES
#define IOCTL_DISK_GET_DISK_ATTRIBUTES CTL_CODE(IOCTL_DISK_BASE, 0x003c, METHOD_BUFFERED, FILE_ANY_ACCESS)
#define DISK_ATTRIBUTE_OFFLINE 0x0000000000000001ULL
typedef struct _GET_DISK_ATTRIBUTES {
    DWORD Version;
    DWORD Reserved1;
    ULONGLONG Attributes;
} GET_DISK_ATTRIBUTES;
#endif

static HANDLE open_disk(int number, DWORD access) {
    wchar_t path[64];
    _snwprintf(path, 64, L"\\\\.\\PhysicalDrive%d", number);
    return CreateFileW(path, access, FILE_SHARE_READ | FILE_SHARE_WRITE,
                       NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
}

static int win_error(void) {
    DWORD error = GetLastError();
    return error == 0 ? ERROR_GEN_FAILURE : (int)error;
}

static int query_offline(HANDLE disk, int *offline) {
    GET_DISK_ATTRIBUTES attributes;
    DWORD returned = 0;
    ZeroMemory(&attributes, sizeof(attributes));
    attributes.Version = sizeof(attributes);
    if (!DeviceIoControl(disk, IOCTL_DISK_GET_DISK_ATTRIBUTES, NULL, 0,
                         &attributes, sizeof(attributes), &returned, NULL)) {
        return win_error();
    }
    *offline = (attributes.Attributes & DISK_ATTRIBUTE_OFFLINE) != 0;
    return RD_OK;
}

static int is_system_disk_number(int disk_number) {
    wchar_t windows_path[MAX_PATH];
    wchar_t volume_path[] = L"\\\\.\\C:";
    BYTE buffer[sizeof(VOLUME_DISK_EXTENTS) + sizeof(DISK_EXTENT) * 32];
    VOLUME_DISK_EXTENTS *extents = (VOLUME_DISK_EXTENTS *)buffer;
    DWORD returned = 0;
    DWORD index;
    HANDLE volume;

    if (GetWindowsDirectoryW(windows_path, MAX_PATH) > 1 && windows_path[1] == L':') {
        volume_path[4] = windows_path[0];
    }
    volume = CreateFileW(volume_path, 0, FILE_SHARE_READ | FILE_SHARE_WRITE,
                         NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (volume == INVALID_HANDLE_VALUE) {
        return 1; /* Fail closed if the Windows volume cannot be mapped. */
    }
    if (!DeviceIoControl(volume, IOCTL_VOLUME_GET_VOLUME_DISK_EXTENTS, NULL, 0,
                         buffer, sizeof(buffer), &returned, NULL)) {
        CloseHandle(volume);
        return 1;
    }
    CloseHandle(volume);
    for (index = 0; index < extents->NumberOfDiskExtents; index++) {
        if ((int)extents->Extents[index].DiskNumber == disk_number) return 1;
    }
    return 0;
}

static int volume_has_mount_point(const wchar_t *volume_name) {
    wchar_t paths[4096];
    DWORD required = 0;
    ZeroMemory(paths, sizeof(paths));
    if (GetVolumePathNamesForVolumeNameW(volume_name, paths,
                                         (DWORD)(sizeof(paths) / sizeof(paths[0])), &required)) {
        return paths[0] != L'\0';
    }
    return 0;
}

static int disk_has_mounted_volume(int disk_number) {
    wchar_t volume_name[MAX_PATH];
    wchar_t handle_name[MAX_PATH];
    HANDLE search = FindFirstVolumeW(volume_name, MAX_PATH);
    if (search == INVALID_HANDLE_VALUE) return 0;
    do {
        BYTE buffer[sizeof(VOLUME_DISK_EXTENTS) + sizeof(DISK_EXTENT) * 32];
        VOLUME_DISK_EXTENTS *extents = (VOLUME_DISK_EXTENTS *)buffer;
        DWORD returned = 0;
        DWORD index;
        HANDLE volume;
        size_t length;
        wcsncpy(handle_name, volume_name, MAX_PATH - 1);
        handle_name[MAX_PATH - 1] = L'\0';
        length = wcslen(handle_name);
        if (length > 0 && handle_name[length - 1] == L'\\') handle_name[length - 1] = L'\0';
        volume = CreateFileW(handle_name, 0,
                             FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                             NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
        if (volume == INVALID_HANDLE_VALUE) continue;
        if (DeviceIoControl(volume, IOCTL_VOLUME_GET_VOLUME_DISK_EXTENTS, NULL, 0,
                            buffer, sizeof(buffer), &returned, NULL)) {
            for (index = 0; index < extents->NumberOfDiskExtents; index++) {
                if ((int)extents->Extents[index].DiskNumber == disk_number &&
                    volume_has_mount_point(volume_name)) {
                    CloseHandle(volume);
                    FindVolumeClose(search);
                    return 1;
                }
            }
        }
        CloseHandle(volume);
    } while (FindNextVolumeW(search, volume_name, MAX_PATH));
    FindVolumeClose(search);
    return 0;
}

static void copy_descriptor_text(wchar_t *destination, int capacity,
                                 const BYTE *descriptor, DWORD descriptor_size,
                                 DWORD offset) {
    const char *source;
    int used;
    if (offset == 0 || offset >= descriptor_size || capacity <= 1) return;
    source = (const char *)(descriptor + offset);
    used = (int)wcslen(destination);
    if (used > 0 && used < capacity - 1) destination[used++] = L' ';
    MultiByteToWideChar(CP_ACP, 0, source, -1, destination + used, capacity - used);
    destination[capacity - 1] = L'\0';
}

RD_API int rd_is_elevated(void) {
    HANDLE token;
    TOKEN_ELEVATION elevation;
    DWORD size;
    if (!OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &token)) return 0;
    if (!GetTokenInformation(token, TokenElevation, &elevation, sizeof(elevation), &size)) {
        CloseHandle(token);
        return 0;
    }
    CloseHandle(token);
    return elevation.TokenIsElevated != 0;
}

RD_API int rd_query_drive(int drive_number, uint64_t *size_bytes, int *offline,
                          int *mounted, int *system_disk, int *bus_type,
                          wchar_t *model, int model_capacity) {
    HANDLE disk;
    GET_LENGTH_INFORMATION length;
    STORAGE_PROPERTY_QUERY query;
    BYTE descriptor_buffer[2048];
    STORAGE_DEVICE_DESCRIPTOR *descriptor = (STORAGE_DEVICE_DESCRIPTOR *)descriptor_buffer;
    DWORD returned = 0;
    int result;
    if (drive_number < 0 || size_bytes == NULL || offline == NULL || mounted == NULL ||
        system_disk == NULL || bus_type == NULL || model == NULL || model_capacity < 2) {
        return RD_ERROR_INVALID_ARGUMENT;
    }
    model[0] = L'\0';
    /* IOCTL_DISK_GET_LENGTH_INFO requires a handle opened for read access. */
    disk = open_disk(drive_number, GENERIC_READ);
    if (disk == INVALID_HANDLE_VALUE) return win_error();
    if (!DeviceIoControl(disk, IOCTL_DISK_GET_LENGTH_INFO, NULL, 0,
                         &length, sizeof(length), &returned, NULL)) {
        result = win_error(); CloseHandle(disk); return result;
    }
    *size_bytes = (uint64_t)length.Length.QuadPart;
    result = query_offline(disk, offline);
    if (result != RD_OK) { CloseHandle(disk); return result; }
    *mounted = disk_has_mounted_volume(drive_number);
    *system_disk = is_system_disk_number(drive_number);
    *bus_type = BusTypeUnknown;
    ZeroMemory(&query, sizeof(query));
    query.PropertyId = StorageDeviceProperty;
    query.QueryType = PropertyStandardQuery;
    if (DeviceIoControl(disk, IOCTL_STORAGE_QUERY_PROPERTY, &query, sizeof(query),
                        descriptor_buffer, sizeof(descriptor_buffer), &returned, NULL)) {
        *bus_type = (int)descriptor->BusType;
        copy_descriptor_text(model, model_capacity, descriptor_buffer, returned, descriptor->VendorIdOffset);
        copy_descriptor_text(model, model_capacity, descriptor_buffer, returned, descriptor->ProductIdOffset);
    }
    if (model[0] == L'\0') _snwprintf(model, model_capacity, L"Physical drive %d", drive_number);
    CloseHandle(disk);
    return RD_OK;
}

static int write_and_verify(HANDLE handle, uint64_t size, uint64_t offset,
                            const unsigned char *data, uint32_t length) {
    LARGE_INTEGER position;
    DWORD transferred;
    unsigned char *verification;
    if (data == NULL || length == 0 || length > RD_MAX_WRITE) return RD_ERROR_INVALID_ARGUMENT;
    if (offset > size || (uint64_t)length > size - offset) return RD_ERROR_OUT_OF_BOUNDS;
    position.QuadPart = (LONGLONG)offset;
    if (!SetFilePointerEx(handle, position, NULL, FILE_BEGIN)) return win_error();
    if (!WriteFile(handle, data, length, &transferred, NULL) || transferred != length) return win_error();
    if (!FlushFileBuffers(handle)) return win_error();
    verification = (unsigned char *)malloc(length);
    if (verification == NULL) return ERROR_NOT_ENOUGH_MEMORY;
    if (!SetFilePointerEx(handle, position, NULL, FILE_BEGIN) ||
        !ReadFile(handle, verification, length, &transferred, NULL) || transferred != length) {
        int result = win_error(); free(verification); return result;
    }
    if (memcmp(data, verification, length) != 0) { free(verification); return RD_ERROR_VERIFY_FAILED; }
    free(verification);
    return RD_OK;
}

static int write_physical_bytes(HANDLE disk, uint64_t size, uint64_t offset,
                                const unsigned char *data, uint32_t length) {
    DISK_GEOMETRY geometry;
    DWORD returned = 0;
    uint64_t aligned_start;
    uint64_t aligned_end;
    uint64_t aligned_length64;
    DWORD aligned_length;
    DWORD transferred;
    LARGE_INTEGER position;
    unsigned char *buffer;
    int result = RD_OK;
    if (data == NULL || length == 0 || length > RD_MAX_WRITE) return RD_ERROR_INVALID_ARGUMENT;
    if (offset > size || (uint64_t)length > size - offset) return RD_ERROR_OUT_OF_BOUNDS;
    if (!DeviceIoControl(disk, IOCTL_DISK_GET_DRIVE_GEOMETRY, NULL, 0,
                         &geometry, sizeof(geometry), &returned, NULL)) return win_error();
    if (geometry.BytesPerSector == 0) return ERROR_INVALID_DATA;
    aligned_start = offset - (offset % geometry.BytesPerSector);
    aligned_end = ((offset + length + geometry.BytesPerSector - 1) / geometry.BytesPerSector)
                  * geometry.BytesPerSector;
    if (aligned_end > size) aligned_end = size;
    aligned_length64 = aligned_end - aligned_start;
    if (aligned_length64 == 0 || aligned_length64 > RD_MAX_WRITE + 131072U) return RD_ERROR_OUT_OF_BOUNDS;
    aligned_length = (DWORD)aligned_length64;
    buffer = (unsigned char *)malloc(aligned_length);
    if (buffer == NULL) return ERROR_NOT_ENOUGH_MEMORY;
    position.QuadPart = (LONGLONG)aligned_start;
    if (!SetFilePointerEx(disk, position, NULL, FILE_BEGIN) ||
        !ReadFile(disk, buffer, aligned_length, &transferred, NULL) || transferred != aligned_length) {
        result = win_error(); goto cleanup;
    }
    memcpy(buffer + (size_t)(offset - aligned_start), data, length);
    if (!SetFilePointerEx(disk, position, NULL, FILE_BEGIN) ||
        !WriteFile(disk, buffer, aligned_length, &transferred, NULL) || transferred != aligned_length ||
        !FlushFileBuffers(disk)) {
        result = win_error(); goto cleanup;
    }
    ZeroMemory(buffer, aligned_length);
    if (!SetFilePointerEx(disk, position, NULL, FILE_BEGIN) ||
        !ReadFile(disk, buffer, aligned_length, &transferred, NULL) || transferred != aligned_length) {
        result = win_error(); goto cleanup;
    }
    if (memcmp(buffer + (size_t)(offset - aligned_start), data, length) != 0)
        result = RD_ERROR_VERIFY_FAILED;
cleanup:
    free(buffer);
    return result;
}

RD_API int rd_write_physical(int drive_number, uint64_t offset,
                             const unsigned char *data, uint32_t length) {
    HANDLE disk;
    GET_LENGTH_INFORMATION disk_length;
    DWORD returned;
    int result;
    if (drive_number < 0) return RD_ERROR_INVALID_ARGUMENT;
    if (is_system_disk_number(drive_number)) return RD_ERROR_SYSTEM_DISK;
    disk = open_disk(drive_number, GENERIC_READ | GENERIC_WRITE);
    if (disk == INVALID_HANDLE_VALUE) return win_error();
    if (!DeviceIoControl(disk, IOCTL_DISK_GET_LENGTH_INFO, NULL, 0,
                         &disk_length, sizeof(disk_length), &returned, NULL)) {
        result = win_error(); CloseHandle(disk); return result;
    }
    result = write_physical_bytes(disk, (uint64_t)disk_length.Length.QuadPart, offset, data, length);
    CloseHandle(disk);
    return result;
}

RD_API int rd_write_image(const wchar_t *path, uint64_t offset,
                          const unsigned char *data, uint32_t length) {
    HANDLE file;
    LARGE_INTEGER size;
    int result;
    if (path == NULL) return RD_ERROR_INVALID_ARGUMENT;
    file = CreateFileW(path, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING,
                       FILE_ATTRIBUTE_NORMAL, NULL);
    if (file == INVALID_HANDLE_VALUE) return win_error();
    if (!GetFileSizeEx(file, &size)) { result = win_error(); CloseHandle(file); return result; }
    result = write_and_verify(file, (uint64_t)size.QuadPart, offset, data, length);
    CloseHandle(file);
    return result;
}
