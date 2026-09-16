/*
 * Quantis PCI Library for Windows
 *
 * Copyright (C) 2004-2020 ID Quantique SA, Carouge/Geneva, Switzerland
 * All rights reserved.
 *
 * ----------------------------------------------------------------------------
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions, and the following disclaimer,
 *    without modification.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY.
 *
 * ----------------------------------------------------------------------------
 *
 * Alternatively, this software may be distributed under the terms of the
 * GNU General Public License version 2 as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 *
 * ----------------------------------------------------------------------------
 *
 * For history of changes, see ChangeLog.txt
 */

#ifndef DISABLE_QUANTIS_PCIE

#ifndef _WIN32
#error "This module is for Windows only!"
#endif

#ifdef DISABLE_QUANTIS_PCI
#error "DISABLE_QUANTIS_PCI should no be set tu use QuantisPcie_Windows"
#endif

 /* Exclude rarely-used stuff from Windows headers */
#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#include <malloc.h>
#include <setupapi.h>
#include <string.h>
#include <wchar.h>
#pragma comment(lib, "setupapi.lib")

#include <stdio.h> // printf
#include <winioctl.h>

#include "Quantis.h"
#include "Quantis_Internal.h"
#include "QuantisPci_Windows.h"


#include <INITGUID.H>
#include "xdma_public.h"


// Q400 mode (comment for RNG mode)
//#define SAMPLE_MODE

#define REG_INIT_WIDTH 4
#define REG_INIT_LENGTH 10
#define REG_DEINIT_LENGTH 5

#ifdef SAMPLE_MODE
#define FLUSH_FIFO_SIZE (4096*4096)
#else
#define FLUSH_FIFO_SIZE (60*4096)
#endif

#define FPGA_SOFT_RST_REG (0x00)
#define FPGA_MODULE_REG (0x04)
#define FPGA_SPI_MODULE_CRTL_REG (0x08)
#define FPGA_CMD_RAM_END_ADDR_REG (0x0C)
#define FPGA_BURST_MODE_REG (0x10)
#define FPGA_TEST_MODE_REG (0x14)
#define FPGA_Q400_NUM_REG (0x30)

#define FPGA_INIT_STATUS_CHK_REG (0x84)
#define FPGA_LOOPBACK_TEST_REG (0x88)

#define FPGA_BATCH_NUMBER_REG (0x00C4)
#define FPGA_INCREMENTAL_NUMBER_REG (0x00C8)
#define FPGA_PRODUCT_MODEL_REG  (0x00CC)

#define FPGA_BOARD_VERSION_REG (0x100)
#define FPGA_BUILD_DATE_REG (0x104)

#define FPGA_MODE_REG (0x05A8)


// sw_rst register (0x00)
#define FPGA_SOFT_RST_DEACTIVE (1UL << 4)
#define FPGA_SOFT_RST_ACTIVE (0UL << 4)

// Module enable register
#define FPGA_SPI_ENABLE (1UL << 4)

// SPI module control
#define FPGA_SPI_DATA_ACCESS_ENABLE (1UL << 0)
#define FPGA_TEST_MODE_S2Q400_INITIAL (1UL << 2)
#define FPGA_SPI_STATUS_RESET (1UL << 6)
#define FPGA_SPI_RNG_MODULE_ENABLE (1UL << 7)

// Burst mode
#define FPGA_SPI_IN_BUS_SEL (3UL << 0)
#define FPGA_BURST_MODE_EN (1UL << 4)
#define FPGA_QMODE (7UL << 7)

// Test mode & HW reject
#define FPGA_RESERVED1 (1UL << 0)
#define FPGA_REMOVE_HEAD_16BIT (1UL << 1)
#define FPGA_REMOVE_TAIL_16BIT (1UL << 2)
#define FPGA_REMOVE_PKT_TAIL (1UL << 3)
#define FPGA_BIST_MODE (1UL << 6)
#define FPGA_TEST_MODE_RESERVED (1UL << 7)

// q400 num
#define FPGA_Q400_NUM_AUTO (0x00000000)
#define FPGA_Q400_NUM_2 (0x00000010)
#define FPGA_Q400_NUM_12 (0x00000020)

