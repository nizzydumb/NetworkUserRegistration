#ifndef QUANTIS_PCI_WINDOWS_H
#define QUANTIS_PCI_WINDOWS_H

#ifndef _WIN32
# error "This module is for Windows only!"
#endif

#include "Quantis_Internal.h"

#define QUANTIS_SERIAL_MAX_LENGTH 256

/**
 * QuantisPrivateData for Quantis PCI on Unix systems
 */
typedef struct QuantisPrivateData
{
    HANDLE deviceHandle; /* Device handle */
    char serialNumber[QUANTIS_SERIAL_MAX_LENGTH];
} QuantisPrivateData;

int QuantisPciOnlyOpen(QuantisDeviceHandle* deviceHandle); // Adaptation for PCIe, Windows only
int QuantisPcieOnlyOpen(QuantisDeviceHandle* deviceHandle); // Adaptation for PCIe, Windows only

int QuantisPciOnlyCount(); // Adaptation for PCIe, Windows only
int QuantisPcieOnlyCount(); // Adaptation for PCIe, Windows only

#ifndef DISABLE_QUANTIS_PCIE

int QuantisPcieBoardReset(QuantisDeviceHandle* deviceHandle);

void QuantisPcieClose(QuantisDeviceHandle* deviceHandle);

int QuantisPcieGetBoardVersion(QuantisDeviceHandle* deviceHandle);

float QuantisPcieGetDriverVersion();

char* QuantisPcieGetManufacturer(QuantisDeviceHandle* deviceHandle);

int QuantisPcieGetModulesMask(QuantisDeviceHandle* deviceHandle);

int QuantisPcieGetModulesDataRate(QuantisDeviceHandle* deviceHandle);

int QuantisPcieGetModulesPower(QuantisDeviceHandle* deviceHandle);

int QuantisPcieGetModulesStatus(QuantisDeviceHandle* deviceHandle);

char* QuantisPcieGetSerialNumber(QuantisDeviceHandle* deviceHandle);

int QuantisPcieModulesDisable(QuantisDeviceHandle* deviceHandle,
    int moduleMask);

int QuantisPcieModulesEnable(QuantisDeviceHandle* deviceHandle,
    int moduleMask);

int QuantisPcieRead(QuantisDeviceHandle* deviceHandle,
    void* buffer,
    size_t size);

int QuantisPcieGetBusDeviceId(QuantisDeviceHandle* deviceHandle);

int QuantisPcieGetAis31StartupTestsRequestFlag(QuantisDeviceHandle* deviceHandle);

int QuantisPcieClearAis31StartupTestsRequestFlag(QuantisDeviceHandle* deviceHandle);

#endif /* DISABLE_QUANTIS_PCIE */

#endif /* QUANTIS_PCI_WINDOWS_H */