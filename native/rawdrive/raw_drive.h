#ifndef RAW_DRIVE_H
#define RAW_DRIVE_H

#include <stdint.h>
#include <windows.h>

#ifdef RAWDRIVE_EXPORTS
#define RD_API __declspec(dllexport)
#else
#define RD_API __declspec(dllimport)
#endif

#define RD_OK 0
#define RD_ERROR_INVALID_ARGUMENT -1
#define RD_ERROR_NOT_OFFLINE -2
#define RD_ERROR_SYSTEM_DISK -3
#define RD_ERROR_OUT_OF_BOUNDS -4
#define RD_ERROR_VERIFY_FAILED -5

RD_API int rd_is_elevated(void);
RD_API int rd_query_drive(int drive_number, uint64_t *size_bytes, int *offline,
                          int *system_disk, int *bus_type,
                          wchar_t *model, int model_capacity);
RD_API int rd_write_physical(int drive_number, uint64_t offset,
                             const unsigned char *data, uint32_t length);
RD_API int rd_write_image(const wchar_t *path, uint64_t offset,
                          const unsigned char *data, uint32_t length);

#endif