// Q400 status
#define REG_INIT_STATUS_CHK (0x00000084)
#define Q400_SENSOR_READY (0x00001000)
#define Q400_SENSOR_BEING (0x00000FFF)
#define Q400_SENSOR_PKT_ERR (0xFFF00000)
#define Q400_SENSOR_PKT_ERR_SHIFT (20)
#define Q400_SENSOR_READY_MAX_CNT (1000)

DWORD REG_INIT_ADDR[REG_INIT_LENGTH] = {
    FPGA_SOFT_RST_REG, // sw_rst
    FPGA_SOFT_RST_REG, // sw_rst
    FPGA_SOFT_RST_REG, // sw_rst
    FPGA_SPI_MODULE_CRTL_REG, // spi_module_ctrl
    FPGA_Q400_NUM_REG, // q400_num
    FPGA_TEST_MODE_REG, // testmode_hwreject
    FPGA_MODE_REG, // mode_sel
    FPGA_SPI_MODULE_CRTL_REG, // spi_module_ctrl
    FPGA_MODULE_REG, // module_enable
    FPGA_SPI_MODULE_CRTL_REG  // spi_module_ctrl
};
BYTE REG_INIT_VAL[REG_INIT_LENGTH][REG_INIT_WIDTH] = {
    { 0x00, 0x00, 0x00, 0x10},
    { 0x00, 0x00, 0x00, 0x00},  // reset
    { 0x00, 0x00, 0x00, 0x10},
    { 0x00, 0x00, 0x00, 0x00},  //clear
    { 0x00, 0x00, 0x00, 0x00},  //sensor auto 
#ifndef SAMPLE_MODE
    { 0x00, 0x00, 0x00, 0x0a},  // RNG
    { 0x00, 0x0a, 0x00, 0x00},  // RNG
#else
    { 0x00, 0x00, 0x00, 0x0c},  // SAMPLE
    { 0x00, 0x0a, 0x00, 0x01},  // SAMPLE
#endif
    { 0x00, 0x00, 0x00, 0x40},
    { 0x00, 0x00, 0x00, 0x11},
    { 0x00, 0x00, 0x00, 0x81}
};
DWORD REG_DEINIT_ADDR[REG_INIT_LENGTH] = {
    FPGA_SOFT_RST_REG, // sw_rst
    FPGA_SPI_MODULE_CRTL_REG, // spi_module_ctrl
    FPGA_SPI_MODULE_CRTL_REG, // spi_module_ctrl
    FPGA_MODULE_REG, // module_enable
    FPGA_SPI_MODULE_CRTL_REG  // spi_module_ctrl
};
BYTE REG_DEINIT_VAL[REG_DEINIT_LENGTH][REG_INIT_WIDTH] = {
    { 0x00, 0x01, 0x00, 0x00},
    { 0x00, 0x00, 0x00, 0x00},
    { 0x01, 0x00, 0x00, 0x00},
    { 0x00, 0x01, 0x00, 0x01},
    { 0x10, 0x00, 0x00, 0x01}
};

/**
 * @brief Define specific operation to use PCIe driver
 *
 */
QuantisOperations QuantisOperationsPcie =
{
    /*.BoardReset = */          QuantisPcieBoardReset,
    /*.Close = */               QuantisPcieClose,
    /*.Count = */               QuantisPciCount, // Reuse PCI Count
    /*.GetBoardVersion = */     QuantisPcieGetBoardVersion,
    /*.GetDriverVersion = */    QuantisPcieGetDriverVersion,
    /*.GetManufacturer = */     QuantisPcieGetManufacturer,
    /*.GetModulesMask = */      QuantisPcieGetModulesMask,
    /*.GetModulesDataRate = */  QuantisPcieGetModulesDataRate,
    /*.GetModulesPower = */     QuantisPcieGetModulesPower,
    /*.GetModulesStatus = */    QuantisPcieGetModulesStatus,
    /*.GetSerialNumber = */     QuantisPcieGetSerialNumber,
    /*.ModulesDisable = */      QuantisPcieModulesDisable,
    /*.ModulesEnable = */       QuantisPcieModulesEnable,
    /*.Open = */                QuantisPciOpen, // Reuse PCI Open
    /*.Read = */                QuantisPcieRead,
    /*.GetBusDeviceId = */      QuantisPcieGetBusDeviceId,
    /*.QuantisTypeStrError = */      QuantisPciTypeStrError,
    /*.GetAis31StartupTestsRequestFlag*/   QuantisPcieGetAis31StartupTestsRequestFlag,
    /*.ClearAis31StartupTestsRequestFlag*/ QuantisPcieClearAis31StartupTestsRequestFlag
};

