<#
============================================================================
  Build-MBox.ps1 —— 一键编译 MBox (TVBoxMobile) APK 并保存到项目目录
============================================================================
  用法（在项目根目录打开 PowerShell 后执行）：
    .\Build-MBox.ps1                  # 默认编译 Release 版
    .\Build-MBox.ps1 -Debug           # 编译 Debug 版
    .\Build-MBox.ps1 -OutDir D:\apk   # 指定 APK 输出目录（默认: 项目根\APK）
    .\Build-MBox.ps1 -SkipJavaClean   # 编译完成后不清理 Java 进程

  脚本依次做：
    1. 在项目根目录运行 gradlew 编译（assembleRelease / assembleDebug，--no-daemon）
    2. 把生成的 APK 复制到输出目录（默认 项目根\APK，目录自动创建）
    3. 构建结束后清理残留的 Gradle/Kotlin Java 进程，释放内存

  前提：
    - 已安装 JDK 11+（JAVA_HOME 已配置）
    - Android SDK 已安装（项目根目录有 local.properties，或已设置 ANDROID_HOME）
    - 首次运行会自动下载 Gradle 7.3.3 与依赖，需要联网，耗时较长
============================================================================
#>

param(
    [switch]$Debug,          # 编译 Debug 版（默认 Release）
    [string]$OutDir = '',    # APK 输出目录，留空则用 项目根\APK
    [switch]$SkipJavaClean   # 构建结束后跳过 Java 进程清理
)

$ErrorActionPreference = 'Stop'

# ---------- 0. 定位项目根目录（脚本所在目录） ----------
$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }
Set-Location $root

if (-not (Test-Path (Join-Path $root 'gradlew.bat'))) {
    Write-Host "!! 未在 $root 找到 gradlew.bat，请把本脚本放在 TVBoxMobile 项目根目录" -ForegroundColor Red
    exit 1
}

# ---------- 1. 确定编译类型与输出目录 ----------
$buildType  = if ($Debug) { 'debug' } else { 'release' }
$taskName   = 'assemble' + $buildType.Substring(0, 1).ToUpper() + $buildType.Substring(1)
$outDir     = if ($OutDir -ne '') { $OutDir } else { Join-Path $root 'APK' }
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Write-Host ''
Write-Host '==============================================' -ForegroundColor Cyan
Write-Host "  MBox 编译任务 : $taskName" -ForegroundColor Cyan
Write-Host "  输出目录      : $outDir" -ForegroundColor Cyan
Write-Host '==============================================' -ForegroundColor Cyan
Write-Host ''

# ---------- 2. 执行编译（--no-daemon 避免 Gradle 守护进程残留） ----------
Write-Host "==> 开始编译，首次运行需下载依赖，请耐心等待 ..." -ForegroundColor Yellow
& .\gradlew.bat $taskName --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "!! 编译失败（gradle 退出码: $LASTEXITCODE），请查看上方日志" -ForegroundColor Red
    exit $LASTEXITCODE
}

# ---------- 3. 找到刚生成的 APK 并复制到输出目录 ----------
$apkDir = Join-Path $root "app\build\outputs\apk\$buildType"
$apk = Get-ChildItem -Path $apkDir -Filter '*.apk' -ErrorAction SilentlyContinue |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) {
    Write-Host "!! 编译完成但未在 $apkDir 找到 APK 文件" -ForegroundColor Red
    exit 1
}

$dest = Join-Path $outDir $apk.Name
Copy-Item $apk.FullName $dest -Force
Write-Host ''
Write-Host "==> APK 已保存到: $dest" -ForegroundColor Green
Write-Host "    原始位置    : $($apk.FullName)"
Write-Host "    大小        : {0:N1} MB" -f ($apk.Length / 1MB)

# ---------- 4. 清理残留的 Java 进程（Gradle/Kotlin 相关） ----------
if (-not $SkipJavaClean) {
    Write-Host ''
    Write-Host '==> 清理残留的 Java 进程 ...' -ForegroundColor Yellow
    $killed = 0
    try {
        $javaProcs = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction Stop
        foreach ($p in $javaProcs) {
            $cmdline = $p.CommandLine
            if ($cmdline -match 'gradle|kotlin') {
                try {
                    Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop
                    $killed++
                    $brief = $cmdline.Substring(0, [Math]::Min(70, $cmdline.Length))
                    Write-Host "    已结束 PID $($p.ProcessId) : $brief" -ForegroundColor DarkGray
                } catch {
                    Write-Host "    无法结束 PID $($p.ProcessId) : $($_.Exception.Message)" -ForegroundColor Yellow
                }
            }
        }
    } catch {
        Write-Host '    (无法枚举 java 进程，跳过清理)' -ForegroundColor Yellow
    }
    Write-Host "    共清理 $killed 个 Gradle/Kotlin 相关 Java 进程" -ForegroundColor Green
}

Write-Host ''
Write-Host '========== 完成 ==========' -ForegroundColor Green
Write-Host "  APK: $dest" -ForegroundColor Green
Write-Host '===========================' -ForegroundColor Green
