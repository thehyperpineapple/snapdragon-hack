#!/usr/bin/env powershell
# Automated script to push model files to phone via adb
# Usage: .\push_model_to_device.ps1

Write-Host "=================================================="
Write-Host "LLM Model Push to Device (ADB)"
Write-Host "=================================================="
Write-Host ""

$MODEL_SOURCE = "C:\Users\karti\.qaihm\models\llama_v3_2_1b_instruct\v4\llama32_ckpt_w4\llama32_ckpt_w4"
$DEVICE_DEST = "/data/data/com.example.snap_app/files/models/llm"

# Check adb
$adbPath = (Get-Command adb -ErrorAction SilentlyContinue).Path
if (-not $adbPath) {
    Write-Host "ERROR: adb command not found!" -ForegroundColor Red
    Write-Host "Install Android SDK Platform Tools and add to PATH" -ForegroundColor Yellow
    exit 1
}

# Check device
Write-Host "Checking for connected device..." -ForegroundColor Yellow
$devices = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\S+' } | ForEach-Object { $_ -split '\s+' | Select-Object -First 1 }

if ($devices.Count -eq 0) {
    Write-Host "ERROR: No Android device connected!" -ForegroundColor Red
    Write-Host "Connect via USB and enable USB debugging" -ForegroundColor Yellow
    exit 1
}

Write-Host "Device found: $($devices[0])" -ForegroundColor Green
Write-Host ""

# Check files exist
Write-Host "Checking source model files..." -ForegroundColor Yellow
$files = @("config.json", "tokenizer.json", "special_tokens_map.json", "model.encodings", "model.data")
$missingFiles = @()

foreach ($file in $files) {
    $filePath = Join-Path $MODEL_SOURCE $file
    if (Test-Path $filePath) {
        $size = (Get-Item $filePath).Length / 1GB
        Write-Host "  OK: $file $('{0:N2}' -f $size) GB" -ForegroundColor Green
    } else {
        Write-Host "  MISSING: $file" -ForegroundColor Red
        $missingFiles += $file
    }
}

if ($missingFiles.Count -gt 0) {
    Write-Host ""
    Write-Host "ERROR: Missing files: $($missingFiles -join ', ')" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Creating directory on device..." -ForegroundColor Yellow
adb shell mkdir -p $DEVICE_DEST

Write-Host ""
Write-Host "Pushing files to device (5-10 minutes)..." -ForegroundColor Cyan
Write-Host "Total size: 6.2 GB" -ForegroundColor Cyan
Write-Host ""

$startTime = Get-Date
$totalFiles = $files.Count
$currentFile = 0

foreach ($file in $files) {
    $currentFile++
    $filePath = Join-Path $MODEL_SOURCE $file
    $fileSize = (Get-Item $filePath).Length / 1GB
    
    Write-Host "[$currentFile/$totalFiles] Pushing $file $('{0:N2}' -f $fileSize) GB..." -ForegroundColor Yellow
    
    $pushOutput = adb push "$filePath" "$DEVICE_DEST/$file" 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Complete" -ForegroundColor Green
    } else {
        Write-Host "  Failed!" -ForegroundColor Red
        Write-Host $pushOutput
        exit 1
    }
}

$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host ""
Write-Host "=================================================="
Write-Host "SUCCESS: ALL FILES PUSHED" -ForegroundColor Green
Write-Host "=================================================="
Write-Host "Time: $([Math]::Round($duration.TotalSeconds)) seconds" -ForegroundColor Cyan
Write-Host ""

Write-Host "Verifying files on device..." -ForegroundColor Yellow
$deviceFiles = adb shell ls -la $DEVICE_DEST 2>&1
Write-Host $deviceFiles -ForegroundColor White
Write-Host ""

$totalSize = adb shell du -sh "$DEVICE_DEST" 2>&1
Write-Host "Total on device: $totalSize" -ForegroundColor Cyan
Write-Host ""

Write-Host "Next steps:" -ForegroundColor Green
Write-Host "1. Open app on your phone" -ForegroundColor White
Write-Host "2. Go to AI Assistant Chat" -ForegroundColor White
Write-Host "3. Check for Model loaded message" -ForegroundColor White
Write-Host ""

Write-Host "For logs run:" -ForegroundColor Yellow
Write-Host "   adb logcat -s GenieService" -ForegroundColor Cyan
Write-Host ""