int QuantisPcieIoCtl(QuantisDeviceHandle* deviceHandle,
    DWORD ioControlCode,
    void* inBuffer,
    DWORD inBufferSize,
    void* outBuffer,
    DWORD outBufferSize)
{
    QuantisPrivateData* _privateData = (QuantisPrivateData*)deviceHandle->privateData;
    DWORD bytesReturned;
    if (!DeviceIoControl(_privateData->deviceHandle,
        ioControlCode,
        inBuffer,
        inBufferSize,
        outBuffer,
        outBufferSize,
        &bytesReturned,
        NULL))
    {
        return QUANTIS_ERROR_IO;
    }

    if (bytesReturned != outBufferSize)
    {
        return QUANTIS_ERROR_IO;
    }

    return QUANTIS_SUCCESS;
}

int QuantisPcieWriteRegister(HANDLE deviceUserHandle, LONGLONG regAddress, UINT32 value)
{
    BYTE data[sizeof(UINT32)];
    LARGE_INTEGER address;
    address.QuadPart = regAddress;
    if (INVALID_SET_FILE_POINTER == SetFilePointerEx(deviceUserHandle, address, NULL, FILE_BEGIN)) {
        return QUANTIS_ERROR_IO;
    }

    data[0] = (value >> 0) & 0xFF;
    data[1] = (value >> 8) & 0xFF;
    data[2] = (value >> 16) & 0xFF;
    data[3] = (value >> 24) & 0xFF;

    DWORD writeBytes = sizeof(UINT32);

    if (!WriteFile(deviceUserHandle, data, sizeof(UINT32), &writeBytes, NULL)) {
        return QUANTIS_ERROR_IO;
    }

    return QUANTIS_SUCCESS;
}

int QuantisPcieReadRegister(HANDLE deviceUserHandle, LONGLONG regAddress, UINT32* value)
{
    LARGE_INTEGER address;
    BYTE data[4];

    address.QuadPart = regAddress;
    if (INVALID_SET_FILE_POINTER == SetFilePointerEx(deviceUserHandle, address, NULL, FILE_BEGIN)) {
        return QUANTIS_ERROR_IO;
    }

    DWORD readBytes = 0;
    if (!ReadFile(deviceUserHandle, data, 4, &readBytes, NULL)) {
        return QUANTIS_ERROR_IO;
    }

    if (readBytes != sizeof(data))
        return QUANTIS_ERROR_IO;

    *value = data[3] << 24 | data[2] << 16 | data[1] << 8 | data[0];

    return QUANTIS_SUCCESS;
}

int QuantisPcieInternalCreateFile(QuantisDeviceHandle* deviceHandle, HANDLE* deviceUserHandle, wchar_t* pathSuffix)
{
    wchar_t device_path[MAX_PATH + 1] = L"";
    int ret = QuantisPcieOpenInternal(deviceHandle, device_path, MAX_PATH + 1);
    if (ret != QUANTIS_SUCCESS)
    {
        return ret;
    }

    // Copy Device path and add sufix 
    wcscat_s(device_path, MAX_PATH + 1, pathSuffix);

    // Copy Device path and add sufix 
    *deviceUserHandle = CreateFile(device_path,
        GENERIC_READ | GENERIC_WRITE,
        0,
        NULL,
        OPEN_EXISTING,
        FILE_ATTRIBUTE_NORMAL,
        NULL);

    if (*deviceUserHandle == INVALID_HANDLE_VALUE)
    {
        return QUANTIS_ERROR_IO;
    }

    return QUANTIS_SUCCESS;
}

/* Board reset */
int QuantisPcieBoardReset(QuantisDeviceHandle* deviceHandle)
{
    int ret = QuantisPcieInternalDeinit(deviceHandle);
    if (ret != QUANTIS_SUCCESS)
    {
        return ret;
    }
    ret = QuantisPcieInternalInit(deviceHandle);

    return ret;
}


/* Close */
void QuantisPcieClose(QuantisDeviceHandle* deviceHandle)
{
    QuantisPrivateData* _privateData = (QuantisPrivateData*)deviceHandle->privateData;
    if (!_privateData)
    {
        return;
    }

    if (_privateData->deviceHandle != INVALID_HANDLE_VALUE)
    {
        CloseHandle(_privateData->deviceHandle);
    }

    free(_privateData);
    _privateData = NULL;
}


