# 🚀 Quick Start - On-Device Chat

## Build for Phone - 3 Simple Steps

### 1️⃣ Push Model to Device
```powershell
cd C:\Users\karti\StudioProjects\frontend
Set-ExecutionPolicy Bypass -Scope Process -Force
.\push_llama_model.ps1
```
⏱️ Takes: ~5 minutes (1.2GB transfer)

---

### 2️⃣ Build & Install App
```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
⏱️ Takes: ~1 minute

---

### 3️⃣ Test on Phone
1. Open app on device
2. Navigate to **Chat** tab
3. Type: "What should I eat for protein?"
4. Get AI response! 🎉

---

## ✅ What's Implemented

- ✅ **Llama 3.2 1B** running on-device
- ✅ **NPU acceleration** (Snapdragon Hexagon)
- ✅ **Chat UI** with message history
- ✅ **No backend** - completely offline
- ✅ **Privacy-first** - data never leaves device

---

## 🔧 Key Files Modified

| File | Purpose |
|------|---------|
| [GenieService.kt](app/src/main/java/com/example/snap_app/GenieService.kt) | Calls genie-t2t-run for inference |
| [ChatScreen.kt](app/src/main/java/com/example/snap_app/ChatScreen.kt) | Chat UI (already existed) |
| [push_llama_model.ps1](push_llama_model.ps1) | Automated model deployment |

---

## 📊 Expected Performance

| Metric | Value |
|--------|-------|
| Model load | 1-2 sec |
| First token | 0.5-1 sec |
| Tokens/sec | 15-20 |
| Works offline | ✅ Yes |

---

## 🆘 Troubleshooting

**Model not found?**
```powershell
adb shell "ls -lh /data/local/tmp/snap_models/"
```
Should show: 8 files, ~1.2GB total

**Build fails?**
```powershell
.\gradlew.bat clean build -x test
```

**Check logs:**
```powershell
adb logcat | Select-String "GenieService"
```

---

## 📖 Full Documentation

See [ON_DEVICE_CHAT_SETUP.md](ON_DEVICE_CHAT_SETUP.md) for:
- Detailed architecture
- Performance tuning
- JNI integration (advanced)
- API references
