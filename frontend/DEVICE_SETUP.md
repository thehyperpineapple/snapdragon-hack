# Device Setup Guide: Loading LLM Model on Phone

## Overview
The Llama 3.2 1B model is ~6.2GB, too large to bundle in APK. Instead, we push it to the phone using `adb` (Android Debug Bridge) for development/testing.

## Prerequisites
- Android SDK Platform Tools installed
- Snapdragon 8 Elite (or compatible) device connected via USB
- USB debugging enabled on device
- Model files already exported on PC at: `C:\Users\karti\.qaihm\models\llama_v3_2_1b_instruct\v4\llama32_ckpt_w4\llama32_ckpt_w4\`

## Step 1: Prepare Your Device

### Enable USB Debugging
1. Go to **Settings** → **About phone**
2. Tap **Build number** 7 times to enable Developer options
3. Go back to **Settings** → **System** → **Developer options**
4. Enable **USB debugging**
5. Connect phone via USB to PC

### Verify Connection
```powershell
adb devices
```
You should see your device listed as "device" (not "unauthorized")

## Step 2: Push Model Files to Phone

Each file must be pushed individually. The total will take 5-10 minutes depending on USB speed.

### Create Directory on Phone
```powershell
adb shell mkdir -p /data/data/com.example.snap_app/files/models/llm
```

### Push Model Files
Run these commands in PowerShell:

```powershell
# Navigate to model directory
cd "C:\Users\karti\.qaihm\models\llama_v3_2_1b_instruct\v4\llama32_ckpt_w4\llama32_ckpt_w4"

# Push each file (this will take several minutes for model.data - ~2GB file)
Write-Host "Pushing config.json..."
adb push config.json /data/data/com.example.snap_app/files/models/llm/

Write-Host "Pushing tokenizer.json..."
adb push tokenizer.json /data/data/com.example.snap_app/files/models/llm/

Write-Host "Pushing special_tokens_map.json..."
adb push special_tokens_map.json /data/data/com.example.snap_app/files/models/llm/

Write-Host "Pushing model.encodings (~150MB)..."
adb push model.encodings /data/data/com.example.snap_app/files/models/llm/

Write-Host "Pushing model.data (~6GB - this will take 3-5 minutes)..."
adb push model.data /data/data/com.example.snap_app/files/models/llm/
```

### Monitor Progress
Watch the transfer speed. Large files will show progress like:
```
model.data: 2.5GB / 5.99GB (42%)
```

## Step 3: Verify Files on Phone

Check if all files are present:
```powershell
adb shell ls -la /data/data/com.example.snap_app/files/models/llm/
```

Expected output:
```
-rw------- 1 u0_a###   app_#    5999999999 2026-02-07 12:30 model.data
-rw------- 1 u0_a###   app_#    150035762 2026-02-07 12:35 model.encodings  
-rw------- 1 u0_a###   app_#       967 2026-02-07 12:30 config.json
-rw------- 1 u0_a###   app_#     17209920 2026-02-07 12:30 tokenizer.json
-rw------- 1 u0_a###   app_#        342 2026-02-07 12:30 special_tokens_map.json
```

## Step 4: Run App and Test

1. Open the app on your phone
2. Navigate to **AI Assistant** → **Chat**
3. You should see: **"✅ Model loaded"** message
4. If still showing error, check:
   - App logs: `adb logcat | grep -i "genie\|model"`
   - File permissions: All files must be readable by app

### Troubleshoot: Check Logs
```powershell
adb logcat -s "GenieService" -G 16M | grep -E "MODEL|ERROR|Loading"
```

## Step 5: Test Inference

1. Type a message in the chat
2. You should see:
   - **Typing indicator** (animated dots)
   - **Model response** appears after processing
3. First inference may take 5-30 seconds (model JIT compilation + inference)

## Model Performance Notes

### Inference Speed (Expected)
- **First run**: 10-30 seconds (model load + JIT compilation)  
- **Subsequent runs**: 2-10 seconds per message
- **Depends on**: Phone load, thermal state, message length

### Memory Usage
- **Model load**: ~3-4 GB RAM
- **Per inference**: ~500MB additional
- **Device minimum**: 8GB RAM recommended (8 Elite has 12GB)

### Development Notes
The current implementation:
- ✅ Loads model files from phone app storage
- ✅ Provides stub responses (for testing UI/UX)
- ⏳ Requires JNI bindings for actual LLM inference (see [GenieService.kt](frontend/app/src/main/java/com/example/snap_app/GenieService.kt#L105) TODO)

## Next Steps

### For Production
1. Implement download-on-first-launch mechanism
2. Use app expansion files (.obb) for Play Store distribution
3. Implement JNI layer for actual model inference
4. Add model caching to prevent re-downloads

### For Development
Keep using adb push method. To push updated models:
```powershell
adb shell rm /data/data/com.example.snap_app/files/models/llm/*
# Then re-run push commands above
```

## Troubleshooting

| Error | Solution |
|-------|----------|
| `error: device offline` | Enable USB debugging, reconnect USB |
| `permission denied` | App needs READ_EXTERNAL_STORAGE (already in AndroidManifest) |
| `no space left on device` | Phone storage full, free up space |
| `model not found` | File didn't push completely, retry |
| `app crashes on chat` | QAIRT SDK not installed on device (see [INTEGRATION_SETUP.md](INTEGRATION_SETUP.md#step-1-install-qairt-sdk-on-device)) |

## Quick Reference: File Locations

| Component | Location on PC | Location on Phone |
|-----------|----------------|-------------------|
| Model data | `C:\Users\karti\.qaihm\...\model.data` | `/data/data/com.example.snap_app/files/models/llm/model.data` |
| Tokenizer | `C:\Users\karti\.qaihm\...\tokenizer.json` | `/data/data/com.example.snap_app/files/models/llm/tokenizer.json` |
| Config | `C:\Users\karti\.qaihm\...\config.json` | `/data/data/com.example.snap_app/files/models/llm/config.json` |
| App internal | N/A | `/data/data/com.example.snap_app/` |

## USB/ADB Issues?

If adb is not found, ensure Android SDK Platform Tools is installed:
```powershell
# Check if adb is available
where adb

# If not, download from:
# https://developer.android.com/tools/releases/platform-tools
```

Then add to PATH:
```powershell
$env:PATH += ";C:\path\to\android-sdk-windows\platform-tools"
```

---

**Total setup time**: ~10 minutes (mostly file transfer)  
**Once complete**: App will load model automatically on launch ✅