/* Count */
int QuantisPcieOnlyCount()
{
    HDEVINFO hDevInfo;
    SP_DEVICE_INTERFACE_DATA devInfoData;
    LONG result;
    int memberIndex = 0;
    /*
     * Build a list of all devices (with the specified GUID) that are present in
     * the system that have enabled an interface from the storage volume device
     * interface class.
     */
    hDevInfo = SetupDiGetClassDevs(&GUID_DEVINTERFACE_QUANTIS_PCIE,
        NULL,
        NULL,
        DIGCF_PRESENT | DIGCF_INTERFACEDEVICE);
    if (hDevInfo == INVALID_HANDLE_VALUE)
    {
        /* No device found */
        return 0;
    }

    /*
     * Enumerate the system's device interfaces and obtains information on our
     * device interface
     */
    SecureZeroMemory(&devInfoData, sizeof(devInfoData));
    devInfoData.cbSize = sizeof(devInfoData);

    while (1)
    {
        result = SetupDiEnumDeviceInterfaces(hDevInfo,
            0,
            &GUID_DEVINTERFACE_QUANTIS_PCIE,
            memberIndex,
            &devInfoData);
        if (result)
        {
            memberIndex++;
        }
        else
        {
            break;
        }
    }

    /* We don't allow more than MAX_QUANTIS_DEVICE */
    if (memberIndex > MAX_QUANTIS_DEVICE)
    {
        return MAX_QUANTIS_DEVICE;
    }
    else
    {
        return memberIndex;
    }
}


/* GetBoardVersion */
int QuantisPcieGetBoardVersion(QuantisDeviceHandle* deviceHandle)
{
    HANDLE deviceUserHandle;
    int ret = QuantisPcieInternalCreateFile(deviceHandle, &deviceUserHandle, L"\\user");
    if (ret != QUANTIS_SUCCESS)
    {
        return ret;
    }

    UINT32 boardVersion = 0;

    ret = QuantisPcieReadRegister(deviceUserHandle, FPGA_BOARD_VERSION_REG, &boardVersion);
    if (ret != QUANTIS_SUCCESS)
    {
        CloseHandle(deviceUserHandle);
        return ret;
    }
    CloseHandle(deviceUserHandle);

    return boardVersion;
}


/* GetDriverVersion */
float QuantisPcieGetDriverVersion()
{
    int result;
    int deviceNumber = 0;
    int driverVersion = 0;
    QuantisDeviceHandle* deviceHandle = NULL;

    /* Open device */
    result = QuantisOpenInternal(QUANTIS_DEVICE_PCI, deviceNumber, &deviceHandle);
    if (result < 0)
    {
        /* Assumes there is no card installed */
        return 0.0f;
    }

    /* Perform request */
    result = QuantisPcieIoCtl(deviceHandle,
        QUANTIS_IOCTL_GET_DRIVER_VERSION,
        NULL,
        0,
        &driverVersion,
        sizeof(driverVersion));
    if (result < 0)
    {
        /* Assumes there is no card installed */
        driverVersion = 0;
    }

    /* Close device */
    QuantisCloseInternal(deviceHandle);

    return ((float)driverVersion) / 10.0f;
}


/* GetManufacturer */
char* QuantisPcieGetManufacturer(QuantisDeviceHandle* deviceHandle)
{
    deviceHandle = deviceHandle; /* Avoids unused parameter warning */
    /* Quantis PCI do not support manufacturer retrieval */
    return (char*)QUANTIS_NOT_AVAILABLE;
}



/* GetModulesMask */
int QuantisPcieGetModulesMask(QuantisDeviceHandle* deviceHandle)
{
    int modulesMask = 0;

    HANDLE deviceUserHandle;
    int ret = QuantisPcieInternalCreateFile(deviceHandle, &deviceUserHandle, L"\\user");
    if (ret != QUANTIS_SUCCESS)
    {
        return ret;
    }
    
    UINT32 statusValue = 0;

    ret = QuantisPcieReadRegister(deviceUserHandle, FPGA_INIT_STATUS_CHK_REG, &statusValue);
    if (ret != QUANTIS_SUCCESS)
    {
        CloseHandle(deviceUserHandle);
        return ret;
    }
    CloseHandle(deviceUserHandle);

    modulesMask = statusValue & Q400_SENSOR_BEING;

    return modulesMask;
}


