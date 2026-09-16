/*
 * Quantis PCI driver
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
 * terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
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
 * For history of changes, ChangeLog.txt
 *
 * This is the GUID definition mainly used in driver and user mode
 * to identify the device.
 * This file is produced by modifying a microsoft sample file.
 */

/* 
 * GUID definition are required to be outside of header inclusion pragma to avoid
 * error during precompiled headers.
 */
#include <initguid.h>

/* 
 * Define an Interface Guid for AMCC PCI device.
 * This GUID is used to register (IoRegisterDeviceInterface) 
 * an instance of an interface so that user application 
 * can control the device.
 *
 * {F2F5F1E5-380E-4194-8FC0-ED5AF63E93DF}
 */
DEFINE_GUID(GUID_DEVINTERFACE_QUANTIS_PCI,
            0xF2F5F1E5,
            0x380E, 0x4194, 0x8F, 0xC0, 0xED, 0x5A, 0xF6, 0x3E, 0x93, 0xDF);

#include "stdint.h"

#ifndef PUBLIC_H
#define PUBLIC_H

/* 
 * This type is very specific to some IOCTL. Only the bits 0-3 are used and they
 * represent Quantis modules (they can be at most 4 modules per card)
 */
typedef u_int32_t ModuleMask_t;

/* Value to detect the different module in a mask */
#define MODULE1 1
#define MODULE2 2
#define MODULE3 4
#define MODULE4 8

/* This small function returns the number of modules in a mask */
static unsigned char GetModule(ModuleMask_t mask)
{
  unsigned char NbModules = 0;
  if (mask & MODULE1)
  {
    NbModules++;
  }

  if (mask & MODULE2)
  {
    NbModules++;
  }

  if (mask & MODULE3)
  {
    NbModules++;
  }

  if (mask & MODULE4)
  {
    NbModules++;
  }

  return NbModules;
}

/* All the ioctl the data used in IOCTL are of type IoctlData. 
 *  
 *  read-only IOCTL:
 *   QUANTIS_IOCTL_GET_CARD_COUNT
 *   QUANTIS_IOCTL_GET_MODULES_MASK
 *   QUANTIS_IOCTL_GET_BOARD_VERSION
 *   QUANTIS_IOCTL_GET_MODULES_STATUS
 *   QUANTIS_IOCTL_GET_DRIVER_VERSION
 *  
 *  write-only IOCTL:
 *   QUANTIS_IOCTL_ENABLE_MODULE
 *   QUANTIS_IOCTL_DISABLE_MODULE
 *   
 *  no data IOCTL:
 *   QUANTIS_IOCTL_RESET_BOARD
 */

/* get number of detected cards */
#define QUANTIS_IOCTL_GET_CARD_COUNT CTL_CODE(FILE_DEVICE_UNKNOWN, 0x800, METHOD_BUFFERED, FILE_ANY_ACCESS)

/* get mask of detected modules */
#define QUANTIS_IOCTL_GET_MODULES_MASK CTL_CODE(FILE_DEVICE_UNKNOWN, 0x801, METHOD_BUFFERED, FILE_ANY_ACCESS)

/* get card serial number */
#define QUANTIS_IOCTL_GET_BOARD_VERSION CTL_CODE(FILE_DEVICE_UNKNOWN, 0x802, METHOD_BUFFERED, FILE_ANY_ACCESS)

/* reset one board */
#define QUANTIS_IOCTL_RESET_BOARD CTL_CODE(FILE_DEVICE_UNKNOWN, 0x803, METHOD_BUFFERED, FILE_ANY_ACCESS)

/* enable mask module */
#define QUANTIS_IOCTL_ENABLE_MODULE CTL_CODE(FILE_DEVICE_UNKNOWN, 0x804, METHOD_BUFFERED, FILE_ANY_ACCESS)

/* disable mask modules */
#define QUANTIS_IOCTL_DISABLE_MODULE CTL_CODE(FILE_DEVICE_UNKNOWN, 0x805, METHOD_BUFFERED, FILE_ANY_ACCESS)

/* get status of modules */
#define QUANTIS_IOCTL_GET_MODULES_STATUS CTL_CODE(FILE_DEVICE_UNKNOWN, 0x807, METHOD_BUFFERED, FILE_ANY_ACCESS)

/* get driver version */
#define QUANTIS_IOCTL_GET_DRIVER_VERSION CTL_CODE(FILE_DEVICE_UNKNOWN, 0x808, METHOD_BUFFERED, FILE_ANY_ACCESS)

/* get AIS-31 startup Tests Request flag */
#define QUANTIS_IOCTL_GET_AIS31_STARTUP_TESTS_REQUEST_FLAG CTL_CODE(FILE_DEVICE_UNKNOWN, 0x809, METHOD_BUFFERED, FILE_ANY_ACCESS)

/* clear AIS-31 startup Tests Request flag */
#define QUANTIS_IOCTL_CLEAR_AIS31_STARTUP_TESTS_REQUEST_FLAG CTL_CODE(FILE_DEVICE_UNKNOWN, 0x80A, METHOD_BUFFERED, FILE_ANY_ACCESS)

#endif
