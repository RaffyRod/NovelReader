# Local mirror of .github/workflows/e2e-android.yml matrix suites.
param(
    [string[]]$Suites = @("smoke", "features", "features-plugins", "features-flows", "reader", "providers")
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$e2e = Join-Path $root "e2e"
$apk = Join-Path $e2e "apps\QuickNovel-Ultimate.apk"
$appiumLog = Join-Path $e2e "appium-ci.log"
$results = New-Object System.Collections.Generic.List[object]

if (-not (Test-Path $apk)) { throw "Missing APK: $apk" }

function Wait-Boot {
    adb wait-for-device | Out-Null
    for ($i = 0; $i -lt 120; $i++) {
        $boot = (adb shell getprop sys.boot_completed 2>$null | Out-String).Trim()
        if ($boot -eq "1") {
            Write-Host "boot_completed=1"
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "Emulator did not boot"
}

function Wait-Pm {
    for ($i = 0; $i -lt 60; $i++) {
        adb shell pm path android 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "pm OK"
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "Package manager not ready"
}

function Install-Apk {
    adb uninstall com.lagradost.quicknovel.ultimate 2>$null | Out-Null
    adb uninstall com.lagradost.quicknovel.ultimate.debug 2>$null | Out-Null
    for ($a = 1; $a -le 3; $a++) {
        adb install -r -g $apk
        if ($LASTEXITCODE -eq 0) { return }
        Write-Host "install attempt $a failed"
        Start-Sleep -Seconds 5
    }
    throw "adb install failed"
}

function Stop-Port4723 {
    Get-NetTCPConnection -LocalPort 4723 -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}

function Start-AppiumExternal {
    Stop-Port4723
    Start-Sleep -Seconds 1
    if (Test-Path $appiumLog) { Remove-Item $appiumLog -Force }
    $npx = (Get-Command npx.cmd).Source
    $proc = Start-Process -FilePath $npx `
        -ArgumentList @("appium", "--address", "127.0.0.1", "--port", "4723", "--relaxed-security", "--allow-insecure", "adb_shell", "--log-level", "info") `
        -WorkingDirectory $e2e `
        -RedirectStandardOutput $appiumLog `
        -RedirectStandardError (Join-Path $e2e "appium-ci.err.log") `
        -PassThru -WindowStyle Hidden
    for ($i = 0; $i -lt 90; $i++) {
        try {
            $r = Invoke-WebRequest -Uri "http://127.0.0.1:4723/status" -UseBasicParsing -TimeoutSec 2
            if ($r.StatusCode -eq 200) {
                Write-Host "Appium is up (pid $($proc.Id))"
                return $proc
            }
        } catch {}
        if ($proc.HasExited) {
            Write-Host "===== appium log ====="
            Get-Content $appiumLog -ErrorAction SilentlyContinue
            Get-Content (Join-Path $e2e "appium-ci.err.log") -ErrorAction SilentlyContinue
            throw "Appium exited early"
        }
        Start-Sleep -Seconds 1
    }
    throw "Appium did not start"
}

Set-Location $e2e
if (-not (Test-Path "node_modules\@wdio\cli")) {
    npm ci
}
cmd /c "npx appium driver install uiautomator2 >nul 2>&1"
cmd /c "npx appium driver update uiautomator2 >nul 2>&1"

Wait-Boot
Wait-Pm
adb shell input keyevent 82 2>$null | Out-Null
adb shell input keyevent 3 2>$null | Out-Null
adb shell settings put global window_animation_scale 0 2>$null | Out-Null
adb shell settings put global transition_animation_scale 0 2>$null | Out-Null
adb shell settings put global animator_duration_scale 0 2>$null | Out-Null

$env:APP_PATH = $apk
# Release APK package (CI). Local shells often export *.ultimate.debug from debug installs.
$env:APP_PACKAGE = "com.lagradost.quicknovel.ultimate"
$env:CI = "true"
$env:WDIO_LOG_LEVEL = "warn"
$env:APPIUM_EXTERNAL = "1"
$env:APPIUM_HOST = "127.0.0.1"
$env:APPIUM_PORT = "4723"
$env:DISPLAY = ":99"

foreach ($suite in $Suites) {
    Write-Host ""
    Write-Host "========== SUITE: $suite =========="
    Install-Apk
    $appium = Start-AppiumExternal
    $wdio = Join-Path $e2e "node_modules\.bin\wdio.cmd"
    & $wdio run .\wdio.conf.js --suite $suite
    $code = $LASTEXITCODE
    if (-not $appium.HasExited) {
        Stop-Process -Id $appium.Id -Force -ErrorAction SilentlyContinue
    }
    Stop-Port4723
    $status = if ($code -eq 0) { "PASS" } else { "FAIL" }
    $results.Add([pscustomobject]@{ Suite = $suite; Exit = $code; Status = $status })
    Write-Host "SUITE $suite => $status ($code)"
}

Write-Host ""
Write-Host "===== CI E2E SUMMARY ====="
$results | Format-Table -AutoSize
$hardFail = @($results | Where-Object { $_.Exit -ne 0 -and $_.Suite -ne "providers" })
if ($hardFail.Count -gt 0) {
    Write-Host "HARD_FAIL"
    exit 1
}
Write-Host "CI_LOCAL_DONE"
exit 0