/* GetModulesDataRate */
int QuantisPcieGetModulesDataRate(QuantisDeviceHandle* deviceHandle)
{
    int modulesMask = QuantisPcieGetModulesMask(deviceHandle);
    return QUANTIS_MODULE_DATA_RATE * QuantisCountSetBits(modulesMask);
}


/* GetModulesPower */
int QuantisPcieGetModulesPower(QuantisDeviceHandle* deviceHandle)
{
    deviceHandle = deviceHandle; /* Avoids unused parameter warning */

    /* PCI modules are always powered */
    return 1;
}


/* GetModulesStatus */
int QuantisPcieGetModulesStatus(QuantisDeviceHandle* deviceHandle)
{
    UINT32 modulesStatus = 0;
    UINT32 moduleError = 0;

    HANDLE deviceUserHandle;
    int ret = QuantisPcieInternalCreateFile(deviceHandle, &deviceUserHandle, L"\\user");
    if (ret != QUANTIS_SUCCESS)
    {
        return ret;
    }

    UINT32 statusValue = 0;
    
    ret = QuantisPcieReadRegister(deviceUserHandle, FPGA_INIT_STATUS_CHK_REG, &statusValue);
    if (ret != QUANTIS_SUCCESS)
    {
        CloseHandle(deviceUserHandle);
        return ret;
    }
    CloseHandle(deviceUserHandle);

    modulesStatus = statusValue & Q400_SENSOR_BEING;
    moduleError = (statusValue & Q400_SENSOR_PKT_ERR) >> Q400_SENSOR_PKT_ERR_SHIFT;

    modulesStatus &= ~moduleError;

    return modulesStatus;
}


/* GetSerialNumber */
char* QuantisPcieGetSerialNumber(QuantisDeviceHandle* deviceHandle)
{
    UINT32 batchNumber = 0;
    UINT32 incrementalNumber = 0;
    UINT32 productModel = 0;

    QuantisPrivateData* _privateData = (QuantisPrivateData*)deviceHandle->privateData;

    if (_privateData->serialNumber[0] == '\0') {

        HANDLE deviceUserHandle;
        int ret = QuantisPcieInternalCreateFile(deviceHandle, &deviceUserHandle, L"\\user");
        if (ret != QUANTIS_SUCCESS)
        {
            strncpy(&_privateData->serialNumber, QUANTIS_NO_SERIAL, QUANTIS_SERIAL_MAX_LENGTH - 1);
            return _privateData->serialNumber;
        }

        ret = QuantisPcieReadRegister(deviceUserHandle, FPGA_BATCH_NUMBER_REG, &batchNumber);
        if (ret != QUANTIS_SUCCESS)
        {
            CloseHandle(deviceUserHandle);
            strncpy(&_privateData->serialNumber, QUANTIS_NO_SERIAL, QUANTIS_SERIAL_MAX_LENGTH - 1);
            return _privateData->serialNumber;
        }

        ret = QuantisPcieReadRegister(deviceUserHandle, FPGA_INCREMENTAL_NUMBER_REG, &incrementalNumber);
        if (ret != QUANTIS_SUCCESS)
        {
            CloseHandle(deviceUserHandle);
            strncpy(&_privateData->serialNumber, QUANTIS_NO_SERIAL, QUANTIS_SERIAL_MAX_LENGTH - 1);
            return _privateData->serialNumber;
        }

        ret = QuantisPcieReadRegister(deviceUserHandle, FPGA_PRODUCT_MODEL_REG, &productModel);
        if (ret != QUANTIS_SUCCESS)
        {
            CloseHandle(deviceUserHandle);
            strncpy(&_privateData->serialNumber, QUANTIS_NO_SERIAL, QUANTIS_SERIAL_MAX_LENGTH - 1);
            return _privateData->serialNumber;
        }

        CloseHandle(deviceUserHandle);
        
        _privateData->serialNumber[0] = (batchNumber >> 0) & 0xFF;
        _privateData->serialNumber[1] = (batchNumber >> 8) & 0xFF;
        _privateData->serialNumber[2] = (batchNumber >> 16) & 0xFF;
        _privateData->serialNumber[3] = (batchNumber >> 24) & 0xFF;

        _privateData->serialNumber[4] = (incrementalNumber >> 0) & 0xFF;
        _privateData->serialNumber[5] = (incrementalNumber >> 8) & 0xFF;
        _privateData->serialNumber[6] = (incrementalNumber >> 16) & 0xFF;

        _privateData->serialNumber[7] = (productModel >> 0) & 0xFF;
        _privateData->serialNumber[8] = (productModel >> 8) & 0xFF;
        _privateData->serialNumber[9] = (productModel >> 16) & 0xFF;
        _privateData->serialNumber[10] = (productModel >> 24) & 0xFF;

        _privateData->serialNumber[11] = '\0';
    }

    return _privateData->serialNumber;
}

