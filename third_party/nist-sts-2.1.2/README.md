# NIST Statistical Test Suite 2.1.2 reference source

This directory contains an unmodified copy of the NIST SP 800-22 Rev. 1a C reference implementation. It is kept
separate from application code and will be used as the behavioral oracle for the Java port.

## Provenance

- Source: `https://csrc.nist.gov/CSRC/media/Projects/Random-Bit-Generation/documents/sts-2_1_2.zip`
- Retrieved: 2026-08-25
- Archive: `source.zip`
- SHA-256: `0238D2F1D26E120E3CC748ED2D4C674CDC636DE37FC4027C76CC2A394FFF9157`
- Extracted source: `upstream/sts-2.1.2/sts-2.1.2`

Do not edit files below `upstream`. Java ports and their comparison tests belong in the normal project source/test
directories. Every ported test should be checked against p-values produced by this C implementation using the
same bitstreams and parameters.

## Build note

The upstream suite was compiled successfully in WSL with GCC and Make on 2026-08-25. Its supplied `data.pi` file
was tested as ten 100,000-bit streams with the default parameters, producing the reference results under
`experiments/AlgorithmTesting`.

All 15 Java test categories have been verified against p-values produced by this C implementation. The primary
baseline uses ten 100,000-bit `data.pi` streams; the Universal test uses a 1,000,000-bit stream because it is not
applicable at 100,000 bits. The comparison tolerance is `0.0000006`, accounting for the C report's six-decimal
output precision.

Run the complete Java suite with `NistTestSuite.standard()`. It requires the NIST-recommended 1,000,000-bit
(125,000-byte) input stream and returns the flattened multi-result output in canonical test order.
