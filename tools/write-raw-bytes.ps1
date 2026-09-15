[CmdletBinding(DefaultParameterSetName = "Physical")]
param(
    [Parameter(Mandatory = $true, ParameterSetName = "Physical")]
    [ValidateRange(0, 2147483647)]
    [int]$DriveNumber,

    [Parameter(Mandatory = $true, ParameterSetName = "Image")]
    [string]$ImagePath,

    [Parameter(Mandatory = $true)]
    [ValidateRange(0, [long]::MaxValue)]
    [long]$ByteOffset,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$HexBytes,

    [Parameter(ParameterSetName = "Physical")]
    [switch]$Execute,

    [Parameter(ParameterSetName = "Physical")]
    [string]$Confirmation
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function ConvertFrom-HexBytes {
    param([string]$Text)

    $normalized = $Text -replace '(?i)0x', '' -replace '[^0-9A-Fa-f]', ''
    if ($normalized.Length -eq 0 -or $normalized.Length % 2 -ne 0) {
        throw "HexBytes must contain a non-empty, even number of hexadecimal digits."
    }

    $result = [byte[]]::new($normalized.Length / 2)
    for ($index = 0; $index -lt $result.Length; $index++) {
        $result[$index] = [Convert]::ToByte($normalized.Substring($index * 2, 2), 16)
    }
    return $result
}

function Assert-Range {
    param([long]$Length, [long]$Offset, [int]$Count)

    if ($Offset -gt $Length -or $Count -gt ($Length - $Offset)) {
        throw "Write range [$Offset, $($Offset + $Count)) exceeds target length $Length."
    }
}

function Write-AndVerify {
    param([string]$Path, [long]$Length, [long]$Offset, [byte[]]$Bytes)

    Assert-Range -Length $Length -Offset $Offset -Count $Bytes.Length
    $stream = [IO.File]::Open($Path, [IO.FileMode]::Open, [IO.FileAccess]::ReadWrite, [IO.FileShare]::ReadWrite)
    try {
        [void]$stream.Seek($Offset, [IO.SeekOrigin]::Begin)
        $stream.Write($Bytes, 0, $Bytes.Length)
        $stream.Flush($true)

        $actual = [byte[]]::new($Bytes.Length)
        [void]$stream.Seek($Offset, [IO.SeekOrigin]::Begin)
        $read = 0
        while ($read -lt $actual.Length) {
            $count = $stream.Read($actual, $read, $actual.Length - $read)
            if ($count -eq 0) {
                throw "Unexpected end of target during read-back verification."
            }
            $read += $count
        }
        for ($index = 0; $index -lt $Bytes.Length; $index++) {
            if ($Bytes[$index] -ne $actual[$index]) {
                throw "Read-back verification failed at relative byte $index."
            }
        }
    } finally {
        $stream.Dispose()
    }
}

$bytes = ConvertFrom-HexBytes -Text $HexBytes

if ($PSCmdlet.ParameterSetName -eq "Image") {
    $resolvedImage = (Resolve-Path -LiteralPath $ImagePath).Path
    if ([IO.Path]::GetExtension($resolvedImage) -ine ".img") {
        throw "Image test targets must use the .img extension."
    }
    $imageLength = (Get-Item -LiteralPath $resolvedImage).Length
    Write-AndVerify -Path $resolvedImage -Length $imageLength -Offset $ByteOffset -Bytes $bytes
    Write-Output "IMAGE_WRITE_VERIFIED path=$resolvedImage offset=$ByteOffset bytes=$($bytes.Length)"
    exit 0
}

$disk = Get-Disk -Number $DriveNumber -ErrorAction Stop
$expectedConfirmation = "WRITE-PHYSICALDRIVE-$DriveNumber"
$devicePath = "\\.\PhysicalDrive$DriveNumber"

if ($disk.BusType -ne "USB") {
    throw "Refusing disk $DriveNumber because its bus type is '$($disk.BusType)', not USB."
}
if (-not $disk.IsOffline) {
    throw "Refusing disk $DriveNumber because it is online. Take the intended USB disk offline first."
}
Assert-Range -Length ([long]$disk.Size) -Offset $ByteOffset -Count $bytes.Length

if (-not $Execute) {
    Write-Output "DRY_RUN target=$devicePath model='$($disk.FriendlyName)' size=$($disk.Size) offset=$ByteOffset bytes=$($bytes.Length)"
    Write-Output "No bytes were written. Re-run with -Execute -Confirmation '$expectedConfirmation'."
    exit 0
}
if ($Confirmation -cne $expectedConfirmation) {
    throw "Confirmation must exactly equal '$expectedConfirmation'."
}

$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = [Security.Principal.WindowsPrincipal]::new($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "Physical-drive writes require an elevated Administrator PowerShell process."
}

Write-AndVerify -Path $devicePath -Length ([long]$disk.Size) -Offset $ByteOffset -Bytes $bytes
Write-Output "PHYSICAL_WRITE_VERIFIED drive=$DriveNumber offset=$ByteOffset bytes=$($bytes.Length)"