int QuantisPcieInternalInit(QuantisDeviceHandle* deviceHandle)
{
    LARGE_INTEGER address;
    BYTE data[REG_INIT_WIDTH];
    DWORD size = REG_INIT_WIDTH;

    HANDLE deviceUserHandle;
    int ret = QuantisPcieInternalCreateFile(deviceHandle, &deviceUserHandle, L"\\user");
    if (ret != QUANTIS_SUCCESS)
    {
        return ret;
    }


    // Check if already init
    UINT moduleReg;
    ret = QuantisPcieReadRegister(deviceUserHandle, FPGA_MODULE_REG, &moduleReg);
    if (ret != QUANTIS_SUCCESS)
    {
        CloseHandle(deviceUserHandle);
        return ret;
    }
   
    if (moduleReg & FPGA_SPI_ENABLE)
    {
        CloseHandle(deviceUserHandle);
        return QUANTIS_SUCCESS;
    }

    for (int reg_index = 0; reg_index < REG_INIT_LENGTH; reg_index++) {

        address.QuadPart = REG_INIT_ADDR[reg_index];
        if (INVALID_SET_FILE_POINTER == SetFilePointerEx(deviceUserHandle, address, NULL, FILE_BEGIN)) {
            //fprintf(stderr, "Error setting file pointer, win32 error code: %ld\n", GetLastError());
            CloseHandle(deviceUserHandle);
            return QUANTIS_ERROR_IO;
        }
        data[0] = REG_INIT_VAL[reg_index][3];
        data[1] = REG_INIT_VAL[reg_index][2];
        data[2] = REG_INIT_VAL[reg_index][1];
        data[3] = REG_INIT_VAL[reg_index][0];

        if (!WriteFile(deviceUserHandle, data, REG_INIT_WIDTH, &size, NULL)) {
            //fprintf(stderr, "WriteFile to device %s failed with Win32 error code: %d\n", device_path, GetLastError());
            CloseHandle(deviceUserHandle);
            return QUANTIS_ERROR_IO;
        }

        Sleep(100);
    }

    CloseHandle(deviceUserHandle);

    QuantisPrivateData* _privateData = (QuantisPrivateData*)deviceHandle->privateData;
    
    // Read dummy data to flush fifo
    char* zeroBuffer = (char*)malloc(FLUSH_FIFO_SIZE);
    if (zeroBuffer != NULL)
    {
        int ret = QuantisPcieRead(deviceHandle, zeroBuffer, FLUSH_FIFO_SIZE);
        free(zeroBuffer);
    }
    return QUANTIS_SUCCESS;
}

int QuantisPcieInternalDeinit(QuantisDeviceHandle* deviceHandle)
{
    LARGE_INTEGER address;
    BYTE data[REG_INIT_WIDTH];
    DWORD size = REG_INIT_WIDTH;

    HANDLE deviceUserHandle;
    int ret = QuantisPcieInternalCreateFile(deviceHandle, &deviceUserHandle, L"\\user");
    if (ret != QUANTIS_SUCCESS)
    {
        return ret;
    }

    for (int reg_index = 0; reg_index < REG_DEINIT_LENGTH; reg_index++) {

        address.QuadPart = REG_DEINIT_ADDR[reg_index];
        if (INVALID_SET_FILE_POINTER == SetFilePointerEx(deviceUserHandle, address, NULL, FILE_BEGIN)) {
            //fprintf(stderr, "Error setting file pointer, win32 error code: %ld\n", GetLastError());
            CloseHandle(deviceUserHandle);
            return QUANTIS_ERROR_IO;
        }
        data[0] = REG_DEINIT_VAL[reg_index][3];
        data[1] = REG_DEINIT_VAL[reg_index][2];
        data[2] = REG_DEINIT_VAL[reg_index][1];
        data[3] = REG_DEINIT_VAL[reg_index][0];

        if (!WriteFile(deviceUserHandle, data, REG_INIT_WIDTH, &size, NULL)) {
            //fprintf(stderr, "WriteFile to device %s failed with Win32 error code: %d\n", device_path, GetLastError());
            CloseHandle(deviceUserHandle);
            return QUANTIS_ERROR_IO;
        }

        Sleep(100);
    }

    CloseHandle(deviceUserHandle);

    Sleep(2000);

    return QUANTIS_SUCCESS;
}

