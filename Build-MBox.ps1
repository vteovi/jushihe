# ============================================================
#  聚视盒 APK Builder
#  右键“使用 PowerShell 运行”即可
# ============================================================

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

$env:JAVA_HOME    = 'C:\Java\jdk-17'
$env:ANDROID_HOME = 'C:\Android\Sdk'

Write-Host '============================================'
Write-Host '   聚视盒 APK Builder'
Write-Host '============================================'

# --- prerequisite checks ---
if (-not (Test-Path 'gradlew.bat')) {
    Write-Host '[ERROR] gradlew.bat not found.'
    Read-Host 'Press Enter to exit'; exit 1
}
if (-not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Host "[ERROR] JDK 17 not found at $env:JAVA_HOME"
    Read-Host 'Press Enter to exit'; exit 1
}

# --- read version name ---
$ver = 'unknown'
$bg = Get-Content 'app\build.gradle' -Raw
if ($bg -match "versionName\s+'([^']+)'") { $ver = $Matches[1] }
Write-Host "  Project version: $ver"

# --- pick build type ---
$buildType = $args[0]
if (($buildType -ne 'debug') -and ($buildType -ne 'release')) {
    Write-Host ''
    Write-Host '  Select build type:'
    Write-Host '    [1] Debug   - for daily testing'
    Write-Host '    [2] Release - signed, for install and share'
    $choice = Read-Host '  Enter 1 or 2'
    if ($choice -eq '2') { $buildType = 'release' } else { $buildType = 'debug' }
}

# --- stop leftover gradle daemon ---
& .\gradlew.bat --stop 2>$null

Write-Host ''
Write-Host '============================================'
Write-Host "  Building $buildType APK, please wait..."
Write-Host '============================================'
Write-Host ''

if ($buildType -eq 'release') {
    & .\gradlew.bat assembleRelease --console=plain
} else {
    & .\gradlew.bat assembleDebug --console=plain
}

if ($LASTEXITCODE -ne 0) {
    Write-Host ''
    Write-Host '============================================'
    Write-Host '  BUILD FAILED - check errors above'
    Write-Host '============================================'
    & .\gradlew.bat --stop 2>$null
    Read-Host 'Press Enter to exit'; exit 1
}

# --- locate produced apk ---
$outDir = Join-Path 'app\build\outputs\apk' $buildType
$apk = Get-ChildItem -Path "$outDir\*.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) {
    Write-Host "  [WARN] APK not found in $outDir"
    & .\gradlew.bat --stop 2>$null
    Read-Host 'Press Enter to exit'; exit 1
}

# --- copy apk to project root ---
$destName = "JushiHe-v$ver-$buildType.apk"
$destPath = Join-Path $ScriptDir $destName
Copy-Item -Path $apk.FullName -Destination $destPath -Force

Write-Host ''
Write-Host '============================================'
Write-Host '  BUILD SUCCESS'
Write-Host '============================================'
Write-Host "  Size      : $([math]::Round($apk.Length/1MB, 1)) MB"
Write-Host "  Copied to : $destPath"

# --- clean up gradle daemon ---
& .\gradlew.bat --stop 2>$null

Write-Host ''
Read-Host 'Press Enter to exit'
exit 0
