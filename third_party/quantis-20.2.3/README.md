# ID Quantique Quantis USB software 20.2.3

This directory retains the files required to reproduce and deploy this
project's Quantis USB integration:

- `driver/QuantisUsb`: official signed Windows x86/amd64 WinUSB driver package;
- `native`: official Quantis native and JNI source;
- `LICENSE.txt`: upstream redistribution terms;
- `CHANGELOG.txt`: upstream release history.

Source: the official `usb-module-20.2.3/windows` distribution supplied by ID
Quantique.

## Local portability adjustment

`native/QuantisUsb_Windows.cpp` has one mechanical portability adjustment for
the repository's MinGW x64 compiler:

- declarations used by the cleanup labels were moved before the possible
  `goto` statements;
- the wide-character API is called explicitly as `LoadLibraryW`.

The device protocol, data-reading logic, JNI signatures, and error handling
are unchanged. Build it with `build-quantis.ps1` from the repository root.