int QuantisPcieOpenInternal(QuantisDeviceHandle* deviceHandle, WCHAR* basePath, size_t lenBasePath)
{
    SP_DEVICE_INTERFACE_DATA devInfoData;
    HDEVINFO hDevInfo;
    PSP_DEVICE_INTERFACE_DETAIL_DATA detailData = NULL;
    DWORD length = 0;
    DWORD required = 0;

    /* Retrieve the device information for all Quantis devices */
    hDevInfo = SetupDiGetClassDevs(&GUID_DEVINTERFACE_QUANTIS_PCIE,
        NULL,
        NULL,
        DIGCF_DEVICEINTERFACE | DIGCF_PRESENT);
    if (hDevInfo == INVALID_HANDLE_VALUE)
    {
        /* No device present in the system */
        return QUANTIS_ERROR_NO_DEVICE;
    }

    /*
     * Enumerate the system's device interfaces and obtains information on our
     * device interface
     */
    devInfoData.cbSize = sizeof(devInfoData);

    int pciCardCount = QuantisPciOnlyCount();
    // Change deviceNumber to remove PCI Card number padding
    deviceHandle->deviceNumber = deviceHandle->deviceNumber - pciCardCount;

    if (!SetupDiEnumDeviceInterfaces(hDevInfo,
        0,
        &GUID_DEVINTERFACE_QUANTIS_PCIE,
        deviceHandle->deviceNumber,
        &devInfoData))
    {
        // Restore deviceNumber to add PCI Card number padding
        deviceHandle->deviceNumber = deviceHandle->deviceNumber + pciCardCount;
        SetupDiDestroyDeviceInfoList(hDevInfo);
        return QUANTIS_ERROR_NO_DEVICE;
    }
    else
    {
        // Restore deviceNumber to add PCI Card number padding
        deviceHandle->deviceNumber = deviceHandle->deviceNumber + pciCardCount;
    }

    /*
     * Get the required buffer size.
     *
     * Note that this function returns the required buffer size in length
     * variable and fails with GetLastError returning ERROR_INSUFFICIENT_BUFFER.
     */
    SetupDiGetDeviceInterfaceDetail(hDevInfo,
        &devInfoData,
        NULL,
        0,
        &length,
        NULL);

    detailData = (PSP_DEVICE_INTERFACE_DETAIL_DATA)alloca(length);
    detailData->cbSize = sizeof(SP_DEVICE_INTERFACE_DETAIL_DATA);

    /* Returns details about a device interface */
    if (!SetupDiGetDeviceInterfaceDetail(hDevInfo,
        &devInfoData,
        detailData,
        length,
        &required,
        NULL))
    {
        /* Destroy data */
        SetupDiDestroyDeviceInfoList(hDevInfo);
        return QUANTIS_ERROR_IO;
    }

    /* Destroy not more useful data */
    SetupDiDestroyDeviceInfoList(hDevInfo);

    wcscpy_s(basePath, lenBasePath, detailData->DevicePath);

    return QUANTIS_SUCCESS;
}


/* ModulesDisable */
int QuantisPcieModulesDisable(QuantisDeviceHandle* deviceHandle, int moduleMask)
{
    deviceHandle = deviceHandle; /* Avoids unused parameter warning */
    moduleMask = moduleMask; /* Avoids unused parameter warning */

    return QUANTIS_SUCCESS;
}


/* ModulesEnable */
int QuantisPcieModulesEnable(QuantisDeviceHandle* deviceHandle, int moduleMask)
{
    deviceHandle = deviceHandle; /* Avoids unused parameter warning */
    moduleMask = moduleMask; /* Avoids unused parameter warning */

    return QUANTIS_SUCCESS;
}


