# Quantis runtime

This directory contains the Windows x64 JNI runtime loaded by
`com.idquantique.quantis.Quantis` through `System.loadLibrary("Quantis")`.

The DLL is built from the official ID Quantique 20.2.3 source retained under
`third_party/quantis-20.2.3`. It imports only Windows system libraries; no
additional third-party runtime DLL is required beside it.

The signed device driver must still be installed once on the computer. Follow
`docs/QUANTIS_USB_SETUP.md` for the complete setup and verification procedure.
