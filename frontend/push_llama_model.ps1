#!/usr/bin/env pwsh
# Script to push Llama 3.2 1B model files to Android device
# For use with Snapdragon 8 Elite and Genie SDK

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "     Llama 3.2 1B Model Deployment     " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$MODEL_SOURCE = "C:\Users\karti\StudioProjects\llama_model\llama_v3_2_1b_instruct-TargetRuntime.GENIE-w4-qualcomm-snapdragon-8-elite"
$DEVICE_DEST = "/data/local/tmp/snap_models"
$APP_DEST = "/data/data/com.example.snap_app/files/models/llm"

# Check if adb is available
$adbCheck = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbCheck) {
    Write-Host "❌ ERROR: adb not found in PATH" -ForegroundColor Red
    Write-Host "Please install Android SDK Platform Tools" -ForegroundColor Yellow
    exit 1
}

# Check device connection
Write-Host "Checking device connection..." -ForegroundColor Yellow
$devices = adb devices
if ($devices -match "unauthorized") {
    Write-Host "❌ ERROR: Device is unauthorized" -ForegroundColor Red
    Write-Host "Please accept the USB debugging prompt on your device" -ForegroundColor Yellow
    exit 1
} elseif (-not ($devices -match "device$")) {
    Write-Host "❌ ERROR: No device connected" -ForegroundColor Red
    Write-Host "Please connect your Snapdragon device via USB" -ForegroundColor Yellow
    exit 1
}
Write-Host "✅ Device connected" -ForegroundColor Green
Write-Host ""

# Check if model files exist
if (-not (Test-Path $MODEL_SOURCE)) {
    Write-Host "❌ ERROR: Model directory not found at:" -ForegroundColor Red
    Write-Host "  $MODEL_SOURCE" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Please export the model first using:" -ForegroundColor Yellow
    Write-Host "  python -m qai_hub_models.models.llama_v3_2_1b_instruct.export --device 'Snapdragon 8 Elite QRD' --skip-profiling --output-dir ./llama_model" -ForegroundColor Cyan
    exit 1
}
Write-Host "✅ Model files found" -ForegroundColor Green
Write-Host ""

# Create temporary directory on device
Write-Host "Creating directories on device..." -ForegroundColor Yellow
adb shell "mkdir -p $DEVICE_DEST" 2>&1 | Out-Null
Write-Host "✅ Created $DEVICE_DEST" -ForegroundColor Green
Write-Host ""

# List of required files
$files = @(
    "genie_config.json",
    "htp_backend_ext_config.json",
    "config.json",
    "tokenizer.json",
    "tokenizer_config.json",
    "llama_v3_2_1b_instruct_part_1_of_3.bin",
    "llama_v3_2_1b_instruct_part_2_of_3.bin",
    "llama_v3_2_1b_instruct_part_3_of_3.bin"
)

# Push files to device
Write-Host "Pushing model files to device..." -ForegroundColor Yellow
Write-Host "(This will take 3-5 minutes for ~1.2GB of data)" -ForegroundColor Cyan
Write-Host ""

$totalFiles = $files.Count
$currentFile = 0

foreach ($file in $files) {
    $currentFile++
    $sourcePath = Join-Path $MODEL_SOURCE $file
    
    if (Test-Path $sourcePath) {
        $fileSize = (Get-Item $sourcePath).Length / 1MB
        Write-Host "[$currentFile/$totalFiles] Pushing $file ($([math]::Round($fileSize, 2)) MB)..." -ForegroundColor Cyan
        
        $result = adb push $sourcePath "$DEVICE_DEST/$file" 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✅ Success" -ForegroundColor Green
        } else {
            Write-Host "  ❌ Failed: $result" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "⚠ Warning: $file not found, skipping..." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✅ All files pushed successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Verify files on device
Write-Host "Verifying files on device..." -ForegroundColor Yellow
$deviceFiles = adb shell "ls -lh $DEVICE_DEST" 2>&1
Write-Host $deviceFiles
Write-Host ""

# Calculate total size
$totalSize = adb shell "du -sh $DEVICE_DEST" 2>&1
Write-Host "Total size on device: $totalSize" -ForegroundColor Cyan
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "         Next Steps                     " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Build and install the app:" -ForegroundColor Yellow
Write-Host "   cd C:\Users\karti\StudioProjects\frontend" -ForegroundColor Cyan
Write-Host "   .\gradlew.bat assembleDebug" -ForegroundColor Cyan
Write-Host "   adb install -r app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Open the app and navigate to Chat screen" -ForegroundColor Yellow
Write-Host ""
Write-Host "3. Test with a message like:" -ForegroundColor Yellow
Write-Host "   'What should I eat for protein?'" -ForegroundColor Cyan
Write-Host ""
Write-Host "Model location: $DEVICE_DEST" -ForegroundColor Green
Write-Host ""