/* GetAis31StartupTestsRequestFlag */
int QuantisPcieGetAis31StartupTestsRequestFlag(QuantisDeviceHandle* deviceHandle)
{
    // Not supported with this PCIe chip
    return 0;
}


/* ClearAis31StartupTestsRequestFlag */
int QuantisPcieClearAis31StartupTestsRequestFlag(QuantisDeviceHandle* deviceHandle)
{
    // Not supported with this PCIe chip
    return 0;
}

/* Open */
int QuantisPcieOnlyOpen(QuantisDeviceHandle* deviceHandle)
{
    QuantisPrivateData* _privateData = NULL;
    HANDLE deviceUserHandle;
    int ret = QuantisPcieInternalCreateFile(deviceHandle, &deviceUserHandle, L"\\c2h_0");
    if (ret != QUANTIS_SUCCESS)
    {
        return ret;
    }

    /* Allocate memory for private data */
    _privateData = (QuantisPrivateData*)malloc(sizeof(QuantisPrivateData));
    if (!_privateData)
    {
        return QUANTIS_ERROR_NO_MEMORY;
    }

    _privateData->deviceHandle = deviceUserHandle;
    _privateData->serialNumber[0] = '\0';

    deviceHandle->privateData = _privateData;
    deviceHandle->ops = &QuantisOperationsPcie;

    QuantisPcieInternalInit(deviceHandle);

    return QUANTIS_SUCCESS;
}

static int is_printable(char c) {
    // anything below ASCII code 32 is non-printing, 127 is DELETE
    if (c < 32 || c == 127) {
        return FALSE;
    }
    return TRUE;
}
static void print_bytes(ULONGLONG addr, BYTE* data, size_t length)
{
    FILE* output = stdout;

    // formatted output
    for (int row = 0; row < (int)(length / 16 + ((length % 16) ? 1 : 0));
        row++) {

        // Print address
        fprintf(output, "0x%04llX:  ", addr + row * 16);

        // Print bytes
        int column;
        for (column = 0; column < (int)min(16, length - (row * 16));
            column++) {
            fprintf(output, "%02x ", data[(row * 16) + column]);
        }
        for (; column < 16; column++) {
            fprintf(output, "   ");
        }

        // Print gutter
        fprintf(output, "    ");

        // Print characters
        for (column = 0; column < (int)min(16, length - (row * 16));
            column++) {
            fprintf(output, "%c", is_printable(data[(row * 16) + column]) ?
                (data[(row * 16) + column]) : '.');
        }
        for (; column < 16; column++) {
            fprintf(output, " ");
        }
        fprintf(output, "\n");
    }
}

/* Read */
int QuantisPcieRead(QuantisDeviceHandle* deviceHandle, void* buffer, size_t size)
{
    QuantisPrivateData* _privateData = NULL;
    BOOL result;

    _privateData = (QuantisPrivateData*)deviceHandle->privateData;

    long int remaininglength = size;
    DWORD copiedlength = 0;
    DWORD timeoutCounter = 0;

    char* localBuffer = (char*)malloc(4096);

    do {
        DWORD receivedNumBytes = 0;

        result = ReadFile(_privateData->deviceHandle,
            localBuffer,
            4096,
            &receivedNumBytes,
            NULL);

        if (!result)
        {
            DWORD lastError = GetLastError();
            printf("Error while reading, win32 error code: %ld \n", lastError);
            free(localBuffer);
            return QUANTIS_ERROR_IO;
        }

        if (receivedNumBytes == 0) {
            timeoutCounter++;
            if (timeoutCounter > 10)
            {
                free(localBuffer);
                return QUANTIS_ERROR_IO;
            }
        }
        else
        {
            timeoutCounter = 0;
        }

        if (remaininglength >= receivedNumBytes) {
            memcpy(((char*)buffer + copiedlength), localBuffer, receivedNumBytes);
        }
        else {
            memcpy(((char*)buffer + copiedlength), localBuffer, remaininglength);
        }

        copiedlength += receivedNumBytes;
        remaininglength -= receivedNumBytes;

    } while (remaininglength > 0);

    free(localBuffer);
    return (int)size;
}


/* GetBusDeviceId */
int QuantisPcieGetBusDeviceId(QuantisDeviceHandle* deviceHandle)
{
    //only implemented with Unix PCI
    return 0;
}

#endif /* DISABLE_QUANTIS_PCIE */
