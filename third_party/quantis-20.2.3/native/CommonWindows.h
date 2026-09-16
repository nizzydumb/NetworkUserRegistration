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
 * This is the header file common to all programs developped under Windows operating systems
 *Module Name:

    CommonWindows.h  - Definition common to almost all files used for manipulating
                       the Quantis number genrator.

Environment:

    Kernel/User mode
 */
#ifndef __WINDOWS_COMMON_HEADERS_H
#define __WINDOWS_COMMON_HEADERS_H

#ifdef __cplusplus
#define QUANTIS_EXTERN_C extern "C"
#define QUANTIS_EXTERN_C_START \
   extern "C"                  \
   {
#define QUANTIS_EXTERN_C_END }
#else
#define QUANTIS_EXTERN_C
#define QUANTIS_EXTERN_C_START
#define QUANTIS_EXTERN_C_END
#endif

QUANTIS_EXTERN_C_START

#include "stdint.h"

/** Vendor IDs */
#define VENDOR_ID_HESSO 0x179A

/** Devices IDs */
#define DEVICE_ID_QUANTIS_PCI 0x0001

/** Driver information */
#define QUANTIS_PCI_DRIVER_NAME "Quantis PCI RNG driver"
#define QUANTIS_PCI_DRIVER_SHORTNAME "quantis_pci"
#define QUANTIS_PCI_DRIVER_AUTHOR "ID Quantique SA"
#define QUANTIS_PCI_DRIVER_VERSION 202
#define QUANTIS_PCI_DRIVER_LICENSE "Dual BSD/GPL"

/**
 * The FIFO of the random number generator is 4096 bytes big. It can be either
 * empty, 1/4 full, half-full, 3/4 full or completely full.
 */
#define QUANTIS_FIFO_SIZE 4096

/**
 * Maximal size for the buffer used to read data. The driver will
 * never give more random data than this value in one call. this
 * value is 32 * QUANTIS_FIFO_SIZE.
 */
#define QUANTIS_DEVICE_BUFFER_SIZE (32 * 4096)

QUANTIS_EXTERN_C_END

#endif
