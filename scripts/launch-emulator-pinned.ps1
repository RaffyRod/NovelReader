# Launch Pixel_8_Pro emulator small, pinned to saved position.
$ErrorActionPreference = 'Stop'
$posFile = Join-Path $env:USERPROFILE '.android\emulator-window-pos.json'
$emu = Join-Path $env:LOCALAPPDATA 'Android\Sdk\emulator\emulator.exe'
$avd = 'Pixel_8_Pro_API_34'
Start-Process -FilePath $emu -ArgumentList @('-avd', $avd, '-gpu', 'auto')
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $adb wait-for-device | Out-Null
for ($i = 0; $i -lt 60; $i++) {
  if ((& $adb shell getprop sys.boot_completed 2>$null).Trim() -eq '1') { break }
  Start-Sleep 2
}
Start-Sleep 2
if (-not (Test-Path $posFile)) { exit 0 }
$pos = Get-Content $posFile | ConvertFrom-Json
Add-Type @'
using System;
using System.Runtime.InteropServices;
public class EmuPos {
  [DllImport("user32.dll")] public static extern bool MoveWindow(IntPtr hWnd, int X, int Y, int nWidth, int nHeight, bool bRepaint);
  [DllImport("user32.dll")] public static extern bool ShowWindowAsync(IntPtr hWnd, int nCmdShow);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
'@
$p = Get-Process | Where-Object { $_.ProcessName -eq 'qemu-system-x86_64' -and $_.MainWindowHandle -ne [IntPtr]::Zero } | Select-Object -First 1
if ($p) {
  [EmuPos]::ShowWindowAsync($p.MainWindowHandle, 9) | Out-Null
  [EmuPos]::MoveWindow($p.MainWindowHandle, [int]$pos.x, [int]$pos.y, [int]$pos.w, [int]$pos.h, $true) | Out-Null
  [EmuPos]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
}
