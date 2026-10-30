param(
    [string]$AvdName = 'morrow-release-test',
    [string]$AvdHome = $(if ($env:ANDROID_AVD_HOME) { $env:ANDROID_AVD_HOME } else { Join-Path $env:USERPROFILE '.android\avd' }),
    [int]$Port = 5556
)
$ErrorActionPreference = 'Stop'
if ($AvdName -notmatch '^[A-Za-z0-9_.-]+$' -or $AvdName -in @('.', '..')) { throw 'Invalid AVD name.' }
if ($Port -lt 5554 -or $Port -gt 5682 -or $Port % 2 -ne 0) { throw 'Use an even emulator port from 5554 through 5682.' }
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$emulator = Join-Path $sdk 'emulator\emulator.exe'
$adb = Join-Path $sdk 'platform-tools\adb.exe'
if (!(Test-Path -LiteralPath $emulator) -or !(Test-Path -LiteralPath $adb)) { throw 'Android emulator or adb is missing from the SDK.' }
if (!(Test-Path -LiteralPath (Join-Path $AvdHome "$AvdName.ini"))) { throw "AVD '$AvdName' was not found in $AvdHome. Create an isolated Android AVD first." }
$env:ANDROID_AVD_HOME = $AvdHome
$serial = "emulator-$Port"
$devices = & $adb devices
if ($LASTEXITCODE -ne 0) { throw 'Could not query adb devices.' }
if ($devices -match "(?m)^$serial\s+") {
    $runningName = (& $adb -s $serial emu avd name | Select-Object -First 1).Trim()
    if ($LASTEXITCODE -ne 0 -or $runningName -ne $AvdName) { throw "$serial belongs to a different or unavailable emulator." }
} else {
    Start-Process -FilePath $emulator -ArgumentList @('-avd', $AvdName, '-no-window', '-no-audio', '-no-snapshot', '-gpu', 'swiftshader_indirect', '-port', "$Port") -WindowStyle Hidden | Out-Null
}
for ($attempt = 0; $attempt -lt 90; $attempt++) {
    $boot = (& $adb -s $serial shell getprop sys.boot_completed 2>$null)
    if ($LASTEXITCODE -eq 0 -and "$boot".Trim() -eq '1') {
        $runningName = (& $adb -s $serial emu avd name | Select-Object -First 1).Trim()
        if ($runningName -ne $AvdName) { throw "$serial belongs to a different emulator." }
        Write-Output "Ready: $serial ($AvdName). Existing emulator data was not wiped."
        exit 0
    }
    Start-Sleep -Seconds 2
}
throw "Emulator $AvdName did not finish booting; check its logs and SDK setup."
