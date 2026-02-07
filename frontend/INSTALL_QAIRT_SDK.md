# Installing QAIRT SDK on Android Device

## Problem
Your Snapdragon device doesn't have `genie-t2t-run` or QNN libraries installed.

## Solution: Install QAIRT SDK

### Step 1: Download QAIRT SDK

1. Go to: https://softwarecenter.qualcomm.com/catalog/item/Qualcomm_AI_Runtime_Community
2. Create account / Sign in
3. Download: **QAIRT v2.29.0+** for Android (ARM64)
   - File: `qairt-<version>-android.zip` (~500MB)

### Step 2: Extract SDK

```powershell
cd C:\Users\karti\Downloads
# Extract the downloaded ZIP file
Expand-Archive -Path "qairt-*.zip" -DestinationPath "qairt_sdk"
```

### Step 3: Push SDK to Device

```powershell
cd qairt_sdk\qairt

# Push SDK to device (takes 5-10 minutes)
adb push . /data/local/tmp/qairt/

# Verify installation
adb shell "ls -lh /data/local/tmp/qairt/bin/aarch64-android/ | grep genie"
```

Expected output:
```
-rwxr-xr-x 1 shell shell 2.5M ... genie-t2t-run
```

### Step 4: Set Permissions

```powershell
adb shell "chmod -R 755 /data/local/tmp/qairt/bin/"
adb shell "chmod -R 755 /data/local/tmp/qairt/lib/"
```

### Step 5: Test Genie

```powershell
adb shell
cd /data/local/tmp/snap_models
export PATH=/data/local/tmp/qairt/bin/aarch64-android:$PATH
export LD_LIBRARY_PATH=/data/local/tmp/qairt/lib/aarch64-android
export ADSP_LIBRARY_PATH=/data/local/tmp/qairt/lib/hexagon-v73/unsigned

genie-t2t-run -c genie_config.json -p "<|begin_of_text|><|start_header_id|>user<|end_header_id|>

What is protein?<|eot_id|><|start_header_id|>assistant<|end_header_id|>"
```

If this works, your app will work!

---

## Alternative: Use Intelligent Fallback (Works Now!)

I can update the app to use smart rule-based responses while you install QAIRT SDK:

- ✅ Works immediately, no SDK needed
- ✅ Provides helpful fitness/nutrition advice
- ✅ Fast responses (~instant)
- ❌ Not using actual LLM (yet)

---

## Which option do you prefer?

**Type A** - I'll help you install QAIRT SDK (30 min setup, full LLM)
**Type B** - Use fallback responses for now (works immediately, upgrade later)
