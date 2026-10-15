<#
============================================================================
  Build-MBox.ps1 —— 一键编译 聚视盒 APK 并保存到项目目录
  右键 → 使用 PowerShell 运行 即可，结束后窗口会停留不闪退
============================================================================
  用法：
    右键“使用 PowerShell 运行”本脚本（默认编译 Release）
    或在 PowerShell 中：
      .\Build-MBox.ps1                  # Release
      .\Build-MBox.ps1 -Debug           # Debug
      .\Build-MBox.ps1 -OutDir D:\apk   # 指定输出目录
============================================================================
#>

param(
    [switch]$Debug,
    [string]$OutDir = '',
    [switch]$SkipJavaClean
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }
Set-Location $root

try {
    if (-not (Test-Path (Join-Path $root 'gradlew.bat'))) {
        Write-Host "!! 未在 $root 找到 gradlew.bat" -ForegroundColor Red
        exit 1
    }

    $buildType = if ($Debug) { 'debug' } else { 'release' }
    $taskName  = 'assemble' + $buildType.Substring(0, 1).ToUpper() + $buildType.Substring(1)
    $outDir    = if ($OutDir -ne '') { $OutDir } else { Join-Path $root 'APK' }
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null

    Write-Host ''
    Write-Host '==============================================' -ForegroundColor Cyan
    Write-Host "  聚视盒编译任务 : $taskName" -ForegroundColor Cyan
    Write-Host "  输出目录       : $outDir" -ForegroundColor Cyan
    Write-Host '==============================================' -ForegroundColor Cyan
    Write-Host ''

    Write-Host "==> 开始编译，首次运行需下载依赖，请耐心等待 ..." -ForegroundColor Yellow
    & .\gradlew.bat $taskName --no-daemon
    if ($LASTEXITCODE -ne 0) {
        Write-Host "!! 编译失败（gradle 退出码: $LASTEXITCODE）" -ForegroundColor Red
        exit $LASTEXITCODE
    }

    $apkDir = Join-Path $root "app\build\outputs\apk\$buildType"
    $apk = Get-ChildItem -Path $apkDir -Filter '*.apk' -ErrorAction SilentlyContinue |
           Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $apk) {
        Write-Host "!! 编译完成但未在 $apkDir 找到 APK" -ForegroundColor Red
        exit 1
    }

    $dest = Join-Path $outDir $apk.Name
    Copy-Item $apk.FullName $dest -Force
    Write-Host ''
    Write-Host "==> APK 已保存到: $dest" -ForegroundColor Green
    Write-Host ("    大小: {0:N1} MB" -f ($apk.Length / 1MB))

    if (-not $SkipJavaClean) {
        Write-Host ''
        Write-Host '==> 清理残留 Java 进程 ...' -ForegroundColor Yellow
        $killed = 0
        try {
            $javaProcs = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction Stop
            foreach ($p in $javaProcs) {
                if ($p.CommandLine -match 'gradle|kotlin') {
                    try { Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop; $killed++ } catch {}
                }
            }
        } catch {}
        Write-Host "    共清理 $killed 个进程" -ForegroundColor Green
    }

    Write-Host ''
    Write-Host '========== 编译完成 ==========' -ForegroundColor Green
    Write-Host "  APK: $dest" -ForegroundColor Green
    Write-Host '==============================' -ForegroundColor Green
}
catch {
    Write-Host ''
    Write-Host "!! 脚本出错: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host $_.ScriptStackTrace -ForegroundColor DarkGray
}

Write-Host ''
Read-Host '按回车键关闭窗口'
